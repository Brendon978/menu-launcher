package com.brendon978.menulauncher.wallpaper

import android.app.NotificationManager
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.media.AudioManager
import android.os.BatteryManager
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas as ComposeCanvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.brendon978.menulauncher.pet.Accessory
import com.brendon978.menulauncher.pet.Face
import com.brendon978.menulauncher.pet.Mood
import com.brendon978.menulauncher.pet.ROBOT_ASPECT
import com.brendon978.menulauncher.pet.baseMood
import com.brendon978.menulauncher.pet.drawRobot
import com.brendon978.menulauncher.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlin.math.max
import kotlin.random.Random

/**
 * O Glitch no papel de parede da tela de bloqueio. A tela continua sendo a da Samsung (digital
 * direta, notificações, atalhos), e ele só desenha enquanto está à vista. Não recebe toque.
 */
class LockWallpaperService : WallpaperService() {
    override fun onCreateEngine(): Engine = LockEngine()

    private inner class LockEngine : Engine() {
        private val handler = Handler(Looper.getMainLooper())
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        private val canvasScope = CanvasDrawScope()
        private val random = Random(System.nanoTime())
        private val paint = Paint(Paint.FILTER_BITMAP_FLAG)
        private var visible = false
        private var width = 0
        private var height = 0
        private var background: Bitmap? = null
        private var backgroundStamp = -1L
        private var stroke = Color.White
        private var accessory = Accessory.NONE
        private var mood = Mood.NORMAL
        private var dnd = false
        private var moodAt = 0L
        private var act: LockAct? = null
        private var lastAct: LockAct? = null
        private var actAt = 0L
        private var nextActAt = Long.MAX_VALUE
        /** Só em build de teste: dispara uma ação mesmo dançando ou dormindo. */
        private var forced = false
        private var debugReceiver: android.content.BroadcastReceiver? = null
        private val started = System.currentTimeMillis()

        private val frame = object : Runnable {
            override fun run() {
                if (!visible) return
                val now = System.currentTimeMillis()
                if (now - moodAt > 30_000) readMood(now)
                advanceAct(now)
                draw(now)
                handler.postDelayed(this, lockFrameDelay(act, mood))
            }
        }

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
            if (com.brendon978.menulauncher.BuildConfig.DEBUG && !isPreview) {
                debugReceiver = object : android.content.BroadcastReceiver() {
                    override fun onReceive(c: android.content.Context, i: Intent) {
                        act = runCatching { LockAct.valueOf(i.getStringExtra("acao").orEmpty()) }.getOrNull() ?: return
                        forced = true
                        actAt = System.currentTimeMillis()
                    }
                }
                registerReceiver(debugReceiver, IntentFilter("com.brendon978.menulauncher.DEBUG_LOCK_ACT"), android.content.Context.RECEIVER_EXPORTED)
            }
            scope.launch {
                SettingsRepository(applicationContext).settings.collect { s ->
                    stroke = Color(s.strokeColor)
                    accessory = runCatching { Accessory.valueOf(s.glitchAccessory) }.getOrDefault(Accessory.NONE)
                }
            }
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, w: Int, h: Int) {
            width = w
            height = h
            backgroundStamp = -1L
            if (visible) loadBackground()
        }

        override fun onVisibilityChanged(isVisible: Boolean) {
            visible = isVisible
            handler.removeCallbacks(frame)
            if (!isVisible) { act = null; return }
            val now = System.currentTimeMillis()
            loadBackground()
            readMood(now)
            nextActAt = wakeActDelay(random)?.let { now + it } ?: (now + nextActGap(random))
            handler.post(frame)
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            visible = false
            handler.removeCallbacks(frame)
        }

        override fun onDestroy() {
            handler.removeCallbacks(frame)
            debugReceiver?.let { unregisterReceiver(it) }
            scope.cancel()
            background?.recycle()
            super.onDestroy()
        }

        private fun readMood(now: Long) {
            moodAt = now
            val battery = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val level = battery?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)?.takeIf { it >= 0 }
            val status = battery?.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            val charging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
            val music = getSystemService(AudioManager::class.java).isMusicActive
            dnd = getSystemService(NotificationManager::class.java).currentInterruptionFilter > NotificationManager.INTERRUPTION_FILTER_ALL
            val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            mood = if (dnd) Mood.SLEEPING else baseMood(hour, level, charging, music, 0L, now)
        }

        private fun advanceAct(now: Long) {
            val current = act
            when {
                current != null && now - actAt >= current.durationMs -> {
                    lastAct = current
                    act = null
                    forced = false
                    nextActAt = now + nextActGap(random)
                }
                current == null && now >= nextActAt && actsIn(mood) -> {
                    act = pickAct(lastAct, random)
                    actAt = now
                }
            }
        }

        /** Recorta pelo centro para a tela; lê do disco só quando a imagem mudou. */
        private fun loadBackground() {
            val file = LockFiles.background(this@LockWallpaperService)
            val stamp = if (file.exists()) file.lastModified() else 0L
            if (stamp == backgroundStamp || width == 0 || height == 0) return
            backgroundStamp = stamp
            background?.recycle()
            background = null
            if (stamp == 0L) return
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.path, bounds)
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return
            var sample = 1
            while (bounds.outWidth / (sample * 2) >= width && bounds.outHeight / (sample * 2) >= height) sample *= 2
            background = runCatching { BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply { inSampleSize = sample }) }.getOrNull()
        }

        private fun drawBackground(c: Canvas) {
            c.drawColor(android.graphics.Color.rgb(12, 14, 20))
            val b = background ?: return
            val scale = max(c.width.toFloat() / b.width, c.height.toFloat() / b.height)
            val sw = (c.width / scale).toInt()
            val sh = (c.height / scale).toInt()
            val left = (b.width - sw) / 2
            val top = (b.height - sh) / 2
            c.drawBitmap(b, Rect(left, top, left + sw, top + sh), Rect(0, 0, c.width, c.height), paint)
        }

        private fun draw(now: Long) {
            val holder = surfaceHolder
            val c = runCatching { holder.lockCanvas() }.getOrNull() ?: return
            try {
                drawBackground(c)
                val current = act
                val k = if (current != null) ((now - actAt).toFloat() / current.durationMs).coerceIn(0f, 1f) else 0f
                val pose = lockPose(current, k, if (forced) Mood.NORMAL else mood, (now - started) / 1000f).let {
                    it.copy(accessory = if (dnd && it.face == Face.SLEEP) Accessory.SLEEP_MASK else accessory)
                }
                val h = c.height * LOCK_ROBOT_HEIGHT
                val w = h * ROBOT_ASPECT
                canvasScope.draw(Density(1f), LayoutDirection.Ltr, ComposeCanvas(c), Size(w, h)) {
                    translate(c.width * LOCK_ROBOT_X, c.height * LOCK_ROBOT_FEET - h) { drawRobot(stroke, pose) }
                }
            } finally {
                runCatching { holder.unlockCanvasAndPost(c) }
            }
        }
    }

    private companion object {
        // pequeno, no canto de baixo: acima do atalho do telefone e longe do relógio e da digital
        const val LOCK_ROBOT_HEIGHT = .085f
        const val LOCK_ROBOT_X = .07f
        const val LOCK_ROBOT_FEET = .885f
    }
}
