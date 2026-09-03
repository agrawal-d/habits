package com.avantgardelabs.healthyhabitstracker.ui

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import com.google.android.play.core.review.ReviewManagerFactory

object RatingHelper {

    fun findActivity(context: Context): Activity? {
        var currentContext = context
        while (currentContext is ContextWrapper) {
            if (currentContext is Activity) {
                return currentContext
            }
            currentContext = currentContext.baseContext
        }
        return null
    }

    /**
     * Attempts to launch the Google Play In-App Review flow.
     * If fallbackToStoreOnFailure is true and the in-app flow cannot be launched
     * (or is not supported/fails to fetch review info), it falls back to openStorePage().
     */
    fun launchReviewFlow(
        activity: Activity,
        fallbackToStoreOnFailure: Boolean = true,
        onComplete: () -> Unit = {}
    ) {
        val manager = ReviewManagerFactory.create(activity)
        val request = manager.requestReviewFlow()
        request.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val reviewInfo = task.result
                val flow = manager.launchReviewFlow(activity, reviewInfo)
                flow.addOnCompleteListener {
                    onComplete()
                }
            } else {
                if (fallbackToStoreOnFailure) {
                    openStorePage(activity)
                }
                onComplete()
            }
        }
    }

    /**
     * Opens the Google Play Store page directly using market:// URI,
     * gracefully falling back to https://play.google.com/store/apps/details?id=...
     * if the Play Store app is not installed/accessible.
     */
    fun openStorePage(context: Context) {
        val packageName = context.packageName
        val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
        }
        try {
            context.startActivity(marketIntent)
        } catch (e: ActivityNotFoundException) {
            val webIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(webIntent)
            } catch (_: Exception) {
            }
        }
    }
}
