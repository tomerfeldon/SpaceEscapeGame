package com.example.spaceescapegame

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ScoreAdapter(
    private val scores: ArrayList<ScoreItem>,
    private val onScoreClicked: (Double, Double) -> Unit // Callback ללחיצה
) : RecyclerView.Adapter<ScoreAdapter.ScoreViewHolder>() {

    class ScoreViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val lblScore: TextView = itemView.findViewById(R.id.lbl_score)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScoreViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_score, parent, false)
        return ScoreViewHolder(view)
    }

    override fun onBindViewHolder(holder: ScoreViewHolder, position: Int) {
        val item = scores[position]
        holder.lblScore.text = "Score: ${item.score}"

        // בעת לחיצה על השורה - נשלח את המיקום למפה
        holder.itemView.setOnClickListener {
            onScoreClicked(item.lat, item.lon)
        }
    }

    override fun getItemCount() = scores.size
}