package com.simiyami.loveletter.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class MenuController {

    @GetMapping("/")
    public String menu() {
        return "menu";
    }

    @GetMapping("/game")
    public String game() {
        return "game";
    }
}