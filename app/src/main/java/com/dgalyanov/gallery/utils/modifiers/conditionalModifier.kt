package com.dgalyanov.gallery.utils.modifiers

import androidx.compose.ui.Modifier

internal fun Modifier.conditional(
  condition: Boolean,
  ifTrue: Modifier.() -> Modifier,
): Modifier = if (condition) this.then(ifTrue(Modifier)) else this

//internal fun Modifier.conditionalWithElse(
//  condition: Boolean,
//  ifTrue: Modifier.() -> Modifier,
//  ifFalse: Modifier.() -> Modifier,
//): Modifier = if (condition) this.then(ifTrue(Modifier)) else this.then(ifFalse(Modifier))
