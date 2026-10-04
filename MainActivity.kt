package com.example.altitudegps

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.widget.*
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    private lateinit var fused: FusedLocationProviderClient
    private lateinit var altitude: TextView
    private lateinit var accuracy: TextView
    private lateinit var coords: TextView
    private lateinit var status: TextView
    private var feet = true
    private val samples = ArrayDeque<Double>()
    private val maxSamples = 8

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main)
        altitude = findViewById(R.id.altitude)
        accuracy = findViewById(R.id.accuracy)
        coords = findViewById(R.id.coords)
        status = findViewById(R.id.status)
        fused = LocationServices.getFusedLocationProviderClient(this)

        findViewById<Button>(R.id.unitButton).setOnClickListener { feet = !feet; lastLocation?.let { showLocation(it) } }
        findViewById<Button>(R.id.refreshButton).setOnClickListener { requestLocation() }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 10)
        } else requestLocation()
    }

    private var lastLocation: Location? = null

    private fun requestLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return
        status.text = "Getting GPS location…"
        fused.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { loc ->
                if (loc != null) { lastLocation = loc; showLocation(loc) }
                else status.text = "No GPS fix yet. Try again outdoors."
            }
            .addOnFailureListener { status.text = "GPS error: ${it.localizedMessage ?: "unknown error"}" }
    }

    private fun showLocation(loc: Location) {
        val a = loc.altitude
        samples.addLast(a)
        while (samples.size > maxSamples) samples.removeFirst()
        val avg = samples.average()
        val value = if (feet) avg * 3.28084 else avg
        altitude.text = "${value.roundToInt()} ${if (feet) "ft" else "m"}"
        accuracy.text = "GPS altitude accuracy: " + if (loc.hasVerticalAccuracy()) "±${loc.verticalAccuracyMeters.roundToInt()} m" else "not reported"
        coords.text = "Lat ${"%.6f".format(loc.latitude)}   Lon ${"%.6f".format(loc.longitude)}"
        status.text = "Updated just now • averaging ${samples.size} readings"
    }
}
