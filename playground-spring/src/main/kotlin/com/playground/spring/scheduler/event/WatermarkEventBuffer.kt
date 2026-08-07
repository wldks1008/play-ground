package com.playground.spring.scheduler.event

import java.util.ArrayDeque
import java.util.concurrent.Executor
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * highWatermark에 도달했을 때만 비동기 drain 작업을 시작하는 이벤트 버퍼다.
 *
 * 버퍼가 highWatermark에 도달하면 이후 put 호출은 대기한다. drain 작업이 버퍼를
 * lowWatermark까지 줄이면 대기 중인 생산자를 다시 깨우며, 워커는 버퍼가 빌 때까지 계속 처리한다.
 */
class WatermarkEventBuffer<T>(
    private val highWatermark: Int,
    private val lowWatermark: Int,
    private val workerExecutor: Executor,
    private val itemHandler: (T) -> Unit,
) {
    private val lock = ReentrantLock()
    private val writable = lock.newCondition()
    private val items = ArrayDeque<T>()

    private var paused = false
    private var drainRunning = false

    init {
        require(highWatermark > 0) { "highWatermark must be greater than 0" }
        require(lowWatermark in 0 until highWatermark) {
            "lowWatermark must be greater than or equal to 0 and less than highWatermark"
        }
    }

    fun put(item: T) {
        var shouldStartDrain = false

        lock.withLock {
            while (paused) {
                writable.await()
            }

            items.addLast(item)

            if (items.size >= highWatermark) {
                paused = true

                if (!drainRunning) {
                    drainRunning = true
                    shouldStartDrain = true
                }
            }
        }

        if (shouldStartDrain) {
            startDrainWorker()
        }
    }

    fun size(): Int = lock.withLock { items.size }

    fun isPaused(): Boolean = lock.withLock { paused }

    fun isDrainRunning(): Boolean = lock.withLock { drainRunning }

    private fun startDrainWorker() {
        try {
            workerExecutor.execute(::drain)
        } catch (exception: RuntimeException) {
            lock.withLock {
                drainRunning = false
                paused = false
                writable.signalAll()
            }
            throw exception
        }
    }

    private fun drain() {
        try {
            while (true) {
                val item = lock.withLock {
                    if (items.isEmpty()) {
                        drainRunning = false
                        return
                    }

                    items.removeFirst().also {
                        if (paused && items.size <= lowWatermark) {
                            paused = false
                            writable.signalAll()
                        }
                    }
                }

                itemHandler(item)
            }
        } finally {
            lock.withLock {
                // handler 예외로 워커가 종료돼도 생산자가 영원히 대기하지 않게 한다.
                drainRunning = false
                paused = false
                writable.signalAll()
            }
        }
    }
}
