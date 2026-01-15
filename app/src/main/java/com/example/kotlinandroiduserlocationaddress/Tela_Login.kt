package com.example.kotlinandroiduserlocationaddress

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class Tela_Login : AppCompatActivity() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tela_aluno)

        val emailField = findViewById<EditText>(R.id.etEmail)
        val passwordField = findViewById<EditText>(R.id.etPassword)
        val loginButton = findViewById<Button>(R.id.btnLogin)
        val createAccountLink = findViewById<TextView>(R.id.tvCreateAccount)

        // Redirecionar para a tela de criação de conta
        createAccountLink.setOnClickListener {
            val intent = Intent(this, Tela_criar::class.java)
            startActivity(intent)
        }
        // Ação do botão de login
        loginButton.setOnClickListener {
            val email = emailField.text.toString().trim()
            val password = passwordField.text.toString().trim()

            // Verifica se os campos estão preenchidos corretamente
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(
                    this, "Por favor, preencha todos os campos",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            if (!isValidEmail(email)) {
                Toast.makeText(
                    this, "Por favor, insira um email válido",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            // Tentativa de login com Firebase Authentication
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { autenticacao ->
                    if (autenticacao.isSuccessful) {
                        // Login bem-sucedido
                        val usuarioAtual = auth.currentUser
                        updateUI(usuarioAtual)
                    } else {
                        // Login falhou
                        val mensagemErro = autenticacao.exception?.message ?: "Erro ao autenticar."
                        Toast.makeText(this, mensagemErro, Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }

    override fun onStart() {
        super.onStart()
        // Verifica se o usuário já está logado
        val usuarioAtual = auth.currentUser
        if (usuarioAtual != null) {
            updateUI(usuarioAtual)
        }
    }

    private fun updateUI(user: FirebaseUser?) {
        if (user != null) {
            // Redirecionar para a tela principal
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        } else {
            Toast.makeText(this, "Erro ao autenticar! Tente novamente.", Toast.LENGTH_SHORT).show()
        }
    }
    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
}