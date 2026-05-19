package dam_a52057.wastewatch.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "meal_plans",
    indices = [
        Index(value = ["weekStartDate"]),
        Index(value = ["householdId"]),
        Index(value = ["groupId"])
    ]
)
data class MealPlanEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val weekStartDate: Long, // Timestamp da Segunda-feira correspondente à semana do plano
    val dayOfWeek: String,    // "SEGUNDA", "TERCA", "QUARTA", "QUINTA", "SEXTA", "SABADO", "DOMINGO"
    val mealType: String,     // "MATABIXO", "ALMOCO", "JANTAR"
    val recipeName: String,   // Nome do prato/receita
    val ingredients: String,  // JSON ou lista de strings de ingredientes
    val instructions: String? = null, // Modo de preparação
    val isDone: Boolean = false, // Se a refeição já foi realizada/consumida
    val remoteId: String? = null,
    val householdId: String? = null,
    val groupId: String? = null,
    val lastUpdated: Long = System.currentTimeMillis()
)
