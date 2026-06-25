package com.nirami.dao;

import com.nirami.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * DAO (Data Access Object) que gestiona el registro de revisiones de productos en la tabla
 * {@code revisiones_producto}.
 *
 * Tabla destino: {@code revisiones_producto}
 * Columnas: id_revision (PK, AUTO_INCREMENT), id_producto (FK→productos),
 *           id_admin (FK→usuarios, tipo=ADMIN), accion (APROBADO|RECHAZADO),
 *           observacion (motivo del rechazo), fecha_revision (DEFAULT NOW())
 *
 * Propósito: Auditoría de decisiones de revisión.
 * Permite rastrear QUIÉN aprobó o rechazó un producto, CUÁNDO y POR QUÉ.
 * Esto es especialmente útil si el vendedor quiere saber por qué fue rechazado su producto.
 *
 * Clase que usa este DAO:
 *   → RevisionProductoServlet.doPost() (después de que el admin toma la decisión)
 */
public class RevisionDAO {

    /**
     * Registra el resultado de la revisión de un producto por el administrador.
     *
     * Este método se llama DESPUÉS de que ProductoDAO.actualizarEstado() ha sido llamado,
     * garantizando que la revisión siempre queda registrada para auditoría.
     *
     * Llamada por: RevisionProductoServlet.doPost()
     *   cuando accion = "APROBADO" o "RECHAZADO"
     *
     * @param idProducto   ID del producto revisado (FK → productos.id_producto)
     * @param idAdmin      ID del administrador que tomó la decisión (FK → usuarios.id_usuario)
     * @param accion       Resultado de la revisión: "APROBADO" o "RECHAZADO"
     * @param observacion  Motivo de la decisión (obligatorio si es RECHAZADO, opcional si APROBADO).
     *                     El vendedor podrá leer este texto para saber qué corregir.
     * @return true si el INSERT fue exitoso; false si hubo error de BD
     */
    public boolean registrarRevision(int idProducto, int idAdmin, String accion, String observacion) {

        // INSERT en la tabla de auditoría de revisiones
        // La columna fecha_revision usa DEFAULT NOW() → no se necesita especificar la fecha
        String sql = "INSERT INTO revisiones_producto (id_producto, id_admin, accion, observacion) " +
                     "VALUES (?, ?, ?, ?)";

        try (Connection       conn = DBConnection.getConnection();
             PreparedStatement ps  = conn.prepareStatement(sql)) {

            ps.setInt(1, idProducto);    // FK: qué producto fue revisado
            ps.setInt(2, idAdmin);       // FK: qué administrador hizo la revisión
            ps.setString(3, accion);     // Resultado: "APROBADO" o "RECHAZADO"
            ps.setString(4, observacion); // Motivo detallado (null si fue APROBADO y no hay nota)

            // executeUpdate() inserta 1 fila y devuelve cuántas filas afectó (debe ser 1)
            int rows = ps.executeUpdate();
            return rows > 0; // true si la fila fue insertada correctamente

        } catch (SQLException e) {
            // Error de BD (conexión caída, FK inválida, etc.)
            e.printStackTrace();
            return false;
        }
    }
}
