package com.example.spaceescapegame

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class HighScoresActivity : AppCompatActivity() {

    private lateinit var listFragment: ListFragment
    private lateinit var mapFragment: MapFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_high_scores)

        // יצירת הפרגמנטים
        listFragment = ListFragment()
        mapFragment = MapFragment()

        // טעינת הפרגמנטים למסך
        supportFragmentManager.beginTransaction()
            .replace(R.id.frame_list, listFragment)
            .replace(R.id.frame_map, mapFragment)
            .commit()

        // הגדרת התקשורת: כשלוחצים ברשימה -> תזיז את המפה
        listFragment.callback = { lat, lon ->
            mapFragment.zoomToLocation(lat, lon)
        }
    }
}