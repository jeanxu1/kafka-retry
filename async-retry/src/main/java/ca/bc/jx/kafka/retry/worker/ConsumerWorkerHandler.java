package ca.bc.jx.kafka.retry.worker;

import ca.bc.jx.kafka.retry.exception.RetryableException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import lombok.*;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static java.time.temporal.ChronoUnit.MILLIS;

@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
@Log4j2
public class RetryableWorkerHandler<T> {
    protected final RetryableWorker<? super T> retryable;
    protected final KafkaTemplate<?, T> kafkaTemplate;
    protected final RetryProperties properties;

    protected static final String RETRY_HEADER = "XX-KAFKA-RETRY-HEADER";
    protected static ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
    protected static ObjectMapper objectMapper = new ObjectMapper().registerModule(new ParameterNamesModule())
            .registerModule(new Jdk8Module())
            .registerModule(new JavaTimeModule());

    protected Pair<ContainerProperties, ContainerProperties> prepareContainerProperties() {
        ContainerProperties main = new ContainerProperties(properties.getMainTopic());
        ContainerProperties retry = new ContainerProperties(properties.getRetryTopic());
        main.setMessageListener((MessageListener<Object, T>) this::accept);
        retry.setMessageListener((MessageListener<Object, T>) this::accept);
        return Pair.of(main, retry);
    }

    @SneakyThrows
    protected void accept(ConsumerRecord<Object, T> consumerRecord) {
        RetryHeader header = getHeader(consumerRecord.headers());
        if (null == header) {
            header = new RetryHeader(properties.getMaxRetries(), Instant.now());
        }
        try {
            if (header.getRetryRemains() >= properties.getMaxRetries()) {
                log.debug("first time see this recording {}", consumerRecord);
                retryable.accept(consumerRecord.value());
            } else if (header.getRetryRemains() < 0) {
                log.warn("It shouldn't be here. but max retry reached {}", consumerRecord);
                drop(consumerRecord);
            } else if (header.getNextTryTime().isAfter(Instant.now())) {
                log.info("need to wait a little bit longer");
                nextRetry(consumerRecord, header);
            } else {
                log.info("in retry, {} remains for {}", header.retryRemains, consumerRecord);
                retryable.retry(consumerRecord.value());
            }
        } catch (RetryableException e) {
            log.warn("Exception occurred while processing {}", consumerRecord, e);
            nextRetry(consumerRecord, getNextHeader(header.getRetryRemains()));
        } catch (Exception e) {
            drop(consumerRecord);
        }
    }

    protected void nextRetry(ConsumerRecord<Object, T> consumerRecord, RetryHeader header) {
        if (header.retryRemains <= 0) {
            log.warn("max retry reached {}", consumerRecord);
            drop(consumerRecord);
            return;
        }
        Message<T> message = MessageBuilder.withPayload(consumerRecord.value())
                .copyHeaders(header.toMap(consumerRecord.headers()))
                .setHeader(KafkaHeaders.TOPIC, properties.getRetryTopic())
                .build();
        long millis = Duration.between(Instant.now(), header.getNextTryTime()).toMillis();
        executorService.schedule(() -> kafkaTemplate.send(message),
                Math.max(millis, 0), TimeUnit.MILLISECONDS);
    }

    @SneakyThrows
    protected RetryHeader getHeader(Headers headers) {
        if (null != headers && null != headers.lastHeader(RETRY_HEADER)
                && null != headers.lastHeader(RETRY_HEADER).value()) {
            return objectMapper.readValue(headers.lastHeader(RETRY_HEADER).value(), RetryHeader.class);
        }
        return null;
    }

    protected RetryHeader getNextHeader(int remains) {
        long interval = properties.isFixedInterval() ? properties.getRetryInterval() :
                (properties.getMaxRetries() - remains) * properties.getRetryInterval();
        return new RetryHeader(remains - 1,
                Instant.now().plus(interval, MILLIS));
    }

    protected void drop(ConsumerRecord<Object, T> consumerRecord) {
        log.warn("dropping the record {}", consumerRecord);
        if (!StringUtils.isEmpty(properties.getDlQTopic())) {
            MessageBuilder<T> messageBuilder = MessageBuilder.withPayload(consumerRecord.value());
            consumerRecord.headers().forEach(header -> messageBuilder.setHeader(header.key(), header.value()));
            kafkaTemplate.send(messageBuilder.setHeader(KafkaHeaders.TOPIC, properties.getDlQTopic()).build());
        }
        retryable.drop(consumerRecord.value());
    }

    @AllArgsConstructor
    @Getter
    static class RetryHeader {
        private int retryRemains;
        private Instant nextTryTime;

        @SneakyThrows
        public HashMap<String, Object> toMap(Headers headers) {
            HashMap<String, Object> hashMap = new HashMap<>();
            for (Header header : headers) {
                hashMap.put(header.key(), header.value());
            }
            hashMap.put(RETRY_HEADER, objectMapper.writeValueAsBytes(this));
            return hashMap;
        }
    }
}
