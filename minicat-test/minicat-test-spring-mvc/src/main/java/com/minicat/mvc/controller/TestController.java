package com.minicat.mvc.controller;

import com.minicat.mvc.service.TestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
public class TestController {

    @Autowired
    private TestService testService;

    @GetMapping("/hello")
    public String hello(@RequestParam(required = false) String name) {
        return testService.hello(name);
    }

    @PostMapping("/hello2")
    public Object hello2(@RequestBody Map<String, String> map) {
        return map;
    }

}
