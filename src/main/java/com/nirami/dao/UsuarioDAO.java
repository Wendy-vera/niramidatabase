package com.nirami.dao;

import com.nirami.model.Usuario;
import com.nirami.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class UsuarioDAO {

    public boolean registrarUsuario(Usuario usuario, String extraInfo) {
        String sqlUsuario = "INSERT INTO usuarios (nombre, correo, telefono, contrasena_hash, tipo_usuario) VALUES (?, ?, ?, ?, ?)";
        Connection conn = null;
        PreparedStatement psUsuario = null;
        PreparedStatement psSubtipo = null;
        ResultSet rs = null;

        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false); // Transacción

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

            rs = psUsuario.getGeneratedKeys();
            if (rs.next()) {
                int idUsuario = rs.getInt(1);
                usuario.setIdUsuario(idUsuario);

                if ("CLIENTE".equals(usuario.getTipoUsuario())) {
                    String sqlCliente = "INSERT INTO clientes (id_usuario, direccion_envio) VALUES (?, ?)";
                    psSubtipo = conn.prepareStatement(sqlCliente);
                    psSubtipo.setInt(1, idUsuario);
                    psSubtipo.setString(2, extraInfo); // extraInfo puede ser la dirección vacía inicial
                    psSubtipo.executeUpdate();
                } else if ("VENDEDOR".equals(usuario.getTipoUsuario())) {
                    String sqlVendedor = "INSERT INTO vendedores (id_usuario, cuenta_bancaria) VALUES (?, ?)";
                    psSubtipo = conn.prepareStatement(sqlVendedor);
                    psSubtipo.setInt(1, idUsuario);
                    psSubtipo.setString(2, extraInfo); // extraInfo debe ser la cuenta bancaria obligatoria
                    psSubtipo.executeUpdate();
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
            try {
                if (rs != null) rs.close();
                if (psSubtipo != null) psSubtipo.close();
                if (psUsuario != null) psUsuario.close();
                if (conn != null) {
                    conn.setAutoCommit(true);
                    conn.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public Usuario obtenerPorCorreo(String correo) {
        String sql = "SELECT * FROM usuarios WHERE correo = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, correo);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
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
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public java.util.List<Usuario> obtenerUsuariosPorTipo(String tipo) {
        java.util.List<Usuario> lista = new java.util.ArrayList<>();
        String sql = "SELECT * FROM usuarios WHERE tipo_usuario = ?";
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
                    lista.add(u);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return lista;
    }

    public boolean actualizarPerfilCompleto(Usuario usuario, String extraInfo) {
        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);
            
            // Construir SQL dinámico
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
                ps.setInt(paramIdx++, usuario.getIdUsuario());
                
                ps.executeUpdate();
            }
            
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


    public String obtenerExtraInfo(int idUsuario, String tipoUsuario) {
        String info = "";
        String sql = "";
        String column = "";
        
        if ("VENDEDOR".equals(tipoUsuario)) {
            sql = "SELECT cuenta_bancaria FROM vendedores WHERE id_usuario = ?";
            column = "cuenta_bancaria";
        } else if ("CLIENTE".equals(tipoUsuario)) {
            sql = "SELECT direccion_envio FROM clientes WHERE id_usuario = ?";
            column = "direccion_envio";
        } else {
            return info;
        }
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idUsuario);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    info = rs.getString(column);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return info != null ? info : "";
    }
}
