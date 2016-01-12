package com.ne1c.developerstalk.ui;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.text.style.LineBackgroundSpan;

public class SinglelineSpan implements LineBackgroundSpan {
    private int mStart;
    private int mEnd;

    public SinglelineSpan(int start, int end) {
        mStart = start;
        mEnd = end;
    }

    /**
     * @param start - First symbol of text
     * @param end - Last symbol of text
     */
    @Override
    public void drawBackground(Canvas canvas, Paint paint, int left, int right, int top, int baseline, int bottom, CharSequence text, int start, int end, int lnum) {
        float x = 0;

        if (text.subSequence(start, end).toString().equals(" ")) {
            return;
        }

        // If mStart not starting from 0 position, then calculate length spanning text
        if (mStart > 0) {
            x = paint.measureText(text.subSequence(start, mStart).toString()); // From start text to start span text
        }

        RectF rect = new RectF(x, top, x + measureText(paint, text, mStart, mEnd), bottom);

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.rgb(235, 235, 235));
        canvas.drawRoundRect(rect, 4, 4, paint);

        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.rgb(185, 185, 185));
        canvas.drawRoundRect(rect, 4, 4, paint);

        paint.setColor(Color.BLACK);
    }

    private float measureText(Paint paint, CharSequence text, int start, int end) {
        return paint.measureText(text, start, end);
    }
}
