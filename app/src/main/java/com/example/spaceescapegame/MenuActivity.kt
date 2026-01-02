package com.example.spaceescapegame

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class MenuActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu)

        // 1. כפתור למצב כפתורים - איטי
        findViewById<Button>(R.id.btn_mode_slow).setOnClickListener {
            startGame("BUTTONS", "SLOW")
        }

        // 2. כפתור למצב כפתורים - מהיר
        findViewById<Button>(R.id.btn_mode_fast).setOnClickListener {
            startGame("BUTTONS", "FAST")
        }

        // 3. כפתור למצב חיישנים (מהירות משתנה ע"י הטיה)
        findViewById<Button>(R.id.btn_mode_sensor).setOnClickListener {
            startGame("SENSORS", "NORMAL")
        }

        // 4. כפתור טבלת שיאים
        findViewById<Button>(R.id.btn_high_scores).setOnClickListener {
            val intent = Intent(this, HighScoresActivity::class.java)
            startActivity(intent)
        }
    }

    private fun startGame(mode: String, speed: String) {
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("MODE", mode)   // שולח את סוג השליטה
        intent.putExtra("SPEED", speed) // שולח את המהירות
        startActivity(intent)
    }
}