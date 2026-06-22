package com.nirami.dao;

import com.nirami.model.Producto;
import com.nirami.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class FavoritoDAO {

    public boolean agregarFavorito(int idCliente, int idProducto) {
        String sql = "INSERT INTO favoritos (id_cliente, id_producto) VALUES (?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idCliente);
            ps.setInt(2, idProducto);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean quitarFavorito(int idCliente, int idProducto) {
        String sql = "DELETE FROM favoritos WHERE id_cliente = ? AND id_producto = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idCliente);
            ps.setInt(2, idProducto);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean esFavorito(int idCliente, int idProducto) {
        String sql = "SELECT 1 FROM favoritos WHERE id_cliente = ? AND id_producto = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idCliente);
            ps.setInt(2, idProducto);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<Producto> obtenerFavoritosPorCliente(int idCliente) {
        List<Producto> lista = new ArrayList<>();
        String sql = "SELECT p.*, (SELECT ruta FROM producto_imagenes pi WHERE pi.id_producto = p.id_producto AND pi.es_principal = 1 LIMIT 1) AS imagen_principal " +
                     "FROM favoritos f " +
                     "JOIN productos p ON f.id_producto = p.id_producto " +
                     "WHERE f.id_cliente = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idCliente);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Producto p = new Producto();
                    p.setIdProducto(rs.getInt("id_producto"));
                    p.setNombre(rs.getString("nombre"));
                    p.setDescripcion(rs.getString("descripcion"));
                    p.setPrecio(rs.getBigDecimal("precio"));
                    p.setStock(rs.getInt("stock"));
                    p.setIdCategoria(rs.getInt("id_categoria"));
                    p.setEstado(rs.getString("estado"));
                    p.setImagenPrincipal(rs.getString("imagen_principal"));
                    lista.add(p);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return lista;
    }
}
