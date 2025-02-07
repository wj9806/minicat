package io.github.wj9806.minicat.spring.boot.test.service;

import org.springframework.stereotype.Service;

@Service
public class TestService {

    public String hello(String name) {
        return "hello " + name;
    }

}
