package com.example.ping_pong

import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sqrt
import kotlin.random.Random

// Размеры условные: GameView масштабирует поле под любой телефон.
enum class Difficulty(val title: String, val ballSpeed: Float, val computerSpeed: Float, val paddleWidth: Float) {
    EASY("Лёгкий", 260f, 145f, 94f),
    MEDIUM("Средний", 330f, 230f, 82f),
    HARD("Сложный", 410f, 325f, 70f)
}

class GameEngine(val difficulty: Difficulty) {
    companion object {
        const val WIDTH = 360f
        const val HEIGHT = 600f
        const val RADIUS = 7f
        const val PADDLE_HEIGHT = 12f
        const val COMPUTER_Y = 48f
        const val PLAYER_Y = 540f
        const val WIN_SCORE = 7
    }

    var playerX = WIDTH / 2
    var computerX = WIDTH / 2
    var ballX = WIDTH / 2
    var ballY = HEIGHT / 2
    var velocityX = 0f
    var velocityY = 0f
    var playerScore = 0
    var computerScore = 0
    var serveDelay = 1.2f
    var paused = false
    val finished: Boolean get() = playerScore >= WIN_SCORE || computerScore >= WIN_SCORE
    val paddleWidth: Float get() = difficulty.paddleWidth

    init { serve(1f) }

    fun movePlayer(x: Float) {
        // Игрок может менять только X: по вертикали ракетка закреплена.
        playerX = x.coerceIn(paddleWidth / 2, WIDTH - paddleWidth / 2)
    }

    private fun serve(direction: Float) {
        ballX = WIDTH / 2
        ballY = HEIGHT / 2
        val horizontalPart = Random.nextFloat() * 0.8f - 0.4f
        velocityX = difficulty.ballSpeed * horizontalPart
        velocityY = sqrt(difficulty.ballSpeed * difficulty.ballSpeed - velocityX * velocityX) * direction
        serveDelay = 1.2f
    }

    fun update(seconds: Float) {
        if (paused || finished) return
        if (serveDelay > 0f) {
            serveDelay = (serveDelay - seconds).coerceAtLeast(0f)
            return
        }
        // Короткие шаги не дают быстрому мячу пролететь сквозь ракетку.
        var remaining = seconds.coerceIn(0f, 0.05f)
        while (remaining > 0f && !finished && serveDelay == 0f) {
            val step = min(remaining, 1f / 240f)
            moveBall(step)
            remaining -= step
        }
    }

    private fun bounceFromPaddle(paddleX: Float, direction: Float) {
        // Базовое отражение дополняется ударом краем ракетки: игрок может целиться.
        val offset = ((ballX - paddleX) / (paddleWidth / 2)).coerceIn(-1f, 1f)
        val speed = difficulty.ballSpeed
        velocityX = (velocityX + offset * speed * 0.65f).coerceIn(-speed * 0.88f, speed * 0.88f)
        velocityY = sqrt(speed * speed - velocityX * velocityX) * direction
    }

    private fun moveBall(dt: Float) {
        val target = if (velocityY < 0) ballX else WIDTH / 2
        val distance = difficulty.computerSpeed * dt
        computerX += (target - computerX).coerceIn(-distance, distance)
        computerX = computerX.coerceIn(paddleWidth / 2, WIDTH - paddleWidth / 2)

        val oldY = ballY
        ballX += velocityX * dt
        ballY += velocityY * dt
        // Угол падения равен углу отражения: меняем знак одной скорости.
        if (ballX < RADIUS) {
            ballX = 2 * RADIUS - ballX
            velocityX = abs(velocityX)
        } else if (ballX > WIDTH - RADIUS) {
            ballX = 2 * (WIDTH - RADIUS) - ballX
            velocityX = -abs(velocityX)
        }

        val playerFace = PLAYER_Y - RADIUS
        val computerFace = COMPUTER_Y + PADDLE_HEIGHT + RADIUS
        if (velocityY > 0 && oldY <= playerFace && ballY >= playerFace &&
            abs(ballX - playerX) <= paddleWidth / 2 + RADIUS) {
            ballY = playerFace
            bounceFromPaddle(playerX, -1f)
        } else if (velocityY < 0 && oldY >= computerFace && ballY <= computerFace &&
            abs(ballX - computerX) <= paddleWidth / 2 + RADIUS) {
            ballY = computerFace
            bounceFromPaddle(computerX, 1f)
        }

        // Верх и низ — линии гола, а не отражающие стены.
        if (ballY < -RADIUS) {
            playerScore++
            serve(-1f)
        } else if (ballY > HEIGHT + RADIUS) {
            computerScore++
            serve(1f)
        }
    }
}
