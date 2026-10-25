package com.tostrr.data.checklist

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "checklist", primaryKeys = ["orderReference"])
data class ChecklistEntity(
    val orderReference: String,
    val startedAtEpochMs: Long,
    val completedAtEpochMs: Long?,
    val cursor: Int,
)

@Entity(
    tableName = "checklist_answer",
    primaryKeys = ["orderReference", "questionId"],
    foreignKeys = [
        ForeignKey(
            entity = ChecklistEntity::class,
            parentColumns = ["orderReference"],
            childColumns = ["orderReference"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class ChecklistAnswerEntity(
    val orderReference: String,
    val questionId: String,
    /** [com.tostrr.domain.AnswerStatus] name. */
    val status: String,
    val comment: String?,
    val photoPath: String?,
    val answeredAtEpochMs: Long,
)

@Dao
interface ChecklistDao {
    @Query("SELECT * FROM checklist WHERE orderReference = :ref")
    fun observeChecklist(ref: String): Flow<ChecklistEntity?>

    @Query("SELECT * FROM checklist_answer WHERE orderReference = :ref")
    fun observeAnswers(ref: String): Flow<List<ChecklistAnswerEntity>>

    @Query("SELECT * FROM checklist WHERE orderReference = :ref")
    suspend fun checklist(ref: String): ChecklistEntity?

    @Upsert
    suspend fun upsert(checklist: ChecklistEntity)

    @Upsert
    suspend fun upsert(answer: ChecklistAnswerEntity)

    @Query("DELETE FROM checklist_answer WHERE orderReference = :ref")
    suspend fun deleteAnswers(ref: String)

    @Query("DELETE FROM checklist WHERE orderReference = :ref")
    suspend fun deleteChecklist(ref: String)

    @Transaction
    suspend fun delete(ref: String) {
        deleteAnswers(ref)
        deleteChecklist(ref)
    }

    @Transaction
    suspend fun saveAnswer(answer: ChecklistAnswerEntity, checklist: ChecklistEntity) {
        upsert(answer)
        upsert(checklist)
    }
}

@Database(entities = [ChecklistEntity::class, ChecklistAnswerEntity::class], version = 1, exportSchema = false)
abstract class TostrrDatabase : RoomDatabase() {
    abstract fun checklistDao(): ChecklistDao

    companion object {
        fun create(context: Context): TostrrDatabase =
            Room.databaseBuilder(context, TostrrDatabase::class.java, "tostrr.db").build()
    }
}
