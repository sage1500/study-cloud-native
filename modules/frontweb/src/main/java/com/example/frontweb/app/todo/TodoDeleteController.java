package com.example.frontweb.app.todo;

import java.util.Locale;

import com.example.api.frontweb.client.api.TodosApi;
import com.github.dozermapper.core.Mapper;

import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.reactive.result.view.Rendering;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Controller
@RequestMapping("/todo/delete")
@SessionAttributes("todoForm")
@RequiredArgsConstructor
@Slf4j
public class TodoDeleteController {
    private final MessageSource messageSource;
    private final TodosApi todosApi;
    private final Mapper dozerMapper;

    @ModelAttribute("todoForm")
    public TodoForm setupTodoForm() {
        return new TodoForm();
    }

    @PostMapping("/")
    public Mono<String> index(TodoForm todoForm) {
        log.debug("[TODO-DELETE]index: {}", todoForm);

        // API呼出し
        // @formatter:off
        return todosApi.showTodoById(todoForm.getTodoId())
            .map(todo -> {
                // API呼出し結果を TodoForm に反映
                dozerMapper.map(todo, todoForm);
                return "todo/todoDeleteConfirm";
            });
        // @formatter:on
    }

    @PostMapping("execute")
    public Mono<Rendering> execute(TodoForm todoForm) {
        log.debug("[TODO-DELETE]execute: {}", todoForm);

        // API呼出し
        // @formatter:off
        return todosApi.deleteTodo(todoForm.getTodoId())
            .map(result -> {
                log.debug("[TODO-DELETE]result: {}", result);

                String msgKey =  (result) ? "message.todo.delete.success" : "message.todo.delete.success-deleted";
                String msg = messageSource.getMessage(msgKey, null, Locale.ROOT);
                return Rendering.redirectTo("complete?message={message}").modelAttribute("message", msg).build();
            });
        // @formatter:on
    }

    @GetMapping("complete")
    public String complete(SessionStatus sessionStatus, @RequestParam("message") String message, Model model) {
        log.debug("[TODO-DELETE]complete: message={}", message);
        sessionStatus.setComplete();
        model.addAttribute("message", message);
        return "todo/todoDeleteComplete";
    }

    @GetMapping("cancel")
    public String cancel(SessionStatus sessionStatus) {
        log.debug("[TODO-DELETE]cancel");
        sessionStatus.setComplete();
        return "redirect:/todo/";
    }

}
