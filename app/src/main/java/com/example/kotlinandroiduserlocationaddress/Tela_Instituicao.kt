package com.example.kotlinandroiduserlocationaddress

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.FirebaseFirestore

class Tela_Instituicao : AppCompatActivity() {

    private lateinit var salasSpinner: Spinner
    private lateinit var btnChecar: Button
    private lateinit var txtLista: TextView
    private lateinit var fabAdd: FloatingActionButton

    private val db = FirebaseFirestore.getInstance()
    private val salasList = mutableListOf<Sala>() // Lista de objetos Sala
    private var salaSelecionada: Sala? = null // Sala atualmente selecionada

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tela_instituicao)

        // Inicializar as views
        salasSpinner = findViewById(R.id.salasSpinner)
        btnChecar = findViewById(R.id.btnChecar)
        txtLista = findViewById(R.id.txtLista)
        fabAdd = findViewById(R.id.FabAdd)

        // Buscar salas do Firestore e preencher o Spinner
        carregarSalas()

        // Configurar evento do Spinner
        salasSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                salaSelecionada = salasList[position]
                Toast.makeText(this@Tela_Instituicao, "Sala Selecionada: ${salaSelecionada?.nome}", Toast.LENGTH_SHORT).show()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                salaSelecionada = null
            }
        }

        // Configurar evento do botão "Checar Lista"
        btnChecar.setOnClickListener {
            if (salaSelecionada != null) {
                txtLista.text = "Carregando dados para a sala: ${salaSelecionada?.nome}..."
                fetchUserAndPresenceData(db, txtLista)
            } else {
                Toast.makeText(this, "Por favor, selecione uma sala.", Toast.LENGTH_SHORT).show()
            }
        }

        // Configurar botão flutuante para adicionar sala
        fabAdd.setOnClickListener {
            val intent = Intent(this, Tela_Registrar_Sala::class.java)
            startActivity(intent)
        }
    }

    private fun carregarSalas() {
        db.collection("Salas")
            .get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.isEmpty) {
                    salasList.clear()
                    for (document in snapshot) {
                        val sala = Sala(
                            id = document.id,
                            nome = document.getString("nome") ?: "Sala Desconhecida",
                            latitude = document.getDouble("lat") ?: 0.0,
                            longitude = document.getDouble("long") ?: 0.0,
                            raio = document.getDouble("raio") ?: 20.0
                        )
                        salasList.add(sala)
                    }

                    // Preencher o Spinner com os nomes das salas
                    val nomesSalas = salasList.map { it.nome }
                    val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, nomesSalas)
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    salasSpinner.adapter = adapter
                } else {
                    Toast.makeText(this, "Nenhuma sala encontrada.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Erro ao carregar salas: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun fetchUserAndPresenceData(db: FirebaseFirestore, txtLista: TextView) {
        val usuariosCollection = db.collection("Usuários")
        val presencasCollection = db.collection("Presenças")

        // Buscar todos os usuários
        usuariosCollection.get()
            .addOnSuccessListener { usuariosSnapshot ->
                if (!usuariosSnapshot.isEmpty) {
                    val usuariosMap = mutableMapOf<String, String>() // UID -> Nome
                    for (document in usuariosSnapshot) {
                        val uid = document.id
                        val nome = document.getString("Nome") ?: "Nome desconhecido"
                        usuariosMap[uid] = nome
                    }

                    // Agora buscar as presenças
                    presencasCollection.get()
                        .addOnSuccessListener { presencasSnapshot ->
                            if (!presencasSnapshot.isEmpty) {
                                val builder = StringBuilder()
                                builder.append("Lista de Alunos e Presenças na sala: ${salaSelecionada?.nome}\n\n")

                                for (document in presencasSnapshot) {
                                    val uid = document.getString("uid") ?: "UID desconhecido"
                                    val presente = document.getBoolean("presente") ?: false
                                    val timestamp = document.getString("horário") ?: "Horário não registrado"

                                    val nome = usuariosMap[uid] ?: "Nome desconhecido"
                                    val status = if (presente) "Presente" else "Faltou"

                                    builder.append("Nome: $nome\n")
                                    builder.append("Status: $status\n")
                                    builder.append("Horário: $timestamp\n\n")
                                }

                                // Atualiza o TextView com os dados
                                txtLista.text = builder.toString()
                            } else {
                                txtLista.text = "Nenhum registro de presença encontrado."
                            }
                        }
                        .addOnFailureListener { e ->
                            txtLista.text = "Erro ao buscar presenças: ${e.message}"
                        }
                } else {
                    txtLista.text = "Nenhum usuário encontrado."
                }
            }
            .addOnFailureListener { e ->
                txtLista.text = "Erro ao buscar usuários: ${e.message}"
            }
    }
}

data class Sala(
    val id: String,
    val nome: String,
    val latitude: Double,
    val longitude: Double,
    val raio: Double
)