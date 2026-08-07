package com.playground.spring.scheduler.event

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.task.TaskExecutor
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.time.Duration

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(
    prefix = "study.event-buffer",
    name = ["enabled"],
    havingValue = "true",
)
class EventDrivenBufferConfiguration {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Bean
    fun eventBufferExecutor(): ThreadPoolTaskExecutor = ThreadPoolTaskExecutor().apply {
        corePoolSize = 1
        maxPoolSize = 1
        setQueueCapacity(1)
        setThreadNamePrefix("event-buffer-worker-")
    }

    @Bean
    fun eventBuffer(
        @Qualifier("eventBufferExecutor") workerExecutor: TaskExecutor,
        @Value("\${study.event-buffer.high-watermark:5}") highWatermark: Int,
        @Value("\${study.event-buffer.low-watermark:2}") lowWatermark: Int,
        @Value("\${study.event-buffer.item-processing-duration:500ms}") processingDuration: Duration,
    ): WatermarkEventBuffer<String> = WatermarkEventBuffer(
        highWatermark = highWatermark,
        lowWatermark = lowWatermark,
        workerExecutor = workerExecutor,
    ) { item ->
        logger.info("processing item={} | thread={}", item, Thread.currentThread().name)
        Thread.sleep(processingDuration.toMillis())
    }

    @Bean
    @ConditionalOnProperty(
        prefix = "study.event-buffer",
        name = ["demo-enabled"],
        havingValue = "true",
    )
    fun eventBufferDemo(eventBuffer: WatermarkEventBuffer<String>): ApplicationRunner =
        ApplicationRunner { _: ApplicationArguments ->
            repeat(10) { index ->
                val item = "event-${index + 1}"
                logger.info("put requested item={} | bufferSize={}", item, eventBuffer.size())
                eventBuffer.put(item)
                logger.info("put completed item={} | bufferSize={}", item, eventBuffer.size())
            }
        }
}
