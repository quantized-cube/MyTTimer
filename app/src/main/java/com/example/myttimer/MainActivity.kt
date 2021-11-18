package com.example.myttimer

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.drawable.Icon
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Bundle
import android.os.Handler
import android.util.Rational
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    private val handler = Handler()
    private var timeValue = 0   // sec * interval / 1000
    private val interval: Long = 50  // ms
    private val ratio: Int = 1000 / interval.toInt()
    private val ue: Int = 20 * ratio  // 20s -> 30s, ノルウェー用に変更、設定できるようにしたい
    private val sita: Int = 10 * ratio  // 10s -> 30s
    lateinit var sp0: SoundPool
    private var snd0 = 0
    private var snd00 = 0
    private var snd1 = 0
    private var onFlag = 0

    @SuppressLint("ByteOrderMark")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val audioAttributes = AudioAttributes.Builder()
            // USAGE_MEDIA
            // USAGE_GAME
            .setUsage(AudioAttributes.USAGE_GAME)
            // CONTENT_TYPE_MUSIC
            // CONTENT_TYPE_SPEECH, etc.
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build()

        sp0 = SoundPool.Builder()
            .setAudioAttributes(audioAttributes)
            // ストリーム数に応じて
            .setMaxStreams(3)
            .build()

        // ding051.mp3 をロードしておく
        snd0 = sp0.load(this, R.raw.ding_zero_five_one_rev, 1)
        snd00 = sp0.load(this, R.raw.ding_zero_five_one_rev, 1)
        // tin2.mp3 をロードしておく
        snd1 = sp0.load(this, R.raw.tin_two_rev, 1)

        val runnable = object : Runnable {
            override fun run() {
                timeValue++
                val time: Int = timeValue / ratio
                timeToText(time)?.let {
                    timeText.text = it
                }

                var ueShow: Int = ue - (timeValue % (ue + sita))
                if (ueShow == 0) {
                    sp0.play(snd00, 1.0f, 1.0f, 0, 0, 1.0f)
                    // sp0.play(snd0, 1.5f, 1.5f, 1, 0, 1.0f)
                } else if (ueShow == 10 * ratio) {
                    sp0.play(snd0, 1.0f, 1.0f, 0, 0, 1.5f)
                } else if (ueShow < 0) {
                    ueShow = 0
                }
                ueShow /= ratio
                timeToText(ueShow)?.let {
                    timeTextTwenty.text = it
                }

                var sitaShow: Int = (ue + sita) - (timeValue % (ue + sita))
                if (sitaShow == ue + sita) {
                    sp0.play(snd1, 1.0f, 1.0f, 0, 0, 1.0f)
                } else if (sitaShow > sita) {
                    sitaShow = sita
                }
                sitaShow /= ratio
                timeToText(sitaShow)?.let {
                    timeTextTen.text = it
                }
                handler.postDelayed(this, interval)
            }
        }

        start.setOnClickListener {
            if (onFlag == 0) {
                handler.post(runnable)
                start.text = getString(R.string.command_pause)
                onFlag = 1
            } else {
                handler.removeCallbacks(runnable)
                start.text = getString(R.string.command_resume)
                onFlag = 0
            }
        }
        reset.setOnClickListener {
            handler.removeCallbacks(runnable)
            onFlag = 0
            timeValue = 0
            start.text = getString(R.string.command_start)
            timeToText()?.let {
                timeText.text = it
            }
            timeToText(ue / ratio)?.let {
                timeTextTwenty.text = it
            }
            timeToText(sita / ratio)?.let {
                timeTextTen.text = it
            }
        }

        buttonPip.setOnClickListener {
            val params = PictureInPictureParams.Builder().apply {
                setAspectRatio(Rational(4, 3))
            }.build()
            enterPictureInPictureMode(params)
        }

    }

    override fun onUserLeaveHint() {
        // enterPictureInPictureMode()
        buttonPip.callOnClick()
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        // val receiver = null
        if (isInPictureInPictureMode) {
            timeText.setTextSize(40F)
            start.setVisibility(View.INVISIBLE)
            reset.setVisibility(View.INVISIBLE)
            timeTextTwenty.setVisibility(View.INVISIBLE)
            timeTextTen.setVisibility(View.INVISIBLE)
            buttonMinusTwenty.setVisibility(View.INVISIBLE)
            buttonPlusTwenty.setVisibility(View.INVISIBLE)
            buttonMinusTen.setVisibility(View.INVISIBLE)
            buttonPlusTen.setVisibility(View.INVISIBLE)
            buttonPip.setVisibility(View.INVISIBLE)

            val filter = IntentFilter()
            filter.addAction("startstop")
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(
                    context: Context,
                    intent: Intent
                ) {
                    start.callOnClick()
                }
            }
            registerReceiver(receiver, filter)
            val actions = ArrayList<RemoteAction>()
            val actionIntent = Intent("startstop")
            val pendingIntent = PendingIntent.getBroadcast(
                this,
                0, actionIntent, 0
            )
            val icon = Icon.createWithResource(this, R.drawable.sub)
            val remoteAction = RemoteAction(icon, "hoge", "fuga", pendingIntent)
            actions.add(remoteAction)
            val params = PictureInPictureParams.Builder()
                .setActions(actions)
                .build()
            setPictureInPictureParams(params)
        } else {
            // Restore the full-screen UI.
            timeText.setTextSize(60F)
            start.setVisibility(View.VISIBLE)
            reset.setVisibility(View.VISIBLE)
            timeTextTwenty.setVisibility(View.VISIBLE)
            timeTextTen.setVisibility(View.VISIBLE)
            buttonMinusTwenty.setVisibility(View.VISIBLE)
            buttonPlusTwenty.setVisibility(View.VISIBLE)
            buttonMinusTen.setVisibility(View.VISIBLE)
            buttonPlusTen.setVisibility(View.VISIBLE)
            buttonPip.setVisibility(View.VISIBLE)

            // unregisterReceiver(receiver)
        }
    }

    private fun timeToText(time: Int = 0): String? {
        return if (time < 0) {
            null
        } else if (time == 0) {
            "00:00:00"
        } else {
            val h = time / 3600
            val m = time % 3600 / 60
            val s = time % 60
            "%1$02d:%2$02d:%3$02d".format(h, m, s)
        }
    }

}
