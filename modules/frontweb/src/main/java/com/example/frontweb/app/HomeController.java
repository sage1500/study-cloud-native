package com.example.frontweb.app;

import com.example.api.frontweb.client.api.TodosApi;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.reactive.function.client.WebClient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Controller
@RequestMapping("/home")
@RequiredArgsConstructor
@Slf4j
public class HomeController {
    private final WebClient webClientForHello;
    private final TodosApi todosApi;

    @GetMapping
    public Mono<String> home(Model model) {
        log.debug("[HOME]index");

        return Mono.zip(
                // Hello API
                webClientForHello.get().uri("/").retrieve().bodyToMono(String.class),

                // TODO一覧取得 API
                todosApi.listTodos().collectList())
                //
                .map(results -> {
                    var hello = results.getT1();
                    var todoList = results.getT2();

                    // 複数のAPIの結果をビューに反映
                    model.addAttribute("hello", hello);
                    model.addAttribute("todoList", todoList);

                    return "home";
                });
    }

}
