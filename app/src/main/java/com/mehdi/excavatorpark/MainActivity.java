package com.mehdi.excavatorpark;

import android.app.Activity;
import android.os.Bundle;

public class MainActivity extends Activity {

    private Game3DView gameView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        gameView = new Game3DView(this);
        setContentView(gameView);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (gameView != null) {
            gameView.onResume();
        }
    }

    @Override
    protected void onPause() {
        if (gameView != null) {
            gameView.onPause();
        }
        super.onPause();
    }
}
