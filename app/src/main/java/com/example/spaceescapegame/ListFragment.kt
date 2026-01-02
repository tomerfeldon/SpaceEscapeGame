package com.example.spaceescapegame

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ListFragment : Fragment() {

    private lateinit var scoreManager: ScoreManager
    var callback: ((Double, Double) -> Unit)? = null // פונקציה שתופעל בלחיצה

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_list, container, false)

        scoreManager = ScoreManager(requireContext())
        val scores = scoreManager.getScores()

        val rvScores: RecyclerView = view.findViewById(R.id.main_LST_scores)
        rvScores.layoutManager = LinearLayoutManager(context)

        // כאן אנחנו מחברים את ה-Adapter שיצרנו קודם
        val adapter = ScoreAdapter(scores) { lat, lon ->
            // מה קורה כשלוחצים? מפעילים את ה-Callback
            callback?.invoke(lat, lon)
        }
        rvScores.adapter = adapter

        return view
    }
}