package com.dgalyanov.gallery.utils

import androidx.compose.ui.Modifier

fun Modifier.conditional(
  condition: Boolean,
  ifTrue: Modifier.() -> Modifier,
): Modifier = if (condition) this.then(ifTrue(Modifier)) else this

//fun Modifier.conditionalWithElse(
//  condition: Boolean,
//  ifTrue: Modifier.() -> Modifier,
//  ifFalse: Modifier.() -> Modifier,
//): Modifier = if (condition) this.then(ifTrue(Modifier)) else this.then(ifFalse(Modifier))
