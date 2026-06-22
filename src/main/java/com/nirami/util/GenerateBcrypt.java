package com.nirami.util;

public class GenerateBcrypt {
    public static void main(String[] args) {
        String hash = PasswordUtil.hashPassword("123456");
        System.out.println("==================================================");
        System.out.println("EL HASH PARA 123456 ES:");
        System.out.println(hash);
        System.out.println("==================================================");
    }
}
