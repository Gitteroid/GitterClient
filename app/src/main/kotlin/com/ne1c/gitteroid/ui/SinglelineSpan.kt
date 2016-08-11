package com.ne1c.gitteroid.ui

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.text.style.LineBackgroundSpan

class SinglelineSpan(private val mStart: Int, private val mEnd: Int) : LineBackgroundSpan {

    /**
     * @param start - First symbol of text
     * *
     * @param end   - Last symbol of text
     */
    override fun drawBackground(canvas: Canvas, paint: Paint, left: Int, right: Int, top: Int, baseline: Int, bottom: Int, text: CharSequence, start: Int, end: Int, lnum: Int) {
        var x = 0f

        if (text.subSequence(start, end).toString() == " ") {
            return
        }

        // If mStart not starting from 0 position, then calculate length spanning text
        if (mStart > 0 && mStart > start) {
            x = paint.measureText(text.subSequence(start, mStart).toString()) // From start text to start span text
        }

        val lengthText: Float

        // IT'S MAGIC!!!
        if (start >= mStart && end > mEnd) {
            lengthText = measureText(paint, text, start, mEnd)
        } else if (mStart > start && end < mEnd) {
            lengthText = measureText(paint, text, mStart, end)
        } else if (start == mStart && end < mEnd) {
            lengthText = measureText(paint, text, start, end)
        } else if (mStart > start && end >= mEnd) {
            lengthText = measureText(paint, text, mStart, mEnd)
        } else {
            lengthText = measureText(paint, text, start, end)
        }

        val rect = RectF(x, top.toFloat(), x + lengthText, bottom.toFloat())

        paint.style = Paint.Style.FILL
        paint.color = Color.rgb(235, 235, 235)
        canvas.drawRoundRect(rect, 4f, 4f, paint)

        paint.style = Paint.Style.STROKE
        paint.color = Color.rgb(185, 185, 185)
        canvas.drawRoundRect(rect, 4f, 4f, paint)

        paint.color = Color.BLACK
    }

    private fun measureText(paint: Paint, text: CharSequence, start: Int, end: Int): Float {
        return paint.measureText(text, start, end)
    }
}
