package com.example.kotlinandroiduserlocationaddress

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.card.MaterialCardView

class Tela_Inicial : AppCompatActivity() {

    // senha da instituição (depois podemos jogar isso no Firestore)
    private val SENHA_INSTITUICAO = "1234"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tela_inicial)

        val cardAluno = findViewById<MaterialCardView>(R.id.cardAluno)
        val cardInstituicao = findViewById<MaterialCardView>(R.id.cardInstituicao)

        // Aluno entra direto
        cardAluno.setOnClickListener {
            startActivity(Intent(this, Tela_Login::class.java))
        }

        // Instituição pede senha
        cardInstituicao.setOnClickListener {
            pedirSenhaInstituicao()
        }
    }

    /**
     * Dialog para senha da Instituição
     */
    private fun pedirSenhaInstituicao() {
        val inputSenha = EditText(this).apply {
            hint = "Digite a senha da instituição"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            setPadding(48, 32, 48, 32)
        }

        AlertDialog.Builder(this)
            .setTitle("Acesso restrito")
            .setMessage("Informe a senha para acessar a área da instituição")
            .setView(inputSenha)
            .setPositiveButton("Entrar") { _, _ ->
                val senhaDigitada = inputSenha.text.toString()

                if (senhaDigitada == SENHA_INSTITUICAO) {
                    // senha correta
                    startActivity(
                        Intent(this, Tela_Instituicao::class.java)
                    )
                } else {
                    // senha incorreta
                    Toast.makeText(
                        this,
                        "Senha incorreta",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}
