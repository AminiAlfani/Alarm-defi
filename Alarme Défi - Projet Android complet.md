# Alarme Défi - Projet Android complet

Application Android (Kotlin) : une alarme à heure précise qu'on ne peut éteindre qu'en résolvant un défi (calcul mental, énigme facile, code PHP à recopier ou bug à corriger). À chaque échec, le volume monte.

## 1. Compiler l'APK sans installer Android Studio

1. Crée un compte gratuit sur github.com.
2. Crée un nouveau dépôt, par exemple `alarme-defi`.
3. Dans ce dépôt, crée chaque fichier de ce document avec exactement le même chemin (bouton Add file, puis Create new file, et tape le chemin complet dans le nom, les dossiers se créent tout seuls).
4. Dès que le fichier `.github/workflows/build.yml` est enregistré, GitHub compile automatiquement.
5. Va dans l'onglet Actions, ouvre la dernière exécution, et télécharge l'artefact `alarme-defi-apk` en bas de la page. Décompresse le zip : l'APK est dedans.
6. Envoie l'APK sur ton téléphone, autorise l'installation depuis une source inconnue, et installe.

## 2. Structure du projet

```
alarme-defi/
  settings.gradle.kts
  build.gradle.kts
  gradle.properties
  .github/workflows/build.yml
  app/
    build.gradle.kts
    src/main/
      AndroidManifest.xml
      java/com/alarmedefi/
        MainActivity.kt
        AlarmScheduler.kt
        AlarmReceiver.kt
        AlarmService.kt
        ChallengeActivity.kt
        ChallengeGenerator.kt
        BootReceiver.kt
      res/values/strings.xml
      res/values/themes.xml
```

## 3. Fichiers de configuration

### settings.gradle.kts

```kotlin
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "AlarmeDefi"
include(":app")
```

### build.gradle.kts (à la racine)

```kotlin
plugins {
    id("com.android.application") version "8.5.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.24" apply false
}
```

### gradle.properties

```
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
android.useAndroidX=true
kotlin.code.style=official
```

### app/build.gradle.kts

```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.alarmedefi"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.alarmedefi"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
}
```

### .github/workflows/build.yml

```yaml
name: Build APK
on:
  push:
  workflow_dispatch:

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: 17
      - uses: gradle/actions/setup-gradle@v4
        with:
          gradle-version: 8.7
      - name: Build debug APK
        run: gradle assembleDebug
      - uses: actions/upload-artifact@v4
        with:
          name: alarme-defi-apk
          path: app/build/outputs/apk/debug/app-debug.apk
```

### app/src/main/res/values/strings.xml

```xml
<resources>
    <string name="app_name">Alarme Défi</string>
</resources>
```

### app/src/main/res/values/themes.xml

```xml
<resources>
    <style name="Theme.AlarmeDefi" parent="Theme.Material3.DayNight.NoActionBar" />
</resources>
```

### app/src/main/AndroidManifest.xml

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM" />
    <uses-permission android:name="android.permission.USE_EXACT_ALARM" />
    <uses-permission android:name="android.permission.USE_FULL_SCREEN_INTENT" />
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK" />
    <uses-permission android:name="android.permission.WAKE_LOCK" />
    <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
    <uses-permission android:name="android.permission.VIBRATE" />

    <application
        android:allowBackup="true"
        android:label="@string/app_name"
        android:theme="@style/Theme.AlarmeDefi">

        <activity
            android:name=".MainActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <activity
            android:name=".ChallengeActivity"
            android:exported="false"
            android:launchMode="singleTask"
            android:showWhenLocked="true"
            android:turnScreenOn="true"
            android:excludeFromRecents="true" />

        <receiver android:name=".AlarmReceiver" android:exported="false" />

        <receiver android:name=".BootReceiver" android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.BOOT_COMPLETED" />
            </intent-filter>
        </receiver>

        <service
            android:name=".AlarmService"
            android:exported="false"
            android:foregroundServiceType="mediaPlayback" />
    </application>
