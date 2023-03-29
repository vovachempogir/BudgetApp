package com.example.budgetapp.controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
@RestController
public class FirstControllers {
    @GetMapping
    public String helloWord() {
        return "Hello, web";
    }
    @GetMapping("path/to/page")
    public String page(@RequestParam String page) {
        return "page: " + page;
    }
}
