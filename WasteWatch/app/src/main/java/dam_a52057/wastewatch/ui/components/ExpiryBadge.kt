package dam_a52057.wastewatch.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import java.util.concurrent.TimeUnit

@Composable
fun ExpiryBadge(expiryDate: Long) {
    val now = System.currentTimeMillis()
    val diffMillis = expiryDate - now
    val daysRemaining = TimeUnit.MILLISECONDS.toDays(diffMillis)

    val (label, color) = when {
        daysRemaining < 0  -> "Expirou há ${-daysRemaining} dia(s)" to Color(0xFFB71C1C)
        daysRemaining == 0L -> "Expira hoje!" to Color(0xFFB71C1C)
        daysRemaining == 1L -> "Expira amanhã" to Color(0xFFF57F17)
        daysRemaining <= 3  -> "Expira em $daysRemaining dias" to Color(0xFFF57F17)
        else               -> "OK – $daysRemaining dias" to Color(0xFF2E7D32)
    }

    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = Color.White,
        modifier = Modifier
            .background(color = color, shape = RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    )
}
