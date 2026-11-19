package com.alarmedefi

import android.Manifest
import android.animation.LayoutTransition
import android.app.AlertDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.NumberPicker
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.materialswitch.MaterialSwitch
import java.util.Calendar

class MainActivity : AppCompatActivity() {

    private lateinit var listContainer: LinearLayout
    private lateinit var emptyView: LinearLayout

    private val bgBlack = Color.parseColor("#000000")
    private val cardElevated = Color.parseColor("#1C1C1E")
    private val cardSecondary = Color.parseColor("#2C2C2E")
    private val separatorGray = Color.parseColor("#38383A")
    private val labelPrimary = Color.parseColor("#FFFFFF")
    private val labelSecondary = Color.parseColor("#8E8E93")
    private val labelTertiary = Color.parseColor("#636366")
    private val systemRed = Color.parseColor("#FF453A")
    private val systemOrange = Color.parseColor("#FF9F0A")
    private val systemGreen = Color.parseColor("#30D158")

    private val dayOptions = listOf(
        Calendar.MONDAY to "L",
        Calendar.TUESDAY to "M",
        Calendar.WEDNESDAY to "M",
        Calendar.THURSDAY to "J",
        Calendar.FRIDAY to "V",
        Calendar.SATURDAY to "S",
        Calendar.SUNDAY to "D"
    )

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

    private fun dp(value: Float): Int = (value * resources.displayMetrics.density).toInt()

    private fun screenWidthDp(): Int = resources.configuration.screenWidthDp

    private fun horizontalPaddingDp(): Float = when {
        screenWidthDp() >= 600 -> 36f
        screenWidthDp() <= 360 -> 14f
        else -> 20f
    }

