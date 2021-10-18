package ca.bc.jx.ap.retry.kafka;

import ca.bc.jx.kafka.retry.worker.EnableNonBlockingKafkaConsumers;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;

@ActiveProfiles("kafka")
@SpringBootTest(classes = KafkaTestBase.TestApplication.class)
public class KafkaTestBase {

    public static final EmbeddedKafkaBroker KAFKA_BROKER = createEmbeddedKafkaBroker();

    private static EmbeddedKafkaBroker createEmbeddedKafkaBroker() {
        AnnotationConfigApplicationContext context =
                new AnnotationConfigApplicationContext(EmbeddedKafkaBrokerConfig.class);
        EmbeddedKafkaBroker kafkaBroker = context.getBean(EmbeddedKafkaBroker.class);
        Runtime.getRuntime().addShutdownHook(new Thread(context::close));
        return kafkaBroker;
    }

    protected Consumer<String, String> createConsumer() {
        Map<String, Object> consumerProps =
                KafkaTestUtils.consumerProps(
                        this.getClass().getSimpleName() + "-consumer", "true", KAFKA_BROKER);
        DefaultKafkaConsumerFactory<String, String> cf =
                new DefaultKafkaConsumerFactory<>(
                        consumerProps, new StringDeserializer(), new StringDeserializer());
        return cf.createConsumer();
    }

    @SpringBootApplication
    @EnableNonBlockingKafkaConsumers
    static class TestApplication {

        public static void main(String[] args) {
            SpringApplication.run(TestApplication.class, args);
        }
    }
}
