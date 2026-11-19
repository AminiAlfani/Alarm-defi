package com.alarmedefi

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.KeyEvent
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity

class ChallengeActivity : AppCompatActivity() {

    private lateinit var challenge: Challenge
    private lateinit var typeView: TextView
    private lateinit var questionView: TextView
    private lateinit var feedbackView: TextView
    private lateinit var input: EditText
    private var failures = 0
    private var alarmId = -1
    private var alarmLabel = "Réveil"

    private val bgBlack = Color.parseColor("#000000")
    private val cardElevated = Color.parseColor("#1C1C1E")
    private val cardField = Color.parseColor("#2C2C2E")
    private val labelPrimary = Color.parseColor("#FFFFFF")
    private val labelSecondary = Color.parseColor("#8E8E93")
    private val systemRed = Color.parseColor("#FF453A")
    private val systemOrange = Color.parseColor("#FF9F0A")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        alarmId = intent.getIntExtra(AlarmScheduler.EXTRA_ALARM_ID, -1)
        alarmLabel = intent.getStringExtra(AlarmScheduler.EXTRA_ALARM_LABEL) ?: "Réveil"

        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            (getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager)
                .requestDismissKeyguard(this, null)
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { /* pas de report / snooze */ }
        })

        buildUi()
        newChallenge()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            startService(Intent(this, AlarmService::class.java).apply {
                action = AlarmService.ACTION_ESCALATE
            })
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun dp(value: Float): Int = (value * resources.displayMetrics.density).toInt()

    private fun roundedBg(
        fillColor: Int,
        radiusDp: Float,
        strokeColor: Int? = null,
        withRipple: Boolean = false
    ): android.graphics.drawable.Drawable {
        val shape = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(fillColor)
            cornerRadius = dp(radiusDp).toFloat()
            if (strokeColor != null) setStroke(dp(1f), strokeColor)
        }
        return if (withRipple) {
            val mask = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                setColor(Color.WHITE)
                cornerRadius = dp(radiusDp).toFloat()
            }
            RippleDrawable(ColorStateList.valueOf(Color.parseColor("#33FFFFFF")), shape, mask)
        } else {
            shape
        }
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(bgBlack)
            setPadding(dp(24f), dp(32f), dp(24f), dp(32f))
        }

        root.addView(TextView(this).apply {
            text = alarmLabel
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(systemRed)
            gravity = Gravity.CENTER
        })

        root.addView(TextView(this).apply {
            text = "Résolvez le défi"
            textSize = 28f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setTextColor(labelPrimary)
            gravity = Gravity.CENTER
            setPadding(0, dp(4f), 0, dp(24f))
        })

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = roundedBg(cardElevated, 20f)
            setPadding(dp(20f), dp(24f), dp(20f), dp(24f))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(16f) }
        }

        typeView = TextView(this).apply {
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(systemOrange)
            gravity = Gravity.CENTER
        }
        card.addView(typeView)

        questionView = TextView(this).apply {
            textSize = 22f
            typeface = Typeface.MONOSPACE
            setTextColor(labelPrimary)
            gravity = Gravity.CENTER
            setPadding(0, dp(12f), 0, dp(20f))
        }
        card.addView(questionView)

        input = EditText(this).apply {
            hint = "Votre réponse"
            setHintTextColor(labelSecondary)
            setTextColor(labelPrimary)
            textSize = 17f
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            setSingleLine(true)
            background = roundedBg(cardField, 12f)
            setPadding(dp(16f), dp(14f), dp(16f), dp(14f))
        }
        card.addView(input)

        root.addView(card)

        feedbackView = TextView(this).apply {
            textSize = 14f
            setTextColor(systemRed)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(16f))
        }
        root.addView(feedbackView)

        val confirmBtn = Button(this).apply {
            text = "Arrêter l'alarme"
            textSize = 17f
            isAllCaps = false
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(labelPrimary)
            background = roundedBg(systemRed, 14f, withRipple = true)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(54f)
            )
            setOnClickListener { validate() }
        }
        root.addView(confirmBtn)

        setContentView(root)
    }

    private fun newChallenge() {
        challenge = ChallengeGenerator.next()
        typeView.text = challenge.type.uppercase()
        questionView.text = challenge.question
        input.setText("")
    }

    private fun validate() {
        if (ChallengeGenerator.isCorrect(challenge, input.text.toString())) {
            startService(Intent(this, AlarmService::class.java).apply {
                action = AlarmService.ACTION_STOP
            })
            if (alarmId != -1) {
                val alarm = AlarmRepository.getAll(this).find { it.id == alarmId }
                if (alarm != null && alarm.repeatDays.isNotEmpty()) {
                    alarm.enabled = true
                    AlarmRepository.upsert(this, alarm)
                    AlarmScheduler.schedule(this, alarm)
                } else {
                    AlarmRepository.setEnabled(this, alarmId, false)
                    AlarmScheduler.cancel(this, alarmId)
                }
            }
            finish()
        } else {
            failures++
            feedbackView.text = "Réponse incorrecte • Volume à 100 % ($failures)"
            startService(Intent(this, AlarmService::class.java).apply {
                action = AlarmService.ACTION_ESCALATE
            })
            newChallenge()
        }
    }
}
