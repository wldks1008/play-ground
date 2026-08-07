package com.playground.spring.scheduler

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import java.util.concurrent.TimeUnit

@SpringBootTest
@ActiveProfiles("parallel")
@Import(SchedulerConcurrencyProbeConfiguration::class)
class ParallelSchedulerTest {
    @Autowired
    private lateinit var probe: SchedulerConcurrencyProbe

    @AfterEach
    fun releaseBlockingJob() {
        probe.releaseBlockingJob.countDown()
    }

    @Test
    fun `스케줄러 풀에 여유 스레드가 있으면 다른 작업이 병렬로 실행된다`() {
        assertThat(probe.blockingJobStarted.await(2, TimeUnit.SECONDS)).isTrue()
        assertThat(probe.blockingJobRunning).isTrue()

        // blockingJob을 해제하지 않아도 두 번째 스레드가 quickJob을 실행한다.
        assertThat(probe.quickJobStarted.await(2, TimeUnit.SECONDS)).isTrue()
        assertThat(probe.blockingJobRunning).isTrue()
        assertThat(probe.quickJobThreadName).isNotEqualTo(probe.blockingJobThreadName)
    }
}
