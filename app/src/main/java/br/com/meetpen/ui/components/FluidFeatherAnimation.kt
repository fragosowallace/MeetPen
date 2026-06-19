package br.com.meetpen.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import br.com.meetpen.ui.theme.AmberMain
import kotlin.math.sin

@Composable
fun FluidFeatherAnimation(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "feather")
    
    // Movimento de balanço lateral
    val sway by infiniteTransition.animateFloat(
        initialValue = -10f,
        targetValue = 10f,
        animationSpec = infiniteTransitionSpec(2000),
        label = "sway"
    )

    // Movimento de "respiração" (escala e intensidade)
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteTransitionSpec(1500),
        label = "pulse"
    )

    Canvas(modifier = modifier.size(200.dp)) {
        val width = size.width
        val height = size.height
        val centerX = width / 2
        val centerY = height / 2

        // Desenhar a haste central (o "cálamo")
        val shaftPath = Path().apply {
            moveTo(centerX, centerY + 80f)
            quadraticTo(
                centerX + sway, centerY,
                centerX + sway * 1.5f, centerY - 80f
            )
        }
        drawPath(
            path = shaftPath,
            color = AmberMain,
            style = Stroke(width = 4f, cap = StrokeCap.Round)
        )

        // Desenhar as barbas da pena (os fios)
        for (i in -10..10) {
            val yOffset = i * 8f
            val isLeft = i % 2 == 0
            val barbLength = (100f - kotlin.math.abs(yOffset)) * pulse
            
            val barbPath = Path().apply {
                val startX = centerX + (sway * (1 - kotlin.math.abs(yOffset)/100f))
                val startY = centerY + yOffset
                moveTo(startX, startY)
                
                val endX = if (isLeft) startX - barbLength else startX + barbLength
                val endY = startY - 20f + (sway / 2)
                
                quadraticTo(
                    if (isLeft) startX - barbLength/2 else startX + barbLength/2,
                    startY - 10f,
                    endX, endY
                )
            }

            drawPath(
                path = barbPath,
                color = AmberMain.copy(alpha = 0.6f - (kotlin.math.abs(i) * 0.04f)),
                style = Stroke(width = 2f, cap = StrokeCap.Round)
            )
        }
    }
}

@Composable
private fun infiniteTransitionSpec(duration: Int) = infiniteRepeatable<Float>(
    animation = tween(duration, easing = LinearEasing),
    repeatMode = RepeatMode.Reverse
)
