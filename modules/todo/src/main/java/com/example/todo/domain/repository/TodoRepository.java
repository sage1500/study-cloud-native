package com.example.todo.domain.repository;

import com.example.todo.domain.entity.Todo;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface TodoRepository extends ReactiveCrudRepository<Todo, String> {
    Flux<Todo> findAllByUserId(String userId);
    Mono<Todo> findByUserIdAndTodoId(String userId, String todoId);
    Mono<Boolean> deleteByUserIdAndTodoId(String userId, String todoId);
}
