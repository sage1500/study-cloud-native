package com.example.todo.app;

import lombok.Data;

@Data
public class TodoChangeEvent {
    private String eventType;
    private String userId;
    private String todoTitle;
}
