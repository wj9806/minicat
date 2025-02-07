package io.github.wj9806.minicat.test;

import io.github.wj9806.minicat.server.config.Config;

public class ConfTest {
    public static void main(String[] args) {
        Config conf = Config.getInstance();
        System.out.println(conf);
    }
}
