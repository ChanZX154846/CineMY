package com.fypcinemy

import android.util.Log
import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Bundle

class CineMyApplication : Application(), Application.ActivityLifecycleCallbacks {

    private var activityCount = 0
    private val TIMEOUT_MILLIS = 60 * 1000 // 1 minute

    override fun onCreate() {
        super.onCreate()
        registerActivityLifecycleCallbacks(this)
        
        // Reset lock state to locked on every fresh app start
        getPrefs().edit().putBoolean("is_locked", true).apply()
    }

    private fun getPrefs() = getSharedPreferences("cinemy_lock_state", Context.MODE_PRIVATE)

    fun onUnlocked() {
        Log.d("CineMyApplication", "onUnlocked called")
        getPrefs().edit()
            .putBoolean("is_locked", false)
            .putLong("last_background_time", 0) // Reset timestamp to prevent immediate re-lock
            .apply()
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}

    override fun onActivityStarted(activity: Activity) {
        activityCount++
        Log.d("CineMyApplication", "onActivityStarted: ${activity::class.java.simpleName}, count: $activityCount")
        
        if (activity is AppLockActivity) return

        val settingsPrefs = getSharedPreferences("cinemy_settings", Context.MODE_PRIVATE)
        val pin = settingsPrefs.getString("security_pin", null)
        
        if (pin != null) {
            val lockPrefs = getPrefs()
            val isLocked = lockPrefs.getBoolean("is_locked", true)
            val lastBackgroundTime = lockPrefs.getLong("last_background_time", 0)
            val now = System.currentTimeMillis()
            
            val shouldLock = isLocked || (lastBackgroundTime != 0L && now - lastBackgroundTime > TIMEOUT_MILLIS)
            Log.d("CineMyApplication", "Checking Lock: isLocked=$isLocked, delta=${now - lastBackgroundTime}, shouldLock=$shouldLock")

            if (shouldLock) {
                Log.d("CineMyApplication", "Redirecting to AppLockActivity")
                lockPrefs.edit().putBoolean("is_locked", true).apply()
                
                val intent = Intent(activity, AppLockActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                activity.startActivity(intent)
            }
        }
    }

    override fun onActivityResumed(activity: Activity) {}

    override fun onActivityPaused(activity: Activity) {}

    override fun onActivityStopped(activity: Activity) {
        activityCount--
        if (activityCount == 0) {
            // App went to background or was closed
            getPrefs().edit()
                .putLong("last_background_time", System.currentTimeMillis())
                .apply()
        }
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

    override fun onActivityDestroyed(activity: Activity) {}
}