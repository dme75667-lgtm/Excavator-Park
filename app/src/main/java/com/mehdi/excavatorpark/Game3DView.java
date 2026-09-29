package com.mehdi.excavatorpark;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.View;

public class Game3DView extends View {

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private float excavatorX;
    private float excavatorY;

    private boolean up;
    private boolean down;
    private boolean left;
    private boolean right;

    private final float speed = 8f;

    public Game3DView(Context context) {
        super(context);
        paint.setTextAlign(Paint.Align.CENTER);
        setFocusable(true);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int w = getWidth();
        int h = getHeight();

        canvas.drawColor(Color.rgb(135, 200, 235));

        paint.setColor(Color.rgb(75, 55, 35));
        canvas.drawRect(0, h / 2f, w, h, paint);

        paint.setColor(Color.rgb(120, 85, 50));
        canvas.drawRect(0, h * 0.60f, w, h * 0.82f, paint);

        if (up) excavatorY -= speed;
        if (down) excavatorY += speed;
        if (left) excavatorX -= speed;
        if (right) excavatorX += speed;

        float x = w / 2f + excavatorX;
        float y = h * 0.60f + excavatorY;

        x = Math.max(130, Math.min(w - 130, x));
        y = Math.max(h * 0.45f, Math.min(h - 100, y));

        excavatorX = x - w / 2f;
        excavatorY = y - h * 0.60f;

        // جسم الحفارة
        paint.setColor(Color.rgb(245, 170, 20));
        canvas.drawRect(x - 95, y - 35, x + 70, y + 30, paint);

        // الكابينة
        paint.setColor(Color.rgb(45, 55, 60));
        canvas.drawRect(x - 45, y - 95, x + 50, y - 30, paint);

        // زجاج
        paint.setColor(Color.rgb(100, 180, 210));
        canvas.drawRect(x - 35, y - 85, x + 40, y - 40, paint);

        // الذراع
        paint.setColor(Color.rgb(230, 155, 15));
        paint.setStrokeWidth(24);
        canvas.drawLine(x + 45, y - 50, x + 125, y - 105, paint);
        canvas.drawLine(x + 125, y - 105, x + 175, y - 35, paint);

        // الدلو
        paint.setColor(Color.rgb(180, 115, 10));
        canvas.drawRect(x + 155, y - 20, x + 215, y + 20, paint);

        // العجلات
        paint.setColor(Color.DKGRAY);
        canvas.drawCircle(x - 60, y + 35, 27, paint);
        canvas.drawCircle(x + 40, y + 35, 27, paint);

        // العنوان
        paint.setColor(Color.WHITE);
        paint.setTextSize(30);
        canvas.drawText("EXCAVATOR PARK", w / 2f, 45, paint);

        paint.setTextSize(20);
        canvas.drawText(
                "Mission 1 - Drive the Excavator",
                w / 2f,
                80,
                paint
        );

        // أزرار التحكم
        drawButton(canvas, 100, h - 100, "←");
        drawButton(canvas, 260, h - 100, "→");
        drawButton(canvas, w - 180, h - 160, "↑");
        drawButton(canvas, w - 180, h - 70, "↓");
    }

    private void drawButton(
            Canvas canvas,
            float x,
            float y,
            String text
    ) {
        paint.setColor(Color.argb(210, 30, 30, 30));
        canvas.drawCircle(x, y, 70, paint);

        paint.setColor(Color.WHITE);
        paint.setTextSize(45);
        canvas.drawText(text, x, y + 15, paint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        float x = event.getX();
        float y = event.getY();

        int action = event.getAction();

        if (action == MotionEvent.ACTION_DOWN ||
                action == MotionEvent.ACTION_MOVE) {

            up = false;
            down = false;
            left = false;
            right = false;

            int h = getHeight();
            int w = getWidth();

            if (distance(x, y, 100, h - 100) < 75) {
                left = true;
            }

            if (distance(x, y, 260, h - 100) < 75) {
                right = true;
            }

            if (distance(x, y, w - 180, h - 160) < 75) {
                up = true;
            }

            if (distance(x, y, w - 180, h - 70) < 75) {
                down = true;
            }

            invalidate();
            return true;
        }

        if (action == MotionEvent.ACTION_UP ||
                action == MotionEvent.ACTION_CANCEL) {

            up = false;
            down = false;
            left = false;
            right = false;

            invalidate();
            return true;
        }

        return true;
    }

    private float distance(
            float x1,
            float y1,
            float x2,
            float y2
    ) {
        float dx = x1 - x2;
        float dy = y1 - y2;

        return (float) Math.sqrt(dx * dx + dy * dy);
    }
            }
