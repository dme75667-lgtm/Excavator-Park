package com.mehdi.excavatorpark;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.View;

public class Game3DView extends View {

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private float excavatorX = 0;
    private float excavatorY = 0;

    public Game3DView(Context context) {
        super(context);
        paint.setTextAlign(Paint.Align.CENTER);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int w = getWidth();
        int h = getHeight();

        // السماء
        canvas.drawColor(Color.rgb(135, 200, 235));

        // الأرض
        paint.setColor(Color.rgb(80, 55, 30));
        canvas.drawRect(0, h / 2f, w, h, paint);

        // طريق داخل البارك
        paint.setColor(Color.rgb(110, 80, 50));
        canvas.drawRect(0, h * 0.68f, w, h * 0.82f, paint);

        // الحفارة
        float x = w / 2f + excavatorX;
        float y = h * 0.60f + excavatorY;

        // جسم الحفارة
        paint.setColor(Color.rgb(245, 170, 20));
        canvas.drawRect(x - 100, y - 40, x + 70, y + 30, paint);

        // الكابينة
        paint.setColor(Color.rgb(40, 50, 55));
        canvas.drawRect(x - 45, y - 100, x + 50, y - 35, paint);

        // الذراع
        paint.setColor(Color.rgb(230, 155, 15));
        paint.setStrokeWidth(25);
        canvas.drawLine(x + 50, y - 55, x + 140, y - 120, paint);
        canvas.drawLine(x + 140, y - 120, x + 190, y - 45, paint);

        // الدلو
        paint.setColor(Color.rgb(190, 125, 10));
        canvas.drawRect(x + 165, y - 25, x + 225, y + 15, paint);

        // العجلات
        paint.setColor(Color.DKGRAY);
        canvas.drawCircle(x - 65, y + 35, 28, paint);
        canvas.drawCircle(x + 45, y + 35, 28, paint);

        // عنوان اللعبة
        paint.setColor(Color.WHITE);
        paint.setTextSize(32);
        canvas.drawText("EXCAVATOR PARK", w / 2f, 55, paint);

        // المهمة
        paint.setTextSize(22);
        canvas.drawText("Mission 1: Dig the park", w / 2f, 95, paint);

        // التعليمات
        paint.setTextSize(18);
        canvas.drawText("اسحب الشاشة لتحريك الحفارة", w / 2f, h - 35, paint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        if (event.getAction() == MotionEvent.ACTION_MOVE) {
            excavatorX = event.getX() - getWidth() / 2f;
            excavatorY = event.getY() - getHeight() * 0.60f;
            invalidate();
        }

        return true;
    }
}
