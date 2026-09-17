package com.tvremote.app.adb

import android.content.Context
import dadb.AdbKeyPair
import dadb.Dadb
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Drives a real Android TV over the network ADB protocol (port 5555 by default).
 *
 * How this actually controls the TV:
 *  - Most Android TV boxes / Google TV / Chromecast with Google TV / Mi Box / Shield
 *    expose Settings > System > Developer options > "Network debugging", which opens
 *    an ADB daemon on tcp/5555 without needing USB.
 *  - This class connects to that daemon directly (no `adb` binary, no PC, no root)
 *    using dadb (https://github.com/mobile-dev-inc/dadb), a pure-Kotlin ADB client.
 *  - On first connect, the TV will show an "Allow USB debugging?" style prompt with a
 *    fingerprint — this is normal ADB behavior, accept it once per TV.
 *  - Every button in the UI maps to a real `input keyevent` / `am start` shell command
 *    that is executed on the TV, exactly like an official remote app would.
 */
class AdbRemoteClient(context: Context) {

    // dadb needs a writable place to store/generate its RSA keypair.
    private val keyDir = File(context.filesDir, "adbkey").apply { parentFile?.mkdirs() }
    private val privateKeyFile = File(context.filesDir, "adbkey")
    private val publicKeyFile = File(context.filesDir, "adbkey.pub")

    private var dadb: Dadb? = null

    val isConnected: Boolean
        get() = dadb != null

    /** Opens a real ADB connection to [host]:[port]. Suspends off the main thread. */
    suspend fun connect(host: String, port: Int = 5555): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            dadb?.close()
            if (!privateKeyFile.exists() || !publicKeyFile.exists()) {
                AdbKeyPair.generate(privateKeyFile, publicKeyFile)
            }
            val keyPair: AdbKeyPair = AdbKeyPair.read(privateKeyFile, publicKeyFile)
            dadb = Dadb.create(host, port, keyPair)
            // Sanity round-trip so a bad host/port/unaccepted-key fails fast.
            dadb!!.shell("echo connected")
            Result.success(Unit)
        } catch (t: Throwable) {
            dadb?.close()
            dadb = null
            Result.failure(t)
        }
    }

    fun disconnect() {
        dadb?.close()
        dadb = null
    }

    /**
     * Runs a raw shell command on the TV — fire-and-forget.
     * We open the shell stream and close it immediately instead of waiting for the TV
     * to finish executing and stream back full output. For keyevents / launches we never
     * needed that output anyway, and waiting for it was the main source of per-tap lag.
     */
    private suspend fun shell(cmd: String) = withContext(Dispatchers.IO) {
        val d = dadb ?: return@withContext
        try {
            val stream = d.open("shell,v2,raw:$cmd")
            stream.close()
        } catch (t: Throwable) {
            // Fire-and-forget: nothing to report back to the UI for a keyevent.
        }
    }

    private suspend fun keyEvent(code: Int) = shell("input keyevent $code")

    // ---- Buttons from the design, mapped to real Android key codes ----
    suspend fun power() = keyEvent(KeyCodes.POWER)
    suspend fun back() = keyEvent(KeyCodes.BACK)
    suspend fun home() = keyEvent(KeyCodes.HOME)
    suspend fun recentApps() = keyEvent(KeyCodes.APP_SWITCH)
    suspend fun assistant() = shell("am start -a android.intent.action.VOICE_COMMAND")
    suspend fun mute() = keyEvent(KeyCodes.VOLUME_MUTE)
    suspend fun volumeUp() = keyEvent(KeyCodes.VOLUME_UP)
    suspend fun volumeDown() = keyEvent(KeyCodes.VOLUME_DOWN)
    suspend fun menu() = keyEvent(KeyCodes.MENU)
    suspend fun dpadUp() = keyEvent(KeyCodes.DPAD_UP)
    suspend fun dpadDown() = keyEvent(KeyCodes.DPAD_DOWN)
    suspend fun dpadLeft() = keyEvent(KeyCodes.DPAD_LEFT)
    suspend fun dpadRight() = keyEvent(KeyCodes.DPAD_RIGHT)
    suspend fun dpadCenter() = keyEvent(KeyCodes.DPAD_CENTER)
    suspend fun openKeyboard() = shell("input keyevent ${KeyCodes.DPAD_CENTER}") // focuses text field if any

    /** Sets an absolute volume level 0-100 by mapping to the TV's 0-15 stream steps. */
    suspend fun setVolumePercent(percent: Int) {
        val level = (percent.coerceIn(0, 100) * 15 / 100)
        shell("media volume --stream 3 --set $level")
    }

    suspend fun openSettings() = shell("am start -a android.settings.SETTINGS")

    /** Launches an installed app by package name; falls back gracefully if not installed. */
    suspend fun launchApp(packageName: String) =
        shell("monkey -p $packageName -c android.intent.category.LAUNCHER 1")

    /**
     * Touchpad drag: converts a screen-space delta into either DPAD steps (default,
     * works on every Android TV UI) or a raw swipe gesture (better for scrolling feeds).
     */
    suspend fun swipe(x1: Int, y1: Int, x2: Int, y2: Int, durationMs: Int = 120) =
        shell("input swipe $x1 $y1 $x2 $y2 $durationMs")

    suspend fun tap(x: Int, y: Int) = shell("input tap $x $y")
}

object KeyCodes {
    const val POWER = 26
    const val BACK = 4
    const val HOME = 3
    const val APP_SWITCH = 187
    const val MENU = 82
    const val VOLUME_UP = 24
    const val VOLUME_DOWN = 25
    const val VOLUME_MUTE = 164
    const val DPAD_UP = 19
    const val DPAD_DOWN = 20
    const val DPAD_LEFT = 21
    const val DPAD_RIGHT = 22
    const val DPAD_CENTER = 23
}

/** Common Android TV app package names used by the app-shortcut row. */
object TvPackages {
    const val YOUTUBE = "com.google.android.youtube.tv"
    const val NETFLIX = "com.netflix.ninja"
    const val PRIME_VIDEO = "com.amazon.amazonvideo.livingroom"
}
