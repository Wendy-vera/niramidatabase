package com.nirami.dao;

import com.nirami.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class RevisionDAO {

    public boolean registrarRevision(int idProducto, int idAdmin, String accion, String observacion) {
        String sql = "INSERT INTO revisiones_producto (id_producto, id_admin, accion, observacion) VALUES (?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setInt(1, idProducto);
            ps.setInt(2, idAdmin);
            ps.setString(3, accion);
            ps.setString(4, observacion);
            
            int rows = ps.executeUpdate();
            return rows > 0;
            
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}
