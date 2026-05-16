package dam_a52057.wastewatch.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dam_a52057.wastewatch.R
import dam_a52057.wastewatch.data.repository.InventoryRepository
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

@HiltWorker
class ExpiryNotificationWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val inventoryRepository: InventoryRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return Result.success()
        }

        createNotificationChannel()

        val itemsWithProduct = inventoryRepository.getAllActiveItemsWithProduct().first()
        val now = Instant.now()
        val today = LocalDate.now()
        val zoneId = ZoneId.systemDefault()

        val urgentItems = itemsWithProduct.filter { 
            val expiryInstant = Instant.ofEpochMilli(it.item.expiryDate)
            val expiryDate = expiryInstant.atZone(zoneId).toLocalDate()
            val daysBetween = ChronoUnit.DAYS.between(today, expiryDate)
            
            daysBetween <= 2
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        var notificationId = 1

        for (itemInfo in urgentItems) {
            val expiryInstant = Instant.ofEpochMilli(itemInfo.item.expiryDate)
            val expiryDate = expiryInstant.atZone(zoneId).toLocalDate()
            val daysRemaining = ChronoUnit.DAYS.between(today, expiryDate)
            
            val message = when {
                daysRemaining < 0 -> "${itemInfo.product.name} expirou há ${-daysRemaining} dia(s)!"
                daysRemaining == 0L -> "${itemInfo.product.name} expira hoje!"
                daysRemaining == 1L -> "${itemInfo.product.name} expira amanhã!"
                else -> "${itemInfo.product.name} expira em $daysRemaining dias!"
            }

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_alert) // Fallback icon
                .setContentTitle("Alerta de Validade")
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build()

            notificationManager.notify(notificationId++, notification)
        }

        return Result.success()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Avisos de Validade"
            val descriptionText = "Notificações para produtos a expirar"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "EXPIRY_ALERTS"
    }
}
