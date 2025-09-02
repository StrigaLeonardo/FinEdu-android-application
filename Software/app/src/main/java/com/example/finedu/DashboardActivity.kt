package com.example.finedu

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.finedu.portfolio.PortfolioManager
import com.example.finedu.trading.TradingActivity
import com.github.mikephil.charting.charts.PieChart
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class DashboardActivity : AppCompatActivity() {

    private lateinit var pieChart: PieChart
    private lateinit var txtTotalValue: TextView
    private lateinit var myIcon: ImageView
    private lateinit var txtDailyChange: TextView
    private lateinit var portfolioManager: PortfolioManager
    private lateinit var learningModuleManager: LearningModuleManager


    private lateinit var firestore: FirebaseFirestore

    private val learningLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        learningModuleManager.loadModulesAndProgress()

    }

    override fun onResume() {
        super.onResume()
        portfolioManager.refreshPortfolio()
    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        firestore = FirebaseFirestore.getInstance()
        pieChart = findViewById(R.id.pieChart)
        txtTotalValue = findViewById(R.id.txtTotalValue)
        myIcon = findViewById(R.id.myIcon)
        txtDailyChange = findViewById(R.id.txtDailyChange)


        portfolioManager = PortfolioManager(
            context = this,
            pieChart = pieChart,
            txtTotalValue = txtTotalValue,
            onDailyChangeUpdated = { money, percent ->
                updateDailyChangeDisplay(money, percent)
            }
        )

        configurePieChart()
        portfolioManager.startPortfolioListeners()

        val container = findViewById<LinearLayout>(R.id.learningModulesContainer)
        learningModuleManager = LearningModuleManager(
            context = this,
            container = container,
            firestore = firestore,
            learningLauncher = learningLauncher
        )
        learningModuleManager.loadModulesAndProgress()

        setupButtons()
        checkDailyQuizCompletionAndDisableButton()
    }

    private fun configurePieChart() {
        pieChart.setUsePercentValues(true)
        pieChart.description.isEnabled = false
        pieChart.isDrawHoleEnabled = false
        pieChart.setEntryLabelColor(Color.BLACK)
        pieChart.setEntryLabelTextSize(15f)
        pieChart.legend.isEnabled = false
    }

    private fun setupButtons() {
        val btnQuiz = findViewById<Button>(R.id.btnQuiz)
        val btnTrading = findViewById<Button>(R.id.btnTrading)
        val btnPortfolio = findViewById<Button>(R.id.btnPortfolio)

        btnQuiz.setOnClickListener {
            val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return@setOnClickListener
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            firestore.collection("userProgress")
                .document(userId)
                .get()
                .addOnSuccessListener { document ->
                    val quizProgress = document.get("quizProgress") as? Map<String, *>
                    val hasCompletedToday = quizProgress?.containsKey(today) ?: false
                    if (!hasCompletedToday) {
                        startActivity(Intent(this, QuizActivity::class.java))
                    } else {
                        Toast.makeText(this, "Već ste riješili današnji kviz!", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("Dashboard", "Error checking quiz progress: ${e.message}")
                    Toast.makeText(this, "Greška pri provjeri kviza", Toast.LENGTH_SHORT).show()
                }
        }

        btnTrading.setOnClickListener {
            startActivity(Intent(this, TradingActivity::class.java))
        }

        btnPortfolio.setOnClickListener {
            startActivity(Intent(this, PortfolioActivity::class.java))
        }



    }

    private fun checkDailyQuizCompletionAndDisableButton() {
        val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        firestore.collection("userProgress")
            .document(userId)
            .get()
            .addOnSuccessListener { document ->
                val quizProgress = document.get("quizProgress") as? Map<String, *>
                val hasCompletedToday = quizProgress?.containsKey(today) ?: false
                val btnQuiz = findViewById<Button>(R.id.btnQuiz)
                val ivQuizCompleted = findViewById<ImageView>(R.id.ivQuizCompleted)
                if (hasCompletedToday) {
                    ivQuizCompleted.visibility = View.VISIBLE
                    btnQuiz.isEnabled = false
                    btnQuiz.alpha = 0.5f
                } else {
                    ivQuizCompleted.visibility = View.GONE
                    btnQuiz.isEnabled = true
                    btnQuiz.alpha = 1f
                }
            }
            .addOnFailureListener { e ->
                Log.e("Dashboard", "Error checking quiz progress: ${e.message}")
            }
    }

    private fun updateDailyChangeDisplay(
        dailyChangeMoney: Double,
        dailyChangePercent: Double
    ) {
        val isPositive = dailyChangeMoney > 0
        val isNegative = dailyChangeMoney < 0

        val iconRes = when {
            isPositive -> R.drawable.ic_arrow_up
            isNegative -> R.drawable.ic_arrow_down
            else -> R.drawable.ic_arrow_neutral
        }
        val colorRes = when {
            isPositive -> R.color.gain_green
            isNegative -> R.color.loss_red
            else -> R.color.neutral
        }
        val sign = when {
            isPositive -> "+"
            isNegative -> "-"
            else -> ""
        }
        val text = "$sign${"%.2f".format(kotlin.math.abs(dailyChangePercent))}% ($sign${"%.2f".format(kotlin.math.abs(dailyChangeMoney))} €)"

        txtDailyChange.text = text
        txtDailyChange.setTextColor(getColor(colorRes))
        myIcon.setImageResource(iconRes)
        myIcon.setColorFilter(getColor(colorRes))
    }

    override fun onDestroy() {
        super.onDestroy()
        portfolioManager.stopPortfolioListeners()
    }



}
