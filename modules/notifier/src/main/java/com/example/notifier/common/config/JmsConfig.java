package com.example.notifier.common.config;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.example.notifier.app.TodoChangeEvent;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.support.converter.MappingJackson2MessageConverter;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.converter.MessageType;

@Configuration
@EnableJms
public class JmsConfig {
    @Bean
    public MessageConverter jacksonJmsMessageConverter() {
        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        converter.setTargetType(MessageType.TEXT);
        converter.setTypeIdPropertyName("_type");
        converter.setTypeIdMappings(jmsTypeIdMappings());
        return converter;
    }

    @Bean
    public Map<String, Class<?>> jmsTypeIdMappings() {
        Map<String, Class<?>> m = new HashMap<>();

        // Register TypeId Mappings
        m.put("TodoChangeEvent", TodoChangeEvent.class);

        return Collections.unmodifiableMap(m);
    }
}
