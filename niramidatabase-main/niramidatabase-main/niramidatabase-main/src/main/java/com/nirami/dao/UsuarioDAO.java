package com.nirami.dao;

import com.nirami.model.Usuario;
import com.nirami.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO (Data Access Object) para gestionar todas las operaciones CRUD
 * sobre la entidad {@link Usuario} y sus subtipos ({@code clientes}, {@code vendedores}).
 *
 * <p>Este DAO interactúa con las tablas:</p>
 * <ul>
 *   <li>{@code usuarios} — Datos comunes de todos los tipos de usuario</li>
 *   <li>{@code clientes} — Subtipo de usuario con {@code direccion_envio}</li>
 *   <li>{@code vendedores} — Subtipo de usuario con {@code cuenta_bancaria} y {@code biografia}</li>
 * </ul>
 *
 * <p><b>Patrón de subtipo:</b> El sistema usa una herencia de tabla única con
 * discriminador ({@code tipo_usuario}), más tablas de subtipo. Por eso las operaciones
 * de escritura (insertar/actualizar) son transaccionales y tocan múltiples tablas.</p>
 *
 * <p><b>Conexión a BD:</b> Usa {@link com.nirami.util.DBConnection#getConnection()}.
 * Las operaciones de escritura usan {@code setAutoCommit(false)} para garantizar
 * atomicidad.</p>
 *
 * <p><b>Servlets que usan este DAO:</b>
 * <ul>
 *   <li>{@link com.nirami.controller.RegistroServlet}</li>
 *   <li>{@link com.nirami.controller.LoginServlet}</li>
 *   <li>{@link com.nirami.controller.PerfilServlet}</li>
 *   <li>{@link com.nirami.controller.AdminUsuariosServlet}</li>
 *   <li>{@link com.nirami.controller.AdminVentasServlet}</li>
 * </ul>
 * </p>
 *
 * @author Nirami Dev Team
 * @version 1.1
 * @see com.nirami.model.Usuario
 */
public class UsuarioDAO {

    /**
     * Registra un nuevo usuario en el sistema usando una transacción que inserta
     * en {@code usuarios} y en la tabla de subtipo correspondiente.
     *
     * <p><b>Flujo transaccional:</b></p>
     * <ol>
     *   <li>Inserta en {@code usuarios} y recupera el ID generado</li>
     *   <li>Si {@code tipo_usuario = 'CLIENTE'}: inserta en {@code clientes} con {@code extraInfo} como dirección</li>
     *   <li>Si {@code tipo_usuario = 'VENDEDOR'}: inserta en {@code vendedores} con {@code extraInfo} como cuenta bancaria</li>
     *   <li>Si {@code tipo_usuario = 'ADMIN'}: no inserta en subtipo (el admin no tiene tabla de subtipo)</li>
     * </ol>
     *
     * @param usuario   Objeto {@link Usuario} con nombre, correo, teléfono, hash de contraseña y tipo_usuario
     * @param extraInfo Información del subtipo:
     *                  <ul>
     *                    <li>Para CLIENTE: dirección de envío (puede estar vacía inicialmente)</li>
     *                    <li>Para VENDEDOR: cuenta bancaria (requerida por la BD, NOT NULL)</li>
     *                    <li>Para ADMIN: ignorado</li>
     *                  </ul>
     * @return {@code true} si el registro fue exitoso; {@code false} si el correo ya existe o hay error SQL
     */
    public boolean registrarUsuario(Usuario usuario, String extraInfo) {
        String sqlUsuario = "INSERT INTO usuarios (nombre, correo, telefono, contrasena_hash, tipo_usuario) VALUES (?, ?, ?, ?, ?)";
        Connection conn = null;
        PreparedStatement psUsuario = null;
        PreparedStatement psSubtipo = null;
        ResultSet rs = null;

        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false); // ── Inicio de transacción ──

            psUsuario = conn.prepareStatement(sqlUsuario, Statement.RETURN_GENERATED_KEYS);
            psUsuario.setString(1, usuario.getNombre());
            psUsuario.setString(2, usuario.getCorreo());
            psUsuario.setString(3, usuario.getTelefono());
            psUsuario.setString(4, usuario.getContrasenaHash());
            psUsuario.setString(5, usuario.getTipoUsuario());

            int affectedRows = psUsuario.executeUpdate();
            if (affectedRows == 0) {
                conn.rollback();
                return false;
            }

            // Recuperar el ID asignado por AUTO_INCREMENT
            rs = psUsuario.getGeneratedKeys();
            if (rs.next()) {
                int idUsuario = rs.getInt(1);
                usuario.setIdUsuario(idUsuario);

                // Insertar en la tabla de subtipo según el rol
                if ("CLIENTE".equals(usuario.getTipoUsuario())) {
                    String sqlCliente = "INSERT INTO clientes (id_usuario, direccion_envio) VALUES (?, ?)";
                    psSubtipo = conn.prepareStatement(sqlCliente);
                    psSubtipo.setInt(1, idUsuario);
                    psSubtipo.setString(2, extraInfo); // dirección inicial (puede ser vacía)
                    psSubtipo.executeUpdate();
                } else if ("VENDEDOR".equals(usuario.getTipoUsuario())) {
                    String sqlVendedor = "INSERT INTO vendedores (id_usuario, cuenta_bancaria) VALUES (?, ?)";
                    psSubtipo = conn.prepareStatement(sqlVendedor);
                    psSubtipo.setInt(1, idUsuario);
                    psSubtipo.setString(2, extraInfo); // cuenta bancaria (obligatoria)
                    psSubtipo.executeUpdate();
                }
                // ADMIN: no tiene tabla de subtipo
            }

            conn.commit(); // ── Confirmar transacción ──
            return true;

        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            }
            e.printStackTrace();
            return false;
        } finally {
            try {
                if (rs != null) rs.close();
                if (psSubtipo != null) psSubtipo.close();
                if (psUsuario != null) psUsuario.close();
                if (conn != null) { conn.setAutoCommit(true); conn.close(); }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Busca un usuario por su correo electrónico (único en el sistema).
     *
     * <p>Usado principalmente por {@link com.nirami.controller.LoginServlet} para
     * autenticar usuarios. Devuelve todos los campos de la tabla {@code usuarios}
     * incluyendo el hash de contraseña (para comparación con bcrypt).</p>
     *
     * @param correo Correo electrónico del usuario a buscar
     * @return Objeto {@link Usuario} completo si existe; {@code null} si no se encuentra
     */
    public Usuario obtenerPorCorreo(String correo) {
        String sql = "SELECT * FROM usuarios WHERE correo = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, correo);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapearUsuario(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Obtiene una lista de usuarios filtrada por tipo.
     *
     * <p>Usada por el administrador para listar vendedores o clientes en
     * {@link com.nirami.controller.AdminUsuariosServlet}. También usada por
     * {@link com.nirami.controller.AdminVentasServlet} para poblar los selectores de filtro.</p>
     *
     * @param tipo Tipo de usuario a filtrar: {@code "VENDEDOR"}, {@code "CLIENTE"} o {@code "ADMIN"}
     * @return Lista de {@link Usuario} del tipo especificado, ordenada por fecha de registro descendente;
     *         lista vacía si no hay usuarios de ese tipo
     */
    public List<Usuario> obtenerUsuariosPorTipo(String tipo) {
        List<Usuario> lista = new ArrayList<>();
        String sql = "SELECT * FROM usuarios WHERE tipo_usuario = ? ORDER BY fecha_registro DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, tipo);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Usuario u = new Usuario();
                    u.setIdUsuario(rs.getInt("id_usuario"));
                    u.setNombre(rs.getString("nombre"));
                    u.setCorreo(rs.getString("correo"));
                    u.setTelefono(rs.getString("telefono"));
                    u.setTipoUsuario(rs.getString("tipo_usuario"));
                    u.setEstado(rs.getString("estado"));
                    u.setFechaRegistro(rs.getTimestamp("fecha_registro"));
                    u.setUltimaSesion(rs.getTimestamp("ultima_sesion"));
                    lista.add(u);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return lista;
    }

    /**
     * Actualiza el perfil completo de un usuario (datos en {@code usuarios} y subtipo).
     *
     * <p>Construye el SQL dinámicamente: solo actualiza {@code contrasena_hash} si se proporcionó
     * una nueva contraseña, y {@code foto_perfil} si se cargó una nueva imagen.</p>
     *
     * <p>En la misma transacción, actualiza también la tabla de subtipo:
     * {@code vendedores.cuenta_bancaria} o {@code clientes.direccion_envio}.</p>
     *
     * @param usuario   Objeto {@link Usuario} con los datos actualizados.
     *                  Si {@code contrasenaHash} es null o vacío, NO se actualiza la contraseña.
     *                  Si {@code fotoPerfil} es null, NO se actualiza la foto.
     * @param extraInfo Nueva cuenta bancaria (VENDEDOR) o nueva dirección (CLIENTE)
     * @return {@code true} si la actualización fue exitosa; {@code false} en caso de error
     */
    public boolean actualizarPerfilCompleto(Usuario usuario, String extraInfo) {
        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);

            // Construir SQL dinámico según qué campos opcionales se proporcionaron
            StringBuilder sqlUsuario = new StringBuilder("UPDATE usuarios SET nombre = ?, telefono = ?, correo = ?");
            if (usuario.getContrasenaHash() != null && !usuario.getContrasenaHash().isEmpty()) {
                sqlUsuario.append(", contrasena_hash = ?");
            }
            if (usuario.getFotoPerfil() != null) {
                sqlUsuario.append(", foto_perfil = ?");
            }
            sqlUsuario.append(" WHERE id_usuario = ?");

            try (PreparedStatement ps = conn.prepareStatement(sqlUsuario.toString())) {
                int paramIdx = 1;
                ps.setString(paramIdx++, usuario.getNombre());
                ps.setString(paramIdx++, usuario.getTelefono());
                ps.setString(paramIdx++, usuario.getCorreo());

                if (usuario.getContrasenaHash() != null && !usuario.getContrasenaHash().isEmpty()) {
                    ps.setString(paramIdx++, usuario.getContrasenaHash());
                }
                if (usuario.getFotoPerfil() != null) {
                    ps.setString(paramIdx++, usuario.getFotoPerfil());
                }
                ps.setInt(paramIdx, usuario.getIdUsuario());
                ps.executeUpdate();
            }

            // Actualizar la tabla de subtipo
            if ("VENDEDOR".equals(usuario.getTipoUsuario())) {
                String sqlVend = "UPDATE vendedores SET cuenta_bancaria = ? WHERE id_usuario = ?";
                try (PreparedStatement ps = conn.prepareStatement(sqlVend)) {
                    ps.setString(1, extraInfo);
                    ps.setInt(2, usuario.getIdUsuario());
                    ps.executeUpdate();
                }
            } else if ("CLIENTE".equals(usuario.getTipoUsuario())) {
                String sqlCli = "UPDATE clientes SET direccion_envio = ? WHERE id_usuario = ?";
                try (PreparedStatement ps = conn.prepareStatement(sqlCli)) {
                    ps.setString(1, extraInfo);
                    ps.setInt(2, usuario.getIdUsuario());
                    ps.executeUpdate();
                }
            }

            conn.commit();
            return true;
        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            }
            e.printStackTrace();
            return false;
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (SQLException e) { e.printStackTrace(); }
            }
        }
    }

    /**
     * Obtiene la información extra del subtipo de un usuario.
     *
     * <p>Para VENDEDOR: devuelve la {@code cuenta_bancaria}.<br>
     * Para CLIENTE: devuelve la {@code direccion_envio}.<br>
     * Para ADMIN: devuelve cadena vacía.</p>
     *
     * @param idUsuario   ID del usuario
     * @param tipoUsuario Tipo del usuario ({@code "VENDEDOR"}, {@code "CLIENTE"}, {@code "ADMIN"})
     * @return String con el dato del subtipo, o cadena vacía si no aplica o hay error
     */
    public String obtenerExtraInfo(int idUsuario, String tipoUsuario) {
        String sql = "";
        String column = "";

        if ("VENDEDOR".equals(tipoUsuario)) {
            sql = "SELECT cuenta_bancaria FROM vendedores WHERE id_usuario = ?";
            column = "cuenta_bancaria";
        } else if ("CLIENTE".equals(tipoUsuario)) {
            sql = "SELECT direccion_envio FROM clientes WHERE id_usuario = ?";
            column = "direccion_envio";
        } else {
            return ""; // ADMIN no tiene tabla de subtipo
        }

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idUsuario);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String info = rs.getString(column);
                    return info != null ? info : "";
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "";
    }

    /**
     * Actualiza el campo {@code ultima_sesion} del usuario al momento actual.
     *
     * <p>Este método es llamado por {@link com.nirami.controller.LoginServlet}
     * inmediatamente después de que el usuario inicia sesión exitosamente.
     * Permite al administrador ver cuándo fue el último acceso de cada usuario.</p>
     *
     * @param idUsuario ID del usuario que acaba de iniciar sesión
     */
    public void actualizarUltimaSesion(int idUsuario) {
        String sql = "UPDATE usuarios SET ultima_sesion = NOW() WHERE id_usuario = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idUsuario);
            ps.executeUpdate();
        } catch (SQLException e) {
            // Error no crítico: no debe detener el flujo de login
            System.err.println("[UsuarioDAO] No se pudo actualizar ultima_sesion para usuario " + idUsuario);
            e.printStackTrace();
        }
    }

    // ─── Método privado de mapeo ─────────────────────────────────────────────

    /**
     * Mapea todas las columnas de la tabla {@code usuarios} a un objeto {@link Usuario}.
     *
     * @param rs ResultSet posicionado en la fila actual (consulta sobre {@code usuarios})
     * @return Objeto {@link Usuario} completamente mapeado
     * @throws SQLException si alguna columna no existe o hay error de tipo de dato
     */
    private Usuario mapearUsuario(ResultSet rs) throws SQLException {
        Usuario u = new Usuario();
        u.setIdUsuario(rs.getInt("id_usuario"));
        u.setNombre(rs.getString("nombre"));
        u.setCorreo(rs.getString("correo"));
        u.setTelefono(rs.getString("telefono"));
        u.setContrasenaHash(rs.getString("contrasena_hash"));
        u.setTipoUsuario(rs.getString("tipo_usuario"));
        u.setFotoPerfil(rs.getString("foto_perfil"));
        u.setEstado(rs.getString("estado"));
        u.setIntentosFallidos(rs.getInt("intentos_fallidos"));
        u.setBloqueadoHasta(rs.getTimestamp("bloqueado_hasta"));
        u.setFechaRegistro(rs.getTimestamp("fecha_registro"));
        u.setUltimaSesion(rs.getTimestamp("ultima_sesion"));
        return u;
    }
}
