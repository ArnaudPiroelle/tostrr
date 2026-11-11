package app.tostrr.data.checklist

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
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "checklist", primaryKeys = ["orderReference"])
data class ChecklistEntity(
    val orderReference: String,
    val startedAtEpochMs: Long,
    val completedAtEpochMs: Long?,
    val cursor: Int,
    val lockedAtEpochMs: Long? = null,
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
    /** [app.tostrr.domain.checklist.AnswerStatus] name. */
    val status: String,
    val comment: String?,
    /** The photos' paths, one per line, in their order; empty without photo. */
    val photoPaths: String,
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

@Database(entities = [ChecklistEntity::class, ChecklistAnswerEntity::class], version = 2, exportSchema = false)
abstract class TostrrDatabase : RoomDatabase() {
    abstract fun checklistDao(): ChecklistDao

    companion object {
        fun create(context: Context): TostrrDatabase =
            Room.databaseBuilder(context, TostrrDatabase::class.java, "tostrr.db").addMigrations(MIGRATION_1_2).build()

        /**
         * Version 2: a checklist can be locked, and an answer has several photos. SQLite can't change a column, so the
         * answers move to a new table whose `photoPaths` takes the single `photoPath` of version 1.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `checklist` ADD COLUMN `lockedAtEpochMs` INTEGER")
                db.execSQL(
                    "CREATE TABLE `checklist_answer_new` (`orderReference` TEXT NOT NULL, `questionId` TEXT NOT NULL, " +
                        "`status` TEXT NOT NULL, `comment` TEXT, `photoPaths` TEXT NOT NULL, `answeredAtEpochMs` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`orderReference`, `questionId`), FOREIGN KEY(`orderReference`) REFERENCES `checklist`(`orderReference`) " +
                        "ON UPDATE NO ACTION ON DELETE CASCADE )",
                )
                db.execSQL(
                    "INSERT INTO `checklist_answer_new` (`orderReference`, `questionId`, `status`, `comment`, `photoPaths`, `answeredAtEpochMs`) " +
                        "SELECT `orderReference`, `questionId`, `status`, `comment`, COALESCE(`photoPath`, ''), `answeredAtEpochMs` FROM `checklist_answer`",
                )
                db.execSQL("DROP TABLE `checklist_answer`")
                db.execSQL("ALTER TABLE `checklist_answer_new` RENAME TO `checklist_answer`")
            }
        }
    }
}
