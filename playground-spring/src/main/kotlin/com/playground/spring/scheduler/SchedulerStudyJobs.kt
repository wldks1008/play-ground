package com.playground.spring.scheduler

import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.Instant

@Component
@ConditionalOnProperty(
    prefix = "study.scheduler",
    name = ["enabled"],
    havingValue = "true",
)
class SchedulerStudyJobs(
    @Value("\${study.scheduler.slow-job.work-duration:4s}")
    private val slowJobWorkDuration: Duration,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Scheduled(
        fixedRateString = "\${study.scheduler.slow-job.fixed-rate:5s}",
        initialDelayString = "\${study.scheduler.slow-job.initial-delay:0s}",
    )
    fun slowJob() {
        log("slow job started")
        Thread.sleep(slowJobWorkDuration.toMillis())
        log("slow job finished")
    }

    @Scheduled(
        fixedRateString = "\${study.scheduler.heartbeat.fixed-rate:1s}",
        initialDelayString = "\${study.scheduler.heartbeat.initial-delay:500ms}",
    )
    fun heartbeat() {
        log("heartbeat")
    }

    private fun log(message: String) {
        logger.info(
            "{} | thread={} | time={}",
            message,
            Thread.currentThread().name,
            Instant.now(),
        )
    }
}
