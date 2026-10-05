package com.example.ping_pong

import android.app.AlertDialog
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : ComponentActivity() {
    private lateinit var root: LinearLayout
    private var selected = Difficulty.EASY
    private var gameView: GameView? = null
    private var score: TextView? = null
    private var pauseButton: Button? = null
    private val backgroundColor = Color.parseColor("#0A1420")
    private val mint = Color.parseColor("#75F0CA")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
        selected = Difficulty.entries.getOrElse(savedInstanceState?.getInt("difficulty") ?: 0) { Difficulty.EASY }
        val values = savedInstanceState?.getFloatArray("match")
        if (values != null) {
            val game = GameEngine(selected)
            game.playerX = values[0]
            game.computerX = values[1]
            game.ballX = values[2]
            game.ballY = values[3]
            game.velocityX = values[4]
            game.velocityY = values[5]
            game.serveDelay = values[6]
            game.playerScore = savedInstanceState.getInt("playerScore")
            game.computerScore = savedInstanceState.getInt("computerScore")
            game.paused = true
            showGame(game)
        } else showMenu()
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (gameView == null) finish() else confirmMenu()
            }
        })
    }

    private fun newRoot() {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(backgroundColor)
        }
        setContentView(root)
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            view.setPadding(bars.left + dp(22), bars.top + dp(12), bars.right + dp(22), bars.bottom + dp(12))
            insets
        }
        ViewCompat.requestApplyInsets(root)
    }

    private fun showMenu() {
        gameView?.stopFrames()
        gameView = null
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        newRoot()
        val scroll = ScrollView(this).apply { isFillViewport = true }
        root.addView(scroll, LinearLayout.LayoutParams(-1, -1))
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
        }
        scroll.addView(content)
        content.addView(text("КЛАССИКА В ОДНО КАСАНИЕ", 11f, mint, true), space(12))
        content.addView(text("Пинг-Понг", 40f, Color.WHITE, true), space(4))
        content.addView(text("Вы, мяч и достойный соперник.", 16f), space(24))
        val court = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = rounded("#101E2B")
            setPadding(dp(16), dp(12), dp(16), dp(12))
        }
        court.addView(text("━━━━", 23f, Color.parseColor("#FF927F")))
        court.addView(text("·  ·  ·  ·    ●    ·  ·  ·  ·", 19f, Color.WHITE))
        court.addView(text("━━━━", 23f, mint))
        content.addView(court, space(24))
        content.addView(text("ВЫБЕРИТЕ СЛОЖНОСТЬ", 11f, mint, true), space(10))
        val descriptions = listOf("Спокойный темп • широкая ракетка", "Быстрее мяч • активнее соперник", "Высокая скорость • узкая ракетка")
        Difficulty.entries.forEachIndexed { index, difficulty ->
            val active = selected == difficulty
            content.addView(button("${if (active) "●  " else "○  "}${difficulty.title}\n${descriptions[index]}", active) {
                selected = difficulty
                showMenu()
            }, space(8))
        }
        content.addView(button("Начать игру  →", true) { showGame(GameEngine(selected)) }, space(18))
        content.addView(text("Ведите пальцем влево и вправо.\nОтбивайте мяч нижней ракеткой.\nПропущенный мяч — очко сопернику.\nПервый до 7 очков побеждает.", 14f), space(0))
    }

    private fun showGame(game: GameEngine) {
        gameView?.stopFrames()
        newRoot()
        root.addView(text("ПИНГ-ПОНГ  /  ${selected.title.uppercase()}", 11f, mint, true), space(6))
        score = text("", 25f, Color.WHITE, true)
        root.addView(score, space(4))
        root.addView(text("МАТЧ ДО ${GameEngine.WIN_SCORE} ОЧКОВ", 10f), space(10))
        val field = GameView(this, game) { updateControls() }
        gameView = field
        root.addView(field, LinearLayout.LayoutParams(-1, 0, 1f))
        root.addView(text("↔  Двигайте нижнюю ракетку пальцем", 12f), space(4))
        val buttons = LinearLayout(this)
        pauseButton = button("Пауза", true) {
            if (game.finished) showGame(GameEngine(selected))
            else {
                game.paused = !game.paused
                updateControls()
            }
        }
        buttons.addView(pauseButton, LinearLayout.LayoutParams(0, dp(52), 1f).apply { marginEnd = dp(8) })
        buttons.addView(button("Меню", false) { confirmMenu() }, LinearLayout.LayoutParams(0, dp(52), 1f))
        root.addView(buttons)
        updateControls()
        field.startFrames()
    }

    private fun updateControls() {
        val game = gameView?.game ?: return
        score?.text = "Вы  ${game.playerScore} : ${game.computerScore}  Компьютер"
        pauseButton?.text = when {
            game.finished -> "Ещё раз"
            game.paused -> "Продолжить"
            else -> "Пауза"
        }
        if (game.paused || game.finished) window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        else window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        gameView?.invalidate()
    }

    private fun confirmMenu() {
        val game = gameView?.game ?: return
        if (game.finished) { showMenu(); return }
        game.paused = true
        updateControls()
        AlertDialog.Builder(this)
            .setTitle("Вернуться в меню?")
            .setMessage("Текущий счёт будет сброшен.")
            .setPositiveButton("В меню") { _, _ -> showMenu() }
            .setNegativeButton("Остаться", null)
            .show()
    }

    override fun onPause() {
        super.onPause()
        gameView?.game?.paused = true
        gameView?.stopFrames()
        updateControls()
    }

    override fun onResume() {
        super.onResume()
        gameView?.startFrames()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("difficulty", selected.ordinal)
        gameView?.game?.let { game ->
            outState.putFloatArray("match", floatArrayOf(game.playerX, game.computerX, game.ballX, game.ballY,
                game.velocityX, game.velocityY, game.serveDelay))
            outState.putInt("playerScore", game.playerScore)
            outState.putInt("computerScore", game.computerScore)
        }
    }

    private fun text(value: String, size: Float, color: Int = Color.parseColor("#9BAFC0"), bold: Boolean = false) = TextView(this).apply {
        text = value
        textSize = size
        setTextColor(color)
        gravity = Gravity.CENTER
        if (bold) setTypeface(typeface, Typeface.BOLD)
        setLineSpacing(dp(3).toFloat(), 1f)
    }

    private fun button(value: String, accent: Boolean, action: () -> Unit) = Button(this).apply {
        text = value
        textSize = 14f
        isAllCaps = false
        minHeight = dp(54)
        minimumHeight = dp(54)
        setTextColor(if (accent) backgroundColor else Color.WHITE)
        background = rounded(if (accent) "#75F0CA" else "#192B3B")
        setPadding(dp(12), dp(8), dp(12), dp(8))
        setOnClickListener { action() }
    }

    private fun rounded(color: String) = GradientDrawable().apply {
        setColor(Color.parseColor(color))
        cornerRadius = dp(14).toFloat()
    }

    private fun space(bottom: Int) = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(bottom) }
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
