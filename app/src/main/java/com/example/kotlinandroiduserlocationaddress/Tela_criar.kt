package com.example.kotlinandroiduserlocationaddress

import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore

class Tela_criar : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private val db = FirebaseFirestore.getInstance()
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tela_criar_conta)

        auth = FirebaseAuth.getInstance()

        val nameField = findViewById<EditText>(R.id.etName)
        val raField = findViewById<EditText>(R.id.etRA)
        val emailField = findViewById<EditText>(R.id.etEmail)
        val passwordField = findViewById<EditText>(R.id.etPassword)
        val createAccountButton = findViewById<Button>(R.id.btnCreateAccount)
        progressBar = findViewById(R.id.progressBar)

        createAccountButton.setOnClickListener {
            val name = nameField.text.toString().trim()
            val ra = raField.text.toString().trim()
            val email = emailField.text.toString().trim()
            val password = passwordField.text.toString().trim()

            // Validação dos campos
            if (!validateFields(name, ra, email, password)) {
                return@setOnClickListener
            }

            // Mostrar ProgressBar
            showProgressBar()

            // Criar conta no Firebase Authentication
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        val user = auth.currentUser
                        saveUserToFirestore(name, ra, email, user)
                    } else {
                        hideProgressBar()
                        Log.w(TAG, "createUserWithEmail:failure", task.exception)
                        Toast.makeText(
                            this,
                            "Falha de Autenticação: ${task.exception?.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                        updateUI(null)
                    }
                }
        }
    }

    private fun validateFields(name: String, ra: String, email: String, password: String): Boolean {
        if (name.isEmpty()) {
            Toast.makeText(this, "Por favor, insira seu nome.", Toast.LENGTH_SHORT).show()
            return false
        }

        if (ra.isEmpty()) {
            Toast.makeText(this, "Por favor, insira seu RA.", Toast.LENGTH_SHORT).show()
            return false
        }

        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Por favor, insira um email válido.", Toast.LENGTH_SHORT).show()
            return false
        }

        if (password.isEmpty() || password.length < 6) {
            Toast.makeText(this, "A senha deve ter pelo menos 6 caracteres.", Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }

    private fun saveUserToFirestore(name: String, ra: String, email: String, user: FirebaseUser?) {
        val userMap = hashMapOf(
            "Nome" to name,
            "RA" to ra,
            "Email" to email
        )

        db.collection("Usuários").document(user?.uid ?: "")
            .set(userMap)
            .addOnCompleteListener {
                hideProgressBar()
                if (it.isSuccessful) {
                    Log.d(TAG, "Sucesso ao cadastrar usuário!")
                    updateUI(user)
                } else {
                    Log.w(TAG, "Falha ao cadastrar usuário no Firestore", it.exception)
                    Toast.makeText(
                        this,
                        "Erro ao salvar os dados no Firestore.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .addOnFailureListener {
                hideProgressBar()
                Toast.makeText(this, "Erro ao salvar os dados no Firestore.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateUI(user: FirebaseUser?) {
        if (user != null) {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        } else {
            Toast.makeText(this, "Erro ao cadastrar! Tente novamente.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showProgressBar() {
        progressBar.visibility = View.VISIBLE
    }

    private fun hideProgressBar() {
        progressBar.visibility = View.GONE
    }
}