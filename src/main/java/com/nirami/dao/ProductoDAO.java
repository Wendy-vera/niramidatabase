package com.nirami.dao;

import com.nirami.model.Producto;
import com.nirami.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class ProductoDAO {

    public List<Producto> obtenerProductosAprobados() {
        List<Producto> lista = new ArrayList<>();
        String sql = "SELECT p.*, COALESCE(SUM(vp.stock_local), 0) AS stock_real, " +
                     "(SELECT ruta FROM producto_imagenes pi WHERE pi.id_producto = p.id_producto AND pi.es_principal = 1 LIMIT 1) AS imagen_principal " +
                     "FROM productos p " +
                     "LEFT JOIN vendedor_producto vp ON p.id_producto = vp.id_producto " +
                     "WHERE p.estado = 'APROBADO' GROUP BY p.id_producto";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                lista.add(mapResultSetToProducto(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return lista;
    }

    public List<Producto> obtenerPorCategoria(int idCategoria) {
        List<Producto> lista = new ArrayList<>();
        String sql = "SELECT p.*, COALESCE(SUM(vp.stock_local), 0) AS stock_real, " +
                     "(SELECT ruta FROM producto_imagenes pi WHERE pi.id_producto = p.id_producto AND pi.es_principal = 1 LIMIT 1) AS imagen_principal " +
                     "FROM productos p " +
                     "LEFT JOIN vendedor_producto vp ON p.id_producto = vp.id_producto " +
                     "WHERE p.estado = 'APROBADO' AND p.id_categoria = ? GROUP BY p.id_producto";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
             
            ps.setInt(1, idCategoria);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapResultSetToProducto(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return lista;
    }
    
    public Producto obtenerPorId(int id) {
        String sql = "SELECT p.*, vp.id_vendedor, COALESCE(vp.stock_local, 0) AS stock_real, (SELECT ruta FROM producto_imagenes pi WHERE pi.id_producto = p.id_producto AND pi.es_principal = 1 LIMIT 1) AS imagen_principal " +
                     "FROM productos p " +
                     "LEFT JOIN vendedor_producto vp ON p.id_producto = vp.id_producto " +
                     "WHERE p.id_producto = ?";
                     
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
             
            ps.setInt(1, id);
            try(ResultSet rs = ps.executeQuery()) {
                if(rs.next()) {
                    return mapResultSetToProducto(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // Nuevo: Obtener productos vinculados a un vendedor específico
    public List<Producto> obtenerPorVendedor(int idVendedor, String estado) {
        List<Producto> lista = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
            "SELECT p.*, vp.stock_local, (SELECT ruta FROM producto_imagenes pi WHERE pi.id_producto = p.id_producto AND pi.es_principal = 1 LIMIT 1) AS imagen_principal " +
            "FROM productos p " +
            "JOIN vendedor_producto vp ON p.id_producto = vp.id_producto " +
            "WHERE vp.id_vendedor = ?"
        );

        if (estado != null && !estado.trim().isEmpty()) {
            sql.append(" AND p.estado = ?");
        }

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
             
            ps.setInt(1, idVendedor);
            if (estado != null && !estado.trim().isEmpty()) {
                ps.setString(2, estado);
            }

            try(ResultSet rs = ps.executeQuery()) {
                while(rs.next()) {
                    Producto p = mapResultSetToProducto(rs);
                    p.setStock(rs.getInt("stock_local")); // Sobrescribimos con el stock del vendedor para la vista
                    lista.add(p);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return lista;
    }

    public List<Producto> obtenerPorVendedor(int idVendedor) {
        return obtenerPorVendedor(idVendedor, null);
    }

    // Nuevo: Crear un producto sugerido por un vendedor
    public boolean crearProducto(Producto producto, int idVendedor, String rutaImagen) {
        String sqlProd = "INSERT INTO productos (nombre, descripcion, precio, id_categoria, estado) VALUES (?, ?, ?, ?, 'PENDIENTE')";
        String sqlVP = "INSERT INTO vendedor_producto (id_vendedor, id_producto, stock_local, es_creador) VALUES (?, ?, ?, 1)";
        String sqlImg = "INSERT INTO producto_imagenes (id_producto, ruta, es_principal) VALUES (?, ?, 1)";

        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);

            // 1. Insertar Producto
            PreparedStatement psProd = conn.prepareStatement(sqlProd, Statement.RETURN_GENERATED_KEYS);
            psProd.setString(1, producto.getNombre());
            psProd.setString(2, producto.getDescripcion());
            psProd.setBigDecimal(3, producto.getPrecio());
            psProd.setInt(4, producto.getIdCategoria());
            psProd.executeUpdate();

            ResultSet rs = psProd.getGeneratedKeys();
            int idProd = 0;
            if (rs.next()) {
                idProd = rs.getInt(1);
            }

            // 2. Insertar Vendedor-Producto
            PreparedStatement psVP = conn.prepareStatement(sqlVP);
            psVP.setInt(1, idVendedor);
            psVP.setInt(2, idProd);
            psVP.setInt(3, producto.getStock());
            psVP.executeUpdate();

            // 3. Insertar Imagen si existe
            if (rutaImagen != null && !rutaImagen.isEmpty()) {
                PreparedStatement psImg = conn.prepareStatement(sqlImg);
                psImg.setInt(1, idProd);
                psImg.setString(2, rutaImagen);
                psImg.executeUpdate();
                psImg.close();
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

    public boolean actualizarProducto(Producto producto, String rutaImagen) {
        String sqlProd = "UPDATE productos SET nombre = ?, descripcion = ?, precio = ?, id_categoria = ?, estado = 'PENDIENTE' WHERE id_producto = ?";
        String sqlVP = "UPDATE vendedor_producto SET stock_local = ? WHERE id_producto = ? AND id_vendedor = ?";
        String sqlImgCheck = "SELECT count(*) FROM producto_imagenes WHERE id_producto = ?";
        String sqlImgUpdate = "UPDATE producto_imagenes SET ruta = ? WHERE id_producto = ? AND es_principal = 1";
        String sqlImgInsert = "INSERT INTO producto_imagenes (id_producto, ruta, es_principal) VALUES (?, ?, 1)";

        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);

            try (PreparedStatement psProd = conn.prepareStatement(sqlProd)) {
                psProd.setString(1, producto.getNombre());
                psProd.setString(2, producto.getDescripcion());
                psProd.setBigDecimal(3, producto.getPrecio());
                psProd.setInt(4, producto.getIdCategoria());
                psProd.setInt(5, producto.getIdProducto());
                psProd.executeUpdate();
            }

            try (PreparedStatement psVP = conn.prepareStatement(sqlVP)) {
                psVP.setInt(1, producto.getStock());
                psVP.setInt(2, producto.getIdProducto());
                psVP.setInt(3, producto.getIdVendedor());
                psVP.executeUpdate();
            }

            if (rutaImagen != null && !rutaImagen.isEmpty()) {
                boolean hasImage = false;
                try (PreparedStatement psCheck = conn.prepareStatement(sqlImgCheck)) {
                    psCheck.setInt(1, producto.getIdProducto());
                    try (ResultSet rs = psCheck.executeQuery()) {
                        if (rs.next() && rs.getInt(1) > 0) hasImage = true;
                    }
                }
                
                if (hasImage) {
                    try (PreparedStatement psUpd = conn.prepareStatement(sqlImgUpdate)) {
                        psUpd.setString(1, rutaImagen);
                        psUpd.setInt(2, producto.getIdProducto());
                        psUpd.executeUpdate();
                    }
                } else {
                    try (PreparedStatement psIns = conn.prepareStatement(sqlImgInsert)) {
                        psIns.setInt(1, producto.getIdProducto());
                        psIns.setString(2, rutaImagen);
                        psIns.executeUpdate();
                    }
                }
            }

            conn.commit();
            return true;
        } catch (SQLException e) {
            if (conn != null) try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            e.printStackTrace();
            return false;
        } finally {
            if (conn != null) try { conn.setAutoCommit(true); conn.close(); } catch (SQLException e) { e.printStackTrace(); }
        }
    }

    // Nuevo: Obtener productos pendientes de revisión
    public List<Producto> obtenerProductosPendientes() {
        List<Producto> lista = new ArrayList<>();
        String sql = "SELECT p.*, vp.id_vendedor, COALESCE(vp.stock_local, 0) AS stock_real, " +
                     "(SELECT ruta FROM producto_imagenes pi WHERE pi.id_producto = p.id_producto AND pi.es_principal = 1 LIMIT 1) AS imagen_principal " +
                     "FROM productos p " +
                     "LEFT JOIN vendedor_producto vp ON p.id_producto = vp.id_producto " +
                     "WHERE p.estado = 'PENDIENTE'";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                lista.add(mapResultSetToProducto(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return lista;
    }

    // Nuevo: Actualizar el estado de un producto (Aprobar/Rechazar)
    public boolean actualizarEstado(int idProducto, String nuevoEstado) {
        String sql = "UPDATE productos SET estado = ? WHERE id_producto = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, nuevoEstado);
            ps.setInt(2, idProducto);
            int rows = ps.executeUpdate();
            return rows > 0;
            
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Nuevo: Buscar productos aprobados por nombre
    public List<Producto> buscarProductosPorNombre(String query) {
        List<Producto> lista = new ArrayList<>();
        String sql = "SELECT p.*, COALESCE(SUM(vp.stock_local), 0) AS stock_real, " +
                     "(SELECT ruta FROM producto_imagenes pi WHERE pi.id_producto = p.id_producto AND pi.es_principal = 1 LIMIT 1) AS imagen_principal " +
                     "FROM productos p " +
                     "LEFT JOIN vendedor_producto vp ON p.id_producto = vp.id_producto " +
                     "WHERE p.estado = 'APROBADO' AND p.nombre LIKE ? GROUP BY p.id_producto";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
             
            ps.setString(1, "%" + query + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapResultSetToProducto(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return lista;
    }

    private Producto mapResultSetToProducto(ResultSet rs) throws SQLException {
        Producto p = new Producto();
        p.setIdProducto(rs.getInt("id_producto"));
        p.setNombre(rs.getString("nombre"));
        p.setDescripcion(rs.getString("descripcion"));
        p.setPrecio(rs.getBigDecimal("precio"));
        // 'stock' podría venir de 'vp.stock_local' (en JOINS con vp) o 'p.stock' (como alias o por defecto en la BD, si existe)
        // Lee "stock_real" primero (alias sin colisión con p.stock=0 de la tabla).
        // Si no existe, intenta "stock" como fallback.
        try {
            p.setStock(rs.getInt("stock_real"));
        } catch (SQLException e1) {
            try { p.setStock(rs.getInt("stock")); } catch (SQLException e2) {}
        }
        p.setIdCategoria(rs.getInt("id_categoria"));
        p.setEstado(rs.getString("estado"));
        p.setVistas(rs.getInt("vistas"));
        p.setFechaCreacion(rs.getTimestamp("fecha_creacion"));
        p.setFechaActualizacion(rs.getTimestamp("fecha_actualizacion"));
        
        try { p.setImagenPrincipal(rs.getString("imagen_principal")); } catch (SQLException e) {}
        
        // Mapeo seguro de id_vendedor en caso de que la query haya hecho JOIN con vendedor_producto
        try {
            p.setIdVendedor(rs.getInt("id_vendedor"));
        } catch (SQLException e) {
            // Ignorar si la columna no existe en el ResultSet
        }
        
        return p;
    }

    public boolean tieneCompras(int idProducto) {
        String sql = "SELECT COUNT(*) FROM orden_detalle WHERE id_producto = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idProducto);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public List<Producto> obtenerTodosAdmin(String estadoFiltro, String fechaFiltro, Integer idVendedorFiltro) {
        List<Producto> lista = new ArrayList<>();
        // Siempre se hace LEFT JOIN con vendedor_producto para obtener el stock_local real.
        // SUM(vp.stock_local) agrupa el stock de todos los vendedores de ese producto.
        // COALESCE garantiza 0 si el producto no tiene ningún vendedor asociado.
        // Cuando se filtra por vendedor específico, el WHERE sobre vp.id_vendedor
        // ya restringe la suma al stock de ese vendedor puntual.
        StringBuilder sql = new StringBuilder(
            "SELECT p.*, COALESCE(SUM(vp.stock_local), 0) AS stock_real, " +
            "(SELECT ruta FROM producto_imagenes pi WHERE pi.id_producto = p.id_producto AND pi.es_principal = 1 LIMIT 1) AS imagen_principal "
        );
        sql.append("FROM productos p ");
        sql.append("LEFT JOIN vendedor_producto vp ON p.id_producto = vp.id_producto ");
        sql.append("WHERE p.estado != 'ELIMINADO' ");

        if (estadoFiltro != null && !estadoFiltro.isEmpty()) {
            sql.append("AND p.estado = ? ");
        }
        if (idVendedorFiltro != null && idVendedorFiltro > 0) {
            // Filtra el JOIN al vendedor elegido; SUM() solo suma su stock_local
            sql.append("AND vp.id_vendedor = ? ");
        }
        if (fechaFiltro != null && !fechaFiltro.isEmpty()) {
            sql.append("AND DATE(p.fecha_creacion) = ? ");
        }
        // GROUP BY obligatorio al usar SUM()
        sql.append("GROUP BY p.id_producto ");
        sql.append("ORDER BY p.fecha_creacion DESC");

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
             
            int paramIndex = 1;
            if (estadoFiltro != null && !estadoFiltro.isEmpty()) {
                ps.setString(paramIndex++, estadoFiltro);
            }
            if (idVendedorFiltro != null && idVendedorFiltro > 0) {
                ps.setInt(paramIndex++, idVendedorFiltro);
            }
            if (fechaFiltro != null && !fechaFiltro.isEmpty()) {
                ps.setString(paramIndex++, fechaFiltro);
            }
            
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapResultSetToProducto(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return lista;
    }
}
