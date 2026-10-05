package com.example.ping_pong

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View
import kotlin.math.min

class GameView(context: Context, val game: GameEngine, private val onScoreChanged: () -> Unit) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val shape = RectF()
    private var scale = 1f
    private var offsetX = 0f
    private var offsetY = 0f
    private var lastFrame = 0L
    private var active = false

    private val frame = object : Runnable {
        override fun run() {
            if (!active) return
            val now = System.nanoTime()
            val seconds = if (lastFrame == 0L) 0f else (now - lastFrame) / 1_000_000_000f
            lastFrame = now
            val oldScore = game.playerScore + game.computerScore
            game.update(seconds)
            if (oldScore != game.playerScore + game.computerScore) onScoreChanged()
            invalidate()
            postOnAnimation(this)
        }
    }

    init {
        contentDescription = "Игровое поле. Ведите пальцем влево и вправо, чтобы двигать нижнюю ракетку."
        isClickable = true
    }

    fun startFrames() {
        if (active) return
        active = true
        lastFrame = 0L
        postOnAnimation(frame)
    }

    fun stopFrames() {
        active = false
        removeCallbacks(frame)
        lastFrame = 0L
    }

    override fun onDetachedFromWindow() {
        stopFrames()
        super.onDetachedFromWindow()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        scale = min(w / GameEngine.WIDTH, h / GameEngine.HEIGHT)
        offsetX = (w - GameEngine.WIDTH * scale) / 2
        offsetY = (h - GameEngine.HEIGHT * scale) / 2
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.save()
        canvas.translate(offsetX, offsetY)
        canvas.scale(scale, scale)
        rectangle(canvas, 0f, 0f, 360f, 600f, "#101E2B", 18f)
        rectangle(canvas, 0f, 0f, 360f, 25f, "#302536", 0f)
        rectangle(canvas, 0f, 575f, 360f, 600f, "#163333", 0f)
        label(canvas, "ЛИНИЯ ГОЛА", 180f, 17f, 9f, "#BF91A2")
        label(canvas, "ЛИНИЯ ГОЛА", 180f, 590f, 9f, "#72B7A6")
        paint.color = Color.parseColor("#2A3B4A")
        paint.strokeWidth = 2f
        for (x in 12..348 step 20) canvas.drawLine(x.toFloat(), 300f, x + 8f, 300f, paint)
        paint.style = Paint.Style.STROKE
        canvas.drawCircle(180f, 300f, 34f, paint)
        paint.style = Paint.Style.FILL
        val half = game.paddleWidth / 2
        rectangle(canvas, game.computerX - half, 48f, game.computerX + half, 60f, "#FF927F", 6f)
        rectangle(canvas, game.playerX - half, 540f, game.playerX + half, 552f, "#75F0CA", 6f)
        paint.color = Color.parseColor("#26FFFFFF")
        canvas.drawCircle(game.ballX, game.ballY, 13f, paint)
        paint.color = Color.WHITE
        canvas.drawCircle(game.ballX, game.ballY, GameEngine.RADIUS, paint)

        if (game.paused || game.finished) {
            rectangle(canvas, 0f, 0f, 360f, 600f, "#DC101E2B", 18f)
            val title = when {
                game.finished && game.playerScore >= GameEngine.WIN_SCORE -> "Вы победили!"
                game.finished -> "Победил компьютер"
                else -> "Пауза"
            }
            label(canvas, title, 180f, 288f, 25f, "#FFFFFF")
            label(canvas, if (game.finished) "Ещё один матч?" else "Нажмите «Продолжить»", 180f, 321f, 15f, "#A8BBCB")
        } else if (game.serveDelay > 0f) {
            label(canvas, "Приготовьтесь", 180f, 355f, 18f, "#FFFFFF")
        }
        canvas.restore()
    }

    private fun rectangle(canvas: Canvas, l: Float, t: Float, r: Float, b: Float, color: String, radius: Float) {
        paint.color = Color.parseColor(color)
        shape.set(l, t, r, b)
        canvas.drawRoundRect(shape, radius, radius, paint)
    }

    private fun label(canvas: Canvas, text: String, x: Float, y: Float, size: Float, color: String) {
        paint.color = Color.parseColor(color)
        paint.textSize = size
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText(text, x, y, paint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_DOWN || event.actionMasked == MotionEvent.ACTION_MOVE) {
            if (!game.paused && !game.finished && scale > 0f) game.movePlayer((event.x - offsetX) / scale)
            return true
        }
        if (event.actionMasked == MotionEvent.ACTION_UP) performClick()
        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }
}
