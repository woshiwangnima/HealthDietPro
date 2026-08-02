package com.woshiwangnima.healthdietpro.model.archive

import org.junit.Assert.assertEquals
import org.junit.Test

class StableJsonTest {
    @Test
    fun `sorts object keys recursively while retaining array order`() {
        assertEquals(
            "{\"a\":{\"a\":1,\"b\":2},\"list\":[{\"a\":2,\"z\":1},{\"a\":4,\"z\":3}],\"z\":0}",
            stableJsonString("{\"z\":0,\"list\":[{\"z\":1,\"a\":2},{\"z\":3,\"a\":4}],\"a\":{\"b\":2,\"a\":1}}", prettyPrint = false),
        )
    }

    @Test
    fun `leaves non JSON plaintext unchanged`() {
        assertEquals("not json", stableJsonString("not json", prettyPrint = true))
    }

    @Test
    fun `keeps bundle contract keys ordered while sorting nested objects`() {
        assertEquals(
            "{\"formatVersion\":{},\"appVersion\":\"test\",\"sourceUserId\":\"user\",\"metadata\":{\"a\":1,\"z\":2}}",
            stableUserArchiveJsonString(
                "{\"metadata\":{\"z\":2,\"a\":1},\"sourceUserId\":\"user\",\"appVersion\":\"test\",\"formatVersion\":{}}",
            ),
        )
    }
}
