package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.animation.IosMotion
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

private const val MORPH_DIALOG_START_SCALE = 0.12f

@Composable
fun MorphDialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    properties: DialogProperties = DialogProperties(usePlatformDefaultWidth = false),
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val reduceMotion = remember(context) { IosMotion.isReducedMotion(context) }
    val currentOnDismissRequest by rememberUpdatedState(onDismissRequest)

    var closeRequested by remember { mutableStateOf(false) }

    val scaleX = remember { Animatable(if (reduceMotion) 1f else MORPH_DIALOG_START_SCALE) }
    val scaleY = remember { Animatable(if (reduceMotion) 1f else MORPH_DIALOG_START_SCALE) }
    val alpha = remember { Animatable(if (reduceMotion) 1f else 0f) }

    LaunchedEffect(closeRequested, reduceMotion) {
        if (!closeRequested) {
            if (reduceMotion) {
                scaleX.snapTo(1f)
                scaleY.snapTo(1f)
                alpha.snapTo(1f)
            } else {
                scaleX.snapTo(MORPH_DIALOG_START_SCALE)
                scaleY.snapTo(MORPH_DIALOG_START_SCALE)
                alpha.snapTo(0f)
                coroutineScope {
                    launch {
                        alpha.animateTo(
                            targetValue = 1f,
                            animationSpec = tween(durationMillis = 90),
                        )
                    }
                    launch {
                        scaleX.animateTo(
                            targetValue = 1f,
                            animationSpec = spring(dampingRatio = 0.72f, stiffness = 700f),
                        )
                    }
                    launch {
                        scaleY.animateTo(
                            targetValue = 1f,
                            animationSpec = spring(dampingRatio = 0.78f, stiffness = 420f),
                        )
                    }
                }
            }
        } else {
            if (reduceMotion) {
                closeRequested = false
                currentOnDismissRequest()
            } else {
                coroutineScope {
                    launch {
                        scaleX.animateTo(
                            targetValue = MORPH_DIALOG_START_SCALE,
                            animationSpec = spring(dampingRatio = 1f, stiffness = 900f),
                        )
                    }
                    launch {
                        scaleY.animateTo(
                            targetValue = MORPH_DIALOG_START_SCALE,
                            animationSpec = spring(dampingRatio = 1f, stiffness = 900f),
                        )
                    }
                    launch {
                        alpha.animateTo(
                            targetValue = 0f,
                            animationSpec = tween(durationMillis = 140, delayMillis = 40),
                        )
                    }
                }
                closeRequested = false
                currentOnDismissRequest()
            }
        }
    }

    Dialog(
        onDismissRequest = {
            if (!closeRequested) {
                closeRequested = true
            }
        },
        properties = properties,
    ) {
        BackHandler(enabled = properties.dismissOnBackPress && !closeRequested) {
            closeRequested = true
        }

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            if (properties.dismissOnClickOutside) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                if (!closeRequested) {
                                    closeRequested = true
                                }
                            },
                        ),
                )
            }

            Box(
                modifier = modifier
                    .graphicsLayer {
                        this.scaleX = if (reduceMotion) 1f else scaleX.value
                        this.scaleY = if (reduceMotion) 1f else scaleY.value
                        this.alpha = if (reduceMotion) 1f else alpha.value
                        this.transformOrigin = TransformOrigin.Center
                    },
            ) {
                content()
            }
        }
    }
}

@Composable
fun MorphAlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: @Composable (() -> Unit)? = null,
    icon: @Composable (() -> Unit)? = null,
    title: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
    shape: Shape = AlertDialogDefaults.shape,
    containerColor: Color = AlertDialogDefaults.containerColor,
    tonalElevation: Dp = AlertDialogDefaults.TonalElevation,
    properties: DialogProperties = DialogProperties(usePlatformDefaultWidth = false),
) {
    MorphDialog(
        onDismissRequest = onDismissRequest,
        properties = properties,
    ) {
        Surface(
            modifier = modifier
                .padding(horizontal = 24.dp)
                .widthIn(min = 280.dp, max = 560.dp)
                .fillMaxWidth(),
            shape = shape,
            color = containerColor,
            tonalElevation = tonalElevation,
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
            ) {
                if (icon != null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(bottom = 16.dp),
                    ) {
                        CompositionLocalProvider(LocalContentColor provides AlertDialogDefaults.iconContentColor) {
                            icon()
                        }
                    }
                }
                if (title != null) {
                    Box(
                        modifier = Modifier
                            .align(if (icon == null) Alignment.Start else Alignment.CenterHorizontally)
                            .padding(bottom = 16.dp),
                    ) {
                        CompositionLocalProvider(LocalContentColor provides AlertDialogDefaults.titleContentColor) {
                            ProvideTextStyle(MaterialTheme.typography.headlineSmall) {
                                title()
                            }
                        }
                    }
                }
                if (text != null) {
                    Box(
                        modifier = Modifier
                            .weight(weight = 1f, fill = false)
                            .padding(bottom = 24.dp)
                            .align(Alignment.Start),
                    ) {
                        CompositionLocalProvider(LocalContentColor provides AlertDialogDefaults.textContentColor) {
                            ProvideTextStyle(MaterialTheme.typography.bodyMedium) {
                                text()
                            }
                        }
                    }
                }
                Row(
                    modifier = Modifier.align(Alignment.End),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.primary) {
                        ProvideTextStyle(MaterialTheme.typography.labelLarge) {
                            dismissButton?.invoke()
                            confirmButton()
                        }
                    }
                }
            }
        }
    }
}
