package com.example.finedu

import android.os.Bundle
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.example.finedu.model.Module
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LearningActivity : AppCompatActivity() {
    private lateinit var viewPager: ViewPager2
    private lateinit var progressBar: ProgressBar
    private lateinit var tvModuleTitle: TextView
    private lateinit var btnContinueLater: Button
    private lateinit var currentModule: Module

    private var startSlideIndex: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_learning)

        val moduleId = intent.getStringExtra("MODULE_ID") ?: return finish()
        startSlideIndex = intent.getIntExtra("LAST_SLIDE_INDEX", 0)

        viewPager = findViewById(R.id.viewPager)
        progressBar = findViewById(R.id.progressBar)
        tvModuleTitle = findViewById(R.id.tvModuleTitle)
        btnContinueLater = findViewById(R.id.btnContinueLater)

        btnContinueLater.setOnClickListener {
            finish()
        }

        FirebaseFirestore.getInstance().collection("modules")
            .document(moduleId)
            .get()
            .addOnSuccessListener { document ->
                document.toObject(Module::class.java)?.let {
                    currentModule = it.copy(id = document.id)
                    setupViewPager()
                } ?: run {
                    finish()
                }
            }
    }

    private fun setupViewPager() {
        tvModuleTitle.text = currentModule.naziv

        viewPager.adapter = SlideAdapter(currentModule.slides)
        progressBar.max = currentModule.slides.size

        // Postavi ViewPager na zadnji spremljeni slide
        viewPager.setCurrentItem(startSlideIndex, false)
        // Postavi progress bar odmah na pravi slajd
        progressBar.progress = (startSlideIndex + 1).coerceAtMost(progressBar.max)

        btnContinueLater.setOnClickListener {
            finish()
        }

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                btnContinueLater.visibility =
                    if (position < currentModule.slides.size) Button.VISIBLE else Button.GONE

                if (position < currentModule.slides.size) {
                    progressBar.progress = position + 1
                    saveUserProgress(position + 1)
                } else {
                    progressBar.progress = progressBar.max
                }
            }
        })
    }

    private fun saveUserProgress(currentSlide: Int) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        FirebaseFirestore.getInstance().collection("userProgress")
            .document(userId)
            .get()
            .addOnSuccessListener { document ->
                val currentProgress = document.get("modulesProgress") as? Map<String, Any> ?: emptyMap()
                val updatedProgress = currentProgress.toMutableMap()
                updatedProgress[currentModule.id] = currentSlide

                FirebaseFirestore.getInstance().collection("userProgress")
                    .document(userId)
                    .update("modulesProgress", updatedProgress)
            }
    }
}
