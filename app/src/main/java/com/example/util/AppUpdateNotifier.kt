package com.example.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.R

object AppUpdateNotifier {

    private const val TAG = "AppUpdateNotifier"
    const val UPDATE_CHANNEL_ID = "sounding_app_update_channel"
    private const val UPDATE_NOTIFICATION_ID = 4109

    fun ensureNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        try {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return
            val existing = manager.getNotificationChannel(UPDATE_CHANNEL_ID)
            if (existing != null) return

            val channel = NotificationChannel(
                UPDATE_CHANNEL_ID,
                context.getString(R.string.update_notification_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.update_notification_channel_desc)
            }
            manager.createNotificationChannel(channel)
        } catch (t: Throwable) {
            Log.w(TAG, "Failed to create update notification channel safely", t)
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return try {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } catch (_: Throwable) {
            false
        }
    }

    fun showUpdateNotification(
        context: Context,
        release: GitHubReleaseInfo
    ): Boolean {
        return try {
            if (!hasNotificationPermission(context)) {
                return false
            }
            ensureNotificationChannel(context)

            val targetUrl = release.bestDownloadUrl.takeIf { AppUpdateChecker.isSafeReleaseUrl(it) }
                ?: AppUpdateChecker.GITHUB_RELEASES_PAGE_URL

            val viewIntent = Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            val downloadPendingIntent = PendingIntent.getActivity(
                context,
                UPDATE_NOTIFICATION_ID,
                viewIntent,
                pendingFlags
            )

            val title = context.getString(
                R.string.update_notification_title,
                release.displayVersion
            )
            val cleanedNotes = release.body
                .replace("\r\n", "\n")
                .trim()
                .ifEmpty { context.getString(R.string.update_release_notes_fallback) }
            val shortSummary = cleanedNotes
                .lineSequence()
                .map { it.trim().trimStart('#', '-', '*', ' ') }
                .firstOrNull { it.isNotEmpty() }
                ?.take(140)
                ?: context.getString(R.string.update_release_notes_fallback)

            val notification = NotificationCompat.Builder(context, UPDATE_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_update_notification)
                .setContentTitle(title)
                .setContentText(shortSummary)
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .setBigContentTitle(title)
                        .bigText(cleanedNotes.take(600))
                )
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(downloadPendingIntent)
                .addAction(
                    0,
                    context.getString(R.string.update_action_download_apk),
                    downloadPendingIntent
                )
                .build()

            NotificationManagerCompat.from(context).notify(UPDATE_NOTIFICATION_ID, notification)
            true
        } catch (se: SecurityException) {
            Log.w(TAG, "POST_NOTIFICATIONS denied; skipping local notification", se)
            false
        } catch (t: Throwable) {
            Log.w(TAG, "Failed to post local update notification", t)
            false
        }
    }
}
