package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.util.AppUpdateCheckResult
import com.example.util.AppUpdateChecker
import com.example.util.GitHubReleaseInfo
import com.example.util.UpdateErrorReason
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AppUpdateStatus {
    IDLE,
    CHECKING,
    UPDATE_AVAILABLE,
    UP_TO_DATE,
    OFFLINE_OR_TIMEOUT,
    RATE_LIMITED,
    NO_RELEASES_YET,
    ERROR
}

data class AppUpdateUiState(
    val status: AppUpdateStatus = AppUpdateStatus.IDLE,
    val currentVersionLabel: String = AppUpdateChecker.formatCurrentVersionLabel(BuildConfig.VERSION_NAME),
    val currentVersionCode: Int = BuildConfig.VERSION_CODE,
    val availableRelease: GitHubReleaseInfo? = null,
    val latestTagSeen: String = "",
    val showUpdateDialog: Boolean = false,
    val showUpdateBanner: Boolean = false,
    val lastCheckWasManual: Boolean = false,
    val manualCheckSequence: Int = 0,
    val pendingNotificationRelease: GitHubReleaseInfo? = null
) {
    val isChecking: Boolean get() = status == AppUpdateStatus.CHECKING
    val hasUpdate: Boolean get() = status == AppUpdateStatus.UPDATE_AVAILABLE && availableRelease != null
}

class AppUpdateViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(AppUpdateUiState())
    val uiState: StateFlow<AppUpdateUiState> = _uiState.asStateFlow()

    private var hasRunAutomaticStartupCheck = false
    private var lastNotifiedTag: String? = null

    private val safeHandler = CoroutineExceptionHandler { _, throwable ->
        Log.w(TAG, "Suppressed unexpected error during update check", throwable)
        _uiState.update { current ->
            if (current.isChecking) {
                current.copy(status = AppUpdateStatus.ERROR)
            } else {
                current
            }
        }
    }

    fun checkAutomaticallyOnce() {
        if (hasRunAutomaticStartupCheck) return
        hasRunAutomaticStartupCheck = true
        checkForUpdates(isManual = false)
    }

    fun checkForUpdates(isManual: Boolean) {
        if (_uiState.value.isChecking) return

        _uiState.update {
            it.copy(
                status = AppUpdateStatus.CHECKING,
                lastCheckWasManual = isManual
            )
        }

        viewModelScope.launch(safeHandler) {
            val result = try {
                AppUpdateChecker.checkLatestRelease(
                    currentVersionName = BuildConfig.VERSION_NAME,
                    currentVersionCode = BuildConfig.VERSION_CODE
                )
            } catch (ce: CancellationException) {
                throw ce
            } catch (t: Throwable) {
                Log.w(TAG, "Update check failed safely", t)
                AppUpdateCheckResult.Error(
                    reason = UpdateErrorReason.SERVER_OR_PARSE_ERROR,
                    detail = t.localizedMessage.orEmpty()
                )
            }

            when (result) {
                is AppUpdateCheckResult.UpdateAvailable -> {
                    val shouldNotify = isManual || lastNotifiedTag != result.release.tagName
                    if (shouldNotify) {
                        lastNotifiedTag = result.release.tagName
                    }
                    _uiState.update { current ->
                        current.copy(
                            status = AppUpdateStatus.UPDATE_AVAILABLE,
                            currentVersionLabel = result.currentVersion,
                            availableRelease = result.release,
                            latestTagSeen = result.release.displayVersion,
                            showUpdateDialog = true,
                            showUpdateBanner = true,
                            lastCheckWasManual = isManual,
                            manualCheckSequence = if (isManual) current.manualCheckSequence + 1 else current.manualCheckSequence,
                            pendingNotificationRelease = if (shouldNotify) result.release else current.pendingNotificationRelease
                        )
                    }
                }

                is AppUpdateCheckResult.UpToDate -> {
                    _uiState.update { current ->
                        current.copy(
                            status = AppUpdateStatus.UP_TO_DATE,
                            currentVersionLabel = result.currentVersion,
                            availableRelease = null,
                            latestTagSeen = result.latestTag,
                            showUpdateDialog = false,
                            showUpdateBanner = false,
                            lastCheckWasManual = isManual,
                            manualCheckSequence = if (isManual) current.manualCheckSequence + 1 else current.manualCheckSequence
                        )
                    }
                }

                is AppUpdateCheckResult.Error -> {
                    val mappedStatus = when (result.reason) {
                        UpdateErrorReason.OFFLINE_OR_TIMEOUT -> AppUpdateStatus.OFFLINE_OR_TIMEOUT
                        UpdateErrorReason.RATE_LIMITED -> AppUpdateStatus.RATE_LIMITED
                        UpdateErrorReason.NO_RELEASES_YET -> AppUpdateStatus.NO_RELEASES_YET
                        UpdateErrorReason.SERVER_OR_PARSE_ERROR -> AppUpdateStatus.ERROR
                    }
                    _uiState.update { current ->
                        // On automatic background check failure, keep prior update state if one was already known
                        if (!isManual && current.availableRelease != null) {
                            current.copy(status = AppUpdateStatus.UPDATE_AVAILABLE)
                        } else {
                            current.copy(
                                status = mappedStatus,
                                lastCheckWasManual = isManual,
                                manualCheckSequence = if (isManual) current.manualCheckSequence + 1 else current.manualCheckSequence
                            )
                        }
                    }
                }
            }
        }
    }

    fun openUpdateDialog() {
        if (_uiState.value.availableRelease != null) {
            _uiState.update { it.copy(showUpdateDialog = true) }
        }
    }

    fun dismissUpdateDialog() {
        _uiState.update { it.copy(showUpdateDialog = false) }
    }

    fun dismissUpdateBanner() {
        _uiState.update { it.copy(showUpdateBanner = false) }
    }

    fun consumePendingNotification() {
        _uiState.update { it.copy(pendingNotificationRelease = null) }
    }

    companion object {
        private const val TAG = "AppUpdateViewModel"
    }
}
