package app.tostrr.data.checklist

import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import app.tostrr.domain.checklist.AnswerStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class ChecklistDatabaseMigrationTest {
    private val context = RuntimeEnvironment.getApplication()

    /** A database as version 1 left it, with one answer with a photo and one without. */
    private fun createVersion1(name: String) {
        val file = context.getDatabasePath(name).apply { parentFile!!.mkdirs(); delete() }
        SQLiteDatabase.openOrCreateDatabase(file, null).use { db ->
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `checklist` (`orderReference` TEXT NOT NULL, `startedAtEpochMs` INTEGER NOT NULL, " +
                    "`completedAtEpochMs` INTEGER, `cursor` INTEGER NOT NULL, PRIMARY KEY(`orderReference`))",
            )
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `checklist_answer` (`orderReference` TEXT NOT NULL, `questionId` TEXT NOT NULL, " +
                    "`status` TEXT NOT NULL, `comment` TEXT, `photoPath` TEXT, `answeredAtEpochMs` INTEGER NOT NULL, " +
                    "PRIMARY KEY(`orderReference`, `questionId`), FOREIGN KEY(`orderReference`) REFERENCES `checklist`(`orderReference`) " +
                    "ON UPDATE NO ACTION ON DELETE CASCADE )",
            )
            db.execSQL("INSERT INTO checklist VALUES ('RN1', 1, 2, 0)")
            db.execSQL("INSERT INTO checklist_answer VALUES ('RN1', 'exterior-01', 'VERIFIED', 'rayure', '/p/a.jpg', 3)")
            db.execSQL("INSERT INTO checklist_answer VALUES ('RN1', 'exterior-02', 'SKIPPED', NULL, NULL, 4)")
            db.version = 1
        }
    }

    @Test fun `version 1 answers keep their photo, as the first of a list, and checklists start unlocked`() = runTest {
        createVersion1("migration.db")
        val database = Room.databaseBuilder(context, TostrrDatabase::class.java, "migration.db")
            .addMigrations(TostrrDatabase.MIGRATION_1_2).allowMainThreadQueries().build()
        try {
            val checklist = ChecklistRepositoryImpl(database.checklistDao(), context).observe("RN1").first()!!
            assertFalse(checklist.locked)
            assertEquals(2L, checklist.completedAtEpochMs)
            val withPhoto = checklist.answers.getValue("exterior-01")
            assertEquals(listOf("/p/a.jpg"), withPhoto.photoPaths)
            assertEquals("rayure", withPhoto.comment)
            assertEquals(AnswerStatus.VERIFIED, withPhoto.status)
            assertEquals(emptyList<String>(), checklist.answers.getValue("exterior-02").photoPaths)
        } finally {
            database.close()
        }
    }
}
