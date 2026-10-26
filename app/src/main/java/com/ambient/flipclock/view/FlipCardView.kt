package com.ambient.flipclock.view

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator

class FlipCardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var currentValue: String = "11"
        private set
    private var previousValue: String = "11"

    var amPmText: String = "" // "AM", "PM", or ""
    var animationDuration: Long = 400L

    private var flipProgress: Float = 1.0f
    private var isFlipping: Boolean = false
    private var currentAnimator: ValueAnimator? = null

    private val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#202020")
        style = Paint.Style.FILL
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#D0D0D0")
        typeface = Typeface.create("sans-serif-condensed", Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }

    private val amPmPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#808080")
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        textAlign = Paint.Align.LEFT
    }

    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        style = Paint.Style.FILL
    }

    private val seamPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        style = Paint.Style.FILL
    }

    private val camera = Camera()
    private val matrix3d = Matrix()

    private val cardBounds = RectF()
    private val topClipRect = RectF()
    private val bottomClipRect = RectF()

    private val slitHeight = 3.5f
    private val cornerRadius = 32f
    private val notchRadius = 8f

    fun setValue(newValue: String, animate: Boolean = true) {
        if (newValue == currentValue && !isFlipping) return

        currentAnimator?.cancel()

        if (!animate) {
            previousValue = newValue
            currentValue = newValue
            flipProgress = 1.0f
            isFlipping = false
            invalidate()
            return
        }

        previousValue = currentValue
        currentValue = newValue
        isFlipping = true

        currentAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = animationDuration
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { anim ->
                flipProgress = anim.animatedValue as Float
                invalidate()
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    isFlipping = false
                    previousValue = currentValue
                    flipProgress = 1.0f
                    invalidate()
                }
            })
            start()
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        cardBounds.set(0f, 0f, w.toFloat(), h.toFloat())

        val midY = h / 2f
        topClipRect.set(0f, 0f, w.toFloat(), midY - slitHeight / 2f)
        bottomClipRect.set(0f, midY + slitHeight / 2f, w.toFloat(), h.toFloat())

        // Scaled text size to fit card comfortably (approx 68% of card height)
        textPaint.textSize = h * 0.70f
        amPmPaint.textSize = h * 0.08f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()
        val midY = h / 2f

        if (w <= 0 || h <= 0) return

        if (!isFlipping || flipProgress >= 1f) {
            // Static display: top and bottom halves show currentValue
            drawCardBackground(canvas)
            drawSlitAndNotches(canvas)

            // Draw Top Half
            canvas.save()
            canvas.clipRect(topClipRect)
            drawTextAndDetails(canvas, currentValue)
            canvas.restore()

            // Draw Bottom Half
            canvas.save()
            canvas.clipRect(bottomClipRect)
            drawTextAndDetails(canvas, currentValue)
            canvas.restore()
            return
        }

        // --- ANIMATED 3D FLIP ---
        drawCardBackground(canvas)
        drawSlitAndNotches(canvas)

        val angle = flipProgress * 180f

        // 1. Static Top: shows the NEW value (currentValue)
        canvas.save()
        canvas.clipRect(topClipRect)
        drawTextAndDetails(canvas, currentValue)
        canvas.restore()

        // 2. Static Bottom: shows the OLD value (previousValue)
        canvas.save()
        canvas.clipRect(bottomClipRect)
        drawTextAndDetails(canvas, previousValue)
        canvas.restore()

        // 3. Flipping Flap
        if (angle < 90f) {
            // First phase (0 -> 90): top flap of OLD value folding down
            canvas.save()
            canvas.clipRect(topClipRect)

            matrix3d.reset()
            camera.save()
            camera.rotateX(-angle)
            camera.getMatrix(matrix3d)
            camera.restore()

            // Pivot at midY
            matrix3d.preTranslate(-w / 2f, -midY)
            matrix3d.postTranslate(w / 2f, midY)

            canvas.concat(matrix3d)

            // Draw card surface & old top text
            canvas.drawRoundRect(cardBounds, cornerRadius, cornerRadius, cardPaint)
            drawTextAndDetails(canvas, previousValue)

            // Darkening shadow as it tilts away
            val alpha = (Math.sin(Math.toRadians(angle.toDouble())) * 140).toInt().coerceIn(0, 255)
            shadowPaint.alpha = alpha
            canvas.drawRect(topClipRect, shadowPaint)

            canvas.restore()
        } else {
            // Second phase (90 -> 180): bottom flap of NEW value folding down
            canvas.save()
            canvas.clipRect(bottomClipRect)

            matrix3d.reset()
            camera.save()
            camera.rotateX(180f - angle)
            camera.getMatrix(matrix3d)
            camera.restore()

            // Pivot at midY
            matrix3d.preTranslate(-w / 2f, -midY)
            matrix3d.postTranslate(w / 2f, midY)

            canvas.concat(matrix3d)

            // Draw card surface & new bottom text
            canvas.drawRoundRect(cardBounds, cornerRadius, cornerRadius, cardPaint)
            drawTextAndDetails(canvas, currentValue)

            // Lightening shadow as it lands flat
            val alpha = (Math.sin(Math.toRadians(angle.toDouble())) * 140).toInt().coerceIn(0, 255)
            shadowPaint.alpha = alpha
            canvas.drawRect(bottomClipRect, shadowPaint)

            canvas.restore()
        }
    }

    private fun drawCardBackground(canvas: Canvas) {
        canvas.drawRoundRect(cardBounds, cornerRadius, cornerRadius, cardPaint)
    }

    private fun drawSlitAndNotches(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val midY = h / 2f

        // Horizontal black slit dividing flaps
        canvas.drawRect(0f, midY - slitHeight / 2f, w, midY + slitHeight / 2f, seamPaint)

        // Left mechanical hinge notch
        canvas.drawCircle(0f, midY, notchRadius, seamPaint)

        // Right mechanical hinge notch
        canvas.drawCircle(w, midY, notchRadius, seamPaint)
    }

    private fun drawTextAndDetails(canvas: Canvas, text: String) {
        val w = width.toFloat()
        val h = height.toFloat()

        // Center vertically based on font metrics
        val fontMetrics = textPaint.fontMetrics
        val textBaselineY = (h - fontMetrics.bottom - fontMetrics.top) / 2f

        canvas.drawText(text, w / 2f, textBaselineY, textPaint)

        // Optional AM/PM badge
        if (amPmText.isNotEmpty()) {
            canvas.drawText(amPmText, w * 0.08f, h * 0.16f, amPmPaint)
        }
    }
}
