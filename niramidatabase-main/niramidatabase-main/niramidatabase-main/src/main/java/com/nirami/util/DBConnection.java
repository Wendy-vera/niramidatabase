package com.nirami.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Clase utilitaria que centraliza la creación de conexiones JDBC a la base de datos MySQL.
 *
 * <p><b>Base de datos destino:</b> {@code nirami_prueba} en {@code localhost:3306}</p>
 *
 * <p><b>Driver:</b> {@code com.mysql.cj.jdbc.Driver} (MySQL Connector/J 8.x).
 * El driver debe estar declarado como dependencia en {@code pom.xml}:
 * <pre>{@code
 * <dependency>
 *     <groupId>com.mysql</groupId>
 *     <artifactId>mysql-connector-j</artifactId>
 *     <version>8.0.33</version>
 * </dependency>
 * }</pre>
 * </p>
 *
 * <p><b>Parámetros de conexión URL:</b>
 * <ul>
 *   <li>{@code useSSL=false} — Deshabilita SSL (ambiente de desarrollo local)</li>
 *   <li>{@code serverTimezone=UTC} — Evita desincronización de timestamps entre MySQL y JVM</li>
 *   <li>{@code allowPublicKeyRetrieval=true} — Requerido por MySQL 8+ con autenticación RSA</li>
 * </ul>
 * </p>
 *
 * <p><b>Patrón de uso (try-with-resources):</b> Se recomienda siempre usar este método
 * dentro de un bloque {@code try-with-resources} para garantizar el cierre automático:
 * <pre>{@code
 * try (Connection conn = DBConnection.getConnection();
 *      PreparedStatement ps = conn.prepareStatement("SELECT...")) {
 *     // ... usar la conexión
 * } // La conexión se cierra automáticamente aquí
 * }</pre>
 * </p>
 *
 * <p><b>Nota de producción:</b> Para ambientes de producción, reemplazar esta
 * implementación por un pool de conexiones (p.ej., HikariCP o JNDI DataSource)
 * para mejor rendimiento bajo carga.</p>
 *
 * <p><b>Clases que usan este utilitario:</b> Todos los DAOs del paquete
 * {@code com.nirami.dao}: {@link com.nirami.dao.UsuarioDAO}, {@link com.nirami.dao.OrdenDAO},
 * {@link com.nirami.dao.ProductoDAO}, {@link com.nirami.dao.HistorialDAO}, entre otros.</p>
 *
 * @author Nirami Dev Team
 * @version 1.0
 */
public class DBConnection {

    /**
     * URL de conexión JDBC a la base de datos MySQL.
     * Formato: {@code jdbc:mysql://[host]:[puerto]/[base_de_datos]?[parámetros]}
     */
    private static final String URL =
        "jdbc:mysql://localhost:3306/nirami_prueba?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";

    /**
     * Usuario de MySQL con privilegios sobre la base {@code nirami_prueba}.
     * En producción, obtener de variables de entorno o configuración segura.
     */
    private static final String USER = "root";

    /**
     * Contraseña del usuario MySQL.
     * En producción, obtener de variables de entorno o un gestor de secretos (no hardcodear).
     */
    private static final String PASSWORD = "#Aprendiz2024";

    /**
     * Crea y devuelve una nueva conexión JDBC a la base de datos MySQL.
     *
     * <p>Cada llamada abre una nueva conexión física al servidor MySQL.
     * El llamador es responsable de cerrar la conexión cuando ya no la necesite.</p>
     *
     * <p><b>Flujo interno:</b>
     * <ol>
     *   <li>Registra el driver {@code com.mysql.cj.jdbc.Driver} con {@code Class.forName()}</li>
     *   <li>Establece la conexión con {@link DriverManager#getConnection}</li>
     *   <li>En caso de error, imprime el mensaje en {@code System.err} y devuelve {@code null}</li>
     * </ol>
     * </p>
     *
     * @return Objeto {@link Connection} listo para usar si la conexión fue exitosa;
     *         {@code null} si el driver no se encontró o si la BD no está disponible.
     *         <strong>El llamador debe verificar que el resultado no sea null antes de usarlo.</strong>
     */
    public static Connection getConnection() {
        Connection connection = null;
        try {
            // Cargar el driver de MySQL en el ClassLoader (requerido antes de Java 6; safe en versiones modernas)
            Class.forName("com.mysql.cj.jdbc.Driver");
            // Crear la conexión física con las credenciales configuradas
            connection = DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (ClassNotFoundException e) {
            // El JAR del driver no está en el classpath del servidor
            System.err.println("[DBConnection] Error: Driver MySQL no encontrado - " + e.getMessage());
        } catch (SQLException e) {
            // Error de red, credenciales incorrectas, base de datos inexistente, etc.
            System.err.println("[DBConnection] Error: Conexión a Base de Datos fallida - " + e.getMessage());
        }
        return connection;
    }
}