</manifest>
```

*La suite arrive : le code Kotlin de l'alarme, du service sonore et des défis.*

## 4. Code Kotlin : alarme et son

Tous ces fichiers vont dans `app/src/main/java/com/alarmedefi/`.

### AlarmScheduler.kt

Planifie l'alarme à l'heure exacte et la sauvegarde pour pouvoir la reprogrammer après un redémarrage.

```kotlin
package com.alarmedefi

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.util.Calendar

object AlarmScheduler {

    private const val PREFS = "alarme_prefs"
    private const val KEY_HOUR = "hour"
    private const val KEY_MINUTE = "minute"
    private const val KEY_ENABLED = "enabled"

    fun schedule(context: Context, hour: Int, minute: Int) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit()
            .putInt(KEY_HOUR, hour)
            .putInt(KEY_MINUTE, minute)
            .putBoolean(KEY_ENABLED, true)
            .apply()

        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = pendingIntent(context)
        val info = AlarmManager.AlarmClockInfo(cal.timeInMillis, pi)
        alarmManager.setAlarmClock(info, pi)
    }

    fun cancel(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent(context))
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_ENABLED, false).apply()
    }

    fun rescheduleIfEnabled(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (prefs.getBoolean(KEY_ENABLED, false)) {
            schedule(context, prefs.getInt(KEY_HOUR, 7), prefs.getInt(KEY_MINUTE, 0))
        }
    }

    private fun pendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java)
        return PendingIntent.getBroadcast(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
```

### AlarmReceiver.kt

Reçoit le déclenchement et démarre le service qui joue le son.

```kotlin
package com.alarmedefi

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val serviceIntent = Intent(context, AlarmService::class.java)
        ContextCompat.startForegroundService(context, serviceIntent)
    }
}
```

### BootReceiver.kt

Reprogramme l'alarme quand le téléphone redémarre.

```kotlin
package com.alarmedefi

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            AlarmScheduler.rescheduleIfEnabled(context)
        }
    }
}
```

### AlarmService.kt

Joue la sonnerie en boucle, gère le volume qui monte à chaque échec, et ouvre l'écran de défi par-dessus l'écran verrouillé. Le volume monte aussi tout seul toutes les 20 secondes si tu ne réponds pas.

```kotlin
package com.alarmedefi

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import androidx.core.app.NotificationCompat

class AlarmService : Service() {

    companion object {
        const val ACTION_ESCALATE = "com.alarmedefi.ESCALATE"
        const val ACTION_STOP = "com.alarmedefi.STOP"
        private const val CHANNEL_ID = "alarme_defi"
        private const val NOTIF_ID = 42
        private const val AUTO_ESCALATE_MS = 20_000L
    }

    private var player: MediaPlayer? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private lateinit var audio: AudioManager
    private var level = 0
    private val handler = Handler(Looper.getMainLooper())

