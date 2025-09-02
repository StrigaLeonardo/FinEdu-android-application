package com.example.finedu

import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import com.example.finedu.model.Module
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LearningModuleManager(
    private val context: Context,
    private val container: LinearLayout,
    private val firestore: FirebaseFirestore,
    private val learningLauncher: ActivityResultLauncher<Intent>
) {
    private val modules = mutableListOf<Module>()
    private var progressMap: Map<String, Int> = emptyMap()

    fun loadModulesAndProgress() {
        firestore.collection("modules")
            .get()
            .addOnSuccessListener { result ->
                modules.clear()
                modules.addAll(result.toObjects(Module::class.java))
                setupModuleCards()
                loadUserProgress()
            }
            .addOnFailureListener { e ->
                Log.e("LearningModuleManager", "Error loading modules", e)
                Toast.makeText(context, "Greška pri učitavanju modula", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupModuleCards() {
        container.removeAllViews()
        modules.forEach { module ->
            val card = LayoutInflater.from(context)
                .inflate(R.layout.card_learning_module, container, false)

            card.tag = module.id
            card.findViewById<TextView>(R.id.moduleTitle).text = module.naziv
            card.findViewById<ProgressBar>(R.id.progressBar).progress = 0
            card.findViewById<TextView>(R.id.moduleProgress).text = "Napredak: 0%"

            card.setOnClickListener {
                val lastSlideIndex = progressMap[module.id] ?: 0
                val intent = Intent(context, LearningActivity::class.java).apply {
                    putExtra("MODULE_ID", module.id)
                    putExtra("LAST_SLIDE_INDEX", lastSlideIndex)
                }
                learningLauncher.launch(intent)
            }
            container.addView(card)
        }
    }

    private fun loadUserProgress() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        firestore.collection("userProgress")
            .document(userId)
            .get()
            .addOnSuccessListener { document ->
                val rawMap = document.get("modulesProgress") as? Map<String, Number> ?: emptyMap()
                progressMap = rawMap.mapValues { it.value.toInt() }
                updateCardsWithProgress()
            }
    }

    fun updateCardsWithProgress() {
        for (i in 0 until container.childCount) {
            val card = container.getChildAt(i)
            val moduleId = card.tag as? String ?: continue

            val module = modules.find { it.id == moduleId } ?: continue
            val lastSlideIndex = progressMap[moduleId] ?: 0
            val slideCount = module.slides.size
            val progressPercent = if (slideCount == 0) 0 else (lastSlideIndex.toFloat() / slideCount * 100).toInt()

            card.findViewById<ProgressBar>(R.id.progressBar).progress = progressPercent
            card.findViewById<TextView>(R.id.moduleProgress).text = "Napredak: $progressPercent%"
        }
    }
}
