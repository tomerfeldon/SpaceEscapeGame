package com.example.spaceescapegame

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlin.random.Random

class MainActivity : AppCompatActivity(), SensorEventListener {

    // --- לוגיקה ---
    private var currentLane = 2 // אמצע (0-4)
    private var lives = 3
    private var score = 0
    private var isGameRunning = false
    private var useSensors = false // האם להשתמש בחיישנים?

    // --- חיישנים ---
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var lastSensorMoveTime: Long = 0 // למניעת תזוזה מהירה מדי

    // --- מיקום ---
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    // --- UI ---
    private lateinit var scoreText: TextView
    private lateinit var hearts: Array<ImageView>
    private lateinit var playerPositions: Array<ImageView>
    private lateinit var lanes: Array<RelativeLayout>
    private lateinit var btnLeft: FloatingActionButton
    private lateinit var btnRight: FloatingActionButton
    private lateinit var controlsContainer: LinearLayout

    // --- מנוע משחק ---
    private val handler = Handler(Looper.getMainLooper())
    private val obstacles = mutableListOf<ImageView>()
    private var baseSpeed = 20f
    private var currentSpeed = 20f
    private val gameUpdateSpeed = 30L

    // סאונד
    private var crashSound: MediaPlayer? = null
    private var coinSound: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // קבלת המצב מהתפריט (כפתורים או חיישנים)
        val mode = intent.getStringExtra("MODE")
        useSensors = (mode == "SENSORS")

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // אתחול חיישנים
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        initViews()

        // אם בחרנו חיישנים - נעלים כפתורים
        if (useSensors) {
            controlsContainer.visibility = View.GONE
            Toast.makeText(this, "Tilt phone to move!", Toast.LENGTH_SHORT).show()
        } else {
            controlsContainer.visibility = View.VISIBLE
            setupButtonListeners()
        }

