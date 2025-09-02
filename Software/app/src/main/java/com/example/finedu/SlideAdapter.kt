package com.example.finedu

import android.app.Activity
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class SlideAdapter(
    private val slides: List<String>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_SLIDE = 0
        private const val TYPE_CONGRATS = 1
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == slides.size) TYPE_CONGRATS else TYPE_SLIDE
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_CONGRATS -> CongratsViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_congrats, parent, false)
            )
            else -> SlideViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_slide, parent, false)
            )
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is SlideViewHolder -> holder.bind(slides[position])
            is CongratsViewHolder -> holder.bind()
        }
    }

    override fun getItemCount() = slides.size + 1  // +1 for congratulatory slide

    class SlideViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        fun bind(slideContent: String) {
            itemView.findViewById<TextView>(R.id.slideContent).text = slideContent
        }
    }

    class CongratsViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        fun bind() {
            itemView.findViewById<Button>(R.id.btnBackToDashboard).setOnClickListener {
                val context = itemView.context
                context.startActivity(Intent(context, DashboardActivity::class.java))
                (context as Activity).finish()
            }
        }
    }
}

