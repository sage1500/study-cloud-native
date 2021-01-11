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
@RequestMapping("/todo/create")
@SessionAttributes("todoForm")
@RequiredArgsConstructor
@Slf4j
public class TodoCreateController {
    private final TodosApi todosApi;
    private final Mapper dozerMapper;

    @ModelAttribute("todoForm")
    public TodoForm setupTodoForm() {
        return new TodoForm();
    }

    @RequestMapping(value = "/", method = { RequestMethod.GET, RequestMethod.POST })
    public String index(SessionStatus sessionStatus) {
        log.debug("[TODO-CREATE]index");

        // URL直接入力されると、セッションが残ったままの可能性があるため、
        // セッションから削除して、入力画面にリダイレクト
        sessionStatus.setComplete();
        return "redirect:input";
    }

    @RequestMapping(value = "input", method = { RequestMethod.GET, RequestMethod.POST })
    public String input(TodoForm todoForm) {
        log.debug("[TODO-CREATE]input: {}", todoForm);
        return "todo/todoCreateInput";
    }

    @PostMapping("confirm")
    public String confirm(TodoForm todoForm) {
        log.debug("[TODO-CREATE]confirm: {}", todoForm);
        return "todo/todoCreateConfirm";
    }

    @PostMapping("execute")
    public Mono<Rendering> execute(TodoForm todoForm) {
        log.debug("[TODO-CREATE]execute: {}", todoForm);

        // TodoForm を TodoResource に変換
        TodoResource todo = dozerMapper.map(todoForm, TodoResource.class);
        log.debug("[TODO-CREATE]execute : resource={}", todo);

        // API呼出し
        // @formatter:off
        return todosApi.createTodo(todo)
            .map(result -> {
                log.debug("[TODO-CREATE]todo created: {}", result);

                // API呼出し結果を TodoForm に反映 
                dozerMapper.map(result, todoForm);

                // 完了画面にリダイレクト
                String msg = "作成しました。";
                return Rendering.redirectTo("complete?message={message}").modelAttribute("message", msg).build();
            });
        // @formatter:on
    }

    @GetMapping("complete")
    public String complete(SessionStatus sessionStatus, @RequestParam("message") String message, Model model) {
        log.debug("[TODO-CREATE]complete: message={}", message);
        sessionStatus.setComplete();
        model.addAttribute("message", message);
        return "todo/todoCreateComplete";
    }

    @GetMapping("cancel")
    public String cancel(SessionStatus sessionStatus) {
        log.debug("[TODO-CREATE]cancel");
        sessionStatus.setComplete();
        return "redirect:/todo/";
    }
}
