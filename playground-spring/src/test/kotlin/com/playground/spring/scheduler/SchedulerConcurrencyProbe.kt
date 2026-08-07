package com.playground.spring.scheduler

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.scheduling.annotation.Scheduled
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

@TestConfiguration(proxyBeanMethods = false)
class SchedulerConcurrencyProbeConfiguration {
    @Bean
    fun schedulerConcurrencyProbe(): SchedulerConcurrencyProbe = SchedulerConcurrencyProbe()
}

class SchedulerConcurrencyProbe {
    val blockingJobStarted = CountDownLatch(1)
    val releaseBlockingJob = CountDownLatch(1)
    val quickJobStarted = CountDownLatch(1)
    val blockingJobRunning = AtomicBoolean(false)

    @Volatile
    var blockingJobThreadName: String? = null
        private set

    @Volatile
    var quickJobThreadName: String? = null
        private set

    @Scheduled(initialDelay = 0, fixedDelay = 60_000)
    fun blockingJob() {
        blockingJobThreadName = Thread.currentThread().name
        blockingJobRunning.set(true)
        blockingJobStarted.countDown()

        try {
            releaseBlockingJob.await(10, TimeUnit.SECONDS)
        } finally {
            blockingJobRunning.set(false)
        }
    }

    @Scheduled(initialDelay = 200, fixedDelay = 60_000)
    fun quickJob() {
        quickJobThreadName = Thread.currentThread().name
        quickJobStarted.countDown()
    }
}
