package com.example.jumpjump;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

public class GameOverActivity extends AppCompatActivity {

    private TextView Pontuacao;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_over);

        Pontuacao = findViewById(R.id.mostraPontuacao);
        int score = getIntent().getIntExtra("SCORE", 0); // 0 é o valor padrão caso não haja pontuação
        Pontuacao.setText("Pontuação: " + score);

        findViewById(R.id.Jogar_novamente).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(GameOverActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        });

        findViewById(R.id.voltar_menu).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(GameOverActivity.this, MenuActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }
}