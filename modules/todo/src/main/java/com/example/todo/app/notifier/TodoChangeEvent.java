package com.example.todo.app.notifier;

import lombok.Data;

@Data
public class TodoChangeEvent {
    private String eventType;
    private String userId;
    private String todoTitle;
}
