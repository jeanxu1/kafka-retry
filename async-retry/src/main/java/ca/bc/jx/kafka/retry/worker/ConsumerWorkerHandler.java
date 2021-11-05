package ca.bc.jx.kafka.retry.worker;

import ca.bc.jx.kafka.retry.domain.RetryProperties;
import ca.bc.jx.kafka.retry.exception.RetryableException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import lombok.*;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static java.time.temporal.ChronoUnit.MILLIS;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
@Log4j2
final class ConsumerWorkerHandler<K, T> {
    private final ConsumerWorker<? super T> consumerWorker;
    private final KafkaTemplate<K, T> kafkaTemplate;
    private final RetryProperties properties;

    private static final String RETRY_HEADER = "XX-KAFKA-RETRY-HEADER";

    private static final ScheduledExecutorService SCHEDULED_EXECUTOR = Executors.newSingleThreadScheduledExecutor();

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().registerModule(new ParameterNamesModule())
            .registerModule(new Jdk8Module())
            .registerModule(new JavaTimeModule());

    @SneakyThrows
    void accept(ConsumerRecord<K, T> consumerRecord) {
        RetryHeader header = getHeaderWithDefault(consumerRecord.headers());
        try {
            if (header.getRetryRemains() >= properties.getMaxRetries()) {
                log.debug("first time see this recording {}", consumerRecord.value());
                consumerWorker.accept(consumerRecord.value());
            } else if (header.getRetryRemains() < 0) {
                log.warn("It shouldn't be here. but max retry reached {}", consumerRecord.value());
                drop(consumerRecord);
            } else if (header.getNextTryTime().isAfter(Instant.now())) {
                log.warn("It shouldn't bee here. to wait a little bit longer");
                nextRetry(consumerRecord, header);
            } else {
                log.info("in retry, {} remains for {}", header.retryRemains, consumerRecord.value());
                consumerWorker.retry(consumerRecord.value());
            }
        } catch (RetryableException e) {
            log.warn("Exception occurred while processing {}", consumerRecord.value(), e);
            nextRetry(consumerRecord, getNextHeader(header.getRetryRemains()));
        } catch (Exception e) {
            drop(consumerRecord);
        }
    }

    private void nextRetry(ConsumerRecord<K, T> consumerRecord, RetryHeader header) {
        if (header.retryRemains < 0) {
            log.warn("max retry reached {}", consumerRecord);
            drop(consumerRecord);
            return;
        }
        Message<T> message = MessageBuilder.withPayload(consumerRecord.value())
                .copyHeaders(header.toMap(consumerRecord.headers()))
                .setHeader(KafkaHeaders.TOPIC, properties.getTopicRetry())
                .build();
        long millis = Duration.between(Instant.now(), header.getNextTryTime()).toMillis();
        SCHEDULED_EXECUTOR.schedule(() -> kafkaTemplate.send(message),
                Math.max(millis, 0), TimeUnit.MILLISECONDS);
    }

    @SneakyThrows
    private RetryHeader getHeaderWithDefault(Headers headers) {
        if (null != headers && null != headers.lastHeader(RETRY_HEADER)
                && null != headers.lastHeader(RETRY_HEADER).value()) {
            return OBJECT_MAPPER.readValue(headers.lastHeader(RETRY_HEADER).value(), RetryHeader.class);
        }
        // if not found, we create a fresh new one
        return new RetryHeader(properties.getMaxRetries(), Instant.now());
    }

    private RetryHeader getNextHeader(int remains) {
        long interval = properties.isFixedInterval() ? properties.getRetryInterval() :
                (properties.getMaxRetries() - remains) * properties.getRetryInterval();
        return new RetryHeader(remains - 1, Instant.now().plus(interval, MILLIS));
    }

    private void drop(ConsumerRecord<K, T> consumerRecord) {
        log.warn("dropping the record {}", consumerRecord.value());
        if (!StringUtils.isEmpty(properties.getTopicDeadLetter())) {
            MessageBuilder<T> messageBuilder = MessageBuilder.withPayload(consumerRecord.value());
            consumerRecord.headers().forEach(header -> messageBuilder.setHeader(header.key(), header.value()));
            kafkaTemplate.send(messageBuilder.setHeader(KafkaHeaders.TOPIC, properties.getTopicDeadLetter()).build());
        }
        consumerWorker.drop(consumerRecord.value());
    }

    @Getter
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class RetryHeader {
        private int retryRemains;
        private Instant nextTryTime;

        @SneakyThrows
        public Map<String, Object> toMap(Headers headers) {
            HashMap<String, Object> hashMap = new HashMap<>();
            for (Header header : headers) {
                hashMap.put(header.key(), header.value());
            }
            hashMap.put(RETRY_HEADER, OBJECT_MAPPER.writeValueAsBytes(this));
            return hashMap;
        }
    }
}
