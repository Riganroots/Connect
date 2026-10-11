package com.example

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import com.example.data.database.ConnectDatabase
import com.example.data.models.AppNotification
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class NotificationIsolationTest {
    @Test fun accountsCannotReadOrModifyEachOthersNotifications() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), ConnectDatabase::class.java).build()
        try {
            val dao = db.connectDao()
            for (uid in listOf("account-a", "account-b", "preview-user", "")) {
                dao.insertNotification(AppNotification(ownerId = uid, title = uid, description = "Reminder", systemCategory = "Reminder"))
            }
            val a = dao.getNotificationsFlow("account-a").first().single()
            assertEquals("account-a", a.title)
            assertEquals("account-b", dao.getNotificationsFlow("account-b").first().single().title)
            dao.markNotificationAsRead(a.id, "account-b")
            assertFalse(dao.getNotificationsFlow("account-a").first().single().isRead)
            dao.deleteNotification(a.id, "account-b")
            assertEquals(1, dao.getNotificationsFlow("account-a").first().size)
            dao.deleteAllNotifications("account-b")
            assertTrue(dao.getNotificationsFlow("account-b").first().isEmpty())
            assertEquals(1, dao.getNotificationsFlow("account-a").first().size)
            assertEquals(1, dao.getNotificationsFlow("preview-user").first().size)
            dao.markNotificationAsRead(a.id, "account-a")
            assertTrue(dao.getNotificationsFlow("account-a").first().single().isRead)
        } finally { db.close() }
    }

    @Test fun legacyMigrationPreservesUnownedRowsWithoutAssigningAnAccount() {
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(ApplicationProvider.getApplicationContext())
                .callback(object : SupportSQLiteOpenHelper.Callback(3) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        db.execSQL("CREATE TABLE notifications (id INTEGER PRIMARY KEY, title TEXT NOT NULL)")
                        db.execSQL("INSERT INTO notifications VALUES (7, 'Legacy reminder')")
                    }
                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
                }).build())
        try {
            val db = helper.writableDatabase
            ConnectDatabase.MIGRATION_3_4.migrate(db)
            db.query("SELECT id, title, ownerId FROM notifications").use {
                assertTrue(it.moveToFirst())
                assertEquals(7, it.getInt(0))
                assertEquals("Legacy reminder", it.getString(1))
                assertEquals("", it.getString(2))
            }
            db.query("SELECT id FROM notifications WHERE ownerId = 'account-a'").use { assertFalse(it.moveToFirst()) }
        } finally { helper.close() }
    }
}
