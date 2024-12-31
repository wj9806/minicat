package com.minicat.spring.boot.controller;

import com.minicat.spring.boot.service.TestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.IOException;
import java.util.Map;

@RestController
public class TestController {

    @Autowired
    private TestService testService;

    @GetMapping("/hello")
    public String hello(@RequestParam(required = false) String name, HttpServletRequest req) {
        return testService.hello(name);
    }

    @PostMapping("/hello2")
    public Object hello2(@RequestBody Map<String, String> map) {
        return map;
    }


    @PostMapping("/upload")
    public String upload(MultipartFile file) throws IOException {
        file.transferTo(new File("file/" + file.getOriginalFilename()));
        return "upload";
    }
}
