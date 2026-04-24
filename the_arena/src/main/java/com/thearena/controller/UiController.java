package com.thearena.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.view.RedirectView;

@Controller
@RequestMapping("/ui")
public class UiController {
    @GetMapping
    public RedirectView root() {
        return new RedirectView("/ui/login");
    }

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @GetMapping("/arena")
    public String arenaPage() {
        return "arena";
    }
}
