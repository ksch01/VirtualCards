package com.example.virtualcards;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.view.View;

import com.example.virtualcards.view.GameGLSurfaceView;

public class MainActivity extends AppCompatActivity {

    private GLSurfaceView glSurfaceView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        glSurfaceView = new GameGLSurfaceView(this);
        setContentView(glSurfaceView);
    }

    @Override
    protected void onPause(){
        super.onPause();
        glSurfaceView.onPause();
    }

    @Override
    protected void onResume(){
        super.onResume();
        glSurfaceView.onResume();

        hideSystemUI();
    }

    private void hideSystemUI(){
        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null)actionBar.hide();

        View decorView = getWindow().getDecorView();
        int uiOptions =
                View.SYSTEM_UI_FLAG_FULLSCREEN|
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY|
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
        decorView.setSystemUiVisibility(uiOptions);
    }
}