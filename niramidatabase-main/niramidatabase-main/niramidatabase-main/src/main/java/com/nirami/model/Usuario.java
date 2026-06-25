package com.nirami.model;

import java.sql.Timestamp;

/**
 * POJO que representa un usuario del sistema (Cliente, Vendedor o Admin).
 * Mapea la tabla {@code usuarios} de la base de datos.
 *
 * Tabla mapeada: {@code usuarios}
 * Columnas: id_usuario (PK), nombre, correo, telefono, contrasena_hash, tipo_usuario,
 *           foto_perfil, estado, intentos_fallidos, bloqueado_hasta, fecha_registro, ultima_sesion
 *
 * Tipos de usuario (columna tipo_usuario):
 *   - "CLIENTE"  → puede comprar, ver catálogo, guardar favoritos
 *   - "VENDEDOR" → puede publicar productos, ver sus ventas y pagos
 *   - "ADMIN"    → puede revisar productos, gestionar usuarios, ejecutar pagos
 *
 * Tablas hijas (subtablas por tipo):
 *   - {@code clientes (id_usuario FK, direccion_envio)}    → para tipo CLIENTE
 *   - {@code vendedores (id_usuario FK, cuenta_bancaria)}  → para tipo VENDEDOR
 *
 * IMPORTANTE: Este objeto se guarda en la SESIÓN HTTP con clave "usuarioLogueado".
 * Todos los servlets lo leen con: session.getAttribute("usuarioLogueado")
 * Lo guarda: LoginServlet (al autenticar)
 * Lo elimina: LogoutServlet (al invalidar la sesión)
 * Lo actualiza: PerfilServlet (al guardar cambios del perfil)
 *
 * Clases que usan este modelo:
 *   → UsuarioDAO     (CRUD sobre la tabla usuarios y subtablas)
 *   → LoginServlet   (crea el objeto y lo guarda en sesión)
 *   → Todos los Servlets que verifican el rol del usuario
 *   → Múltiples JSPs (muestran nombre, foto, correo del usuario logueado)
 */
public class Usuario {

    // ── Columnas de la tabla usuarios ──

    /** PK de la tabla usuarios. AUTO_INCREMENT. */
    private int idUsuario;

    /** Nombre completo del usuario. Columna: nombre (VARCHAR NOT NULL). */
    private String nombre;

    /**
     * Correo electrónico. Sirve también como nombre de usuario para el login.
     * Columna: correo (VARCHAR UNIQUE NOT NULL).
     * La restricción UNIQUE evita que dos usuarios tengan el mismo correo.
     */
    private String correo;

    /** Teléfono de contacto. Columna: telefono (VARCHAR, nullable). */
    private String telefono;

    /**
     * Hash bcrypt de la contraseña. NUNCA se guarda la contraseña en texto plano.
     * Columna: contrasena_hash (VARCHAR(255) NOT NULL).
     * Generado por: PasswordUtil.hashPassword() → BCrypt.hashpw()
     * Verificado por: PasswordUtil.checkPassword() → BCrypt.checkpw()
     * Formato: "$2a$10$[22_chars_salt][31_chars_hash]" (siempre 60 caracteres)
     */
    private String contrasenaHash;

    /**
     * Tipo de usuario. Determina a qué secciones puede acceder.
     * Valores: "CLIENTE", "VENDEDOR", "ADMIN".
     * Columna: tipo_usuario (VARCHAR NOT NULL).
     * Leído por todos los servlets para control de acceso.
     */
    private String tipoUsuario;

    /**
     * Ruta relativa de la foto de perfil del usuario.
     * Columna: foto_perfil (VARCHAR, nullable).
     * Sigue el mismo patrón que Producto.imagenPrincipal: "uploads/timestamp_foto.jpg"
     * Servida por ImageServlet. Guardada en C:\NiramiUploads\ por PerfilServlet.
     */
    private String fotoPerfil;

    /**
     * Estado de la cuenta del usuario.
     * Columna: estado (VARCHAR DEFAULT 'ACTIVO').
     * Valores: "ACTIVO" (puede usar el sistema), "INACTIVO" (bloqueado por admin o intentos fallidos)
     */
    private String estado;

    /**
     * Contador de intentos de login fallidos consecutivos.
     * Columna: intentos_fallidos (INT DEFAULT 0).
     * Incrementado por LoginServlet cuando el usuario falla la contraseña.
     * Reseteado a 0 cuando el login es exitoso.
     * Si supera 5 intentos, la cuenta se bloquea temporalmente.
     */
    private int intentosFallidos;

    /**
     * Timestamp hasta el cual la cuenta está bloqueada.
     * Columna: bloqueado_hasta (TIMESTAMP, nullable).
     * Es null si la cuenta no está bloqueada.
     * Se establece en LoginServlet cuando intentos_fallidos supera el límite.
     * Se revisa en LoginServlet antes de verificar la contraseña.
     */
    private Timestamp bloqueadoHasta;

