package ca.bc.jx.kafka.server;

import ca.bc.jx.kafka.retry.domain.NonBlockingKafkaConsumer;
import ca.bc.jx.kafka.retry.worker.ConsumerManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/")
@RequiredArgsConstructor
@Log4j2
public class MsgGeneratorController {
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ConsumerManager<?, ?> consumerWorkerFactory;

    @GetMapping("/nonblocking/{name}")
    public void order(@PathVariable("name") final String name) {
        kafkaTemplate.send(MessageBuilder.withPayload(name).setHeader(KafkaHeaders.TOPIC, "nonblocking-orders").build());
    }

    @GetMapping("/async2/{name}")
    public void sendAsyncMessage2(@PathVariable("name") final String name) {
        kafkaTemplate.send(MessageBuilder.withPayload(name).setHeader(KafkaHeaders.TOPIC, "async-main2").build());
    }

    @GetMapping("/async/{name}")
    public void sendAsyncMessage(@PathVariable("name") final String name) {
        kafkaTemplate.send(MessageBuilder.withPayload(name).setHeader(KafkaHeaders.TOPIC, "async-main1").build());
    }

    @GetMapping("/sync/{name}")
    public void sendSyncMessage(@PathVariable("name") final String name) {
        kafkaTemplate.send(MessageBuilder.withPayload(name).setHeader(KafkaHeaders.TOPIC, "0-spring-retry").build());
    }

    @GetMapping("/pause/{topicName}")
    public String pause(@PathVariable("topicName") final String topicName) {
        NonBlockingKafkaConsumer<?, ?> consumer = consumerWorkerFactory.getNonBlockingKafkaConsumerMap().get(topicName);
        if (consumer != null) {
            consumer.getMainMessageListenerContainer().pause();
            consumer.getRetryMessageListenerContainer().pause();
            log.info("consumer {} paused.", topicName);
            return "OK";
        } else {
            log.warn("consumer {} not found", topicName);
            return "NO";
        }
    }

    @GetMapping("/resume/{topicName}")
    public String resume(@PathVariable("topicName") final String topicName) {
        NonBlockingKafkaConsumer<?, ?> consumer = consumerWorkerFactory.getNonBlockingKafkaConsumerMap().get(topicName);
        if (consumer != null) {
            consumer.getMainMessageListenerContainer().resume();
            consumer.getRetryMessageListenerContainer().resume();
            log.info("consumer {} resumed.", topicName);
            return "OK";
        } else {
            log.warn("consumer {} not found", topicName);
            return "NO";
        }
    }
}