        setupAudio()
        startGame()
    }

    private fun initViews() {
        scoreText = findViewById(R.id.scoreText)
        controlsContainer = findViewById(R.id.controlsContainer)

        hearts = arrayOf(findViewById(R.id.heart1), findViewById(R.id.heart2), findViewById(R.id.heart3))

        // 5 נתיבים
        lanes = arrayOf(
            findViewById(R.id.lane_0), findViewById(R.id.lane_1), findViewById(R.id.lane_2),
            findViewById(R.id.lane_3), findViewById(R.id.lane_4)
        )

        // 5 חלליות
        playerPositions = arrayOf(
            findViewById(R.id.player_lane_0), findViewById(R.id.player_lane_1), findViewById(R.id.player_lane_2),
            findViewById(R.id.player_lane_3), findViewById(R.id.player_lane_4)
        )

        btnLeft = findViewById(R.id.btn_left)
        btnRight = findViewById(R.id.btn_right)
    }

    private fun setupButtonListeners() {
        btnLeft.setOnClickListener { movePlayer(-1) }
        btnRight.setOnClickListener { movePlayer(1) }
    }

    private fun setupAudio() {
        try {
            // טעינת סאונד התנגשות
            crashSound = MediaPlayer.create(this, R.raw.sound_crash)
            // אם הוספת גם סאונד למטבע, בטל את ההערה:
            // coinSound = MediaPlayer.create(this, R.raw.sound_coin)

        } catch (e: Exception) { e.printStackTrace() }
    }

    // --- לוגיקת חיישנים ---
    override fun onResume() {
        super.onResume()
        if (useSensors && accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    override fun onPause() {
        super.onPause()
        if (useSensors) {
            sensorManager.unregisterListener(this)
        }
        stopGame()
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || !isGameRunning) return

        val x = event.values[0] // הטיה לצדדים
        val y = event.values[1] // הטיה קדימה/אחורה

        // 1. תזוזה לצדדים (X)
        if (System.currentTimeMillis() - lastSensorMoveTime > 300) {
            if (x > 3.0) { // הטיה שמאלה
                movePlayer(-1)
                lastSensorMoveTime = System.currentTimeMillis()
            } else if (x < -3.0) { // הטיה ימינה
                movePlayer(1)
                lastSensorMoveTime = System.currentTimeMillis()
            }
        }

        // 2. שליטה במהירות (Y) - הבונוס!
        if (y > 3.0) { // מהיר
            currentSpeed = baseSpeed * 1.5f
        } else if (y < -3.0) { // לאט
            currentSpeed = baseSpeed * 0.7f
        } else {
            currentSpeed = baseSpeed // רגיל
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    // --- לוגיקת משחק ---

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                saveScoreAndLocation()
            } else {
                saveScoreToPrefs(0.0, 0.0)
            }
        }
    }

    private fun startGame() {
        isGameRunning = true
        lives = 3
        score = 0
        currentLane = 2

        // --- קביעת המהירות לפי בחירת המשתמש ---
        val speedMode = intent.getStringExtra("SPEED")
        if (speedMode == "FAST") {
            baseSpeed = 40f
        } else {
            baseSpeed = 20f // SLOW או SENSORS
        }
        currentSpeed = baseSpeed

        scoreText.text = "Score: 0"

        // איפוס ויזואלי
        playerPositions.forEach { it.visibility = View.INVISIBLE }
        playerPositions[currentLane].visibility = View.VISIBLE
        hearts.forEach { it.visibility = View.VISIBLE }

        obstacles.forEach { (it.parent as? RelativeLayout)?.removeView(it) }
        obstacles.clear()

        handler.post(gameLoopRunnable)
        handler.post(spawnObstacleRunnable)
    }

    private fun stopGame() {
        isGameRunning = false
        handler.removeCallbacksAndMessages(null)
    }

    private fun movePlayer(direction: Int) {
        if (!isGameRunning) return
        val newLane = currentLane + direction
        if (newLane in 0..4) {
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

                // Odometer: העלאת ניקוד כל הזמן (מד מרחק)
                score++
                updateScoreUI()

                handler.postDelayed(this, gameUpdateSpeed)
            }
        }
    }

    private fun updateScoreUI() {
        runOnUiThread {
            scoreText.text = "Score: $score"
        }
    }

    private val spawnObstacleRunnable = object : Runnable {
        override fun run() {
            if (isGameRunning) {
                spawnObstacle()
                handler.postDelayed(this, Random.nextLong(1000, 2000))
            }
        }
    }

    private fun spawnObstacle() {
        val randomLaneIndex = Random.nextInt(0, 5)
        val parentLane = lanes[randomLaneIndex]
        val obstacle = ImageView(this)

        // 20% סיכוי למטבע
        val isCoin = Random.nextInt(1, 101) > 80

        if (isCoin) {
            obstacle.setImageResource(R.drawable.ic_coin)
            obstacle.tag = "COIN"
        } else {
            obstacle.setImageResource(R.drawable.ic_stone)
            obstacle.tag = "STONE"
        }

        obstacle.contentDescription = randomLaneIndex.toString()

        val params = RelativeLayout.LayoutParams(120, 120)
        params.addRule(RelativeLayout.CENTER_HORIZONTAL)
        obstacle.layoutParams = params
        obstacle.y = -200f

        parentLane.addView(obstacle)
        obstacles.add(obstacle)
    }

    private fun moveObstacles() {
        val iterator = obstacles.iterator()
        while (iterator.hasNext()) {
            val obstacle = iterator.next()
            obstacle.y += currentSpeed

            if (obstacle.y > lanes[0].height) {
                (obstacle.parent as? RelativeLayout)?.removeView(obstacle)
                iterator.remove()
            }
        }
    }

    private fun checkCollisions() {
        val playerY = playerPositions[currentLane].y

        for (i in obstacles.indices.reversed()) {
            val obstacle = obstacles[i]
            val obstacleLane = obstacle.contentDescription.toString().toInt()

            if (obstacleLane == currentLane) {
                if (obstacle.y + obstacle.height > playerY + 20 && obstacle.y < playerY + playerPositions[currentLane].height - 20) {
                    val type = obstacle.tag.toString()

                    if (type == "STONE") {
                        handleCrash(obstacle)
                    } else if (type == "COIN") {
                        collectCoin(obstacle)
                    }
                    break
                }
            }
        }
    }

    private fun collectCoin(obstacle: ImageView) {
        score += 10 // בונוס על לקיחת מטבע
        // אין צורך לעדכן כאן טקסט כי הוא מתעדכן אוטומטית בלולאה הראשית
        coinSound?.start()

        (obstacle.parent as? RelativeLayout)?.removeView(obstacle)
        obstacles.remove(obstacle)
    }

    private fun handleCrash(obstacle: ImageView) {
        crashSound?.start()
        vibrateDevice()
        Toast.makeText(this, "CRASH!", Toast.LENGTH_SHORT).show()

        (obstacle.parent as? RelativeLayout)?.removeView(obstacle)
        obstacles.remove(obstacle)
        lives--

        updateLivesUI()

        if (lives <= 0) {
            gameOver()
        }
    }

    private fun updateLivesUI() {
        if (lives < 3) hearts[2].visibility = View.INVISIBLE
        if (lives < 2) hearts[1].visibility = View.INVISIBLE
        if (lives < 1) hearts[0].visibility = View.INVISIBLE
    }

    private fun vibrateDevice() {
        val v = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v.vibrate(VibrationEffect.createOneShot(300, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            v.vibrate(300)
        }
    }

    private fun gameOver() {
        stopGame()
        saveScoreAndLocation()
    }

    private fun saveScoreAndLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1001)
            return
        }

        val cancellationTokenSource = CancellationTokenSource()

        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancellationTokenSource.token)
            .addOnSuccessListener { location ->
                if (location != null) {
                    saveScoreToPrefs(location.latitude, location.longitude)
                } else {
                    saveScoreToPrefs(0.0, 0.0)
                }
            }
            .addOnFailureListener {
                saveScoreToPrefs(0.0, 0.0)
            }
    }

    private fun saveScoreToPrefs(lat: Double, lon: Double) {
        val scoreManager = ScoreManager(this)
        scoreManager.saveScore(score, lat, lon)

        Toast.makeText(this, "Score Saved!", Toast.LENGTH_SHORT).show()
        finish()
    }
}