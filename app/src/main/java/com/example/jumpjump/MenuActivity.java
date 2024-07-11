package com.example.jumpjump;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MenuActivity extends AppCompatActivity {

    private MediaPlayer mediaPlayer;
    private Button button;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        button = findViewById(R.id.iniciar);

        mediaPlayer = MediaPlayer.create(this,R.raw.menutheme);
        mediaPlayer.setLooping(true);
        mediaPlayer.start();

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MenuActivity.this, MainActivity.class);
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onPause(){
        super.onPause();
        mediaPlayer.pause();
    }


    @Override
    protected void onResume(){
        super.onResume();
        mediaPlayer.start();
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        mediaPlayer.release();
    }

}