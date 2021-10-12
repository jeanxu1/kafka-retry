package ca.bc.jx.ap.retry.kafka.server;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/msg")
@RequiredArgsConstructor
public class MsgGeneratorController {
    private final KafkaTemplate<String,String> kafkaTemplate;
    private final Application.Topic1 topic1;

    @GetMapping("/{name}")
    public void lookup(@PathVariable("name") final String name) {
        kafkaTemplate.send(MessageBuilder.withPayload(name).setHeader(KafkaHeaders.TOPIC, topic1.getMainTopic()).build());
    }
}
