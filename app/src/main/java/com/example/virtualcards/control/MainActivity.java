package com.example.virtualcards.control;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;

import com.example.virtualcards.model.Card;
import com.example.virtualcards.model.Model;
import com.example.virtualcards.view.VirtualCardsView;

public class MainActivity extends AppCompatActivity {

    private VirtualCardsView virtualCardsView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Control.updateScreenModelRatio(this);
        Model model = Model.getModel();
        virtualCardsView = new VirtualCardsView(this, Control.getControl(model));
        model.subscribeView(virtualCardsView.getSubscriber());
        setContentView(virtualCardsView);

        float x = (Model.WIDTH) * 0.5f;
        float y = (Model.HEIGHT) * 0.5f;
        model.moveObject(model.getObject(x,  y), x, y);
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