    private fun smoothPress(view: View) {
        view.setOnTouchListener { v, event ->
            when (event.action) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    v.animate()
                        .scaleX(0.96f)
                        .scaleY(0.96f)
                        .setDuration(110)
                        .start()
                }
                android.view.MotionEvent.ACTION_UP,
                android.view.MotionEvent.ACTION_CANCEL -> {
                    v.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(180)
                        .setInterpolator(OvershootInterpolator(1.2f))
                        .start()
                }
            }
            false
        }
    }

    private fun roundedBg(
        fillColor: Int,
        radiusDp: Float,
        strokeColor: Int? = null,
        strokeWidthDp: Float = 1f,
        withRipple: Boolean = false
    ): Drawable {
        val radiusPx = dp(radiusDp).toFloat()
        val drawable = GradientDrawable().apply {
            setShape(GradientDrawable.RECTANGLE)
            setColor(fillColor)
            cornerRadius = radiusPx
            if (strokeColor != null) {
                setStroke(dp(strokeWidthDp), strokeColor)
            }
        }
        return if (withRipple) {
            val maskDrawable = GradientDrawable().apply {
                setShape(GradientDrawable.RECTANGLE)
                setColor(Color.WHITE)
                cornerRadius = radiusPx
            }
            RippleDrawable(ColorStateList.valueOf(Color.parseColor("#33FFFFFF")), drawable, maskDrawable)
        } else {
            drawable
        }
    }

    private fun buildUi() {
        val hPad = horizontalPaddingDp()
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bgBlack)
            setPadding(dp(hPad), dp(36f), dp(hPad), dp(20f))
            layoutTransition = LayoutTransition().apply {
                enableTransitionType(LayoutTransition.CHANGING)
                setDuration(220)
            }
        }

        val topBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(10f) }
        }

        val testPill = Button(this).apply {
            text = "Tester le son max"
            textSize = if (screenWidthDp() <= 360) 12f else 13f
            isAllCaps = false
            setTextColor(systemOrange)
            background = roundedBg(cardElevated, 20f, strokeColor = separatorGray, withRipple = true)
            setPadding(dp(14f), dp(8f), dp(14f), dp(8f))
            minHeight = 0
            minimumHeight = 0
            smoothPress(this)
            setOnClickListener {
                val intent = Intent(this@MainActivity, AlarmService::class.java).apply {
                    putExtra(AlarmScheduler.EXTRA_ALARM_ID, -1)
                    putExtra(AlarmScheduler.EXTRA_ALARM_LABEL, "Alarme Défi")
                }
                ContextCompat.startForegroundService(this@MainActivity, intent)
                startActivity(Intent(this@MainActivity, ChallengeActivity::class.java).apply {
                    putExtra(AlarmScheduler.EXTRA_ALARM_ID, -1)
                    putExtra(AlarmScheduler.EXTRA_ALARM_LABEL, "Alarme Défi")
                })
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            }
        }

        val spacer = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, 1, 1f)
        }

        val addButton = Button(this).apply {
            text = "+"
            textSize = 24f
            setTextColor(Color.BLACK)
            background = roundedBg(systemOrange, 22f, withRipple = true)
            layoutParams = LinearLayout.LayoutParams(dp(44f), dp(44f))
            setPadding(0, 0, 0, 0)
            smoothPress(this)
            setOnClickListener { openCreateDialog() }
        }

        topBar.addView(testPill)
        topBar.addView(spacer)
        topBar.addView(addButton)
        root.addView(topBar)

        root.addView(TextView(this).apply {
            text = "Alarmes"
            textSize = if (screenWidthDp() <= 360) 30f else 34f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setTextColor(labelPrimary)
            letterSpacing = -0.02f
        })

        root.addView(TextView(this).apply {
            text = "Sans report • Jours récurrents • Son max"
            textSize = 13f
            setTextColor(labelSecondary)
            setPadding(0, dp(4f), 0, dp(18f))
        })

        emptyView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = roundedBg(cardElevated, 18f, strokeColor = Color.parseColor("#26FFFFFF"), strokeWidthDp = 0.75f)
            setPadding(dp(24f), dp(36f), dp(24f), dp(36f))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            addView(TextView(this@MainActivity).apply {
                text = "Aucune alarme"
                textSize = 17f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(labelPrimary)
                gravity = Gravity.CENTER
            })
            addView(TextView(this@MainActivity).apply {
                text = "Touchez + pour régler l'horloge et choisir ses jours."
                textSize = 14f
                setTextColor(labelSecondary)
                gravity = Gravity.CENTER
                setPadding(0, dp(6f), 0, 0)
            })
        }
        root.addView(emptyView)

        val scroll = ScrollView(this).apply {
            overScrollMode = View.OVER_SCROLL_NEVER
            isVerticalScrollBarEnabled = false
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
            )
        }
        listContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutTransition = LayoutTransition().apply {
                enableTransitionType(LayoutTransition.APPEARING)
                enableTransitionType(LayoutTransition.DISAPPEARING)
                enableTransitionType(LayoutTransition.CHANGING)
                setDuration(200)
            }
        }
        scroll.addView(listContainer)
        root.addView(scroll)

        setContentView(root)
    }

    private fun refreshList() {
        listContainer.removeAllViews()
        val alarms = AlarmRepository.getAll(this)
        emptyView.visibility = if (alarms.isEmpty()) View.VISIBLE else View.GONE

        val compact = screenWidthDp() <= 360
        for ((index, alarm) in alarms.withIndex()) {
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                background = roundedBg(
                    fillColor = cardElevated,
                    radiusDp = 18f,
                    strokeColor = Color.parseColor("#1AFFFFFF"),
                    strokeWidthDp = 0.75f,
                    withRipple = true
                )
                setPadding(dp(if (compact) 14f else 18f), dp(16f), dp(if (compact) 10f else 14f), dp(16f))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = dp(12f) }
                alpha = 0f
                translationY = dp(14f).toFloat()
                animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setStartDelay((index * 35L).coerceAtMost(210L))
                    .setDuration(240)
                    .start()
            }

            val infoCol = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                setOnClickListener { openEditDialog(alarm) }
            }

            val timeText = TextView(this).apply {
                text = "%02d:%02d".format(alarm.hour, alarm.minute)
                textSize = if (compact) 38f else 44f
                typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
                fontFeatureSettings = "tnum"
                setTextColor(if (alarm.enabled) labelPrimary else labelTertiary)
            }

            val metaRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, dp(2f), 0, 0)
            }

            metaRow.addView(TextView(this).apply {
                text = "${alarm.label} • ${alarm.repeatSummary()}"
                textSize = if (compact) 12.5f else 14f
                setTextColor(if (alarm.enabled) labelSecondary else labelTertiary)
            })

            if (alarm.enabled) {
                metaRow.addView(TextView(this).apply {
                    text = " • Actif"
                    textSize = 13f
                    setTextColor(systemGreen)
                })
            }

            infoCol.addView(timeText)
            infoCol.addView(metaRow)

            val toggle = MaterialSwitch(this).apply {
                isChecked = alarm.enabled
                thumbTintList = ColorStateList.valueOf(Color.WHITE)
                trackTintList = ColorStateList(
                    arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
                    intArrayOf(systemGreen, cardSecondary)
                )
                setOnCheckedChangeListener { _, isChecked ->
                    alarm.enabled = isChecked
                    AlarmRepository.setEnabled(this@MainActivity, alarm.id, isChecked)
                    if (isChecked) {
                        AlarmScheduler.schedule(this@MainActivity, alarm)
                        Toast.makeText(
                            this@MainActivity,
                            "Alarme réglée à %02d:%02d (%s)".format(alarm.hour, alarm.minute, alarm.repeatSummary()),
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        AlarmScheduler.cancel(this@MainActivity, alarm.id)
                    }
                    refreshList()
                }
            }

            val deleteBtn = TextView(this).apply {
                text = "✕"
                textSize = 15f
                setTextColor(labelSecondary)
                gravity = Gravity.CENTER
                background = roundedBg(cardSecondary, 16f, withRipple = true)
                layoutParams = LinearLayout.LayoutParams(dp(32f), dp(32f)).apply {
                    marginStart = dp(8f)
                }
                smoothPress(this)
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

    private fun styleTimePickerDialog(dialog: TimePickerDialog) {
        dialog.window?.setBackgroundDrawable(roundedBg(cardElevated, 22f, strokeColor = separatorGray))
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.apply {
                setTextColor(systemOrange)
                typeface = Typeface.DEFAULT_BOLD
            }
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.apply {
                setTextColor(labelSecondary)
            }
            stylePickersRecursively(dialog.window?.decorView)
        }
    }

    private fun stylePickersRecursively(view: View?) {
        if (view == null) return
        if (view is NumberPicker) {
            view.setTextColor(labelPrimary)
            view.setFormatter { value -> "%02d".format(value) }
        }
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                stylePickersRecursively(view.getChildAt(i))
            }
        }
    }

    private fun openCreateDialog() {
        val now = Calendar.getInstance()
        val picker = TimePickerDialog(
            this,
            R.style.Theme_AlarmeDefi_TimePickerDialog,
            { _, hour, minute ->
                openConfigDialog(
                    AlarmItem(
                        id = AlarmRepository.nextId(this),
                        hour = hour,
                        minute = minute,
                        label = "Réveil",
                        enabled = true,
                        repeatDays = mutableSetOf()
                    )
                )
            },
            now.get(Calendar.HOUR_OF_DAY),
            now.get(Calendar.MINUTE),
            true
        )
        styleTimePickerDialog(picker)
        picker.show()
    }

    private fun openEditDialog(existing: AlarmItem) {
        val picker = TimePickerDialog(
            this,
            R.style.Theme_AlarmeDefi_TimePickerDialog,
            { _, hour, minute ->
                existing.hour = hour
                existing.minute = minute
                existing.enabled = true
                openConfigDialog(existing)
            },
            existing.hour,
            existing.minute,
            true
        )
        styleTimePickerDialog(picker)
        picker.show()
    }

    private fun openConfigDialog(item: AlarmItem) {
        val selectedDays = item.repeatDays.toMutableSet()
        val compact = screenWidthDp() <= 360
        val daySize = if (compact) 32f else 36f

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20f), dp(14f), dp(20f), dp(8f))
        }

        val labelInput = EditText(this).apply {
            setText(item.label)
            hint = "Libellé (ex. Travail, Sport)"
            setTextColor(labelPrimary)
            setHintTextColor(labelSecondary)
            background = roundedBg(cardSecondary, 12f)
            setPadding(dp(14f), dp(12f), dp(14f), dp(12f))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(16f) }
        }
        container.addView(labelInput)

        container.addView(TextView(this).apply {
            text = "Jours de répétition"
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(labelSecondary)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(10f) }
        })

        val daysRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(12f) }
        }

        val dayButtons = mutableListOf<Pair<Int, Button>>()

        fun styleDayButton(btn: Button, active: Boolean) {
            btn.setTextColor(if (active) Color.BLACK else labelPrimary)
            btn.background = roundedBg(
                fillColor = if (active) systemOrange else cardSecondary,
                radiusDp = daySize / 2f,
                withRipple = true
            )
            btn.animate().scaleX(if (active) 1.05f else 1f).scaleY(if (active) 1.05f else 1f).setDuration(140).start()
        }

        for ((dayConst, shortLetter) in dayOptions) {
            val isSelected = dayConst in selectedDays
            val dayBtn = Button(this).apply {
                text = shortLetter
                textSize = 14f
                typeface = Typeface.DEFAULT_BOLD
                isAllCaps = false
                setPadding(0, 0, 0, 0)
                minWidth = 0
                minimumWidth = 0
                minHeight = 0
                minimumHeight = 0
                layoutParams = LinearLayout.LayoutParams(dp(daySize), dp(daySize)).apply {
                    marginEnd = if (dayConst != Calendar.SUNDAY) dp(if (compact) 4f else 6f) else 0
                }
                styleDayButton(this, isSelected)
                setOnClickListener {
                    if (dayConst in selectedDays) {
                        selectedDays.remove(dayConst)
                        styleDayButton(this, false)
                    } else {
                        selectedDays.add(dayConst)
                        styleDayButton(this, true)
                    }
                }
            }
            dayButtons.add(dayConst to dayBtn)
            daysRow.addView(dayBtn)
        }
        container.addView(daysRow)

        val presetsRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }

        fun applyPreset(days: Set<Int>) {
            selectedDays.clear()
            selectedDays.addAll(days)
            for ((dayConst, btn) in dayButtons) {
                styleDayButton(btn, dayConst in selectedDays)
            }
        }

        val weekdaysSet = setOf(
            Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY,
            Calendar.THURSDAY, Calendar.FRIDAY
        )
        val allDaysSet = weekdaysSet + setOf(Calendar.SATURDAY, Calendar.SUNDAY)

        fun presetChip(title: String, target: Set<Int>): Button = Button(this).apply {
            text = title
            textSize = 12f
            isAllCaps = false
            setTextColor(systemOrange)
            background = roundedBg(cardElevated, 14f, strokeColor = separatorGray, withRipple = true)
            setPadding(dp(10f), dp(6f), dp(10f), dp(6f))
            minHeight = 0
            minimumHeight = 0
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { marginEnd = dp(8f) }
            smoothPress(this)
            setOnClickListener { applyPreset(target) }
        }

        presetsRow.addView(presetChip("Lun–Ven", weekdaysSet))
        presetsRow.addView(presetChip("Tous les jours", allDaysSet))
        presetsRow.addView(presetChip("1 fois", emptySet()))
        container.addView(presetsRow)

        val dialog = AlertDialog.Builder(this, R.style.Theme_AlarmeDefi_TimePickerDialog)
            .setTitle("%02d:%02d — Configuration".format(item.hour, item.minute))
            .setView(container)
            .setPositiveButton("Enregistrer") { _, _ ->
                val text = labelInput.text.toString().trim()
                if (text.isNotEmpty()) item.label = text
                item.repeatDays = selectedDays
                AlarmRepository.upsert(this, item)
                AlarmScheduler.schedule(this, item)
                refreshList()
                Toast.makeText(
                    this,
                    "Alarme réglée à %02d:%02d (%s)".format(item.hour, item.minute, item.repeatSummary()),
                    Toast.LENGTH_SHORT
                ).show()
            }
            .setNegativeButton("Annuler", null)
            .create()

        dialog.window?.setBackgroundDrawable(roundedBg(cardElevated, 22f, strokeColor = separatorGray))
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.apply {
                setTextColor(systemOrange)
                typeface = Typeface.DEFAULT_BOLD
            }
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.apply {
                setTextColor(labelSecondary)
            }
        }
        dialog.show()
    }
}
