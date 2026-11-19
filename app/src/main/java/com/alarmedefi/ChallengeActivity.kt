package com.alarmedefi

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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

    private fun buildUi() {
        val pad = (24 * resources.displayMetrics.density).toInt()
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(pad, pad, pad, pad)
        }

        root.addView(TextView(this).apply {
            text = "⏰ RÉVEIL !"
            textSize = 30f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        })

        typeView = TextView(this).apply {
            textSize = 16f
            gravity = Gravity.CENTER
            setPadding(0, pad, 0, 0)
        }
        root.addView(typeView)

        questionView = TextView(this).apply {
            textSize = 24f
            typeface = Typeface.MONOSPACE
            gravity = Gravity.CENTER
            setPadding(0, pad / 2, 0, pad)
        }
        root.addView(questionView)

        input = EditText(this).apply {
            hint = "Ta réponse"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            setSingleLine(true)
        }
        root.addView(input)

        feedbackView = TextView(this).apply {
            textSize = 16f
            gravity = Gravity.CENTER
            setPadding(0, pad / 2, 0, pad / 2)
        }
        root.addView(feedbackView)

        root.addView(Button(this).apply {
            text = "Valider"
            setOnClickListener { validate() }
        })

        setContentView(root)
    }

    private fun newChallenge() {
        challenge = ChallengeGenerator.next()
        typeView.text = challenge.type
        questionView.text = challenge.question
        input.setText("")
    }

    private fun validate() {
        if (ChallengeGenerator.isCorrect(challenge, input.text.toString())) {
            startService(Intent(this, AlarmService::class.java).apply {
                action = AlarmService.ACTION_STOP
            })
            AlarmScheduler.cancel(this)
            finish()
        } else {
            failures++
            feedbackView.text = "Faux ! Le volume monte... (échecs : $failures)"
            startService(Intent(this, AlarmService::class.java).apply {
                action = AlarmService.ACTION_ESCALATE
            })
            newChallenge()
        }
    }
}
