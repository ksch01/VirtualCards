package com.example.virtualcards;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;

import com.example.virtualcards.model.Model;
import com.example.virtualcards.view.VirtualCardsView;

public class MainActivity extends AppCompatActivity {

    private VirtualCardsView virtualCardsView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        virtualCardsView = new VirtualCardsView(this);
        Model model = Model.getModel();
        model.subscribeView(virtualCardsView.getSubscriber());
        setContentView(virtualCardsView);

        model.moveObject(model.getObject(320, 180), 320, 180);
    }

    @Override
    protected void onPause(){
        super.onPause();
        virtualCardsView.onPause();
    }

    @Override
    protected void onResume(){
        super.onResume();
        virtualCardsView.onResume();

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