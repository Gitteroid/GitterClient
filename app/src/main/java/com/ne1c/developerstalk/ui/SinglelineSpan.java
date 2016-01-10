package com.ne1c.developerstalk.ui;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.text.style.ReplacementSpan;

public class SinglelineSpan extends ReplacementSpan {
    @Override
    public int getSize(Paint paint, CharSequence text, int start, int end, Paint.FontMetricsInt fm) {
        return Math.round(measureText(paint, text, start, end));
    }

    @Override
    public void draw(Canvas canvas, CharSequence text, int start, int end, float x, int top, int y, int bottom, Paint paint) {
        RectF rect = new RectF(x + 2, top + 2, x + measureText(paint, text, start, end) - 2, bottom - 2); // Padding for border
        RectF rectCorners = new RectF(x, top, x + measureText(paint, text, start, end), bottom);

        paint.setColor(Color.rgb(185, 185, 185));
        canvas.drawRoundRect(rectCorners, 5, 5, paint);

        paint.setColor(Color.rgb(235, 235, 235));
        canvas.drawRect(rect, paint);

        paint.setColor(Color.BLACK);
        canvas.drawText(text, start, end, x, y, paint);
    }

    private float measureText(Paint paint, CharSequence text, int start, int end) {
        return paint.measureText(text, start, end);
    }
}
