package com.example.todo.controller;

import javax.validation.Valid;

import com.example.api.todo.server.api.TodosApi;
import com.example.api.todo.server.model.TodoResource;
import com.example.todo.app.notifier.TodoChangeEvent;
import com.example.todo.domain.entity.Todo;
import com.example.todo.domain.service.TodoService;

import org.springframework.http.ResponseEntity;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
@Slf4j
public class TodoController implements TodosApi {
    private final TodoService todoService;
    private final JmsTemplate jmsTemplate;

    @Override
    public Mono<ResponseEntity<Flux<TodoResource>>> listTodos(ServerWebExchange exchange) {
        // @formatter:off
        return exchange.getPrincipal()
            .cast(JwtAuthenticationToken.class)
            .flatMapMany(jwt -> todoService.findAllByUserId(getUser(jwt)))
            .map(todo -> toTodoResource(todo))
            .as(f -> Mono.just(ResponseEntity.ok(f)));
        // @formatter:on
    }

    @Override
    public Mono<ResponseEntity<TodoResource>> createTodo(@Valid Mono<TodoResource> todoResource,
            ServerWebExchange exchange) {
        // @formatter:off
        return Mono.zip(
                exchange.getPrincipal().cast(JwtAuthenticationToken.class),
                todoResource)
            .flatMap(t -> {
                var jwt = t.getT1().getToken();
                var rsrc = t.getT2();
                String todoId = null;
                return todoService.save(toTodo(rsrc, todoId, jwt));
            })
            .doOnNext(this::notifyCreateTodo)
            .map(todo -> ResponseEntity.ok(toTodoResource(todo)));
        // @formatter:on
    }

    @Override
    public Mono<ResponseEntity<Boolean>> deleteTodo(String todoId, ServerWebExchange exchange) {
        // @formatter:off
        return exchange.getPrincipal()
            .cast(JwtAuthenticationToken.class)
            .flatMap(jwt -> todoService.deleteByUserIdAndTodoId(getUser(jwt), todoId))
            .map(result -> ResponseEntity.ok(result));
        // @formatter:on
    }

    @Override
    public Mono<ResponseEntity<TodoResource>> showTodoById(String todoId, ServerWebExchange exchange) {
        // @formatter:off
        return exchange.getPrincipal()
            .cast(JwtAuthenticationToken.class)
            .flatMap(jwt -> todoService.findByUserIdAndTodoId(getUser(jwt), todoId))
            .map(todo -> ResponseEntity.ok(toTodoResource(todo)));
        // @formatter:on
    }

    @Override
    public Mono<ResponseEntity<TodoResource>> updateTodo(String todoId, @Valid Mono<TodoResource> todoResource,
            ServerWebExchange exchange) {
        // @formatter:off
        return Mono.zip(
                exchange.getPrincipal().cast(JwtAuthenticationToken.class),
                todoResource)
            .flatMap(t -> {
                var jwt = t.getT1().getToken();
                var rsrc = t.getT2();
                return todoService.save(toTodo(rsrc, todoId, jwt));
            }).map(todo -> ResponseEntity.ok(toTodoResource(todo)));
        // @formatter:on
    }

    private String getUser(Jwt jwt) {
        return jwt.getClaimAsString("preferred_username");
    }

    private String getUser(JwtAuthenticationToken token) {
        var jwt = token.getToken();
        log.info("★token pri = {}", token.getPrincipal());
        return jwt.getClaimAsString("preferred_username");
    }

    private TodoResource toTodoResource(Todo todo) {
        TodoResource todoResource = new TodoResource();
        todoResource.setTodoId(todo.getTodoId());
        todoResource.setTodoTitle(todo.getTodoTitle());
        todoResource.setVersion(todo.getVersion());
        return todoResource;
    }

    private Todo toTodo(TodoResource todoResource, String todoId, Jwt jwt) {
        Todo todo = new Todo();
        todo.setTodoId(todoId);
        todo.setUserId(getUser(jwt));
        todo.setTodoTitle(todoResource.getTodoTitle());
        if (todoResource.getVersion() != null) {
            todo.setVersion(todoResource.getVersion());
        }
        return todo;
    }

    private void notifyCreateTodo(Todo todo) {
        try {
            // TodoChangeEvent を通知
            // ※JmsTemplate はリアクティブではないため、本来は JmsTemplateを扱う専用スレッドに委譲した方がよい。
            var ev = new TodoChangeEvent();
            ev.setEventType("create");
            ev.setUserId(todo.getUserId());
            ev.setTodoTitle(todo.getTodoTitle());
            jmsTemplate.convertAndSend("todoChangeEvent", ev);
        } catch (RuntimeException e) {
            log.error("[TODO]通知エラー", e);
        }
    }

}
