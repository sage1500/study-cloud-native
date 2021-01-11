package com.example.notifier.app;

import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.support.JmsHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class TodoChangeEventController {
    @JmsListener(destination = "todoChangeEvent")
    public void todoChangeEvent(TodoChangeEvent event, @Header(JmsHeaders.MESSAGE_ID) String messageId) {
        log.info("[TodoChange]event={} messageId={}", event, messageId);        
    }
}
