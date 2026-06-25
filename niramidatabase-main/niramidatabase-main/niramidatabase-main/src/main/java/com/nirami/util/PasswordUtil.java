package com.nirami.util;

import org.mindrot.jbcrypt.BCrypt;

/**
 * Clase utilitaria para el manejo seguro de contraseñas usando el algoritmo bcrypt.
 *
 * <p><b>Algoritmo:</b> BCrypt (implementación por {@code org.mindrot:jbcrypt}).
 * BCrypt es un algoritmo de hashing adaptativo basado en Blowfish, diseñado para
 * ser computacionalmente costoso y resistente a ataques de fuerza bruta y rainbow tables.
 * Incorpora un salt aleatorio en cada hash, por lo que dos llamadas con la misma
 * contraseña producen hashes distintos — pero ambos son válidos para verificación.</p>
 *
 * <p><b>Dependencia Maven:</b>
 * <pre>{@code
 * <dependency>
 *     <groupId>org.mindrot</groupId>
 *     <artifactId>jbcrypt</artifactId>
 *     <version>0.4</version>
 * </dependency>
 * }</pre>
 * </p>
 *
 * <p><b>Factor de costo:</b> {@link BCrypt#gensalt()} usa el factor por defecto (10 rondas),
 * lo que significa 2^10 = 1024 iteraciones del algoritmo. Es suficiente para la mayoría
 * de aplicaciones web.</p>
 *
 * <p><b>Formato del hash almacenado en BD:</b> {@code $2a$10$[salt22chars][hash31chars]}
 * Ejemplo: {@code $2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy}</p>
 *
 * <p><b>Clases que usan este utilitario:</b>
 * <ul>
 *   <li>{@link com.nirami.controller.LoginServlet} — Verificación al autenticar</li>
 *   <li>{@link com.nirami.controller.RegistroServlet} — Hasheo al registrar</li>
 *   <li>{@link com.nirami.controller.PerfilServlet} — Hasheo al cambiar contraseña</li>
 * </ul>
 * </p>
 *
 * @author Nirami Dev Team
 * @version 1.0
 */
public class PasswordUtil {

    /**
     * Genera un hash bcrypt de una contraseña en texto plano.
     *
     * <p>Este método debe ser llamado ANTES de guardar la contraseña en la base de datos.
     * El hash resultante incluye el salt de manera integrada, por lo que NO es necesario
     * almacenar el salt por separado.</p>
     *
     * <p><b>Almacenamiento:</b> Guardar el resultado directamente en la columna
     * {@code usuarios.contrasena_hash} (VARCHAR(255)).</p>
     *
     * <p><b>Ejemplo de uso:</b>
     * <pre>{@code
     * String hash = PasswordUtil.hashPassword("miContraseñaSegura123!");
     * // hash = "$2a$10$..." (60 caracteres)
     * }</pre>
     * </p>
     *
     * @param plainTextPassword Contraseña en texto plano ingresada por el usuario.
     *                          No debe ser null ni vacía.
     * @return String con el hash bcrypt (60 caracteres), listo para almacenar en BD.
     */
    public static String hashPassword(String plainTextPassword) {
        // BCrypt.gensalt() genera un salt aleatorio con factor de costo 10
        return BCrypt.hashpw(plainTextPassword, BCrypt.gensalt());
    }

    /**
     * Verifica si una contraseña en texto plano coincide con un hash bcrypt almacenado.
     *
     * <p>Este método es llamado durante el proceso de login. Extrae automáticamente el
     * salt del hash almacenado para calcular el hash de la contraseña proporcionada
     * y compara ambos. La comparación es a tiempo constante (resistente a timing attacks).</p>
     *
     * <p><b>Ejemplo de uso:</b>
     * <pre>{@code
     * String hashAlmacenado = usuario.getContrasenaHash(); // "$2a$10$..."
     * boolean esValida = PasswordUtil.checkPassword("miContraseña", hashAlmacenado);
     * }</pre>
     * </p>
     *
     * @param plainTextPassword Contraseña ingresada por el usuario en el formulario de login.
     *                          Se compara con el hash almacenado.
     * @param hashedPassword    Hash bcrypt almacenado en la BD (columna {@code contrasena_hash}).
     *                          Debe comenzar con {@code "$2a$"} (formato bcrypt).
     * @return {@code true} si la contraseña coincide con el hash; {@code false} en caso contrario.
     * @throws IllegalArgumentException si {@code hashedPassword} es null o no es un hash bcrypt válido
     *                                  (no comienza con {@code "$2a$"}).
     */
    public static boolean checkPassword(String plainTextPassword, String hashedPassword) {
        // Validación de seguridad: rechazar valores nulos o con formato incorrecto
        // Un hash bcrypt siempre comienza con "$2a$" (versión 2a de bcrypt)
        if (hashedPassword == null || !hashedPassword.startsWith("$2a$")) {
            throw new IllegalArgumentException("Invalid hash provided for comparison");
        }
        return BCrypt.checkpw(plainTextPassword, hashedPassword);
    }
}
