package com.alarmedefi

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.TimePicker
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1
            )
        }

        val pad = (24 * resources.displayMetrics.density).toInt()
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(pad, pad, pad, pad)
        }

        root.addView(TextView(this).apply {
            text = "Alarme Défi"
            textSize = 28f
            gravity = Gravity.CENTER
        })

        val picker = TimePicker(this).apply { setIs24HourView(true) }
        root.addView(picker)

        root.addView(Button(this).apply {
            text = "Activer l'alarme"
            setOnClickListener {
                AlarmScheduler.schedule(
                    this@MainActivity, picker.hour, picker.minute
                )
                Toast.makeText(
                    this@MainActivity,
                    "Alarme réglée à %02d:%02d".format(picker.hour, picker.minute),
                    Toast.LENGTH_LONG
                ).show()
            }
        })

        root.addView(Button(this).apply {
            text = "Annuler l'alarme"
            setOnClickListener {
                AlarmScheduler.cancel(this@MainActivity)
                Toast.makeText(this@MainActivity, "Alarme annulée", Toast.LENGTH_SHORT).show()
            }
        })

        setContentView(root)
    }
}
