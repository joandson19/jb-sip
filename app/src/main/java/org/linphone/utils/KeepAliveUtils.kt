/*
 * Copyright (c) 2010-2023 Belledonne Communications SARL.
 *
 * This file is part of linphone-android
 * (see https://www.linphone.org).
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.linphone.utils

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.annotation.AnyThread
import org.linphone.core.tools.Log

/**
 * Fork: helpers to keep the app alive in background on Android.
 *
 * Android kills background apps aggressively, and some manufacturers add an extra
 * "autostart" permission on top of the standard battery optimization exemption.
 * These helpers let the app detect those settings and open the relevant system screens.
 */
object KeepAliveUtils {
    private const val TAG = "[Keep Alive Utils]"

    private const val PREFERENCES = "jb_voip_keep_alive"
    private const val PREF_LAST_PROMPT = "last_prompt_timestamp"
    private const val PREF_DO_NOT_ASK = "do_not_ask_again"

    // Ask again at most once per 12 hours while the app isn't exempted
    private const val PROMPT_INTERVAL_MS = 12 * 60 * 60 * 1000L

    private const val PACKAGE_URI_PREFIX = "package:"

    @AnyThread
    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            ?: return true
        return try {
            powerManager.isIgnoringBatteryOptimizations(context.packageName)
        } catch (e: Exception) {
            Log.e("$TAG Failed to check battery optimization exemption: $e")
            false
        }
    }

    @AnyThread
    fun openBatteryOptimizationSettings(context: Context) {
        val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
        if (!startActivitySafely(context, intent)) {
            openAppDetailsSettings(context)
        }
    }

    @AnyThread
    fun openAppDetailsSettings(context: Context) {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.parse("$PACKAGE_URI_PREFIX${context.packageName}")
        )
        if (!startActivitySafely(context, intent)) {
            openSettings(context)
        }
    }

    @AnyThread
    fun openSettings(context: Context) {
        startActivitySafely(context, Intent(Settings.ACTION_SETTINGS))
    }

    /**
     * Try to open the manufacturer "autostart"/"protected apps" screen.
     * There is no standard API for this, so we fall back to the battery settings screen
     * and finally to the app details screen.
     */
    @AnyThread
    fun openAutoStartSettings(context: Context) {
        for (component in getAutoStartComponents()) {
            val intent = Intent().apply {
                this.component = component
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (startActivitySafely(context, intent, logFailure = false)) {
                Log.i("$TAG Opened manufacturer autostart settings [${component.flattenToShortString()}]")
                return
            }
        }

        Log.w("$TAG No manufacturer autostart settings found, opening battery optimization settings")
        openBatteryOptimizationSettings(context)
    }

    @AnyThread
    fun isAutoStartSettingsAvailable(context: Context): Boolean {
        return getAutoStartComponents().any {
            try {
                context.packageManager.resolveActivity(
                    Intent().setComponent(it),
                    0
                ) != null
            } catch (e: Exception) {
                false
            }
        }
    }

    @AnyThread
    fun shouldAskForExemption(context: Context): Boolean {
        if (isIgnoringBatteryOptimizations(context)) return false
        if (hasUserOptedOut(context)) return false

        val lastPrompt = getPreferences(context).getLong(PREF_LAST_PROMPT, 0L)
        return System.currentTimeMillis() - lastPrompt > PROMPT_INTERVAL_MS
    }

    @AnyThread
    fun onExemptionPromptShown(context: Context) {
        getPreferences(context).edit().putLong(PREF_LAST_PROMPT, System.currentTimeMillis()).apply()
    }

    @AnyThread
    fun setDoNotAskAgain(context: Context, value: Boolean) {
        getPreferences(context).edit().putBoolean(PREF_DO_NOT_ASK, value).apply()
    }

    @AnyThread
    private fun hasUserOptedOut(context: Context): Boolean {
        return getPreferences(context).getBoolean(PREF_DO_NOT_ASK, false)
    }

    @AnyThread
    private fun getPreferences(context: Context) = context.getSharedPreferences(
        PREFERENCES,
        Context.MODE_PRIVATE
    )

    @AnyThread
    private fun startActivitySafely(
        context: Context,
        intent: Intent,
        logFailure: Boolean = true
    ): Boolean {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            if (logFailure) {
                Log.w("$TAG Unable to start intent [$intent]: $e")
            }
            false
        }
    }

    @AnyThread
    private fun getAutoStartComponents(): List<ComponentName> {
        val components = mutableListOf<ComponentName>()

        // Xiaomi / Redmi / Poco
        components.add(
            ComponentName(
                "com.miui.securitycenter",
                "com.miui.permcenter.autostart.AutoStartManagementActivity"
            )
        )
        // Huawei / Honor
        components.add(
            ComponentName(
                "com.huawei.systemmanager",
                "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"
            )
        )
        components.add(
            ComponentName(
                "com.huawei.systemmanager",
                "com.huawei.systemmanager.optimize.process.ProtectActivity"
            )
        )
        // Oppo / Realme
        components.add(
            ComponentName(
                "com.coloros.safecenter",
                "com.coloros.safecenter.permission.startup.StartupAppListActivity"
            )
        )
        components.add(
            ComponentName(
                "com.oppo.safe",
                "com.oppo.safe.permission.startup.StartupAppListActivity"
            )
        )
        // Vivo / iQOO
        components.add(
            ComponentName(
                "com.vivo.permissionmanager",
                "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"
            )
        )
        components.add(
            ComponentName(
                "com.iqoo.secure",
                "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity"
            )
        )
        // Samsung
        components.add(
            ComponentName(
                "com.samsung.android.lool",
                "com.samsung.android.sm.ui.battery.BatteryActivity"
            )
        )
        // OnePlus
        components.add(
            ComponentName(
                "com.oneplus.security",
                "com.oneplus.security.chainlaunch.view.ChainLaunchAppListActivity"
            )
        )
        // Asus
        components.add(
            ComponentName("com.asus.mobilemanager", "com.asus.mobilemanager.MainActivity")
        )
        // Letv
        components.add(
            ComponentName(
                "com.letv.android.letvsafe",
                "com.letv.android.letvsafe.AutobootManageActivity"
            )
        )
        // Nokia / HMD
        components.add(
            ComponentName(
                "com.evenwell.powersaving.g3",
                "com.evenwell.powersaving.g3.exceptionlist.ExceptionlistDialogActivity"
            )
        )

        return components
    }
}
