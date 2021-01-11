package com.example.frontweb.controller.todo;

import java.io.Serializable;

import lombok.Data;

@Data
public class TodoForm implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String todoId;
    private String todoTitle;
    private long version;
}
