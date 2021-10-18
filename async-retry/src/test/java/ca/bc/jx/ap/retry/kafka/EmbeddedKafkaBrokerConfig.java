package ca.bc.jx.ap.retry.kafka;

import kafka.server.KafkaConfig;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.test.EmbeddedKafkaBroker;

@TestConfiguration
public class EmbeddedKafkaBrokerConfig {

    private final int embeddedKafkaBrokerPort = 39092;

    @Bean
    public EmbeddedKafkaBroker embeddedKafkaBroker() {
        return new EmbeddedKafkaBroker(
                1,
                false,
                3,
                "async-main1",
                "async-retry1",
                "async-dlq1")
                .kafkaPorts(embeddedKafkaBrokerPort)
                .brokerProperty(KafkaConfig.AutoCreateTopicsEnableProp(), "false");
    }

//    @Bean
//    KafkaTemplate<String, String> getKafkaTemplate() {
//        private final KafkaTemplate<String, String> kafkaTemplate;
//        ProducerFactory<java.lang.String, java.lang.String> factory = new DefaultKafkaProducerFactory<>()
//        return new KafkaTemplate<String, String>(factory);
//    }
}
