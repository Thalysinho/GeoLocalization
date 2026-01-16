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
    private var salaSelecionada: Sala? = null // Sala atualmente selecionada

    private val foregroundLocationPermissions = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    private var permissionManager: PermissionManager? = null
    private var locationManager: LocationManager? = null
    private var localBroadcastIntentFilter: IntentFilter? = null

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

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

        // Buscar salas do Firestore e preencher o Spinner
        carregarSalas()

        // Configurar evento do Spinner
        ddSala.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                salaSelecionada = salasList[position]
                Toast.makeText(this@MainActivity, "Sala Selecionada: ${salaSelecionada?.nome}", Toast.LENGTH_SHORT).show()
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
        db.collection("Salas")
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

                    // Preencher o Spinner com os nomes das salas
                    val nomesSalas = salasList.map { it.nome }
                    val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, nomesSalas)
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    ddSala.adapter = adapter
                } else {
                    Toast.makeText(this, "Nenhuma sala encontrada.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Erro ao carregar salas: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun handleLocationAndSavePresence() {
        val geocoder = Geocoder(this@MainActivity, Locale.getDefault())

        if (!permissionManager!!.checkPermissions(foregroundLocationPermissions)) {
            permissionManager?.askPermissions(this@MainActivity, foregroundLocationPermissions, 100)
        } else {
            if (locationManager!!.isLocationEnabled) {
                showProgressBar()
                val location: Location? = locationManager?.lastLocation
                if (location != null) {
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

                        // Verificar se o usuário está dentro ou fora da sala
                        val isInsideRoom = checkUserLocation(location.latitude, location.longitude)

                        // Salvar no Firestore
                        savePresenceToFirestore(isInsideRoom, location.latitude, location.longitude)

                    } catch (e: IOException) {
                        e.printStackTrace()
                        hideProgressBar()
                        Toast.makeText(this@MainActivity, "Erro ao obter o endereço.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    hideProgressBar()
                    Toast.makeText(this@MainActivity, "Aguarde! Encontrando sua localização...", Toast.LENGTH_SHORT).show()
                    Toast.makeText(this@MainActivity, "Tente novamente.", Toast.LENGTH_SHORT).show()
                }
            } else {
                locationManager?.createLocationRequest()
            }
        }
    }

    private fun checkUserLocation(currentLat: Double, currentLng: Double): Boolean {
        // Latitude central (Unisanta)
        // val roomLat = -23.96419
        // val roomLng = -46.32137

        // casa
        // val roomLat = -24.2060031
        // val roomLng = -46.8345173

        val roomLat = salaSelecionada!!.latitude
        val roomLng = salaSelecionada!!.longitude

        val radius = 20 // Em metros
        val distance = calculateDistance(currentLat, currentLng, roomLat, roomLng)

        Toast.makeText(this@MainActivity, "Distância: ${distance}", Toast.LENGTH_LONG).show()

        return distance <= radius
    }

    private fun savePresenceToFirestore(isInsideRoom: Boolean, lat: Double, lng: Double) {
        val user = auth.currentUser
        if (user == null) {
            Toast.makeText(this, "Usuário não autenticado. Faça login novamente.", Toast.LENGTH_SHORT).show()
            hideProgressBar()
            return
        }

        val presenceData = hashMapOf(
            "uid" to user.uid,
            "presente" to isInsideRoom,
            "latitude" to lat,
            "longitude" to lng,
            "horário" to SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        )

        db.collection("Presenças")
            .add(presenceData)
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
        val earthRadius = 6371000.0 // Earth's radius in meters

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