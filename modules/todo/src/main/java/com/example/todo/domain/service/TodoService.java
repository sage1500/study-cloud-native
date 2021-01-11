package com.example.todo.domain.service;

import com.example.todo.domain.entity.Todo;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface TodoService {
    Mono<Todo> save(Todo entity);
    Flux<Todo> findAllByUserId(String userId);
    Mono<Todo> findByUserIdAndTodoId(String userId, String todoId);
    Mono<Boolean> deleteByUserIdAndTodoId(String userId, String todoId);
}
