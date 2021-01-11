package com.example.todo.domain.service;

import java.util.UUID;

import com.example.todo.domain.entity.Todo;
import com.example.todo.domain.repository.TodoRepository;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class TodoServiceImpl implements TodoService {
    private final TodoRepository todoRepository;

    @Override
    public Mono<Todo> save(Todo entity) {
        // 追加時はIDを割り振る
        if (entity.getTodoId() == null) {
            entity.setTodoId(UUID.randomUUID().toString());
            entity.setVersion(0);
        }

        log.info("★save {}", entity);
        
        return todoRepository.save(entity);
    }

    @Override
    public Flux<Todo> findAllByUserId(String userId) {
        return todoRepository.findAllByUserId(userId);
    }

    @Override
    public Mono<Todo> findByUserIdAndTodoId(String userId, String todoId) {
        return todoRepository.findByUserIdAndTodoId(userId, todoId);
    }

    @Override
    public Mono<Boolean> deleteByUserIdAndTodoId(String userId, String todoId) {
        return todoRepository.deleteByUserIdAndTodoId(userId, todoId);
    }

}
