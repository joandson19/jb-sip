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
package org.linphone.ui.main.help.viewmodel

import androidx.annotation.UiThread
import androidx.annotation.WorkerThread
import androidx.lifecycle.MutableLiveData
import org.linphone.BuildConfig
import org.linphone.LinphoneApplication.Companion.coreContext
import org.linphone.LinphoneApplication.Companion.corePreferences
import org.linphone.R
import org.linphone.core.Core
import org.linphone.core.CoreListenerStub
import org.linphone.core.Factory
import org.linphone.core.tools.Log
import org.linphone.ui.GenericViewModel
import org.linphone.utils.AppUtils
import org.linphone.utils.Event

class HelpViewModel
    @UiThread
    constructor() : GenericViewModel() {
    companion object {
        private const val TAG = "[Help ViewModel]"

        private const val NUMBER_OF_CLICK_TO_ENABLE_DEVELOPER_MODE = 6
    }

    val version = MutableLiveData<String>()

    val appVersion = MutableLiveData<String>()

    val uploadLogsAvailable = MutableLiveData<Boolean>()

    val logsUploadInProgress = MutableLiveData<Boolean>()

    val printLogsInLogcat = MutableLiveData<Boolean>()

    val developerSettingsEnabled = MutableLiveData<Boolean>()

    val sourceCodeAvailable = MutableLiveData<Boolean>()

    val logsFileCompressedEvent: MutableLiveData<Event<String>> by lazy {
        MutableLiveData()
    }

    val debugLogsCleanedEvent: MutableLiveData<Event<Boolean>> by lazy {
        MutableLiveData()
    }

    val uploadDebugLogsFinishedEvent: MutableLiveData<Event<String>> by lazy {
        MutableLiveData()
    }

    val uploadDebugLogsErrorEvent: MutableLiveData<Event<Boolean>> by lazy {
        MutableLiveData()
    }

    private var versionClickCount: Int = 0

    private val coreListener = object : CoreListenerStub() {
        @WorkerThread
        override fun onLogCollectionUploadStateChanged(
            core: Core,
            state: Core.LogCollectionUploadState,
            info: String
        ) {
            Log.i("$TAG Logs upload state changed [$state]")
            if (state == Core.LogCollectionUploadState.Delivered) {
                logsUploadInProgress.postValue(false)
                uploadDebugLogsFinishedEvent.postValue(Event(info))
            } else if (state == Core.LogCollectionUploadState.NotDelivered) {
                logsUploadInProgress.postValue(false)
                uploadDebugLogsErrorEvent.postValue(Event(true))
            }
        }
    }

    init {
        val currentVersion = BuildConfig.VERSION_NAME
        version.value = currentVersion

        appVersion.value = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"

        sourceCodeAvailable.value = AppUtils.getString(R.string.website_source_code_url).isNotEmpty()

        logsUploadInProgress.value = false

        versionClickCount = if (corePreferences.showDeveloperSettings) {
            Log.i("$TAG Developer settings are already enabled")
            NUMBER_OF_CLICK_TO_ENABLE_DEVELOPER_MODE
        } else {
            0
        }

        coreContext.postOnCoreThread { core ->
            core.addListener(coreListener)

            printLogsInLogcat.postValue(corePreferences.printLogsInLogcat)
            developerSettingsEnabled.postValue(corePreferences.showDeveloperSettings)
            uploadLogsAvailable.postValue(!core.logCollectionUploadServerUrl.isNullOrEmpty())
        }
    }

    @UiThread
    override fun onCleared() {
        super.onCleared()

        coreContext.postOnCoreThread { core ->
            core.removeListener(coreListener)
        }
    }

    @UiThread
    fun versionClicked() {
        versionClickCount += 1
        Log.i("$TAG Version was clicked [$versionClickCount] times")
        when (versionClickCount) {
            NUMBER_OF_CLICK_TO_ENABLE_DEVELOPER_MODE - 2 -> {
                showGreenToast(R.string.settings_developer_two_more_clicks_required_toast, R.drawable.gear)
            }
            NUMBER_OF_CLICK_TO_ENABLE_DEVELOPER_MODE - 1 -> {
                showGreenToast(R.string.settings_developer_one_more_click_required_toast, R.drawable.gear)
            }
            NUMBER_OF_CLICK_TO_ENABLE_DEVELOPER_MODE -> {
                showGreenToast(R.string.settings_developer_enabled_toast, R.drawable.gear)
                coreContext.postOnCoreThread {
                    Log.w("$TAG Enabling developer settings")
                    corePreferences.showDeveloperSettings = true
                    developerSettingsEnabled.postValue(true)
                }
            }
            NUMBER_OF_CLICK_TO_ENABLE_DEVELOPER_MODE + 1 -> {
                showGreenToast(R.string.settings_developer_already_enabled_toast, R.drawable.gear)
            }
            else -> { }
        }
    }

    @UiThread
    fun cleanLogs() {
        coreContext.postOnCoreThread { core ->
            core.resetLogCollection()
            Log.i("$TAG Debug logs have been cleaned")
            debugLogsCleanedEvent.postValue(Event(true))
        }
    }

    @UiThread
    fun shareLogs() {
        coreContext.postOnCoreThread { core ->
            Log.i("$TAG Uploading debug logs for sharing")
            logsUploadInProgress.postValue(true)
            core.uploadLogCollection()
        }
    }

    // Fork: share the SDK logs as a file (the upload server isn't configured).
    @UiThread
    fun shareLogsFile() {
        coreContext.postOnCoreThread { core ->
            Log.i("$TAG Compressing log collection to share it as a file")
            try {
                val path = core.compressLogCollection()
                if (path.isNullOrEmpty()) {
                    Log.e("$TAG Failed to compress log collection")
                    uploadDebugLogsErrorEvent.postValue(Event(true))
                } else {
                    Log.i("$TAG Log collection compressed to [$path]")
                    logsFileCompressedEvent.postValue(Event(path))
                }
            } catch (e: Exception) {
                Log.e("$TAG Failed to compress log collection: $e")
                uploadDebugLogsErrorEvent.postValue(Event(true))
            }
        }
    }

    @UiThread
    fun toggleLogcat() {
        val newValue = printLogsInLogcat.value == false
        coreContext.postOnCoreThread {
            corePreferences.printLogsInLogcat = newValue
            coreContext.updateLogcatEnabledSetting(newValue)
            Factory.instance().enableLogcatLogs(newValue)
            printLogsInLogcat.postValue(newValue)
        }
    }
}
