package com.minicat.spring.boot.service;

import org.springframework.stereotype.Service;

@Service
public class TestService {

    public String hello(String name) {
        return "hello " + name;
    }

}
