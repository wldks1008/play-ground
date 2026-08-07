package com.playground.spring.scheduler.event

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class WatermarkEventBufferTest {
    @Test
    fun `상한선에 도달하기 전에는 워커를 실행하지 않는다`() {
        val workerStarted = CountDownLatch(1)
        val workerExecutor = Executors.newSingleThreadExecutor()

        try {
            val buffer = WatermarkEventBuffer<Int>(
                highWatermark = 3,
                lowWatermark = 1,
                workerExecutor = workerExecutor,
            ) {
                workerStarted.countDown()
            }

            buffer.put(1)
            buffer.put(2)

            assertThat(workerStarted.await(300, TimeUnit.MILLISECONDS)).isFalse()
            assertThat(buffer.size()).isEqualTo(2)
            assertThat(buffer.isDrainRunning()).isFalse()
        } finally {
            workerExecutor.shutdownNow()
        }
    }

    @Test
    fun `상한선에서 워커를 트리거하고 하한선까지 비우면 막힌 생산자를 다시 연다`() {
        val firstItemProcessingStarted = CountDownLatch(1)
        val allowProcessing = CountDownLatch(1)
        val initialItemsProcessed = CountDownLatch(3)
        val processedItems = CopyOnWriteArrayList<Int>()
        val workerThreadName = CopyOnWriteArrayList<String>()
        val workerExecutor = Executors.newSingleThreadExecutor()
        val producerExecutor = Executors.newSingleThreadExecutor()

        try {
            val buffer = WatermarkEventBuffer(
                highWatermark = 3,
                lowWatermark = 1,
                workerExecutor = workerExecutor,
            ) { item: Int ->
                workerThreadName.add(Thread.currentThread().name)
                firstItemProcessingStarted.countDown()
                allowProcessing.await(2, TimeUnit.SECONDS)
                processedItems.add(item)
                if (item <= 3) {
                    initialItemsProcessed.countDown()
                }
            }

            buffer.put(1)
            buffer.put(2)
            buffer.put(3)

            assertThat(firstItemProcessingStarted.await(2, TimeUnit.SECONDS)).isTrue()
            assertThat(buffer.isPaused()).isTrue()

            val blockedPut = producerExecutor.submit { buffer.put(4) }
            assertThatThrownBy { blockedPut.get(300, TimeUnit.MILLISECONDS) }
                .isInstanceOf(TimeoutException::class.java)

            allowProcessing.countDown()

            assertThat(blockedPut.get(2, TimeUnit.SECONDS)).isNull()
            assertThat(initialItemsProcessed.await(2, TimeUnit.SECONDS)).isTrue()
            assertThat(processedItems).contains(1, 2, 3)
            assertThat(buffer.isPaused()).isFalse()
            assertThat(workerThreadName).allMatch { it != Thread.currentThread().name }
        } finally {
            allowProcessing.countDown()
            producerExecutor.shutdownNow()
            workerExecutor.shutdownNow()
        }
    }
}
