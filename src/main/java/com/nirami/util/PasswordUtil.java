package com.nirami.util;

import org.mindrot.jbcrypt.BCrypt;

public class PasswordUtil {
    
    // Generar hash a partir de una contraseña en texto plano
    public static String hashPassword(String plainTextPassword) {
        return BCrypt.hashpw(plainTextPassword, BCrypt.gensalt());
    }
    
    // Verificar si una contraseña en texto plano coincide con el hash
    public static boolean checkPassword(String plainTextPassword, String hashedPassword) {
        if (hashedPassword == null || !hashedPassword.startsWith("$2a$")) {
            throw new IllegalArgumentException("Invalid hash provided for comparison");
        }
        return BCrypt.checkpw(plainTextPassword, hashedPassword);
    }
}
