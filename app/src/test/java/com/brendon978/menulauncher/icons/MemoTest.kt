package com.brendon978.menulauncher.icons

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class MemoTest {
    @Test fun variasThreadsCalculamUmaVezSo() {
        val calls = AtomicInteger()
        val memo = Memo<Int, String> { calls.incrementAndGet(); Thread.sleep(5); "v$it" }
        val pool = Executors.newFixedThreadPool(16)
        val start = CountDownLatch(1)
        repeat(64) { i -> pool.execute { start.await(); memo.get(i % 4) } }
        start.countDown()
        pool.shutdown()
        pool.awaitTermination(10, TimeUnit.SECONDS)
        assertEquals(4, calls.get())
        assertEquals("v2", memo.get(2))
    }

    @Test fun guardaNuloSemRecalcular() {
        val calls = AtomicInteger()
        val memo = Memo<Int, String?> { calls.incrementAndGet(); null }
        assertNull(memo.get(1))
        assertNull(memo.get(1))
        assertEquals(1, calls.get())
    }
}
