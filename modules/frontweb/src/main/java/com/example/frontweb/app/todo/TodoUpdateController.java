package com.example.frontweb.app.todo;

import com.example.api.frontweb.client.api.TodosApi;
import com.example.api.frontweb.client.model.TodoResource;
import com.github.dozermapper.core.Mapper;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.reactive.result.view.Rendering;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Controller
@RequestMapping("/todo/update")
@SessionAttributes("todoForm")
@RequiredArgsConstructor
@Slf4j
public class TodoUpdateController {
    private final TodosApi todosApi;
    private final Mapper dozerMapper;

    @ModelAttribute("todoForm")
    public TodoForm setupTodoForm() {
        return new TodoForm();
    }

    @RequestMapping(method = { RequestMethod.GET, RequestMethod.POST })
    public Mono<String> index(TodoForm todoForm) {
        log.debug("[TODO-UPDATE]index: {}", todoForm);

        // API呼出し
        return todosApi.showTodoById(todoForm.getTodoId()).map(todo -> {
            // API呼出し結果を TodoForm に反映
            dozerMapper.map(todo, todoForm);
            return "todo/todoUpdateInput";
        });
    }

    @PostMapping("input")
    public String input(TodoForm todoForm) {
        log.debug("[TODO-UPDATE]input: {}", todoForm);
        return "todo/todoUpdateInput";
    }

    @PostMapping("confirm")
    public String confirm(TodoForm todoForm) {
        log.debug("[TODO-UPDATE]confirm: {}", todoForm);
        return "todo/todoUpdateConfirm";
    }

    @PostMapping("execute")
    public Mono<Rendering> execute(TodoForm todoForm) {
        log.debug("[TODO-UPDATE]execute: {}", todoForm);

        // TodoForm を TodoResource に変換
        TodoResource todo = dozerMapper.map(todoForm, TodoResource.class);

        // API呼出し
        // @formatter:off
        return todosApi.updateTodo(todo.getTodoId(), todo)
            .map(result -> {
                log.debug("[TODO-UPDATE]updated: {}", result);

                // API呼出し結果を TodoForm に反映 
                dozerMapper.map(result, todoForm);

                return Rendering.redirectTo("complete?message={message}").modelAttribute("message", "更新しました。").build();
            });
        // @formatter:on
    }

    @GetMapping("complete")
    public String complete(SessionStatus sessionStatus, @RequestParam("message") String message, Model model) {
        log.debug("[TODO-UPDATE]complete: message={}", message);
        sessionStatus.setComplete();
        model.addAttribute("message", message);
        return "todo/todoUpdateComplete";
    }

    @GetMapping("cancel")
    public String cancel(SessionStatus sessionStatus) {
        log.debug("[TODO-UPDATE]cancel");
        sessionStatus.setComplete();
        return "redirect:/todo/";
    }

}
