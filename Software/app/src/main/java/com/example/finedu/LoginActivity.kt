package com.example.finedu

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()
        sharedPreferences = getSharedPreferences("login_prefs", MODE_PRIVATE)

        val emailEditText = findViewById<EditText>(R.id.editTextEmail)
        val passwordEditText = findViewById<EditText>(R.id.editTextPassword)
        val loginButton = findViewById<Button>(R.id.buttonLogin)
        val registerButton = findViewById<Button>(R.id.buttonRegister)
        val forgotPasswordText = findViewById<TextView>(R.id.textForgotPassword)
        val rememberMeCheckBox = findViewById<CheckBox>(R.id.checkboxRememberMe)

        // Load saved email and password if "Remember Me" was checked
        val savedEmail = sharedPreferences.getString("email", "")
        val savedPassword = sharedPreferences.getString("password", "")
        val rememberMeChecked = sharedPreferences.getBoolean("rememberMe", false)
        emailEditText.setText(savedEmail)
        passwordEditText.setText(savedPassword)
        rememberMeCheckBox.isChecked = rememberMeChecked

        loginButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Unesi e-mail i lozinku!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (rememberMeCheckBox.isChecked) {
                sharedPreferences.edit()
                    .putString("email", email)
                    .putString("password", password)
                    .putBoolean("rememberMe", true)
                    .apply()
            } else {
                sharedPreferences.edit()
                    .remove("email")
                    .remove("password")
                    .putBoolean("rememberMe", false)
                    .apply()
            }

            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this, "Uspješna prijava!", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this, DashboardActivity::class.java)

                        startActivity(intent)
                        finish()
                    } else {
                        Toast.makeText(
                            this,
                            "Greška: ${task.exception?.localizedMessage}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
        }

        registerButton.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }

        forgotPasswordText.setOnClickListener {
            val emailInput = EditText(this)
            emailInput.hint = "E-mail"
            emailInput.setText(emailEditText.text.toString().trim())

            val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Obnovi lozinku")
                .setMessage("Unesite svoj e-mail za reset lozinke. Poslat ćemo vam upute na e-mail adresu.")
                .setView(emailInput)
                .setPositiveButton("Pošalji") { _, _ ->
                    val email = emailInput.text.toString().trim()
                    if (email.isEmpty()) {
                        Toast.makeText(this, "Unesite e-mail adresu!", Toast.LENGTH_SHORT).show()
                    } else {
                        auth.sendPasswordResetEmail(email)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    Toast.makeText(this, "E-mail za reset lozinke je poslan.", Toast.LENGTH_LONG).show()
                                } else {
                                    Toast.makeText(
                                        this,
                                        "Greška: ${task.exception?.localizedMessage}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                    }
                }
                .setNegativeButton("Odustani", null)
                .create()

            dialog.show()
        }
    }
}
