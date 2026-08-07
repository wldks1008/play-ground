package com.playground.spring.scheduler

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import java.util.concurrent.TimeUnit

@SpringBootTest
@Import(SchedulerConcurrencyProbeConfiguration::class)
class SingleThreadSchedulerTest {
    @Autowired
    private lateinit var probe: SchedulerConcurrencyProbe

    @AfterEach
    fun releaseBlockingJob() {
        probe.releaseBlockingJob.countDown()
    }

    @Test
    fun `느린 작업이 실행 중이면 다른 스케줄 작업도 시작하지 못한다`() {
        assertThat(probe.blockingJobStarted.await(2, TimeUnit.SECONDS)).isTrue()
        assertThat(probe.blockingJobRunning).isTrue()

        // quickJob의 initialDelay(200ms)가 지났지만 유일한 스레드는 blockingJob이 점유 중이다.
        assertThat(probe.quickJobStarted.await(500, TimeUnit.MILLISECONDS)).isFalse()

        probe.releaseBlockingJob.countDown()

        assertThat(probe.quickJobStarted.await(2, TimeUnit.SECONDS)).isTrue()
        assertThat(probe.quickJobThreadName).isEqualTo(probe.blockingJobThreadName)
    }
}
