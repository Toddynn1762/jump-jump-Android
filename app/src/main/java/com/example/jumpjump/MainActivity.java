package com.example.jumpjump;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.media.MediaPlayer;
import android.util.Log;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.Random;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    public TextView scoreTextView;
    private MediaPlayer mediaPlayer;
    public int score = 0;
    private final float SCORE_SCALE_FACTOR = 10;
    private SensorManager sensorManager;
    private final int numeroDePlataformasIniciais = 5;
    private View character;
    private float characterX;
    private float velocityY = 0; // velocidade vertical
    private final float gravity = 1.0f; //força da gravidade
    private final float jump = -25; //poder de pulo (negativo para subir)
    private View staticPlataform, movingPlataform;
    private final int maximoDePlataformasNaTela = 10;
    private int alturaMinimaInicial = 100; // Altura mínima para a primeira plataforma
    private int alturaMaximaInicial =100; // Altura máxima para a primeira plataforma
    private final int alturaParaNovaPlataforma = 300;
    private int alturaUltimaPlataformaGerada; // Armazenar a altura da última plataforma gerada
    private float HEIGHT_THRESHOLD;
    // Constantes para o jogo
    private final int PLATFORM_GAP = 200; // Ajuste esse valor conforme necessário para o seu jogo


    private Handler handler = new Handler();
    private Runnable gameLoop = new Runnable() {
        @Override
        public void run() {
            updatePhysics();

            // Condição para gerar novas plataformas
            if (deveGerarNovaPlataforma()) {
                int newPlatformHeight = calculatePlatformHeight();
                generatePlataform(newPlatformHeight);
            }

            removeOffScreamPlataforms();
            handler.postDelayed(this, 17); // Atualiza a cada 20ms
        }
    };

    private void preloadPlatforms() {
        int preloadCount = 5; // Defina o número de plataformas para pré-carregar
        int startYPosition = -100; // Posição Y inicial para as plataformas pré-carregadas (fora da tela)

        for (int i = 0; i < preloadCount; i++) {
            generatePlataform(startYPosition);
            startYPosition -= PLATFORM_GAP; // Aumenta a distância vertical entre as plataformas pré-carregadas
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mediaPlayer = MediaPlayer.create(this, R.raw.musicagame);
        mediaPlayer.setLooping(true);
        mediaPlayer.start();

        scoreTextView = findViewById(R.id.scoreTextView);

        staticPlataform = findViewById(R.id.staticPlataform);
        movingPlataform = findViewById(R.id.movingPlataform);
        character = findViewById(R.id.character);
        characterX = character.getX();
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        // Definição da HEIGHT_THRESHOLD com base em uma fração da altura da tela
        HEIGHT_THRESHOLD = getWindowManager().getDefaultDisplay().getHeight() / 4;

        generateInitialPlatforms();
    }


    @Override
    protected void onResume() {
        super.onResume();
        Sensor accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);

        handler.post(gameLoop);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mediaPlayer != null){
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }

        sensorManager.unregisterListener(this);

        handler.removeCallbacks(gameLoop);
    }

    public void onSensorChanged(SensorEvent event){
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER){
            float x = event.values[0];

            updateCharacterPosition(x);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Não usar por enquanto
    }

    private int calculatePlatformHeight() {
        //calcular a altura da próxima plataforma com base na posição do personagem
        int screenHeight = getWindowManager().getDefaultDisplay().getHeight();
        int characterY = (int) character.getY();

        // Reduzindo a altura de geração para tornar as plataformas mais acessíveis
        return Math.max(screenHeight - characterY - 150, screenHeight / 4); // Reduzir para 150 e usar 1/4 da altura da tela
    }

    private void generateInitialPlatforms() {

        int screenHeight = getWindowManager().getDefaultDisplay().getHeight();
        int platformHeight = screenHeight / 2; // Inicia a geração das plataformas na metade da tela

        //gera as plataformas de cima para baixo
        for (int i = 0; i < numeroDePlataformasIniciais; i++) {
            generatePlataform(platformHeight);
            platformHeight -= PLATFORM_GAP;
        }

        // Gera plataformas adicionais abaixo, até a altura da plataforma de madeira
        int lowerPlatformHeight = screenHeight - alturaMinimaInicial;
        while(lowerPlatformHeight > platformHeight) {
            generatePlataform(lowerPlatformHeight);
            lowerPlatformHeight -= PLATFORM_GAP;
        }

        alturaUltimaPlataformaGerada = platformHeight + (PLATFORM_GAP * numeroDePlataformasIniciais);
    }


    private void updateCharacterPosition(float x) {
        characterX -= x * 10;

        characterX = Math.max(characterX, 0);
        characterX = Math.min(characterX, getWindowManager().getDefaultDisplay().getWidth() - character.getWidth());

        character.setX(characterX);

        float currentY = character.getY();
        float targetY = currentY + velocityY;
        float interpolatedY = currentY + (targetY - currentY) * 0.1f; //fator de interpolação
        character.setY(interpolatedY);

        if (x > 1.0) {
            // Inclina o dispositivo para a direita
            character.setBackgroundResource(R.drawable.personagemright);
        } else if (x < -1.0) {
            // Inclina o dispositivo para a esquerda
            character.setBackgroundResource(R.drawable.personagem);
        }
    }


    private void jump(){
        velocityY = jump;
    }

    private void updatePhysics(){

        if (isOnPlataform() && velocityY >= 0) { // Adiciona condicao para verificar a direção da velocidade
            velocityY = jump;
        }

        velocityY += gravity;
        float potentialNewY = character.getY() + velocityY;

        // se o personagem está subindo e atinge o HEIGHT_THRESHOLD, mova as plataformas para baixo
        if (potentialNewY < HEIGHT_THRESHOLD && velocityY < 0) {
            movePlatformsDown(-velocityY);
        } else {
            character.setY(potentialNewY);// Atualiza a posição Y do personagem se ele não atingir o HEIGHT_THRESHOLD
        }

        // mova as plataformas para baixo se o personagem estiver subindo e atingir o HEIGHT_THRESHOLD
        if (character.getY() < HEIGHT_THRESHOLD && velocityY < 0) {
            movePlatformsDown(-velocityY);
        } else {
            // atualiza a posição Y do personagem se não atingir o HEIGHT_THRESHOLD
            character.setY(character.getY() + velocityY);
        }

        // Sempre verifica se há necessidade de gerar novas plataformas e se o jogo terminou
        removeOffScreamPlataforms();
        generatePlatformsIfNeeded();
        checkGameOver();
    }

    //interpolarização linear
    private void movePlatformsDown(float deltaY) {
        RelativeLayout layout = findViewById(R.id.main_id);
        for (int i = 0; i < layout.getChildCount(); i++) {
            View child = layout.getChildAt(i);
            if ("GeneratedPlatform".equals(child.getTag())) {
                float currentY = child.getY();
                float targetY = currentY + deltaY;
                //suavizar a transição
                float interpolatedY = currentY + (targetY - currentY) * 1.0f; // Ajustar o fator de interpolação
                child.setY(interpolatedY);
            }
        }

        score += deltaY / SCORE_SCALE_FACTOR;
        scoreTextView.setText("Pontos: " + score);
    }

    private boolean isOnPlataform() {
        RelativeLayout layout = findViewById(R.id.main_id);
        for (int i = 0; i < layout.getChildCount(); i++) {
            View child = layout.getChildAt(i);

            // Verificar se a view é uma plataforma gerada
            if ("GeneratedPlatform".equals(child.getTag()) || child.equals(movingPlataform)) {
                if (checkCollisionWithPlataform(child)) {
                    return true;
                }
            }
        }
        return false;

    }

    private boolean checkCollisionWithPlataform(View plataform) {

        if (plataform == null) return false;

        // Aumenta a margem de colisão para compensar a alta velocidade de queda
        int collisionThreshold = velocityY > 5 ? 20 : 10;

        int[] platformPos = new int[2];
        plataform.getLocationOnScreen(platformPos);

        int[] characterPos = new int[2];
        character.getLocationOnScreen(characterPos);

        int characterBottom = characterPos[1] + character.getHeight();
        int platformTop = platformPos[1];

        boolean isWithinXBounds = characterPos[0] + character.getWidth() > platformPos[0] &&
                characterPos[0] < platformPos[0] + plataform.getWidth();
        boolean isTouchingPlatform = characterBottom >= platformTop - collisionThreshold &&
                characterBottom <= platformTop + collisionThreshold;

        // Permitir colisão apenas se estiver caindo (velocityY positivo)
        return isWithinXBounds && isTouchingPlatform && velocityY > 0;
    }

    private void generatePlatformsIfNeeded() {
        RelativeLayout layout = findViewById(R.id.main_id);

        // Gera novas plataformas se houver menos do que o número máximo permitido
        while (layout.getChildCount() < maximoDePlataformasNaTela) {
            // Posição Y da última plataforma ou um valor alto se não houver nenhuma plataforma
            int lastPlatformYPosition = layout.getChildCount() > 0 ?
                    (int) layout.getChildAt(layout.getChildCount() - 1).getY() :
                    Integer.MAX_VALUE;

            int newPlatformYPosition = lastPlatformYPosition - PLATFORM_GAP;

            generatePlataform(newPlatformYPosition);
        }
    }

    private void generatePlataform(int yPosition){

        View newPlatform = new View(this);
        newPlatform.setLayoutParams(new RelativeLayout.LayoutParams(160, 20));
        newPlatform.setBackgroundResource(R.drawable.plataformagrama);
        newPlatform.setTag("GeneratedPlatform");

        Random random = new Random();
        int x = random.nextInt(getWindowManager().getDefaultDisplay().getWidth() - 80); // Posição X aleatória

        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) newPlatform.getLayoutParams();
        params.leftMargin = x;
        params.topMargin = yPosition - PLATFORM_GAP; // Use PLATFORM_GAP para posicionar acima
        newPlatform.setLayoutParams(params);

        RelativeLayout layout = findViewById(R.id.main_id);
        layout.addView(newPlatform);
    }


    private void removeOffScreamPlataforms() {   //Metodo para remover as plataformas Antigas
        RelativeLayout layout = findViewById(R.id.main_id);
        for (int i = 0; i < layout.getChildCount(); ) {
            View child = layout.getChildAt(i);
            // Se a plataforma está abaixo da tela, será removida
            if (child.getY() > getWindowManager().getDefaultDisplay().getHeight()) {
                layout.removeView(child);
            } else {
                i++;
            }
        }
    }

    private boolean deveGerarNovaPlataforma() {
        RelativeLayout layout = findViewById(R.id.main_id);
        // Limitar o número de plataformas na tela
        if (layout.getChildCount() < maximoDePlataformasNaTela) {
            View ultimaPlataforma = layout.getChildAt(layout.getChildCount() - 1);
            int screenHeight = getWindowManager().getDefaultDisplay().getHeight();
            return ultimaPlataforma == null || ultimaPlataforma.getY() + ultimaPlataforma.getHeight() < screenHeight / 2;
        }
        return false;
    }

    private void checkGameOver() {
        if(character.getY() > getWindowManager().getDefaultDisplay().getHeight()) {
            if (mediaPlayer != null) {
                mediaPlayer.stop();
                mediaPlayer.release();
                mediaPlayer = null;
            }

            Intent intent = new Intent(MainActivity.this, GameOverActivity.class);
            intent.putExtra("SCORE", score);
            startActivity(intent);
            finish();
        }
    }
}