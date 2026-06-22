package com.nirami.dao;

import com.nirami.model.CartItem;
import com.nirami.util.DBConnection;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import com.nirami.model.Orden;

public class OrdenDAO {

    public boolean crearOrden(int idCliente, BigDecimal subtotal, BigDecimal iva, BigDecimal total, String direccion, List<CartItem> carrito) {
        String sqlOrden      = "INSERT INTO ordenes (id_cliente, subtotal, iva, total, direccion, estado_pago) VALUES (?, ?, ?, ?, ?, 'PENDIENTE')";
        String sqlDetalle    = "INSERT INTO orden_detalle (id_orden, id_producto, id_vendedor, cantidad, precio_unitario, subtotal_linea) VALUES (?, ?, ?, ?, ?, ?)";
        String sqlFindVendedor = "SELECT id_vendedor FROM vendedor_producto WHERE id_producto = ? LIMIT 1";
        // Reducir stock en vendedor_producto al momento de crear la orden
        String sqlReduceStock = "UPDATE vendedor_producto SET stock_local = stock_local - ? WHERE id_producto = ? AND id_vendedor = ? AND stock_local >= ?";
        // Crear pago pendiente para el vendedor (el admin lo aprueba después)
        String sqlPago = "INSERT INTO pagos_vendedor (id_vendedor, id_detalle, monto, porcentaje_comision, monto_comision, monto_neto, estado) VALUES (?, ?, ?, 0.00, 0.00, ?, 'PENDIENTE')";

        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);

            // 1. Insertar Orden
            PreparedStatement psOrden = conn.prepareStatement(sqlOrden, Statement.RETURN_GENERATED_KEYS);
            psOrden.setInt(1, idCliente);
            psOrden.setBigDecimal(2, subtotal);
            psOrden.setBigDecimal(3, iva);
            psOrden.setBigDecimal(4, total);
            psOrden.setString(5, direccion);
            psOrden.executeUpdate();

            ResultSet rs = psOrden.getGeneratedKeys();
            int idOrden = 0;
            if (rs.next()) {
                idOrden = rs.getInt(1);
            }

            // 2. Insertar Detalles + reducir stock + crear pago pendiente
            PreparedStatement psDetalle    = conn.prepareStatement(sqlDetalle, Statement.RETURN_GENERATED_KEYS);
            PreparedStatement psFindVendedor = conn.prepareStatement(sqlFindVendedor);
            PreparedStatement psReduceStock  = conn.prepareStatement(sqlReduceStock);
            PreparedStatement psPago         = conn.prepareStatement(sqlPago);

