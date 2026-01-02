package com.example.spaceescapegame

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class ScoreManager(private val context: Context) {
    private val gson = Gson()
    private val prefs = context.getSharedPreferences("GamePrefs", Context.MODE_PRIVATE)

    fun saveScore(score: Int, lat: Double, lon: Double) {
        val scores = getScores().toMutableList()
        scores.add(ScoreItem(score, lat, lon))

        // מיון מהגדול לקטן
        scores.sortByDescending { it.score }

        // שמירה רק של ה-10 הראשונים
        val top10 = if (scores.size > 10) scores.subList(0, 10) else scores

        // שמירה לזיכרון
        val json = gson.toJson(top10)
        prefs.edit().putString("SCORES_LIST", json).apply()
    }

    fun getScores(): ArrayList<ScoreItem> {
        val json = prefs.getString("SCORES_LIST", null) ?: return ArrayList()
        val type = object : TypeToken<ArrayList<ScoreItem>>() {}.type
        return gson.fromJson(json, type)
    }
}