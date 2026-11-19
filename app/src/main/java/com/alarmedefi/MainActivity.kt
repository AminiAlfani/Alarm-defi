package com.alarmedefi

import android.Manifest
import android.app.AlertDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.Calendar

class MainActivity : AppCompatActivity() {

    private lateinit var listContainer: LinearLayout
    private lateinit var emptyView: TextView

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

        buildUi()
    }

    override fun onResume() {
        super.onResume()
        refreshList()
    }

    private fun buildUi() {
        val d = resources.displayMetrics.density
        val pad = (20 * d).toInt()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#101216"))
            setPadding(pad, (28 * d).toInt(), pad, pad)
        }

        // En-tête
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, 0, (16 * d).toInt())
        }
        header.addView(TextView(this).apply {
            text = "⏰ Alarme Défi"
            textSize = 30f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
        })
        header.addView(TextView(this).apply {
            text = "Multi-alarmes • Son verrouillé au maximum"
            textSize = 14f
            setTextColor(Color.parseColor("#9AA3B2"))
            setPadding(0, (4 * d).toInt(), 0, 0)
        })
        root.addView(header)

        // Barre d'actions principales
        val actionRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = (18 * d).toInt() }
        }

        val addBtn = Button(this).apply {
            text = "+ Nouvelle alarme"
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#FF3D00"))
                cornerRadius = 14 * d
            }
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.3f).apply {
                marginEnd = (8 * d).toInt()
            }
            setOnClickListener { openCreateDialog() }
        }

        val testBtn = Button(this).apply {
            text = "🔊 Tester"
            textSize = 14f
            setTextColor(Color.parseColor("#FFD600"))
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#222630"))
                cornerRadius = 14 * d
                setStroke((1 * d).toInt(), Color.parseColor("#FFD600"))
            }
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.7f)
            setOnClickListener {
                val intent = Intent(this@MainActivity, AlarmService::class.java).apply {
                    putExtra(AlarmScheduler.EXTRA_ALARM_ID, -1)
                    putExtra(AlarmScheduler.EXTRA_ALARM_LABEL, "Test Sirène Max")
                }
                ContextCompat.startForegroundService(this@MainActivity, intent)
                startActivity(Intent(this@MainActivity, ChallengeActivity::class.java).apply {
                    putExtra(AlarmScheduler.EXTRA_ALARM_ID, -1)
                    putExtra(AlarmScheduler.EXTRA_ALARM_LABEL, "Test Sirène Max")
                })
            }
        }

        actionRow.addView(addBtn)
        actionRow.addView(testBtn)
        root.addView(actionRow)

        emptyView = TextView(this).apply {
            text = "Aucune alarme programmée.
Appuie sur « + Nouvelle alarme » pour commencer."
            textSize = 15f
            setTextColor(Color.parseColor("#7A8291"))
            gravity = Gravity.CENTER
            setPadding(0, (48 * d).toInt(), 0, 0)
        }
        root.addView(emptyView)

        val scroll = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
            )
        }
        listContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        scroll.addView(listContainer)
        root.addView(scroll)

        setContentView(root)
    }

    private fun refreshList() {
        listContainer.removeAllViews()
        val alarms = AlarmRepository.getAll(this)
        emptyView.visibility = if (alarms.isEmpty()) TextView.VISIBLE else TextView.GONE

        val d = resources.displayMetrics.density
        for (alarm in alarms) {
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding((18 * d).toInt(), (16 * d).toInt(), (16 * d).toInt(), (16 * d).toInt())
                background = GradientDrawable().apply {
                    setColor(Color.parseColor("#1A1D24"))
                    cornerRadius = 18 * d
                    setStroke(
                        (1 * d).toInt(),
                        if (alarm.enabled) Color.parseColor("#FF5252") else Color.parseColor("#2B303C")
                    )
                }
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = (12 * d).toInt() }
            }

            val infoCol = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                setOnClickListener { openEditDialog(alarm) }
            }

            val timeText = TextView(this).apply {
                text = "%02d:%02d".format(alarm.hour, alarm.minute)
                textSize = 34f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(if (alarm.enabled) Color.WHITE else Color.parseColor("#6E7685"))
            }
            val labelText = TextView(this).apply {
                text = alarm.label
                textSize = 14f
                setTextColor(if (alarm.enabled) Color.parseColor("#FFD600") else Color.parseColor("#5C6370"))
            }
            infoCol.addView(timeText)
            infoCol.addView(labelText)

            val toggle = Switch(this).apply {
                isChecked = alarm.enabled
                setOnCheckedChangeListener { _, isChecked ->
                    alarm.enabled = isChecked
                    AlarmRepository.setEnabled(this@MainActivity, alarm.id, isChecked)
                    if (isChecked) {
                        AlarmScheduler.schedule(this@MainActivity, alarm)
                        Toast.makeText(
                            this@MainActivity,
                            "Activée : %02d:%02d".format(alarm.hour, alarm.minute),
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        AlarmScheduler.cancel(this@MainActivity, alarm.id)
                    }
                    refreshList()
                }
            }

            val deleteBtn = TextView(this).apply {
                text = "🗑️"
                textSize = 20f
                setPadding((14 * d).toInt(), (8 * d).toInt(), (4 * d).toInt(), (8 * d).toInt())
                setOnClickListener {
                    AlarmScheduler.cancel(this@MainActivity, alarm.id)
                    AlarmRepository.delete(this@MainActivity, alarm.id)
                    refreshList()
                }
            }

            card.addView(infoCol)
            card.addView(toggle)
            card.addView(deleteBtn)
            listContainer.addView(card)
        }
    }

    private fun openCreateDialog() {
        val now = Calendar.getInstance()
        TimePickerDialog(
            this,
            { _, hour, minute ->
                askLabelAndSave(
                    AlarmItem(
                        id = AlarmRepository.nextId(this),
                        hour = hour,
                        minute = minute,
                        label = "Réveil",
                        enabled = true
                    )
                )
            },
            now.get(Calendar.HOUR_OF_DAY),
            now.get(Calendar.MINUTE),
            true
        ).show()
    }

    private fun openEditDialog(existing: AlarmItem) {
        TimePickerDialog(
            this,
            { _, hour, minute ->
                existing.hour = hour
                existing.minute = minute
                existing.enabled = true
                askLabelAndSave(existing)
            },
            existing.hour,
            existing.minute,
            true
        ).show()
    }

    private fun askLabelAndSave(item: AlarmItem) {
        val input = EditText(this).apply {
            setText(item.label)
            hint = "Nom de l'alarme (ex: Travail, Sport...)"
        }
        AlertDialog.Builder(this)
            .setTitle("Libellé de l'alarme (%02d:%02d)".format(item.hour, item.minute))
            .setView(input)
            .setPositiveButton("Enregistrer") { _, _ ->
                val text = input.text.toString().trim()
                if (text.isNotEmpty()) item.label = text
                AlarmRepository.upsert(this, item)
                AlarmScheduler.schedule(this, item)
                refreshList()
                Toast.makeText(
                    this,
                    "Alarme réglée à %02d:%02d".format(item.hour, item.minute),
                    Toast.LENGTH_SHORT
                ).show()
            }
            .setNegativeButton("Annuler", null)
            .show()
    }
}
