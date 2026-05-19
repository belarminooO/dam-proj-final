package dam_a52057.wastewatch.data.local.dao

import androidx.room.*
import dam_a52057.wastewatch.data.local.entity.MealPlanEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MealPlanDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: MealPlanEntity): Long

    @Update
    suspend fun update(item: MealPlanEntity)

    @Query("DELETE FROM meal_plans WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("DELETE FROM meal_plans")
    suspend fun clearAll()

    @Query("SELECT * FROM meal_plans WHERE id = :id")
    suspend fun getById(id: Int): MealPlanEntity?

    @Query("SELECT * FROM meal_plans WHERE remoteId = :remoteId")
    suspend fun getByRemoteId(remoteId: String): MealPlanEntity?

    @Query("DELETE FROM meal_plans WHERE remoteId = :remoteId")
    suspend fun deleteByRemoteId(remoteId: String)

    @Query("SELECT * FROM meal_plans WHERE weekStartDate = :weekStartDate AND householdId IS NULL AND groupId IS NULL")
    fun getPersonalMealPlansForWeek(weekStartDate: Long): Flow<List<MealPlanEntity>>

    @Query("SELECT * FROM meal_plans WHERE weekStartDate = :weekStartDate AND householdId = :householdId")
    fun getHouseholdMealPlansForWeek(householdId: String, weekStartDate: Long): Flow<List<MealPlanEntity>>

    @Query("SELECT * FROM meal_plans WHERE weekStartDate = :weekStartDate AND groupId = :groupId")
    fun getGroupMealPlansForWeek(groupId: String, weekStartDate: Long): Flow<List<MealPlanEntity>>
}
