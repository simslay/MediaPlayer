package com.example.mediaplayer;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private RecyclerView rvSongs;
    private ImageView btnPrevious, btnPlay, btnNext;
    private TextView tvSongTitle, tvCurrentPos, tvTotalDuration;
    private SeekBar sbPosition;
    private MediaPlayer mediaPlayer;
    private ArrayList<ModelSong> songArrayList;
    private AdapterSong adapterSong;
    private LinearLayoutManager linearLayoutManager;
    private double currentPosition, totalDuration;
    private int songIndex = 0;

    private void init() {
        rvSongs = findViewById(R.id.rv_songs);
        btnPrevious = findViewById(R.id.iv_btn_previous);
        btnPlay = findViewById(R.id.iv_btn_play);
        btnNext = findViewById(R.id.iv_btn_next);
        tvSongTitle = findViewById(R.id.tv_song_title);
        tvCurrentPos = findViewById(R.id.tv_current_pos);
        tvTotalDuration = findViewById(R.id.tv_total_duration);
        sbPosition = findViewById(R.id.sb_position);

        mediaPlayer = new MediaPlayer();
        songArrayList = new ArrayList<>();

        linearLayoutManager = new LinearLayoutManager(
                this,
                LinearLayoutManager.VERTICAL,
                false
        );
    }

    public void checkPermission() {
        if (ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(
                    MainActivity.this, new String[] {
                            Manifest.permission.READ_MEDIA_AUDIO
                    },
                    100
            );
        } else {
//            Toast.makeText(this, "Permission already granted", Toast.LENGTH_SHORT).show();
            setSong();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 100) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Read music folder is granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Read music folder denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /** get audio files from the ternminal */
    private void getAudioFiles() {
        ContentResolver contentResolver = getContentResolver();

        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;

        String[] projections = {
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.ALBUM_ID
        };

        String selection = MediaStore.Audio.Media.IS_MUSIC + "!=0";

        Cursor cursor = contentResolver.query(uri, projections, selection, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            do {
                String title = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE));
                String artist = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST));
                String album = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM));
                String duration = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION));
                String data = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA));
                long album_id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID));

                Uri uriCoverFolder = Uri.parse("content://media/external/audio/albumart");
                Uri uriAlbumArt = ContentUris.withAppendedId(uriCoverFolder, album_id);

                ModelSong modelSong = new ModelSong();
                modelSong.setSongTitle(title);
                modelSong.setSongArtist(artist);
                modelSong.setSongAlbum(album);
                modelSong.setSongDuration(duration);
                modelSong.setSongUri(Uri.parse(data));
                modelSong.setSongCover(uriAlbumArt);

                songArrayList.add(modelSong);
            } while (cursor.moveToNext());
        }
    }

    private void managerRv() {
        adapterSong = new AdapterSong(MainActivity.this, songArrayList);
        rvSongs.setLayoutManager(linearLayoutManager);
        rvSongs.setAdapter(adapterSong);

        adapterSong.setMyOnItemClickListener(new AdapterSong.MyOnItemClickListener() {
            @Override
            public void onItemClick(int position, View view) {
                playSong(position);
            }
        });
    }

    private void playSong(int position) {
        try {
            mediaPlayer.reset();
            mediaPlayer.setDataSource(this, songArrayList.get(position).getSongUri());
            mediaPlayer.prepare();
            mediaPlayer.start();

            btnPlay.setImageResource(R.drawable.ic_pause_48_w);
            tvSongTitle.setText(songArrayList.get(position).getSongTitle());
            songIndex = position;
        } catch (Exception e) {
            e.printStackTrace();
        }

        songProgess();
    }

    private void songProgess() {
        currentPosition = mediaPlayer.getCurrentPosition();
        totalDuration = mediaPlayer.getDuration();

        tvCurrentPos.setText(timerConversion((long) currentPosition));
        tvTotalDuration.setText(timerConversion((long) totalDuration));

        sbPosition.setMax((int) totalDuration);

        final Handler handler = new Handler();

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    currentPosition = mediaPlayer.getCurrentPosition();
                    tvCurrentPos.setText(timerConversion((long) currentPosition));
                    sbPosition.setProgress((int) currentPosition);
                    handler.postDelayed(this, 1000);
                } catch (IllegalThreadStateException itse) {
                    itse.printStackTrace();
                }
            }
        };

        handler.postDelayed(runnable, 1000);
    }

    private String timerConversion(long value) {
        String songDuration;
        int dur = (int) value;
        int hrs = dur / 3_600_000;
        int mns = dur / 60_000;
        int scs = (dur / 1000) % 60;

        if (hrs > 0) {
            songDuration = String.format("%02d:%02d:%02d", hrs, mns, scs);
        } else {
            songDuration = String.format("%02d:%02d", mns, scs);
        }

        return songDuration;
    }

    private void pauseSong() {
        btnPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.pause();
                    btnPlay.setImageResource(R.drawable.ic_play_48_w);
                } else {
                    mediaPlayer.start();
                    btnPlay.setImageResource(R.drawable.ic_pause_48_w);
                }
            }
        });
    }

    private void previousSong() {
        btnPrevious.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (songIndex > 0) {
                    songIndex--;
                } else {
                    songIndex = songArrayList.size() - 1;
                }

                playSong(songIndex);
            }
        });
    }

    private void nextSong() {
        btnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (songIndex < songArrayList.size() - 1) {
                    songIndex++;
                } else {
                    songIndex = 0;
                }

                playSong(songIndex);
            }
        });
    }

    private void setSong() {
        getAudioFiles();
        managerRv();

        sbPosition.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                currentPosition = seekBar.getProgress();
                mediaPlayer.seekTo((int) currentPosition);
                mediaPlayer.start();
            }
        });

        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                songIndex++;

                if (songIndex >= songArrayList.size()) {
                    songIndex = 0;
                }

                playSong(songIndex);
            }
        });

        if (!songArrayList.isEmpty()) {
            playSong(songIndex);
            pauseSong();
            previousSong();
            nextSong();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        init();

        checkPermission();

//        getAudioFiles();

//        managerRv();
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (mediaPlayer != null) {
            mediaPlayer.stop();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mediaPlayer != null) {
            mediaPlayer.release();
        }
    }
}