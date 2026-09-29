package com.mehdi.excavatorpark;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.TextView;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        TextView screen = new TextView(this);

        screen.setText(
                "🚜 EXCAVATOR PARK\n\n" +
                "مهمة 1\n" +
                "احفر منطقة البارك\n\n" +
                "المهمة طويلة ومتعددة المراحل\n\n" +
                "ابدأ الحفر!"
        );

        screen.setTextSize(28);
        screen.setTextColor(Color.WHITE);
        screen.setGravity(Gravity.CENTER);
        screen.setBackgroundColor(Color.rgb(45, 35, 25));

        setContentView(screen);
    }
}
