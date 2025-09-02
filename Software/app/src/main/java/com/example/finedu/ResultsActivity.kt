package com.example.finedu

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.finedu.model.Question


class ResultsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_results)

        // Dobavi podatke
        val questions = intent.getSerializableExtra("QUESTIONS") as? ArrayList<Question> ?: arrayListOf()
        val userAnswersBundle = intent.getBundleExtra("USER_ANSWERS")
        val userAnswers = userAnswersBundle?.keySet()?.associate {
            it.toInt() to userAnswersBundle.getInt(it)
        } ?: emptyMap()

        val totalEarned = intent.getIntExtra("TOTAL_EARNED", 0)

        // Postavi RecyclerView
        val recyclerView = findViewById<RecyclerView>(R.id.rvResults)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = ResultsAdapter(questions, userAnswers)

        // Postavi ukupnu zaradu
        findViewById<TextView>(R.id.tvTotalEarned).text = "Zaradili ste: $totalEarned USD"



        findViewById<Button>(R.id.btnMainMenu).setOnClickListener {
            val intent = Intent(this, DashboardActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
            finish()
        }

    }
}
