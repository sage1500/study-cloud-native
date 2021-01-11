package com.example.todo.domain.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

import lombok.Data;

@Data
public class Todo {
    private String userId;
    @Id
    private String todoId;
    private String todoTitle;
    @Version
    private long version;
}
