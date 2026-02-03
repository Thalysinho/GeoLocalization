package com.example.kotlinandroiduserlocationaddress

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore

class Tela_Registrar_Sala : AppCompatActivity() {

    private lateinit var etRegistraSala: EditText
    private lateinit var btnRegistrarSala: Button
    private lateinit var rvSalas: RecyclerView

    private val db = FirebaseFirestore.getInstance()

    private val salasList = mutableListOf<Sala>()
    private lateinit var adapter: SalaDeleteAdapter

    companion object {
        private const val COL_SALAS = "Salas"
        // Se você quiser apagar presenças junto, precisa do campo salaId:
        private const val COL_PRESENCAS = "Presenças"
        private const val FIELD_SALA_ID = "salaId"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tela_registrar_sala)

        etRegistraSala = findViewById(R.id.etRegistraSala)
        btnRegistrarSala = findViewById(R.id.btnRegistrarSala)
        rvSalas = findViewById(R.id.rvSalas)

        adapter = SalaDeleteAdapter(salasList) { sala ->
            confirmarDelete(sala)
        }

        rvSalas.layoutManager = LinearLayoutManager(this)
        rvSalas.adapter = adapter

        btnRegistrarSala.setOnClickListener {
            registrarSala()
        }

        carregarSalas()
    }

    private fun registrarSala() {
        val nome = etRegistraSala.text.toString().trim()

        if (nome.isBlank()) {
            Toast.makeText(this, "Digite o nome da sala.", Toast.LENGTH_SHORT).show()
            return
        }

        // Exemplo: cria sala somente com nome
        val data = hashMapOf(
            "nome" to nome,
            "latitude" to 0.0,
            "longitude" to 0.0,
            "raio" to 20.0
        )

        db.collection(COL_SALAS)
            .add(data)
            .addOnSuccessListener {
                etRegistraSala.setText("")
                Toast.makeText(this, "Sala cadastrada!", Toast.LENGTH_SHORT).show()
                carregarSalas()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Erro ao cadastrar: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun carregarSalas() {
        db.collection(COL_SALAS)
            .get()
            .addOnSuccessListener { snapshot ->
                val lista = snapshot.documents.map { doc ->
                    Sala(
                        id = doc.id,
                        nome = doc.getString("nome") ?: "Sala sem nome",
                        latitude = doc.getDouble("latitude") ?: 0.0,
                        longitude = doc.getDouble("longitude") ?: 0.0,
                        raio = doc.getDouble("raio") ?: 20.0
                    )
                }.sortedBy { it.nome }

                adapter.update(lista)
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Erro ao carregar salas: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun confirmarDelete(sala: Sala) {
        AlertDialog.Builder(this)
            .setTitle("Deletar sala")
            .setMessage("Tem certeza que deseja apagar a sala \"${sala.nome}\"?")
            .setPositiveButton("Apagar") { _, _ ->
                deletarSala(sala)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun deletarSala(sala: Sala) {
        db.collection(COL_SALAS)
            .document(sala.id)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(this, "Sala apagada!", Toast.LENGTH_SHORT).show()
                carregarSalas()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Erro ao apagar: ${e.message}", Toast.LENGTH_SHORT).show()
            }

        // apagar presenças dessa sala tbm
        db.collection(COL_PRESENCAS)
            .whereEqualTo(FIELD_SALA_ID, sala.id)
            .get()
            .addOnSuccessListener { presencas ->
                val batch = db.batch()
                presencas.documents.forEach { doc ->
                    batch.delete(doc.reference)
                }
                batch.commit()
            }
    }
}
