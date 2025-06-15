package com.dgalyanov.gallery.utils

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings

internal fun openAppSettings(activity: Activity) {
  val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
  val uri = Uri.fromParts("package", activity.packageName, null)
  intent.setData(uri)
  activity.startActivity(intent)
}