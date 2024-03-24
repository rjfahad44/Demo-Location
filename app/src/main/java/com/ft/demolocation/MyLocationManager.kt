package com.ft.demolocation

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.Priority

class MyLocationManager(private val context: Activity) {

    private var fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
    private var locationManager: LocationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    var locationCallback: ((location: Location?)-> Unit)? = null
    var retryPermissionCallback: ((isGranted: Boolean)-> Unit)? = null
    private val prefs = context.getSharedPreferences("LOCATION_DB", Context.MODE_PRIVATE)

    fun initialize() {
        if (!isLocationEnabled()) {
            enableLocation()
        } else {
            getLastLocation()
        }
    }

    fun reTryPermission() {
        if (!isLocationEnabled()) {
            enableLocation()
        } else {
            if (prefs.getBoolean(PERMISSION_COUNT, false)){
                openAppPermissionSettings()
            }else{
                getLastLocation()
            }
        }
    }

    private fun enableLocation() {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 0).apply {
            setMinUpdateDistanceMeters(1f)
            setWaitForAccurateLocation(true)
        }.build()

        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)

        LocationServices.getSettingsClient(context)
            .checkLocationSettings(builder.build())
            .addOnSuccessListener { response ->
                Log.d("getLastLocation", "addOnSuccessListener => response : $response")
                getLastLocation()
            }
            .addOnFailureListener { ex ->
                Log.d("getLastLocation", "addOnFailureListener => ex : $ex")
                if (ex is ResolvableApiException) {
                    try {
                        ex.startResolutionForResult(context, PERMISSIONS_REQUEST_LOCATION)
                    } catch (_: IntentSender.SendIntentException) { }
                }
            }
    }

    @SuppressLint("MissingPermission")
    private fun getLastLocation() {
        if (
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                context,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                PERMISSIONS_REQUEST_LOCATION
            )
        } else {
            retryPermissionCallback?.invoke(false)
            prefs.edit().clear().apply()
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    locationCallback?.invoke(location)
                }
                .addOnFailureListener { e ->
                    locationCallback?.invoke(null)
                }
        }
    }

    private fun openAppPermissionSettings() {
        context.startActivityForResult(Intent().apply {
            action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
            val uri = Uri.fromParts("package", context.packageName, null)
            data = uri
        }, PERMISSIONS_RETRY_LOCATION)
        Toast.makeText(context, "Goto Permissions -> Location -> Allow", Toast.LENGTH_LONG).show()
    }

    private fun isLocationEnabled(): Boolean {
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == PERMISSIONS_REQUEST_LOCATION && resultCode == Activity.RESULT_OK) {
            Log.d("getLastLocation", "onActivityResult if")
            getLastLocation()
            retryPermissionCallback?.invoke(true)
        }else{
            if (prefs.getBoolean(PERMISSION_COUNT, false)){
                retryPermissionCallback?.invoke(true)
                getLastLocation()
            }else{
                retryPermissionCallback?.invoke(false)
            }
            Log.d("getLastLocation", "onActivityResult else")
        }
    }

    fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == PERMISSIONS_REQUEST_LOCATION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLastLocation()
                retryPermissionCallback?.invoke(false)
                Log.d("getLastLocation", "onRequestPermissionsResult if")
            } else {
                Log.d("getLastLocation", "onRequestPermissionsResult else")
                Toast.makeText(context, "Location permission is required for this feature", Toast.LENGTH_LONG).show()
                prefs.edit().putBoolean(PERMISSION_COUNT, true).apply()
                retryPermissionCallback?.invoke(true)
            }
        }
    }


    companion object {
        private const val PERMISSIONS_REQUEST_LOCATION = 100
        private const val PERMISSIONS_RETRY_LOCATION = 101
        private const val PERMISSION_COUNT = "PERMISSION_COUNT"
    }
}