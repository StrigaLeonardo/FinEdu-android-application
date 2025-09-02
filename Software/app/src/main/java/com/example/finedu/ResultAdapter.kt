package com.example.finedu

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.finedu.model.Question
import androidx.core.content.ContextCompat

class ResultsAdapter(
    private val questions: List<Question>,
    private val userAnswers: Map<Int, Int>
) : RecyclerView.Adapter<ResultsAdapter.ResultViewHolder>() {

    class ResultViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvQuestion: TextView = view.findViewById(R.id.tvQuestion)
        val tvUserAnswer: TextView = view.findViewById(R.id.tvUserAnswer)
        val tvCorrectAnswer: TextView = view.findViewById(R.id.tvCorrectAnswer)
        val ivStatus: ImageView = view.findViewById(R.id.ivStatus)
        val tvEarned: TextView = view.findViewById(R.id.tvEarned)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ResultViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_quiz_result, parent, false)
        return ResultViewHolder(view)
    }

    override fun onBindViewHolder(holder: ResultViewHolder, position: Int) {
        val question = questions[position]
        val userAnswerIndex = userAnswers[position] ?: -1
        val isCorrect = userAnswerIndex == question.correctAnswerIndex

        holder.tvQuestion.text = question.text
        holder.tvUserAnswer.text = if (userAnswerIndex != -1) question.answers[userAnswerIndex] else "Nije odgovoreno"
        holder.tvCorrectAnswer.text = question.answers[question.correctAnswerIndex]


        holder.tvUserAnswer.setTextColor(
            ContextCompat.getColor(
                holder.itemView.context,
                if (isCorrect) R.color.gain_green else R.color.loss_red
            )
        )


        holder.ivStatus.setImageResource(
            if (isCorrect) R.drawable.ic_quiz else R.drawable.ic_close
        )


        holder.tvEarned.text = if (isCorrect) "+100 USD" else ""
        holder.tvEarned.setTextColor(
            ContextCompat.getColor(holder.itemView.context, R.color.gain_green)
        )
    }

    override fun getItemCount() = questions.size
}

