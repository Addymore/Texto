/*
 * Copyright (C) 2017 Moez Bhatti <moez.bhatti@gmail.com>
 *
 * This file is part of QKSMS.
 *
 * QKSMS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * QKSMS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with QKSMS.  If not, see <http://www.gnu.org/licenses/>.
 */
package dev.octoshrimpy.quik.common.widget

import android.content.Context
import android.util.AttributeSet
import kotlin.math.ceil

class TightTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : QkTextView(context, attrs) {

    var selectionHighlight = android.graphics.Color.TRANSPARENT
    private val selectionPath = android.graphics.Path()
    private val selectionPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
    override fun onDraw(canvas: android.graphics.Canvas) {
        val textLayout=layout
        val start=minOf(selectionStart,selectionEnd)
        val end=maxOf(selectionStart,selectionEnd)
        if(isTextSelectable && textLayout != null && start>=0 && end>start) {
            selectionPaint.color=selectionHighlight
            val save=canvas.save()
            canvas.translate(totalPaddingLeft.toFloat()-scrollX,totalPaddingTop.toFloat()-scrollY)
            val radius=4*resources.displayMetrics.density
            // Layout's selection path handles bidi text and multi-line paragraphs correctly.
            selectionPath.reset()
            textLayout.getSelectionPath(start,end,selectionPath)
            selectionPaint.pathEffect=android.graphics.CornerPathEffect(radius)
            canvas.drawPath(selectionPath,selectionPaint)
            canvas.restoreToCount(save)
            super.onDraw(canvas)
        } else super.onDraw(canvas)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        // Get a non-null copy of the layout, if available. Then ensure we've got multiple lines
        val layout = layout ?: return
        if (layout.lineCount <= 1) {
            return
        }

        val maxLineWidth = (0 until layout.lineCount).maxOfOrNull(layout::getLineWidth) ?: 0f

        val width = ceil(maxLineWidth.toDouble()).toInt() + compoundPaddingLeft + compoundPaddingRight
        if (width < measuredWidth) {
            val widthSpec = MeasureSpec.makeMeasureSpec(width, MeasureSpec.getMode(widthMeasureSpec))
            super.onMeasure(widthSpec, heightMeasureSpec)
        }
    }

}