package com.apps.bubblepopping.ui.icons

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import com.apps.bubblepopping.Res
import com.apps.bubblepopping.play
import org.jetbrains.compose.resources.painterResource

/** The Play icon from the art (play.png), for every button that starts or resumes play. */
@Composable
fun playIcon(): Painter = painterResource(Res.drawable.play)
