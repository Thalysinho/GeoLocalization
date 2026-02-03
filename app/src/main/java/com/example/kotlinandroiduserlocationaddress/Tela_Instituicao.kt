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
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class Tela_Instituicao : AppCompatActivity() {

    private lateinit var salasSpinner: Spinner
    private lateinit var btnChecar: Button
    private lateinit var txtLista: TextView
    private lateinit var fabAdd: FloatingActionButton

    // filtro bonito
    private lateinit var toggleFiltro: MaterialButtonToggleGroup

    private val db = FirebaseFirestore.getInstance()
    private val salasList = mutableListOf<Sala>()
    private var salaSelecionada: Sala? = null

    // guardar a última sala buscada para re-filtrar sem puxar do Firestore de novo
    private var ultimaSalaConsultada: Sala? = null

    private enum class FiltroPresenca { TODOS, PRESENTES, FALTARAM }
    private var filtroAtual: FiltroPresenca = FiltroPresenca.TODOS

    companion object {
        private const val FIELD_SALA_ID = "salaId"

        private const val COL_SALAS = "Salas"
        private const val COL_USUARIOS = "Usuários"
        private const val COL_PRESENCAS = "Presenças"
    }

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tela_instituicao)

        salasSpinner = findViewById(R.id.salasSpinner)
        btnChecar = findViewById(R.id.btnChecar)
        txtLista = findViewById(R.id.txtLista)
        fabAdd = findViewById(R.id.FabAdd)

        // toggle do filtro (IDs do XML que você colocou)
        toggleFiltro = findViewById(R.id.toggleFiltro)

        // seleciona "Todos" por padrão
        toggleFiltro.check(R.id.btnFiltroTodos)

        toggleFiltro.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener

            filtroAtual = when (checkedId) {
                R.id.btnFiltroPresentes -> FiltroPresenca.PRESENTES
                R.id.btnFiltroFaltaram -> FiltroPresenca.FALTARAM
                else -> FiltroPresenca.TODOS
            }

            // se já buscou uma sala, refaz a lista com o filtro novo
            val sala = ultimaSalaConsultada
            if (sala != null) {
                fetchUserAndPresenceData(sala)
            }
        }

        carregarSalas()

        salasSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                if (position in salasList.indices) {
                    salaSelecionada = salasList[position]
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                salaSelecionada = null
            }
        }

        btnChecar.setOnClickListener {
            val sala = salaSelecionada
            if (sala != null) {
                ultimaSalaConsultada = sala
                txtLista.text = "Carregando dados..."
                fetchUserAndPresenceData(sala)
            } else {
                Toast.makeText(this, "Por favor, selecione uma sala.", Toast.LENGTH_SHORT).show()
            }
        }

        fabAdd.setOnClickListener {
            startActivity(Intent(this, Tela_Registrar_Sala::class.java))
        }
    }

    private fun carregarSalas() {
        db.collection(COL_SALAS)
            .get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.isEmpty) {
                    salasList.clear()

                    for (document in snapshot) {
                        val sala = Sala(
                            id = document.id,
                            nome = document.getString("nome") ?: "Sala Desconhecida",
                            latitude = document.getDouble("latitude") ?: 0.0,
                            longitude = document.getDouble("longitude") ?: 0.0,
                            raio = document.getDouble("raio") ?: 20.0
                        )
                        salasList.add(sala)
                    }

                    val nomesSalas = salasList.map { it.nome }
                    val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, nomesSalas)
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    salasSpinner.adapter = adapter

                    if (salasList.isNotEmpty()) salaSelecionada = salasList[0]
                } else {
                    Toast.makeText(this, "Nenhuma sala encontrada.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Erro ao carregar salas: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    /**
     * Filtra por sala (salaId)
     * Evita usuário repetido (dedup por uid, pega registro mais recente)
     * Aplica filtro: Todos / Presentes / Faltaram
     */
    private fun fetchUserAndPresenceData(sala: Sala) {
        val usuariosCollection = db.collection(COL_USUARIOS)
        val presencasCollection = db.collection(COL_PRESENCAS)

        usuariosCollection.get()
            .addOnSuccessListener { usuariosSnapshot ->
                val usuariosMap = mutableMapOf<String, String>()

                for (document in usuariosSnapshot) {
                    val uid = document.id
                    val nome = document.getString("Nome")
                        ?: document.getString("nome")
                        ?: "Nome desconhecido"
                    usuariosMap[uid] = nome
                }

                presencasCollection
                    .whereEqualTo(FIELD_SALA_ID, sala.id)
                    .get()
                    .addOnSuccessListener { presencasSnapshot ->
                        if (presencasSnapshot.isEmpty) {
                            txtLista.text = "Nenhum registro encontrado."
                            return@addOnSuccessListener
                        }

                        val presencasPorUid = linkedMapOf<String, PresencaInfo>()

                        for (document in presencasSnapshot) {
                            val uid = document.getString("uid") ?: continue
                            val presente = document.getBoolean("presente") ?: false

                            val ts: Timestamp? = document.getTimestamp("horario")
                            val horarioStr: String? = document.getString("horarioTexto")

                            val info = PresencaInfo(
                                uid = uid,
                                presente = presente,
                                timestamp = ts,
                                horarioTexto = horarioStr
                            )

                            val atual = presencasPorUid[uid]
                            if (atual == null) {
                                presencasPorUid[uid] = info
                            } else {
                                val atualMillis = atual.timestamp?.toDate()?.time
                                val novoMillis = info.timestamp?.toDate()?.time

                                val deveTrocar = when {
                                    atualMillis == null && novoMillis != null -> true
                                    atualMillis != null && novoMillis != null -> novoMillis > atualMillis
                                    else -> false
                                }

                                if (deveTrocar) presencasPorUid[uid] = info
                            }
                        }

                        // aplica filtro de presença
                        val filtrada = presencasPorUid.values.filter { p ->
                            when (filtroAtual) {
                                FiltroPresenca.TODOS -> true
                                FiltroPresenca.PRESENTES -> p.presente
                                FiltroPresenca.FALTARAM -> !p.presente
                            }
                        }

                        val listaOrdenada = filtrada.sortedBy {
                            usuariosMap[it.uid] ?: "ZZZ"
                        }

                        val builder = StringBuilder()
                        builder.append("Total: ${listaOrdenada.size}\n\n")

                        for (p in listaOrdenada) {
                            val nome = usuariosMap[p.uid] ?: "Nome desconhecido"
                            val status = if (p.presente) "Presente" else "Faltou"

                            val horarioFinal = when {
                                p.timestamp != null -> formatarDataBrasil(p.timestamp)
                                !p.horarioTexto.isNullOrBlank() -> p.horarioTexto
                                else -> "Horário não registrado"
                            }

                            builder.append("Nome: $nome\n")
                            builder.append("Status: $status\n")
                            builder.append("Horário: $horarioFinal\n\n")
                        }

                        txtLista.text = builder.toString()
                    }
                    .addOnFailureListener { e ->
                        txtLista.text = "Erro ao buscar presenças: ${e.message}\n\n" +
                                "Dica: confirme se existe o campo '$FIELD_SALA_ID' em '$COL_PRESENCAS'."
                    }
            }
            .addOnFailureListener { e ->
                txtLista.text = "Erro ao buscar usuários: ${e.message}"
            }
    }

    // ✅ Formata data no padrão BR e fuso de Brasília
    private fun formatarDataBrasil(timestamp: Timestamp): String {
        val sdf = SimpleDateFormat("EEEE, dd/MM/yyyy 'às' HH:mm", Locale("pt", "BR"))
        sdf.timeZone = TimeZone.getTimeZone("America/Sao_Paulo")
        return sdf.format(timestamp.toDate())
    }

    // (se quiser usar em algum lugar depois, pode deixar)
    private fun nomeFiltroAtual(): String {
        return when (filtroAtual) {
            FiltroPresenca.TODOS -> "Todos"
            FiltroPresenca.PRESENTES -> "Presentes"
            FiltroPresenca.FALTARAM -> "Faltaram"
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

data class PresencaInfo(
    val uid: String,
    val presente: Boolean,
    val timestamp: Timestamp?,
    val horarioTexto: String?
)