    /**
     * Fecha y hora en que el usuario se registró en el sistema.
     * Columna: fecha_registro (TIMESTAMP DEFAULT CURRENT_TIMESTAMP).
     * Se establece automáticamente en la BD al hacer INSERT.
     */
    private Timestamp fechaRegistro;

    /**
     * Fecha y hora del último inicio de sesión exitoso.
     * Columna: ultima_sesion (TIMESTAMP, nullable).
     * Actualizado por: UsuarioDAO.actualizarUltimaSesion() llamado desde LoginServlet.
     * Útil para detectar cuentas inactivas y para auditoría de accesos.
     */
    private Timestamp ultimaSesion;

    // ── Constructores ──

    /** Constructor vacío requerido por JDBC y frameworks. */
    public Usuario() {}

    /**
     * Constructor completo para mapear directamente desde un ResultSet de JDBC.
     * Usado por UsuarioDAO al hacer SELECT * FROM usuarios.
     */
    public Usuario(int idUsuario, String nombre, String correo, String telefono,
                   String contrasenaHash, String tipoUsuario, String fotoPerfil,
                   String estado, int intentosFallidos, Timestamp bloqueadoHasta,
                   Timestamp fechaRegistro, Timestamp ultimaSesion) {
        this.idUsuario        = idUsuario;
        this.nombre           = nombre;
        this.correo           = correo;
        this.telefono         = telefono;
        this.contrasenaHash   = contrasenaHash;   // Hash bcrypt; NO es la contraseña original
        this.tipoUsuario      = tipoUsuario;      // CLIENTE, VENDEDOR o ADMIN
        this.fotoPerfil       = fotoPerfil;       // Ruta relativa o null
        this.estado           = estado;           // ACTIVO o INACTIVO
        this.intentosFallidos = intentosFallidos; // Contador de fallos de login
        this.bloqueadoHasta   = bloqueadoHasta;   // null si no está bloqueado
        this.fechaRegistro    = fechaRegistro;    // Automático por la BD
        this.ultimaSesion     = ultimaSesion;     // Actualizado en cada login exitoso
    }

    // ── Getters y Setters ──

    /** @return ID único del usuario (PK de la tabla usuarios) */
    public int getIdUsuario() { return idUsuario; }
    public void setIdUsuario(int idUsuario) { this.idUsuario = idUsuario; }

    /** @return Nombre completo del usuario (para mostrar en la interfaz) */
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    /** @return Correo electrónico (también funciona como username de login) */
    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }

    /** @return Teléfono de contacto (puede ser null) */
    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    /**
     * @return Hash bcrypt de la contraseña. NUNCA enviar al frontend.
     *         Usado solo por: PasswordUtil.checkPassword() en LoginServlet
     */
    public String getContrasenaHash() { return contrasenaHash; }
    /** @param contrasenaHash SIEMPRE debe ser un hash bcrypt, NUNCA texto plano */
    public void setContrasenaHash(String contrasenaHash) { this.contrasenaHash = contrasenaHash; }

    /**
     * @return Tipo de usuario: "CLIENTE", "VENDEDOR" o "ADMIN".
     *         Leído por todos los servlets para control de acceso por rol.
     */
    public String getTipoUsuario() { return tipoUsuario; }
    public void setTipoUsuario(String tipoUsuario) { this.tipoUsuario = tipoUsuario; }

    /**
     * @return Ruta de la foto de perfil ("uploads/xxx.jpg") o null si no tiene.
     *         Mostrada en perfil.jsp con <img src="${pageContext.request.contextPath}/${usuario.fotoPerfil}">
     */
    public String getFotoPerfil() { return fotoPerfil; }
    public void setFotoPerfil(String fotoPerfil) { this.fotoPerfil = fotoPerfil; }

    /** @return Estado de la cuenta: "ACTIVO" o "INACTIVO" */
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    /**
     * @return Número de veces consecutivas que el usuario ha fallado el login.
     *         Si llega a 5, LoginServlet bloquea la cuenta.
     */
    public int getIntentosFallidos() { return intentosFallidos; }
    public void setIntentosFallidos(int intentosFallidos) { this.intentosFallidos = intentosFallidos; }

    /**
     * @return Timestamp hasta el cual la cuenta está bloqueada.
     *         null = cuenta activa. Comparado con el momento actual en LoginServlet.
     */
    public Timestamp getBloqueadoHasta() { return bloqueadoHasta; }
    public void setBloqueadoHasta(Timestamp bloqueadoHasta) { this.bloqueadoHasta = bloqueadoHasta; }

    /** @return Fecha de registro del usuario en el sistema */
    public Timestamp getFechaRegistro() { return fechaRegistro; }
    public void setFechaRegistro(Timestamp fechaRegistro) { this.fechaRegistro = fechaRegistro; }

    /**
     * @return Timestamp del último login exitoso.
     *         Actualizado por UsuarioDAO.actualizarUltimaSesion() en cada login.
     */
    public Timestamp getUltimaSesion() { return ultimaSesion; }
    public void setUltimaSesion(Timestamp ultimaSesion) { this.ultimaSesion = ultimaSesion; }
}
