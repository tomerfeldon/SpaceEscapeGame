package com.example.spaceescapegame

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlin.random.Random

class MainActivity : AppCompatActivity() {

    private var currentLane = 1
    private var lives = 3
    private var score = 0

    private var loopCounter = 0
    private var isGameRunning = false
    private var playerName = ""

    private lateinit var layoutGame: View
    private lateinit var layoutStart: View
    private lateinit var layoutGameOver: View

    private lateinit var inputName: EditText
    private lateinit var btnStart: Button
    private lateinit var textFinalScore: TextView
    private lateinit var btnRestart: Button
    private lateinit var btnExit: Button

    private lateinit var scoreText: TextView
    private lateinit var hearts: Array<ImageView>
    private lateinit var playerPositions: Array<ImageView>
    private lateinit var lanes: Array<RelativeLayout>
    private lateinit var btnLeft: FloatingActionButton
    private lateinit var btnRight: FloatingActionButton

    private val handler = Handler(Looper.getMainLooper())
    private val obstacles = mutableListOf<ImageView>()
    private var obstacleSpeed = 25f
    private val gameUpdateSpeed = 30L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        setupListeners()
        showStartScreen()
    }

    private fun initViews() {
        layoutGame = findViewById(R.id.game_layout)
        layoutStart = findViewById(R.id.layout_start)
        layoutGameOver = findViewById(R.id.layout_game_over)

        inputName = findViewById(R.id.input_name)
        btnStart = findViewById(R.id.btn_start_game)
        textFinalScore = findViewById(R.id.text_final_score)
        btnRestart = findViewById(R.id.btn_restart)
        btnExit = findViewById(R.id.btn_exit)

        scoreText = findViewById(R.id.scoreText)
        hearts = arrayOf(findViewById(R.id.heart1), findViewById(R.id.heart2), findViewById(R.id.heart3))
        playerPositions = arrayOf(findViewById(R.id.player_lane_0), findViewById(R.id.player_lane_1), findViewById(R.id.player_lane_2))
        lanes = arrayOf(findViewById(R.id.lane_0), findViewById(R.id.lane_1), findViewById(R.id.lane_2))
        btnLeft = findViewById(R.id.btn_left)
        btnRight = findViewById(R.id.btn_right)
    }

    private fun setupListeners() {
        btnLeft.setOnClickListener { movePlayer(-1) }
        btnRight.setOnClickListener { movePlayer(1) }

        btnStart.setOnClickListener {
            val name = inputName.text.toString()
            if (name.isEmpty()) {
                Toast.makeText(this, "Please enter your name!", Toast.LENGTH_SHORT).show()
            } else {
                playerName = name
                startGame()
            }
        }

        btnRestart.setOnClickListener {
            startGame()
        }

        btnExit.setOnClickListener {
            finish()
        }
    }

    private fun showStartScreen() {
        layoutStart.visibility = View.VISIBLE
        layoutGame.visibility = View.INVISIBLE
        layoutGameOver.visibility = View.GONE
        isGameRunning = false
    }

    private fun startGame() {
        layoutStart.visibility = View.GONE
        layoutGameOver.visibility = View.GONE
        layoutGame.visibility = View.VISIBLE

        isGameRunning = true
        lives = 3
        score = 0
        loopCounter = 0
        currentLane = 1
        obstacleSpeed = 25f

        scoreText.text = "$playerName - Score: 0"

        playerPositions[0].visibility = View.INVISIBLE
        playerPositions[1].visibility = View.VISIBLE
        playerPositions[2].visibility = View.INVISIBLE

        hearts.forEach { it.visibility = View.VISIBLE }
        obstacles.forEach { (it.parent as? RelativeLayout)?.removeView(it) }
        obstacles.clear()

        handler.removeCallbacksAndMessages(null)
        handler.post(gameLoopRunnable)
        handler.post(spawnObstacleRunnable)
    }

    private fun gameOver() {
        isGameRunning = false

        handler.removeCallbacksAndMessages(null)

        layoutGame.visibility = View.INVISIBLE
        layoutGameOver.visibility = View.VISIBLE

        textFinalScore.text = "$playerName, your Score: $score"
    }

    private fun movePlayer(direction: Int) {
        if (!isGameRunning) return
        val newLane = currentLane + direction
        if (newLane in 0..2) {
            playerPositions[currentLane].visibility = View.INVISIBLE
            currentLane = newLane
            playerPositions[currentLane].visibility = View.VISIBLE
        }
    }

    private val gameLoopRunnable = object : Runnable {
        override fun run() {
            if (isGameRunning) {
                moveObstacles()
                checkCollisions()
                handler.postDelayed(this, gameUpdateSpeed)
            }
        }
    }

    private val spawnObstacleRunnable = object : Runnable {
        override fun run() {
            if (isGameRunning) {
                spawnObstacle()
                val delay = Random.nextLong(1000, 2000)
                handler.postDelayed(this, delay)
            }
        }
    }

    private fun spawnObstacle() {
        val randomLaneIndex = Random.nextInt(0, 3)
        val parentLane = lanes[randomLaneIndex]
        val obstacle = ImageView(this)
        obstacle.setImageResource(R.drawable.ic_stone)
        val params = RelativeLayout.LayoutParams(150, 150)
        params.addRule(RelativeLayout.CENTER_HORIZONTAL)
        obstacle.layoutParams = params
        obstacle.y = -200f
        obstacle.tag = randomLaneIndex
        parentLane.addView(obstacle)
        obstacles.add(obstacle)
    }

    private fun moveObstacles() {
        val iterator = obstacles.iterator()
        while (iterator.hasNext()) {
            val obstacle = iterator.next()

            obstacle.y += obstacleSpeed

            if (obstacle.y > lanes[0].height) {
                score++
                scoreText.text = "$playerName - Score: $score"

                if (score % 10 == 0) {
                    obstacleSpeed += 5f
                    Toast.makeText(this, "Speed Up!", Toast.LENGTH_SHORT).show()
                }

                (obstacle.parent as? RelativeLayout)?.removeView(obstacle)
                iterator.remove()
            }
        }
    }

    private fun checkCollisions() {
        val playerY = playerPositions[currentLane].y
        val playerHeight = playerPositions[currentLane].height
        for (obstacle in obstacles) {
            val obstacleLane = obstacle.tag as Int
            if (obstacleLane == currentLane) {
                if (obstacle.y + obstacle.height > playerY + 20 && obstacle.y < playerY + playerHeight - 20) {
                    handleCrash(obstacle)
                    break
                }
            }
        }
    }

    private fun handleCrash(obstacle: ImageView) {
        vibrateDevice()
        Toast.makeText(this, "CRASH!", Toast.LENGTH_SHORT).show()
        (obstacle.parent as? RelativeLayout)?.removeView(obstacle)
        obstacles.remove(obstacle)
        lives--

        if (lives < 3) hearts[2].visibility = View.INVISIBLE
        if (lives < 2) hearts[1].visibility = View.INVISIBLE
        if (lives < 1) hearts[0].visibility = View.INVISIBLE

        if (lives <= 0) {
            gameOver()
        }
    }

    private fun vibrateDevice() {
        val v = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v.vibrate(VibrationEffect.createOneShot(300, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            v.vibrate(300)
        }
    }
}