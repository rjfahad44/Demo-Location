package com.ft.demolocation

import android.content.Intent
import android.location.Location
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import com.ft.demolocation.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var locationManager: MyLocationManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        if (locationManager == null){
            locationManager = MyLocationManager(this)
        }
        locationManager?.initialize()

        locationManager?.locationCallback = object : (Location?) -> Unit {
            override fun invoke(location: Location?) {
                binding.textView.append("\nlatitude: ${location?.latitude}\nlongitude: ${location?.longitude}")
                Log.d("locationCallback", "\nlatitude: ${location?.latitude}\nlongitude: ${location?.longitude}")
            }
        }

        locationManager?.retryPermissionCallback = object : (Boolean) -> Unit {
            override fun invoke(isGranted: Boolean) {
                binding.reTryBtn.isVisible = isGranted
            }
        }

        binding.reTryBtn.setOnClickListener {
            locationManager?.reTryPermission()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        locationManager?.onActivityResult(requestCode, resultCode, data)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        locationManager?.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

}