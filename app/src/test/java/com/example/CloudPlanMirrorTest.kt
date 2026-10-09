package com.example

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.database.ConnectDatabase
import com.example.data.models.Plan
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CloudPlanMirrorTest {
    private fun plan(id: String = "walk") = Plan(cloudId = id, organizerId = "host", title = "Walk",
        category = "Explore", location = "Thamel", date = "Tomorrow", time = "8 AM",
        pricePerPerson = "Free", participantsNeeded = 4, description = "Meet at the gate", organizerName = "Host")

    @Test fun concurrentPublishAndListenerUseOneStableLocalRow() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), ConnectDatabase::class.java).build()
        try {
            val dao = db.connectDao()
            (1..10).map { async { dao.upsertCloudPlan(plan()) } }.awaitAll()
            val first = dao.getAllPlansFlow().first().single()
            dao.syncCloudPlans(listOf(plan().copy(isSaved = true, joinedCount = 2)))
            val updated = dao.getAllPlansFlow().first().single()
            assertEquals(first.id, updated.id)
            assertTrue(updated.isSaved)
            assertEquals(2, updated.joinedCount)
            dao.insertPlan(plan().copy(id = 0, cloudId = ""))
            dao.syncCloudPlans(emptyList())
            assertEquals("", dao.getAllPlansFlow().first().single().cloudId)
        } finally { db.close() }
    }
}
