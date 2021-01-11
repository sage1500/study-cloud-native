package com.example.frontweb.controller.todo;

import com.example.api.frontweb.client.api.TodosApi;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Controller
@RequestMapping("/todo")
@RequiredArgsConstructor
@Slf4j
public class TodoListController {
    private final TodosApi todosApi;

    @GetMapping("/")
    public Mono<String> index(Model model) {
        log.debug("[TODO]index");

        // TODO一覧取得API呼出し
        return todosApi.listTodos().collectList().map(list -> {
            // 結果をTODO一覧画面に反映
            model.addAttribute("list", list);
            return "todo/todoList";
        });
    }

}
