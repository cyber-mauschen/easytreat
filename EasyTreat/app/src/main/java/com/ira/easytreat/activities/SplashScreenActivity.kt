package com.ira.easytreat.activities

import android.R
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.ira.easytreat.databinding.ActivitySplashScreenBinding


/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
class SplashScreenActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashScreenBinding
    private lateinit var titleLabel: TextView
    private lateinit var geminiLabel: TextView
    private lateinit var fadeInAnimation: Animation
    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySplashScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)
        geminiLabel = binding.geminiTextView
        geminiLabel.visibility = View.INVISIBLE
        titleLabel = binding.titleTextView
        fadeInAnimation = AnimationUtils.loadAnimation(this, R.anim.fade_in)

        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
    }

    override fun onResume() {
        super.onResume()
        val fadeInAnimation = AlphaAnimation(0.0f, 1.0f)
        fadeInAnimation.duration = 1000
        val a: Animation = AnimationUtils.loadAnimation(this, R.anim.fade_in)
        a.reset()
        //geminiLabel.clearAnimation()
        geminiLabel.startAnimation(fadeInAnimation)
    }

    private inline fun View.fadeIn(durationMillis: Long = 250) {
        this.visibility = View.VISIBLE
        this.startAnimation(AlphaAnimation(0F, 1F).apply {
            duration = durationMillis
            fillAfter = true
        })
    }

    inline fun View.fadeOut(durationMillis: Long = 250) {
        this.startAnimation(AlphaAnimation(1F, 0F).apply {
            duration = durationMillis
            fillAfter = true
        })
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        if (hasFocus) {
            geminiLabel.fadeIn(1000)
            android.os.Handler().postDelayed({
                launchActivity(MainActivity::class.java, Intent.FLAG_ACTIVITY_NEW_TASK)
            }, 2000)
        }
    }

    fun Context.launchActivity(
        cls: Class<*>,
        flags: Int = 0,
        intentTransformer: Intent.() -> Unit = {}
    ) {
        val intent = Intent(this, cls).apply {
            addFlags(flags)
            intentTransformer()
        }
        this.startActivity(intent)
        finish()
    }
}