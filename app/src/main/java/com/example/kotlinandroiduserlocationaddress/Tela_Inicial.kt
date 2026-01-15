package com.example.kotlinandroiduserlocationaddress

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.card.MaterialCardView

class Tela_Inicial : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tela_inicial)

        val cardAluno = findViewById<MaterialCardView>(R.id.cardAluno)
        val cardInstituicao = findViewById<MaterialCardView>(R.id.cardInstituicao)

        // Ação ao clicar no Card de Aluno
        cardAluno.setOnClickListener {
            Toast.makeText(this, "Aluno selecionado", Toast.LENGTH_SHORT).show()
            // Navegar para a tela do Aluno
            val intent = Intent(this, Tela_Login::class.java)
            startActivity(intent)
        }

        // Ação ao clicar no Card de Instituição
        cardInstituicao.setOnClickListener {
            Toast.makeText(this, "Instituição selecionada", Toast.LENGTH_SHORT).show()
            // Navegar para a tela da Instituição
            val intent = Intent(this, Tela_Instituicao::class.java)
            startActivity(intent)
        }
    }
}
