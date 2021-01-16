package com.example.frontweb.app.todo;

import java.io.Serializable;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import lombok.Data;

@Data
public class TodoForm implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotNull(groups = { TodoUpdate.class, TodoDelete.class })
    private String todoId;

    @NotNull(groups = { TodoCreate.class, TodoUpdate.class })
    @Size(min = 1, max = 30, groups = { TodoCreate.class, TodoUpdate.class })
    private String todoTitle;
    
    private long version;

    public interface TodoCreate {
    }

    public interface TodoUpdate {
    }

    public interface TodoDelete {
    }
}
