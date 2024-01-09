package com.example.redis.pojo;

import lombok.Data;

@Data
public class User {
    private String name;
    private int age;
    private String addr;

    public User(String name, int age, String addr) {
        this.name = name;
        this.age = age;
        this.addr = addr;
    }
}
