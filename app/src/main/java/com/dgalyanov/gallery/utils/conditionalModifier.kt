package com.dgalyanov.gallery.utils

import androidx.compose.ui.Modifier

fun Modifier.conditional(
  condition: Boolean,
  ifTrue: Modifier.() -> Modifier,
): Modifier = if (condition) this.then(ifTrue(Modifier)) else this

//fun Modifier.conditionalWithProvider(
//  conditionProvider: () -> Boolean,
//  ifTrue: Modifier.() -> Modifier,
//): Modifier = if (conditionProvider()) this.then(ifTrue(Modifier)) else this