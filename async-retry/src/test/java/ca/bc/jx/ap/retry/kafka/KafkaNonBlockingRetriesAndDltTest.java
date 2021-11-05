package ca.bc.jx.ap.retry.kafka;

import ca.bc.jx.kafka.retry.domain.RetryProperties;
import ca.bc.jx.kafka.retry.worker.ConsumerManager;
import lombok.extern.log4j.Log4j2;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.stream.StreamSupport;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;


@RunWith(SpringRunner.class)
@Log4j2
class KafkaNonBlockingRetriesAndDltTest extends KafkaTestBase {

    @Autowired
    private SimpleConsumer simpleConsumer;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private ConsumerManager<String, String> consumerManager;

    @Test
    void testNonBlockingRetriesAndDlt() throws InterruptedException {
        String orderId = UUID.randomUUID().toString();
        String orderJson = "{}";

        kafkaTemplate.send("async-main1", orderId, orderJson);
        log.info("wait the retry to be finished");
        Awaitility.waitAtMost(Duration.ofSeconds(2)).then().until(() -> {
                    try (Consumer<String, String> consumer = createConsumer()) {
                        KAFKA_BROKER.consumeFromAllEmbeddedTopics(consumer);
                        ConsumerRecords<String, String> records = KafkaTestUtils.getRecords(consumer, 10_000, 5);

                        List<String> topics =
                                StreamSupport.stream(records.spliterator(), false)
                                        .peek(
                                                record -> {
                                                    log.info("Topic: {}", record.topic());
                                                    log.info("Key: {}", record.key());
                                                    log.info("Value: {}", record.value());
                                                    log.info(record);
                                                })
                                        .map(ConsumerRecord::topic)
                                        .collect(toList());
                        long retryCount = topics.stream().filter("async-retry1"::equals).count();
                        RetryProperties retryProperties = consumerManager.getNonBlockingKafkaConsumerMap()
                                .get("some-name").getRetryProperties();
                        Assertions.assertEquals(retryProperties.getMaxRetries(), retryCount);
                        assertThat(topics.stream().filter("async-dlq1"::equals)).hasSize(1);
                        Assertions.assertEquals(retryProperties.getMaxRetries() + 1, simpleConsumer.acceptCalled.get());
                        Assertions.assertTrue(simpleConsumer.dropCalled);
                        return true;
                    }
                }
        );
    }
}
