package com.woshiwangnima.healthdietpro.model.profile

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

class BodyRecordSerializationTest {
    @Test
    fun javaSerializationPreservesBaseValueAndSelectedUnit() {
        val records = arrayListOf(
            BodyRecord("2026-07-19 08:30", 170.18f, "m"),
            BodyRecord("2026-07-19 08:30", 68.04f, "lb"),
        )

        val bytes = ByteArrayOutputStream().use { output ->
            ObjectOutputStream(output).use { it.writeObject(records) }
            output.toByteArray()
        }
        val restored = ObjectInputStream(ByteArrayInputStream(bytes)).use { input ->
            @Suppress("UNCHECKED_CAST")
            input.readObject() as ArrayList<BodyRecord>
        }

        assertEquals(records, restored)
    }
}
