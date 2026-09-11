package io.github.straxess.retrykt.internal

import kotlin.test.Test
import kotlin.test.assertEquals

class SleepMingwTest {

    @Test
    fun `maximum finite sleep value does not use Windows infinite timeout`() {
        assertEquals(UInt.MAX_VALUE - 1u, MAX_FINITE_SLEEP_MILLIS.toUInt())
    }
}