    private val autoEscalate = object : Runnable {
        override fun run() {
            raiseVolume()
            handler.postDelayed(this, AUTO_ESCALATE_MS)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopEverything()
                return START_NOT_STICKY
            }
            ACTION_ESCALATE -> {
                raiseVolume()
                return START_STICKY
            }
        }
        startAlarm()
        return START_STICKY
    }

    private fun startAlarm() {
        audio = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "alarmedefi:wake")
        wakeLock?.acquire(30 * 60 * 1000L)

        val channel = NotificationChannel(
            CHANNEL_ID, "Alarme", NotificationManager.IMPORTANCE_HIGH
        )
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .createNotificationChannel(channel)

        val challengeIntent = Intent(this, ChallengeActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val pending = PendingIntent.getActivity(
            this, 0, challengeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Réveil !")
            .setContentText("Résous le défi pour arrêter l'alarme")
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setOngoing(true)
            .setFullScreenIntent(pending, true)
            .setContentIntent(pending)
            .build()

        startForeground(NOTIF_ID, notification)

        level = 0
        setVolumeForLevel()
        playSound()
        handler.postDelayed(autoEscalate, AUTO_ESCALATE_MS)

        // L'ecran de defi s'ouvre via la notification plein ecran ci-dessus.
    }

    private fun playSound() {
        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        player = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            setDataSource(this@AlarmService, uri)
            isLooping = true
            prepare()
            start()
        }
    }

    private fun raiseVolume() {
        level++
        setVolumeForLevel()
    }

    // Niveau 0 = environ 30% du volume max, puis +12% par niveau jusqu'au maximum.
    private fun setVolumeForLevel() {
        val max = audio.getStreamMaxVolume(AudioManager.STREAM_ALARM)
        val fraction = (0.30 + level * 0.12).coerceAtMost(1.0)
        val target = (max * fraction).toInt().coerceIn(1, max)
        audio.setStreamVolume(AudioManager.STREAM_ALARM, target, 0)
    }

    private fun stopEverything() {
        handler.removeCallbacks(autoEscalate)
        player?.run {
            if (isPlaying) stop()
            release()
        }
        player = null
        wakeLock?.let { if (it.isHeld) it.release() }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        handler.removeCallbacks(autoEscalate)
        player?.release()
        wakeLock?.let { if (it.isHeld) it.release() }
        super.onDestroy()
    }
}
```

*La dernière partie arrive : le générateur de défis (calcul, énigmes, PHP) et les deux écrans.*

## 5. Code Kotlin : défis et écrans

### ChallengeGenerator.kt

Tire au hasard un défi parmi quatre types : calcul mental, énigme facile, code PHP à recopier, bug PHP à corriger. Tu peux agrandir les listes d'énigmes et d'extraits quand tu veux.

```kotlin
package com.alarmedefi

import kotlin.random.Random

data class Challenge(
    val type: String,
    val question: String,
    val answers: List<String>
)

object ChallengeGenerator {

    private val riddles = listOf(
        Triple("Qu'est-ce qui monte et ne redescend jamais ?", listOf("l'age", "l'âge", "age", "âge"), 0),
        Triple("Plus j'en ai, moins tu vois. Qu'est-ce que c'est ?", listOf("l'obscurité", "obscurité", "le noir", "noir", "la nuit", "nuit"), 0),
        Triple("Qu'est-ce qui a des dents mais ne mord pas ?", listOf("un peigne", "peigne", "un engrenage", "engrenage"), 0),
        Triple("Qu'est-ce qui appartient à toi mais que les autres utilisent plus que toi ?", listOf("ton nom", "nom", "mon nom", "le nom", "prenom", "prénom"), 0),
        Triple("Je parle sans bouche et j'entends sans oreilles. Qui suis-je ?", listOf("l'écho", "echo", "écho", "l'echo"), 0),
        Triple("Qu'est-ce qui a une tête et un pied mais ni bras ni jambes ?", listOf("un lit", "lit"), 0),
        Triple("Combien font la moitié de deux plus deux ?", listOf("3", "trois"), 0),
        Triple("Quel mois de l'année a 28 jours ?", listOf("tous", "tous les mois", "les douze", "12"), 0)
    )

    private val phpCopy = listOf(
        "echo 'Salut';",
        "\$x = 5;",
        "if (\$a > 3) { echo \$a; }",
        "foreach (\$list as \$item) {}",
        "\$nom = trim(\$_POST['nom']);",
        "return count(\$tab);",
        "\$sum = \$a + \$b;",
        "echo strlen(\$texte);"
    )

