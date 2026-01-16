package com.example.kotlinandroiduserlocationaddress

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class Tela_Registrar_Sala : AppCompatActivity() {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tela_registrar_sala)

        // Inicializar Firestore e FusedLocationProviderClient
        firestore = Firebase.firestore
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Referenciar os elementos de layout
        val registraField = findViewById<EditText>(R.id.etRegistraSala)
        val buttonRegisterRoom: Button = findViewById(R.id.btnRegistrarSala)

        // Configurar o clique do botão
        buttonRegisterRoom.setOnClickListener {
            val nomeSala = registraField.text.toString().trim()

            if (nomeSala.isEmpty()) {
                Toast.makeText(this, "Por favor, insira a sala.", Toast.LENGTH_SHORT).show()
            } else {
                getCurrentLocationAndRegisterRoom(nomeSala)
            }
        }
    }

    private fun getCurrentLocationAndRegisterRoom(nomeSala: String) {
        // Verificar permissão de localização
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
            return
        }

        // Obter localização atual
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val geoPoint = GeoPoint(location.latitude, location.longitude)
                val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                val currentTime = dateFormat.format(Date())


                // Criar dados da sala
                val roomData = hashMapOf(
                    "nome" to nomeSala, // Nome fornecido pelo usuário
                    "latitude" to location.latitude,
                    "longitude" to location.longitude,
                    "criado_em" to currentTime
                )

                // Salvar no Firestore
                firestore.collection("Salas")
                    .add(roomData)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Sala '$nomeSala' registrada com sucesso!", Toast.LENGTH_SHORT)
                            .show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(
                            this,
                            "Erro ao registrar sala: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            } else {
                Toast.makeText(this, "Não foi possível obter a localização.", Toast.LENGTH_SHORT)
                    .show()
            }
        }.addOnFailureListener { e ->
            Toast.makeText(this, "Erro ao obter localização: ${e.message}", Toast.LENGTH_SHORT)
                .show()
        }
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
    }
}