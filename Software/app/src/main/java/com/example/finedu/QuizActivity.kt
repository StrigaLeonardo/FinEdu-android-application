package com.example.finedu
import android.content.Intent

import android.os.Bundle
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.finedu.model.Quiz
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import android.util.Log

fun Map<Int, Int>.toBundle(): Bundle {
    val bundle = Bundle()
    for ((key, value) in this) {
        bundle.putInt(key.toString(), value)
    }
    return bundle
}


class QuizActivity : AppCompatActivity() {
    private lateinit var currentQuiz: Quiz
    private var currentQuestionIndex = 0
    private val userAnswers = mutableMapOf<Int, Int>()
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_quiz)

        db = FirebaseFirestore.getInstance()
        loadDailyQuiz()
    }

    private fun loadDailyQuiz() {
        db.collection("dailyQuizzes")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    val document = querySnapshot.documents[0]
                    currentQuiz = document.toObject(Quiz::class.java)!!
                    currentQuiz.id = document.id
                    showQuestion(currentQuestionIndex)
                    setupNextButton()
                } else {
                    Toast.makeText(this, "Današnji kviz nije dostupan", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Greška pri učitavanju kviza: ${e.message}", Toast.LENGTH_LONG).show()
                finish()
            }
    }

    private fun showQuestion(index: Int) {
        val question = currentQuiz.questions[index]
        findViewById<TextView>(R.id.tvQuestion).text = question.text

        val radioGroup = findViewById<RadioGroup>(R.id.rgAnswers)
        radioGroup.clearCheck()
        radioGroup.removeAllViews()

        question.answers.forEachIndexed { i, answer ->
            val radioButton = RadioButton(this).apply {
                text = answer
                id = i
            }
            radioGroup.addView(radioButton)
        }
    }

    private fun setupNextButton() {
        findViewById<Button>(R.id.btnNext).setOnClickListener {
            val selectedId = findViewById<RadioGroup>(R.id.rgAnswers).checkedRadioButtonId
            if (selectedId == -1) {
                Toast.makeText(this, "Odaberite odgovor!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            userAnswers[currentQuestionIndex] = selectedId

            if (currentQuestionIndex < currentQuiz.questions.size - 1) {
                currentQuestionIndex++
                showQuestion(currentQuestionIndex)
            } else {
                finishQuiz()
            }
        }
    }

    // QuizActivity.kt
    private fun finishQuiz() {
        val correctAnswers = currentQuiz.questions.mapIndexed { index, question ->
            userAnswers[index] == question.correctAnswerIndex
        }
        val score = correctAnswers.count { it }

        // Priprema podataka za Firebase
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val quizId = currentQuiz.id
        val answerMap = userAnswers.mapKeys { it.key.toString() }

        val updates = hashMapOf<String, Any>(
            "quizProgress.$quizId.score" to score,
            "quizProgress.$quizId.date" to FieldValue.serverTimestamp(),
            "quizProgress.$quizId.answers" to answerMap
        )

        // Spremamo u Firestore
        FirebaseFirestore.getInstance().collection("userProgress")
            .document(userId)
            .update(updates)
            .addOnSuccessListener {
                Log.d("Quiz", "Odgovori spremljeni")
                // Navigacija na ResultsActivity
                val intent = Intent(this, ResultsActivity::class.java).apply {
                    putExtra("QUESTIONS", ArrayList(currentQuiz.questions))
                    putExtra("USER_ANSWERS", userAnswers.toBundle())
                    putExtra("TOTAL_EARNED", score * 100)
                    putExtra("QUIZ_ID", quizId)
                }
                startActivity(intent)
                finish()
            }
            .addOnFailureListener { e ->
                Log.e("Quiz", "Greška pri spremanju: ${e.message}")
                Toast.makeText(this, "Greška pri spremanju rezultata", Toast.LENGTH_SHORT).show()
            }
    }


}
