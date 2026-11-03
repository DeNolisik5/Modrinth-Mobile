package myxax.modrinth.client.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin

/**
 * Волнистый индикатор прогресса в духе Material 3 Expressive.
 *
 * Рисуется вручную на Canvas: «рельс» — прямая линия, а заполненная часть —
 * бегущая синусоида, обрезанная по текущему прогрессу. Анимация полностью
 * на стороне композиции, без перерисовки всего списка.
 */
@Composable
fun WavyLinearProgressIndicator(
    progress: () -> Float,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
    strokeWidth: Dp = 6.dp,
    amplitude: Dp = 3.dp,
    wavelength: Dp = 22.dp
) {
    val target = progress().coerceIn(0f, 1f)

    val animated by animateFloatAsState(
        targetValue = target,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 240f),
        label = "waveProgress"
    )

    val transition = rememberInfiniteTransition(label = "wave")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing)
        ),
        label = "wavePhase"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(strokeWidth + amplitude * 2)
    ) {
        val stroke = strokeWidth.toPx()
        val swing = amplitude.toPx()
        val waveLength = wavelength.toPx()
        val centerY = size.height / 2f
        val tau = (2 * PI).toFloat()

        // «рельс» — светлая линия на всю ширину
        drawLine(
            color = trackColor,
            start = Offset(stroke / 2f, centerY),
            end = Offset(size.width - stroke / 2f, centerY),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )

        val filled = size.width * animated
        if (filled <= stroke) return@Canvas

        val path = Path()
        path.moveTo(0f, centerY)

        var x = 0f
        while (x <= filled) {
            val angle = (x / waveLength + phase) * tau
            path.lineTo(x, centerY + sin(angle) * swing)
            x += 2f
        }

        clipRect(right = filled) {
            drawPath(
                path = path,
                color = color,
                style = Stroke(
                    width = stroke,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }
    }
}
