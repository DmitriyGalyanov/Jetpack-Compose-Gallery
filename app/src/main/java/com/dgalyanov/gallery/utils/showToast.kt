package com.dgalyanov.gallery.utils

import android.content.Context
import android.widget.Toast

internal fun showToast(context: Context, message: String) =
  postToMainThread { Toast.makeText(context, message, Toast.LENGTH_LONG).show() }