    // Chaque bug : (code erroné, liste des corrections acceptées)
    private val phpBugs = listOf(
        Pair("echo 'Salut'", listOf("echo 'Salut';", "echo \"Salut\";")),
        Pair("x = 5;", listOf("\$x = 5;", "\$x=5;")),
        Pair("if (\$a > 3 { echo \$a; }", listOf("if (\$a > 3) { echo \$a; }", "if (\$a > 3) {echo \$a;}")),
        Pair("\$total = \$a + ;", listOf("\$total = \$a + \$b;", "\$total=\$a+\$b;")),
        Pair("for (\$i = 0; \$i < 3; \$i++ { }", listOf("for (\$i = 0; \$i < 3; \$i++) { }", "for (\$i=0; \$i<3; \$i++) { }")),
        Pair("\$tab = array(1, 2, 3;", listOf("\$tab = array(1, 2, 3);", "\$tab=array(1,2,3);"))
    )

    fun next(): Challenge = when (Random.nextInt(4)) {
        0 -> math()
        1 -> riddle()
        2 -> copy()
        else -> bug()
    }

    private fun math(): Challenge {
        val kind = Random.nextInt(3)
        return when (kind) {
            0 -> {
                val a = Random.nextInt(12, 60)
                val b = Random.nextInt(12, 60)
                Challenge("Calcul", "$a + $b = ?", listOf((a + b).toString()))
            }
            1 -> {
                val a = Random.nextInt(30, 99)
                val b = Random.nextInt(11, a)
                Challenge("Calcul", "$a - $b = ?", listOf((a - b).toString()))
            }
            else -> {
                val a = Random.nextInt(3, 13)
                val b = Random.nextInt(3, 13)
                Challenge("Calcul", "$a × $b = ?", listOf((a * b).toString()))
            }
        }
    }

    private fun riddle(): Challenge {
        val r = riddles.random()
        return Challenge("Énigme", r.first, r.second)
    }

    private fun copy(): Challenge {
        val code = phpCopy.random()
        return Challenge("Code PHP à recopier", code, listOf(code))
    }

    private fun bug(): Challenge {
        val b = phpBugs.random()
        return Challenge("Bug PHP à corriger", b.first, b.second)
    }

    private fun clean(s: String): String =
        s.trim().replace("\\s+".toRegex(), " ")
            .replace('\u2019', '\'').replace('\u2018', '\'')
            .replace('\u201C', '"').replace('\u201D', '"')

    fun isCorrect(challenge: Challenge, input: String): Boolean {
        val given = clean(input)
        val strict = challenge.type.startsWith("Code") || challenge.type.startsWith("Bug")
        return challenge.answers.any {
            if (strict) clean(it) == given else clean(it).equals(given, ignoreCase = true)
        }
    }
}
```

### ChallengeActivity.kt

L'écran du défi : s'affiche par-dessus l'écran verrouillé, bloque le bouton retour, et fait monter le volume à chaque mauvaise réponse. Crée l'interface en code, donc aucun fichier de mise en page à ajouter.

```kotlin
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
```

### MainActivity.kt

L'écran d'accueil : tu choisis l'heure, tu actives l'alarme, tu peux l'annuler. Au premier lancement, il demande l'autorisation de notifications.

```kotlin
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
```

## 6. Remarques importantes

- Le fichier `build.yml` utilise la commande `gradle assembleDebug`, donc aucun fichier `gradlew` n'est nécessaire dans le dépôt.
- Sur certains téléphones (Xiaomi, Huawei, Samsung), désactive l'optimisation de batterie pour l'application, sinon le système peut retarder l'alarme.
- L'alarme est programmée une seule fois, et se désactive après résolution du défi. Un réglage de répétition quotidienne est facile à ajouter ensuite.
- Je n'ai pas pu compiler ce code ici : si GitHub Actions signale une erreur, copie-moi le message du journal et je corrige.
- Sécurité : le bouton retour est bloqué pendant l'alarme, mais tu peux toujours éteindre le téléphone en cas d'urgence.

## 7. Aperçu de l'interface

&#91;embedded content: maquette des deux écrans de l'application\]

Maquette dessinée à partir du code : l'apparence réelle suivra le thème Material de ton téléphone, mais la disposition sera celle-ci.