            for (CartItem item : carrito) {
                // Encontrar al vendedor responsable de este producto
                psFindVendedor.setInt(1, item.getProducto().getIdProducto());
                ResultSet rsVendedor = psFindVendedor.executeQuery();
                int idVendedor = 0;
                if (rsVendedor.next()) {
                    idVendedor = rsVendedor.getInt("id_vendedor");
                }
                rsVendedor.close();

                if (idVendedor == 0) {
                    conn.rollback();
                    return false;
                }

                // Reducir stock_local del vendedor; si no hay suficiente, rollback
                psReduceStock.setInt(1, item.getCantidad());
                psReduceStock.setInt(2, item.getProducto().getIdProducto());
                psReduceStock.setInt(3, idVendedor);
                psReduceStock.setInt(4, item.getCantidad());
                int filasAfectadas = psReduceStock.executeUpdate();
                if (filasAfectadas == 0) {
                    // Stock insuficiente
                    conn.rollback();
                    return false;
                }

                // Insertar detalle de la orden
                psDetalle.setInt(1, idOrden);
                psDetalle.setInt(2, item.getProducto().getIdProducto());
                psDetalle.setInt(3, idVendedor);
                psDetalle.setInt(4, item.getCantidad());
                psDetalle.setBigDecimal(5, item.getProducto().getPrecio());
                psDetalle.setBigDecimal(6, item.getSubtotal());
                psDetalle.executeUpdate();

                // Obtener el id_detalle recién insertado
                ResultSet rsDetalle = psDetalle.getGeneratedKeys();
                int idDetalle = 0;
                if (rsDetalle.next()) {
                    idDetalle = rsDetalle.getInt(1);
                }
                rsDetalle.close();

                // Crear pago pendiente para el vendedor
                psPago.setInt(1, idVendedor);
                psPago.setInt(2, idDetalle);
                psPago.setBigDecimal(3, item.getSubtotal());
                psPago.setBigDecimal(4, item.getSubtotal()); // monto_neto = monto (comisión 0)
                psPago.executeUpdate();
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
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * El administrador aprueba o rechaza el pago de una orden completa.
     * Si se aprueba: estado_pago → APROBADO, estado_orden → COMPLETADA, pagos_vendedor → PAGADO.
     * Si se rechaza: estado_pago → RECHAZADO, estado_orden → CANCELADA,
     *                se restaura el stock en vendedor_producto y los pagos → PENDIENTE se eliminan.
     */
    public boolean actualizarEstadoPago(int idOrden, String nuevoEstado) {
        String sqlOrdenUpdate = "UPDATE ordenes SET estado_pago = ?, estado_orden = ? WHERE id_orden = ?";
        String sqlPagoUpdate  = "UPDATE pagos_vendedor pv " +
                                "JOIN orden_detalle od ON pv.id_detalle = od.id_detalle " +
                                "SET pv.estado = ?, pv.fecha_pago = IF(? = 'APROBADO', NOW(), NULL) " +
                                "WHERE od.id_orden = ?";
        // Para restaurar stock si se rechaza
        String sqlRestoreStock = "UPDATE vendedor_producto vp " +
                                 "JOIN orden_detalle od ON vp.id_vendedor = od.id_vendedor AND vp.id_producto = od.id_producto " +
                                 "SET vp.stock_local = vp.stock_local + od.cantidad " +
                                 "WHERE od.id_orden = ?";

        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);

            String estadoOrden = nuevoEstado.equals("APROBADO") ? "COMPLETADA" : "CANCELADA";
            String estadoPago  = nuevoEstado.equals("APROBADO") ? "PAGADO" : "PENDIENTE";

            // Actualizar orden
            try (PreparedStatement ps = conn.prepareStatement(sqlOrdenUpdate)) {
                ps.setString(1, nuevoEstado);
                ps.setString(2, estadoOrden);
                ps.setInt(3, idOrden);
                ps.executeUpdate();
            }

            // Actualizar pagos_vendedor
            try (PreparedStatement ps = conn.prepareStatement(sqlPagoUpdate)) {
                ps.setString(1, estadoPago);
                ps.setString(2, nuevoEstado);
                ps.setInt(3, idOrden);
                ps.executeUpdate();
            }

            // Si se rechaza, restaurar stock
            if ("RECHAZADO".equals(nuevoEstado)) {
                try (PreparedStatement ps = conn.prepareStatement(sqlRestoreStock)) {
                    ps.setInt(1, idOrden);
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

    public List<Orden> obtenerOrdenesPorCliente(int idCliente) {
        List<Orden> ordenes = new java.util.ArrayList<>();
        String sql = "SELECT * FROM ordenes WHERE id_cliente = ? ORDER BY fecha_orden DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idCliente);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Orden orden = new Orden();
                    orden.setIdOrden(rs.getInt("id_orden"));
                    orden.setIdCliente(rs.getInt("id_cliente"));
                    orden.setSubtotal(rs.getBigDecimal("subtotal"));
                    orden.setIva(rs.getBigDecimal("iva"));
                    orden.setTotal(rs.getBigDecimal("total"));
                    orden.setDireccion(rs.getString("direccion"));
                    orden.setEstadoPago(rs.getString("estado_pago"));
                    orden.setFechaOrden(rs.getTimestamp("fecha_orden"));
                    orden.setDetalles(obtenerDetallesPorOrden(orden.getIdOrden(), conn));
                    ordenes.add(orden);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return ordenes;
    }

    public Orden obtenerOrdenPorId(int idOrden) {
        String sql = "SELECT * FROM ordenes WHERE id_orden = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idOrden);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Orden orden = new Orden();
                    orden.setIdOrden(rs.getInt("id_orden"));
                    orden.setIdCliente(rs.getInt("id_cliente"));
                    orden.setSubtotal(rs.getBigDecimal("subtotal"));
                    orden.setIva(rs.getBigDecimal("iva"));
                    orden.setTotal(rs.getBigDecimal("total"));
                    orden.setDireccion(rs.getString("direccion"));
                    orden.setEstadoPago(rs.getString("estado_pago"));
                    orden.setFechaOrden(rs.getTimestamp("fecha_orden"));
                    orden.setDetalles(obtenerDetallesPorOrden(orden.getIdOrden(), conn));
                    return orden;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    private List<com.nirami.model.OrdenDetalle> obtenerDetallesPorOrden(int idOrden, Connection conn) throws SQLException {
        List<com.nirami.model.OrdenDetalle> detalles = new java.util.ArrayList<>();
        String sql = "SELECT od.*, p.nombre, p.descripcion, (SELECT ruta FROM producto_imagenes pi WHERE pi.id_producto = p.id_producto AND pi.es_principal = 1 LIMIT 1) AS imagen " +
                     "FROM orden_detalle od " +
                     "JOIN productos p ON od.id_producto = p.id_producto " +
                     "WHERE od.id_orden = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idOrden);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    com.nirami.model.OrdenDetalle detalle = new com.nirami.model.OrdenDetalle();
                    detalle.setIdDetalle(rs.getInt("id_detalle"));
                    detalle.setIdOrden(rs.getInt("id_orden"));
                    detalle.setCantidad(rs.getInt("cantidad"));
                    detalle.setPrecioUnitario(rs.getBigDecimal("precio_unitario"));
                    detalle.setSubtotalLinea(rs.getBigDecimal("subtotal_linea"));

                    com.nirami.model.Producto p = new com.nirami.model.Producto();
                    p.setIdProducto(rs.getInt("id_producto"));
                    p.setNombre(rs.getString("nombre"));
                    p.setDescripcion(rs.getString("descripcion"));
                    p.setImagenPrincipal(rs.getString("imagen"));
                    detalle.setProducto(p);

                    detalles.add(detalle);
                }
            }
        }
        return detalles;
    }

    public List<com.nirami.model.VentaAdminDTO> obtenerVentasAdmin(Integer idProducto, Integer idVendedor, Integer idCliente) {
        List<com.nirami.model.VentaAdminDTO> ventas = new java.util.ArrayList<>();
        StringBuilder sql = new StringBuilder(
            "SELECT o.id_orden, o.fecha_orden, o.estado_pago, c.nombre AS cliente, " +
            "p.nombre AS producto, v.nombre AS vendedor, " +
            "od.id_detalle, od.cantidad, od.precio_unitario, od.subtotal_linea " +
            "FROM orden_detalle od " +
            "JOIN ordenes o ON od.id_orden = o.id_orden " +
            "JOIN usuarios c ON o.id_cliente = c.id_usuario " +
            "JOIN productos p ON od.id_producto = p.id_producto " +
            "JOIN usuarios v ON od.id_vendedor = v.id_usuario " +
            "WHERE 1=1 "
        );

        if (idProducto != null && idProducto > 0) sql.append("AND od.id_producto = ? ");
        if (idVendedor != null && idVendedor > 0) sql.append("AND od.id_vendedor = ? ");
        if (idCliente  != null && idCliente  > 0) sql.append("AND o.id_cliente = ? ");

        sql.append("ORDER BY o.fecha_orden DESC");

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            int paramIndex = 1;
            if (idProducto != null && idProducto > 0) ps.setInt(paramIndex++, idProducto);
            if (idVendedor != null && idVendedor > 0) ps.setInt(paramIndex++, idVendedor);
            if (idCliente  != null && idCliente  > 0) ps.setInt(paramIndex++, idCliente);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    com.nirami.model.VentaAdminDTO v = new com.nirami.model.VentaAdminDTO();
                    v.setIdOrden(rs.getInt("id_orden"));
                    v.setIdDetalle(rs.getInt("id_detalle"));
                    v.setFechaOrden(rs.getTimestamp("fecha_orden"));
                    v.setEstadoPago(rs.getString("estado_pago"));
                    v.setNombreCliente(rs.getString("cliente"));
                    v.setNombreProducto(rs.getString("producto"));
                    v.setNombreVendedor(rs.getString("vendedor"));
                    v.setCantidad(rs.getInt("cantidad"));
                    v.setPrecioUnitario(rs.getBigDecimal("precio_unitario"));
                    v.setSubtotalLinea(rs.getBigDecimal("subtotal_linea"));
                    ventas.add(v);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return ventas;
    }
}
