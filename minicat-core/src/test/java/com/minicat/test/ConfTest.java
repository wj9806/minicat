package com.minicat.test;

import com.minicat.server.config.Config;

public class ConfTest {
    public static void main(String[] args) {
        Config conf = Config.getInstance();
        System.out.println(conf);
    }
}
