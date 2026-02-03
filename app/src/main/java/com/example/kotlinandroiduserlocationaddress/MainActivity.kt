package com.example.kotlinandroiduserlocationaddress

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.IntentFilter
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity() {

    private var btnGetAddress: Button? = null
    private var btnLogout: Button? = null
    private var txtResult: TextView? = null
    private var progressBar: ProgressBar? = null

    private lateinit var ddSala: Spinner
    private val salasList = mutableListOf<Sala>() // Lista de objetos Sala
    private var salaSelecionada: Sala? = null // Sala selecionada

    private val foregroundLocationPermissions = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    private var permissionManager: PermissionManager? = null
    private var locationManager: LocationManager? = null
    private var localBroadcastIntentFilter: IntentFilter? = null

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    companion object {
        private const val COL_SALAS = "Salas"
        private const val COL_PRESENCAS = "Presenças"

        // Campos para vincular a sala
        private const val FIELD_SALA_ID = "salaId"
        private const val FIELD_SALA_NOME = "salaNome"
    }

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        ddSala = findViewById(R.id.ddSala)
        btnGetAddress = findViewById(R.id.btnGetAddress)
        btnLogout = findViewById(R.id.btnDeslogar)
        txtResult = findViewById(R.id.txtResult)
        progressBar = findViewById(R.id.progressBar)

        carregarSalas()

        ddSala.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position in salasList.indices) {
                    salaSelecionada = salasList[position]
                    Toast.makeText(
                        this@MainActivity,
                        "Sala Selecionada: ${salaSelecionada?.nome}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                salaSelecionada = null
            }
        }

        permissionManager = PermissionManager.getInstance(this)
        locationManager = LocationManager.getInstance(this)

        localBroadcastIntentFilter = IntentFilter()
        localBroadcastIntentFilter?.addAction("foreground_location")

        btnGetAddress?.setOnClickListener {
            handleLocationAndSavePresence()
        }

        btnLogout?.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            val intent = Intent(this, Tela_Login::class.java)
            startActivity(intent)
            finish()
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
                    ddSala.adapter = adapter

                    // Opcional: selecionar a primeira automaticamente
                    if (salasList.isNotEmpty()) {
                        salaSelecionada = salasList[0]
                    }
                } else {
                    Toast.makeText(this, "Nenhuma sala encontrada.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Erro ao carregar salas: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun handleLocationAndSavePresence() {
        val sala = salaSelecionada
        if (sala == null) {
            Toast.makeText(this, "Selecione uma sala antes de registrar a presença.", Toast.LENGTH_SHORT).show()
            return
        }

        val geocoder = Geocoder(this@MainActivity, Locale.getDefault())

        if (!permissionManager!!.checkPermissions(foregroundLocationPermissions)) {
            permissionManager?.askPermissions(this@MainActivity, foregroundLocationPermissions, 100)
            return
        }

        if (!locationManager!!.isLocationEnabled) {
            locationManager?.createLocationRequest()
            return
        }

        showProgressBar()

        val location: Location? = locationManager?.lastLocation
        if (location == null) {
            hideProgressBar()
            Toast.makeText(this@MainActivity, "Aguarde! Encontrando sua localização...", Toast.LENGTH_SHORT).show()
            Toast.makeText(this@MainActivity, "Tente novamente.", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
            val address = addresses?.get(0)

            val strAddress = """
                Endereço: ${address?.getAddressLine(0)}
                Cidade: ${address?.locality}
                Estado: ${address?.adminArea}
                País: ${address?.countryName}
                CEP: ${address?.postalCode}
            """.trimIndent()

            txtResult?.text = strAddress

            val isInsideRoom = checkUserLocation(
                currentLat = location.latitude,
                currentLng = location.longitude,
                sala = sala
            )

            savePresenceToFirestore(
                isInsideRoom = isInsideRoom,
                lat = location.latitude,
                lng = location.longitude,
                sala = sala
            )

        } catch (e: IOException) {
            e.printStackTrace()
            hideProgressBar()
            Toast.makeText(this@MainActivity, "Erro ao obter o endereço.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkUserLocation(currentLat: Double, currentLng: Double, sala: Sala): Boolean {
        val roomLat = sala.latitude
        val roomLng = sala.longitude

        // ✅ usa o raio da sala do Firestore
        val radius = sala.raio
        val distance = calculateDistance(currentLat, currentLng, roomLat, roomLng)

        Toast.makeText(this@MainActivity, "Distância: $distance m", Toast.LENGTH_LONG).show()

        return distance <= radius
    }

    /**
     * ✅ Salva a presença vinculada à sala
     * ✅ Evita duplicação: 1 presença por usuário + sala + dia (documentId fixo)
     */
    private fun savePresenceToFirestore(isInsideRoom: Boolean, lat: Double, lng: Double, sala: Sala) {
        val user = auth.currentUser
        if (user == null) {
            Toast.makeText(this, "Usuário não autenticado. Faça login novamente.", Toast.LENGTH_SHORT).show()
            hideProgressBar()
            return
        }

        val hoje = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val docId = "${user.uid}_${sala.id}_$hoje" // ✅ chave única do dia

        val presenceData = hashMapOf(
            "uid" to user.uid,
            "presente" to isInsideRoom,
            "latitude" to lat,
            "longitude" to lng,

            // ✅ vínculo com a sala
            FIELD_SALA_ID to sala.id,
            FIELD_SALA_NOME to sala.nome,

            // ✅ para filtro por dia (se quiser depois)
            "data" to hoje,

            // ✅ melhor para ordenar e comparar
            "horario" to Timestamp.now(),

            // (Opcional) string formatada para exibir fácil
            "horarioTexto" to SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        )

        db.collection(COL_PRESENCAS)
            .document(docId)
            .set(presenceData) // ✅ sobrescreve (não duplica)
            .addOnCompleteListener { task ->
                hideProgressBar()
                if (task.isSuccessful) {
                    val status = if (isInsideRoom) "Presente" else "Faltou"
                    Toast.makeText(this, "Presença registrada: $status", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Erro ao salvar presença no Firestore.", Toast.LENGTH_SHORT).show()
                    Log.e("FirestoreError", "Erro: ${task.exception?.message}")
                }
            }
            .addOnFailureListener { exception ->
                hideProgressBar()
                Toast.makeText(this, "Erro ao salvar presença: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadius = 6371000.0 // metros

        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val a = kotlin.math.sin(dLat / 2) * kotlin.math.sin(dLat / 2) +
                kotlin.math.cos(Math.toRadians(lat1)) * kotlin.math.cos(Math.toRadians(lat2)) *
                kotlin.math.sin(dLon / 2) * kotlin.math.sin(dLon / 2)

        val c = 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))

        return earthRadius * c
    }

    private fun showProgressBar() {
        progressBar?.visibility = View.VISIBLE
    }

    private fun hideProgressBar() {
        progressBar?.visibility = View.GONE
    }
}
