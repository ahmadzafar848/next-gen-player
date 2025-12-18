import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

fun Modifier.glowBorder(
    brush: Brush,
    cornerRadius: Dp = 18.dp,
    alpha: Float = 0.45f
): Modifier = this.then(
    Modifier.drawBehind {
        drawRoundRect(
            brush = brush,
            cornerRadius = CornerRadius(cornerRadius.toPx()),
            alpha = alpha,
            size = size,
            topLeft = androidx.compose.ui.geometry.Offset.Zero
        )
    }
)
