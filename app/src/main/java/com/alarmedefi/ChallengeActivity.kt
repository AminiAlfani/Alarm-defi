package com.alarmedefi

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
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
    private var alarmLabel = "RÉVEIL !"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        alarmId = intent.getIntExtra(AlarmScheduler.EXTRA_ALARM_ID, -1)
        alarmLabel = intent.getStringExtra(AlarmScheduler.EXTRA_ALARM_LABEL) ?: "RÉVEIL !"

        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            (getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager)
                .requestDismissKeyguard(this, null)
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { /* bloqué volontairement */ }
        })

        buildUi()
        newChallenge()
    }

    // Intercepte les boutons physiques Volume Bas / Volume Haut pour remettre immédiatement au max
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            startService(Intent(this, AlarmService::class.java).apply {
                action = AlarmService.ACTION_ESCALATE
            })
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun buildUi() {
        val d = resources.displayMetrics.density
        val pad = (24 * d).toInt()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#101216"))
            setPadding(pad, pad, pad, pad)
        }

        root.addView(TextView(this).apply {
            text = "🚨 $alarmLabel"
            textSize = 28f
            setTextColor(Color.parseColor("#FF5252"))
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        })

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding((20 * d).toInt(), (24 * d).toInt(), (20 * d).toInt(), (24 * d).toInt())
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#1C1F26"))
                cornerRadius = 20 * d
                setStroke((2 * d).toInt(), Color.parseColor("#FF5252"))
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = (24 * d).toInt()
                bottomMargin = (20 * d).toInt()
            }
        }

        typeView = TextView(this).apply {
            textSize = 14f
            setTextColor(Color.parseColor("#FFD600"))
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }
        card.addView(typeView)

        questionView = TextView(this).apply {
            textSize = 22f
            setTextColor(Color.WHITE)
            typeface = Typeface.MONOSPACE
            gravity = Gravity.CENTER
            setPadding(0, (14 * d).toInt(), 0, (18 * d).toInt())
        }
        card.addView(questionView)

        input = EditText(this).apply {
            hint = "Ta réponse ici..."
            setHintTextColor(Color.parseColor("#7A8291"))
            setTextColor(Color.WHITE)
            textSize = 18f
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            setSingleLine(true)
            setPadding((16 * d).toInt(), (14 * d).toInt(), (16 * d).toInt(), (14 * d).toInt())
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#262A34"))
                cornerRadius = 12 * d
            }
        }
        card.addView(input)

        root.addView(card)

        feedbackView = TextView(this).apply {
            textSize = 15f
            setTextColor(Color.parseColor("#FF8A80"))
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, (16 * d).toInt())
        }
        root.addView(feedbackView)

        root.addView(Button(this).apply {
            text = "VALIDER ET COUPER LE SON"
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#FF3D00"))
                cornerRadius = 14 * d
            }
            setPadding((24 * d).toInt(), (16 * d).toInt(), (24 * d).toInt(), (16 * d).toInt())
            setOnClickListener { validate() }
        })

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
                AlarmRepository.setEnabled(this, alarmId, false)
                AlarmScheduler.cancel(this, alarmId)
            }
            finish()
        } else {
            failures++
            feedbackView.text = "Faux ! Volume verrouillé à 100 % (échecs : $failures)"
            startService(Intent(this, AlarmService::class.java).apply {
                action = AlarmService.ACTION_ESCALATE
            })
            newChallenge()
        }
    }
}
