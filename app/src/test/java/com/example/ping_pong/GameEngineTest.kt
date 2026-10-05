package com.example.ping_pong

import org.junit.Assert.*
import org.junit.Test

class GameEngineTest {
    private fun ready() = GameEngine(Difficulty.MEDIUM).apply { serveDelay = 0f }

    @Test fun sideWallReflectsWithoutChangingVerticalSpeed() {
        val game = ready().apply { ballX = 7.1f; velocityX = -100f; velocityY = 200f }
        game.update(0.01f)
        assertTrue(game.velocityX > 0)
        assertEquals(200f, game.velocityY, 0.001f)
        assertTrue(game.ballX >= GameEngine.RADIUS)
    }

    @Test fun paddleReflectsAnIncomingBall() {
        val game = ready().apply { ballY = 532f; ballX = playerX; velocityX = 50f; velocityY = 300f }
        game.update(0.016f)
        assertTrue(game.velocityY < 0)
        assertTrue(game.velocityX > 50f)
        assertEquals(0, game.computerScore)
    }

    @Test fun missedBallCannotBeRescuedFromBehindPaddle() {
        val game = ready().apply { ballY = 560f; ballX = playerX; velocityX = 0f; velocityY = 400f }
        repeat(5) { game.update(0.04f) }
        assertEquals(1, game.computerScore)
        assertEquals(GameEngine.HEIGHT / 2, game.ballY, 0f)
    }

    @Test fun topExitGivesPlayerExactlyOnePointAndCentersBall() {
        val game = ready().apply { ballY = -6f; velocityY = -300f }
        game.update(0.02f)
        repeat(10) { game.update(0.02f) }
        assertEquals(1, game.playerScore)
        assertEquals(180f, game.ballX, 0f)
        assertEquals(300f, game.ballY, 0f)
    }

    @Test fun edgeHitLetsPlayerAimAndKeepsSpeedConstant() {
        val game = ready().apply { ballX = playerX + 30f; ballY = 532f; velocityX = 0f; velocityY = 330f }
        game.update(0.01f)
        assertTrue(game.velocityX > 0f)
        assertTrue(game.velocityY < 0f)
        val speedSquared = game.velocityX * game.velocityX + game.velocityY * game.velocityY
        assertEquals(330f * 330f, speedSquared, 0.1f)
    }

    @Test fun playerCannotMoveOutsideField() {
        val game = ready()
        game.movePlayer(-1000f)
        assertEquals(game.paddleWidth / 2, game.playerX, 0f)
        game.movePlayer(1000f)
        assertEquals(360f - game.paddleWidth / 2, game.playerX, 0f)
    }

    @Test fun pauseAndVictoryStopSimulation() {
        val game = ready().apply { paused = true }
        game.update(0.04f)
        assertEquals(300f, game.ballY, 0f)
        game.paused = false
        game.playerScore = 6
        game.ballY = -6f
        game.velocityY = -300f
        game.update(0.02f)
        assertTrue(game.finished)
        game.update(0.04f)
        assertEquals(7, game.playerScore)
        assertEquals(300f, game.ballY, 0f)
    }

    @Test fun allDifficultiesKeepBallInsideSideWallsDuringLongRally() {
        Difficulty.entries.forEach { difficulty ->
            val game = GameEngine(difficulty)
            repeat(10000) {
                game.movePlayer(game.ballX)
                game.update(1f / 60f)
                assertTrue(game.ballX >= 7f && game.ballX <= 353f)
                assertTrue(game.playerScore <= 7 && game.computerScore <= 7)
            }
        }
    }
}
