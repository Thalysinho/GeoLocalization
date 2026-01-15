package com.example.kotlinandroiduserlocationaddress

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.provider.Settings.SettingNotFoundException
import androidx.core.app.ActivityCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.LocationSettingsResponse
import com.google.android.gms.location.Priority

@Suppress("DEPRECATION")
class LocationManager private constructor() {
    private var context: Context? = null
    private var fusedLocationProviderClient: FusedLocationProviderClient? = null
    var lastLocation: Location? = null
        //gets last location
        get() {
            if (ActivityCompat.checkSelfPermission(
                    context!!,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(
                    context!!,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return null
            }
            fusedLocationProviderClient!!.lastLocation
                .addOnSuccessListener { location: Location? ->
                    if (location == null) {
                        startLocationUpdates()
                        field = null
                    } else {
                        field = location
                    }
                }
            return field
        }
        private set
    private var locationRequest: LocationRequest? = null
    private var locationCallback: LocationCallback? = null
    var foregroundLocationIntent: Intent = Intent("foreground_location")
    var backgroundLocationIntent: Intent = Intent("background_location")
    var stringBuilder: StringBuilder = StringBuilder()
    private var activity: Activity? = null

    private fun init(context: Context) {
        this.context = context
        this.fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
        if (context is Activity) {
            activity = context
        }

        //continuous updates
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
                    if (location != null) {
                        stringBuilder.setLength(0)
                        stringBuilder.append("Time: ").append(System.currentTimeMillis())
                            .append("\nLat: ").append(location.latitude).append("=>")
                            .append("Long: ").append(location.longitude)

                        foregroundLocationIntent.putExtra("location", stringBuilder.toString())
                        LocalBroadcastManager.getInstance(context)
                            .sendBroadcast(foregroundLocationIntent)

                        backgroundLocationIntent.putExtra("location", stringBuilder.toString())
                        LocalBroadcastManager.getInstance(context)
                            .sendBroadcast(backgroundLocationIntent)
                    }
                }
            }
        }

        createLocationRequest()
    }

    //starts location on the device
    fun createLocationRequest() {
        locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 8000)
            .setMinUpdateIntervalMillis(8000) // Fastest interval
            .build()

        val builder =
            LocationSettingsRequest.Builder().addLocationRequest(locationRequest!!)
        val client = LocationServices.getSettingsClient(context!!)
        val task = client.checkLocationSettings(builder.build())

        task.addOnSuccessListener { locationSettingsResponse: LocationSettingsResponse? -> }

        task.addOnFailureListener { e: Exception? ->
            if (e is ResolvableApiException) {
                // Location settings are not satisfied, but this can be fixed
                // by showing the user a dialog.
                try {
                    // Show the dialog by calling startResolutionForResult(),
                    // and check the result in onActivityResult().
                    e.startResolutionForResult(
                        activity!!,
                        REQUEST_CHECK_SETTINGS
                    )
                } catch (sendEx: Exception) {
                    // Ignore the error.
                }
            }
        }
    }

    //call this in onResume
    fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(
                context!!,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(
                context!!,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        fusedLocationProviderClient!!.requestLocationUpdates(
            locationRequest!!,
            locationCallback!!,
            Looper.getMainLooper()
        )
    }

    //call this in onPause
    fun stopLocationUpdates() {
        fusedLocationProviderClient!!.removeLocationUpdates(locationCallback!!)
    }

    val isLocationEnabled: Boolean
        //check if location is enabled on the device
        get() {
            var locationMode = 0
            var locationProviders: String

            try {
                locationMode = Settings.Secure.getInt(
                    context!!.contentResolver,
                    Settings.Secure.LOCATION_MODE
                )
            } catch (e: SettingNotFoundException) {
                e.printStackTrace()
                return false
            }

            return locationMode != Settings.Secure.LOCATION_MODE_OFF
        }

    companion object {
        private var instance: LocationManager? = null
        private const val REQUEST_CHECK_SETTINGS = 200
        fun getInstance(context: Context): LocationManager? {
            if (instance == null) {
                instance = LocationManager()
            }
            instance!!.init(context)
            return instance
        }
        class SensorActivity : Activity(), SensorEventListener {

            private lateinit var sensorManager: SensorManager
            private val accelerometerReading = FloatArray(3)
            private val magnetometerReading = FloatArray(3)

            private val rotationMatrix = FloatArray(9)
            private val orientationAngles = FloatArray(3)

            public override fun onCreate(savedInstanceState: Bundle?) {
                super.onCreate(savedInstanceState)
                setContentView(R.layout.activity_main)
                sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
            }

            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
            }
            override fun onResume() {
                super.onResume()
                sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.also { accelerometer ->
                    sensorManager.registerListener(
                        this,
                        accelerometer,
                        SensorManager.SENSOR_DELAY_NORMAL,
                        SensorManager.SENSOR_DELAY_UI
                    )
                }
                sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)?.also { magneticField ->
                    sensorManager.registerListener(
                        this,
                        magneticField,
                        SensorManager.SENSOR_DELAY_NORMAL,
                        SensorManager.SENSOR_DELAY_UI
                    )
                }
            }
            override fun onPause() {
                super.onPause()
                sensorManager.unregisterListener(this)
            }
            override fun onSensorChanged(event: SensorEvent) {
                if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                    System.arraycopy(event.values, 0, accelerometerReading, 0, accelerometerReading.size)
                } else if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
                    System.arraycopy(event.values, 0, magnetometerReading, 0, magnetometerReading.size)
                }
            }
            fun updateOrientationAngles() {
                SensorManager.getRotationMatrix(
                    rotationMatrix,
                    null,
                    accelerometerReading,
                    magnetometerReading
                )
                SensorManager.getOrientation(rotationMatrix, orientationAngles)
            }
        }
    }
}