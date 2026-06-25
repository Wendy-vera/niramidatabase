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

/**
 * DAO (Data Access Object) que gestiona TODAS las operaciones sobre la tabla {@code productos}
 * y sus tablas relacionadas ({@code vendedor_producto} y {@code producto_imagenes}).
 *
 * Tablas que este DAO toca:
 *   - {@code productos}         → datos principales del producto (nombre, precio, estado, etc.)
 *   - {@code vendedor_producto} → relación N:M entre vendedores y productos con stock_local
 *   - {@code producto_imagenes} → imágenes del producto (ruta, es_principal)
 *
 * Clases que usan este DAO:
 *   → CatalogoServlet        (obtenerProductosAprobados, buscarProductosPorNombre, obtenerPorCategoria)
 *   → CarritoServlet         (obtenerPorId)
 *   → VendedorDashboardServlet (obtenerPorVendedor)
 *   → ProductoVendedorServlet  (crearProducto, actualizarProducto, obtenerPorId)
 *   → AdminDashboardServlet    (obtenerProductosPendientes)
 *   → RevisionProductoServlet  (obtenerPorId, actualizarEstado)
 *   → AdminProductosServlet    (obtenerTodosAdmin, tieneCompras, actualizarEstado)
 *   → AdminVentasServlet       (obtenerPorVendedor para el filtro)
 */
public class ProductoDAO {

    /**
     * Obtiene todos los productos en estado APROBADO para el catálogo público.
     * Calcula el stock total sumando stock_local de todos los vendedores del producto.
     * Incluye la ruta de la imagen principal mediante subconsulta.
     *
     * Query SQL:
     * SELECT p.*, COALESCE(SUM(vp.stock_local), 0) AS stock_real,
     *   (SELECT ruta FROM producto_imagenes WHERE id_producto = p.id_producto AND es_principal=1 LIMIT 1) AS imagen_principal
     * FROM productos p
     * LEFT JOIN vendedor_producto vp ON p.id_producto = vp.id_producto
     * WHERE p.estado = 'APROBADO'
     * GROUP BY p.id_producto
     *
     * LEFT JOIN: permite incluir productos sin vendedor_producto registrado (stock=0)
     * COALESCE: devuelve 0 si SUM es null (sin filas en vendedor_producto)
     * GROUP BY: necesario porque se usa SUM() (función de agregación)
     *
     * Llamada por: CatalogoServlet.doGet() para mostrar todos los productos disponibles
     *
     * @return Lista de todos los productos aprobados con stock total e imagen principal
     */
    public List<Producto> obtenerProductosAprobados() {
        List<Producto> lista = new ArrayList<>();

        // Query con JOIN y subconsulta para imagen; filtra solo APROBADOS
        String sql = "SELECT p.*, COALESCE(SUM(vp.stock_local), 0) AS stock_real, " +
                     "(SELECT ruta FROM producto_imagenes pi WHERE pi.id_producto = p.id_producto AND pi.es_principal = 1 LIMIT 1) AS imagen_principal " +
                     "FROM productos p " +
                     "LEFT JOIN vendedor_producto vp ON p.id_producto = vp.id_producto " +  // JOIN para stock
                     "WHERE p.estado = 'APROBADO' GROUP BY p.id_producto"; // Agrupa para el SUM

        // try-with-resources: cierra automáticamente Connection, PreparedStatement y ResultSet
        try (Connection       conn = DBConnection.getConnection();
             PreparedStatement ps  = conn.prepareStatement(sql);
             ResultSet         rs  = ps.executeQuery()) {

            // Mapear cada fila del ResultSet a un objeto Producto usando el método compartido
            while (rs.next()) {
                lista.add(mapResultSetToProducto(rs)); // Delegar el mapeo al método privado
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return lista;
    }

    /**
     * Obtiene todos los productos APROBADOS de una categoría específica.
     * Útil para el filtro lateral del catálogo por categoría.
     *
     * Query SQL igual a obtenerProductosAprobados() + filtro por id_categoria.
     *
     * Llamada por: CatalogoServlet.doGet() cuando el usuario filtra por una categoría
     *
     * @param idCategoria FK a categorias.id_categoria
     * @return Lista de productos aprobados en esa categoría
     */
    public List<Producto> obtenerPorCategoria(int idCategoria) {
        List<Producto> lista = new ArrayList<>();

        // Igual al query de obtenerProductosAprobados() + AND p.id_categoria = ?
        String sql = "SELECT p.*, COALESCE(SUM(vp.stock_local), 0) AS stock_real, " +
                     "(SELECT ruta FROM producto_imagenes pi WHERE pi.id_producto = p.id_producto AND pi.es_principal = 1 LIMIT 1) AS imagen_principal " +
                     "FROM productos p " +
                     "LEFT JOIN vendedor_producto vp ON p.id_producto = vp.id_producto " +
                     "WHERE p.estado = 'APROBADO' AND p.id_categoria = ? GROUP BY p.id_producto";

        try (Connection       conn = DBConnection.getConnection();
             PreparedStatement ps  = conn.prepareStatement(sql)) {

            ps.setInt(1, idCategoria); // Parámetro: FK de la categoría a filtrar

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

    /**
     * Obtiene un producto específico por su ID, incluyendo el ID del vendedor.
     * Usada en múltiples contextos: ver detalle, editar, agregar al carrito.
     *
     * Query SQL:
     * SELECT p.*, vp.id_vendedor, COALESCE(vp.stock_local, 0) AS stock_real,
     *   (SELECT ruta FROM imagen...) AS imagen_principal
     * FROM productos p
     * LEFT JOIN vendedor_producto vp ON p.id_producto = vp.id_producto
     * WHERE p.id_producto = ?
     *
     * Llamada por:
     *   - CarritoServlet → verificar que existe y tiene stock antes de agregar
     *   - ProductoVendedorServlet → cargar datos para el formulario de edición
     *   - RevisionProductoServlet → cargar el producto que el admin va a revisar
     *
     * @param id ID del producto buscado (productos.id_producto)
     * @return Objeto Producto con todos sus datos; null si no existe
     */
    public Producto obtenerPorId(int id) {
        // Incluye id_vendedor para que los servlets puedan verificar pertenencia
        String sql = "SELECT p.*, vp.id_vendedor, COALESCE(vp.stock_local, 0) AS stock_real, " +
                     "(SELECT ruta FROM producto_imagenes pi WHERE pi.id_producto = p.id_producto AND pi.es_principal = 1 LIMIT 1) AS imagen_principal " +
                     "FROM productos p " +
                     "LEFT JOIN vendedor_producto vp ON p.id_producto = vp.id_producto " +
                     "WHERE p.id_producto = ?";

        try (Connection       conn = DBConnection.getConnection();
             PreparedStatement ps  = conn.prepareStatement(sql)) {

            ps.setInt(1, id); // Establecer el ID a buscar

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {                          // Si hay resultado (el producto existe)
                    return mapResultSetToProducto(rs);    // Mapear y devolver
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null; // No encontrado (ID inválido o producto no existe)
    }

    /**
     * Obtiene todos los productos de un vendedor específico, con filtro opcional de estado.
     * Usada en el dashboard del vendedor para mostrar su inventario.
     *
     * Query dinámico:
     *   Si estado != null: WHERE vp.id_vendedor = ? AND p.estado = ?
     *   Si estado == null: WHERE vp.id_vendedor = ?
     *
     * Usa StringBuilder para construir la query condicionalmente:
     * Este patrón evita vulnerabilidades SQL injection porque los valores siguen siendo ? (parámetros)
     *
     * Llamada por:
     *   - VendedorDashboardServlet.doGet() → con estado del filtro o null
     *   - AdminVentasServlet → para cargar la lista de productos del selector de filtro
     *   - VendedorVentasServlet → para cargar la lista de productos del selector de filtro
     *
     * @param idVendedor ID del vendedor logueado (usuarios.id_usuario)
     * @param estado     Filtro de estado ("APROBADO", "PENDIENTE", etc.) o null para todos
     * @return Lista de productos del vendedor (filtrados si se especificó estado)
     */
    public List<Producto> obtenerPorVendedor(int idVendedor, String estado) {
        List<Producto> lista = new ArrayList<>();

        // Base de la query: JOIN obligatorio con vendedor_producto para el filtro por vendedor
        StringBuilder sql = new StringBuilder(
            "SELECT p.*, vp.stock_local, " +
            "(SELECT ruta FROM producto_imagenes pi WHERE pi.id_producto = p.id_producto AND pi.es_principal = 1 LIMIT 1) AS imagen_principal " +
            "FROM productos p " +
            "JOIN vendedor_producto vp ON p.id_producto = vp.id_producto " +
            "WHERE vp.id_vendedor = ?"                // Filtro base siempre presente
        );

        // Agregar filtro de estado SOLO si se proporcionó (no es null ni vacío)
        if (estado != null && !estado.trim().isEmpty()) {
            sql.append(" AND p.estado = ?");  // Filtro adicional de estado
        }

        try (Connection       conn = DBConnection.getConnection();
             PreparedStatement ps  = conn.prepareStatement(sql.toString())) {

            ps.setInt(1, idVendedor); // Parámetro 1: siempre el ID del vendedor

            // Solo si se añadió el filtro de estado, poner el parámetro 2
            if (estado != null && !estado.trim().isEmpty()) {
                ps.setString(2, estado); // Parámetro 2: estado si se filtró
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Producto p = mapResultSetToProducto(rs);
                    // NOTA: Sobrescribir el stock con el stock_local del vendedor
                    // El método mapResultSetToProducto ya intentará leer "stock_real"
                    // pero aquí se lee "stock_local" directamente para mayor precisión
                    p.setStock(rs.getInt("stock_local")); // Stock de este vendedor específico
                    lista.add(p);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return lista;
    }

    /**
     * Sobrecarga de obtenerPorVendedor sin filtro de estado.
     * Devuelve todos los productos del vendedor sin importar su estado.
     * Convenio: simplifica la llamada cuando no se necesita filtrar.
     *
     * @param idVendedor ID del vendedor
     * @return Lista de todos los productos del vendedor (todos los estados)
     */
    public List<Producto> obtenerPorVendedor(int idVendedor) {
        return obtenerPorVendedor(idVendedor, null); // Delegar con estado=null
    }

    /**
     * Crea un nuevo producto publicado por un vendedor.
     * Ejecuta una TRANSACCIÓN que inserta en 3 tablas en orden:
     *   1. INSERT en {@code productos}         → registrar el producto (estado='PENDIENTE')
     *   2. INSERT en {@code vendedor_producto}  → asociar el producto al vendedor con su stock
     *   3. INSERT en {@code producto_imagenes}  → guardar la imagen si se subió una
     *
     * El estado se fija en 'PENDIENTE' automáticamente (el admin debe aprobarlo).
     *
     * TRANSACCIÓN: Si cualquier INSERT falla, se hace ROLLBACK para no dejar datos parciales.
     * Este es uno de los pocos métodos que maneja la conexión manualmente (no usa try-with-resources)
     * porque necesita controlar el commit/rollback.
     *
     * Llamada por: ProductoVendedorServlet.doPost() cuando action="crear"
     *
     * @param producto    Objeto con nombre, descripcion, precio, stock, idCategoria completados
     * @param idVendedor  ID del vendedor que publica el producto (FK a usuarios.id_usuario)
     * @param rutaImagen  Ruta relativa de la imagen ("uploads/xxx.jpg") o null si no hay
     * @return true si la transacción completa fue exitosa; false si algún INSERT falló
     */
    public boolean crearProducto(Producto producto, int idVendedor, String rutaImagen) {

        // Query 1: Insertar el producto → estado siempre 'PENDIENTE' hasta que el admin apruebe
        String sqlProd = "INSERT INTO productos (nombre, descripcion, precio, id_categoria, estado) " +
                         "VALUES (?, ?, ?, ?, 'PENDIENTE')";

        // Query 2: Vincular el vendedor al producto con su stock local
        // es_creador=1 indica que este vendedor fue quien creó el producto original
        String sqlVP = "INSERT INTO vendedor_producto (id_vendedor, id_producto, stock_local, es_creador) " +
                       "VALUES (?, ?, ?, 1)";

        // Query 3: Guardar la imagen como la principal (es_principal=1)
        String sqlImg = "INSERT INTO producto_imagenes (id_producto, ruta, es_principal) VALUES (?, ?, 1)";

        Connection conn = null; // Declarar fuera del try para acceder en el catch (rollback)
        try {
            conn = DBConnection.getConnection(); // Obtener conexión del pool
            conn.setAutoCommit(false);           // Iniciar transacción (deshabilitar autocommit)

            // ── Paso 1: Insertar el producto ──
            // RETURN_GENERATED_KEYS: permite recuperar el id_producto generado por AUTO_INCREMENT
            PreparedStatement psProd = conn.prepareStatement(sqlProd, Statement.RETURN_GENERATED_KEYS);
            psProd.setString(1, producto.getNombre());
            psProd.setString(2, producto.getDescripcion());
            psProd.setBigDecimal(3, producto.getPrecio());
            psProd.setInt(4, producto.getIdCategoria());
            psProd.executeUpdate(); // Ejecutar el INSERT

            // Recuperar el id_producto generado automáticamente por MySQL (AUTO_INCREMENT)
            ResultSet rs = psProd.getGeneratedKeys();
            int idProd = 0;
            if (rs.next()) {
                idProd = rs.getInt(1); // Columna 1 del ResultSet de getGeneratedKeys() = el ID
            }

            // ── Paso 2: Insertar la relación vendedor-producto ──
            PreparedStatement psVP = conn.prepareStatement(sqlVP);
            psVP.setInt(1, idVendedor); // FK al vendedor que crea el producto
            psVP.setInt(2, idProd);     // FK al producto recién creado (id generado en Paso 1)
            psVP.setInt(3, producto.getStock()); // Stock inicial del vendedor
            psVP.executeUpdate();

            // ── Paso 3: Insertar la imagen principal (solo si se subió una) ──
            if (rutaImagen != null && !rutaImagen.isEmpty()) {
                PreparedStatement psImg = conn.prepareStatement(sqlImg);
                psImg.setInt(1, idProd);        // FK al producto
                psImg.setString(2, rutaImagen); // Ruta relativa: "uploads/uuid_foto.jpg"
                psImg.executeUpdate();
                psImg.close(); // Cerrar manualmente (no en try-with-resources)
            }

            conn.commit(); // COMMIT: confirmar todos los cambios si todos los pasos fueron exitosos
            return true;

        } catch (SQLException e) {
            // ROLLBACK: si cualquier paso falló, deshacer TODOS los cambios
            // Esto garantiza que nunca queden datos parciales (producto sin vendedor o sin imagen)
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            }
            e.printStackTrace();
            return false;

        } finally {
            // SIEMPRE restaurar autocommit y cerrar la conexión al terminar (éxito o error)
            if (conn != null) {
                try {
                    conn.setAutoCommit(true); // Restaurar para que otras operaciones no estén en transacción
                    conn.close();             // Devolver al pool de conexiones
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Actualiza un producto existente (editado por el vendedor).
     * Al editar, el estado SIEMPRE vuelve a 'PENDIENTE' para nueva revisión del admin.
     * Si se subió nueva imagen, actualiza o inserta en producto_imagenes.
     *
     * También es una TRANSACCIÓN (3 operaciones atómicas):
     *   1. UPDATE productos      → nombre, descripcion, precio, categoria, estado='PENDIENTE'
     *   2. UPDATE vendedor_producto → stock_local del vendedor
     *   3. UPDATE/INSERT producto_imagenes → si se subió nueva imagen
     *
     * Llamada por: ProductoVendedorServlet.doPost() cuando action="actualizar"
     *
     * @param producto    Objeto Producto con todos sus campos actualizados (incluyendo idProducto e idVendedor)
     * @param rutaImagen  Ruta de la nueva imagen o null si no se cambió la imagen
     * @return true si la transacción fue exitosa; false si hubo error
     */
    public boolean actualizarProducto(Producto producto, String rutaImagen) {
        // Query 1: Actualizar datos básicos del producto; estado vuelve a PENDIENTE
        String sqlProd = "UPDATE productos SET nombre = ?, descripcion = ?, precio = ?, " +
                         "id_categoria = ?, estado = 'PENDIENTE' WHERE id_producto = ?";

        // Query 2: Actualizar el stock del vendedor en la tabla de relación
        String sqlVP = "UPDATE vendedor_producto SET stock_local = ? WHERE id_producto = ? AND id_vendedor = ?";

        // Query 3a: Verificar si ya existe una imagen para este producto
        String sqlImgCheck = "SELECT count(*) FROM producto_imagenes WHERE id_producto = ?";

        // Query 3b: Si ya existe imagen → actualizarla (UPDATE)
        String sqlImgUpdate = "UPDATE producto_imagenes SET ruta = ? WHERE id_producto = ? AND es_principal = 1";

        // Query 3c: Si NO existe imagen → crear una nueva (INSERT)
        String sqlImgInsert = "INSERT INTO producto_imagenes (id_producto, ruta, es_principal) VALUES (?, ?, 1)";

        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false); // Iniciar transacción

            // ── Paso 1: Actualizar datos del producto ──
            try (PreparedStatement psProd = conn.prepareStatement(sqlProd)) {
                psProd.setString(1, producto.getNombre());
                psProd.setString(2, producto.getDescripcion());
                psProd.setBigDecimal(3, producto.getPrecio());
                psProd.setInt(4, producto.getIdCategoria());
                psProd.setInt(5, producto.getIdProducto()); // PK del producto a actualizar
                psProd.executeUpdate();
            }

            // ── Paso 2: Actualizar stock del vendedor ──
            try (PreparedStatement psVP = conn.prepareStatement(sqlVP)) {
                psVP.setInt(1, producto.getStock());       // Nuevo stock
                psVP.setInt(2, producto.getIdProducto()); // PK del producto
                psVP.setInt(3, producto.getIdVendedor()); // FK del vendedor (para el AND en WHERE)
                psVP.executeUpdate();
            }

            // ── Paso 3: Manejar la imagen (solo si se subió una nueva) ──
            if (rutaImagen != null && !rutaImagen.isEmpty()) {
                boolean hasImage = false;

                // Verificar si ya existe al menos una imagen para este producto
                try (PreparedStatement psCheck = conn.prepareStatement(sqlImgCheck)) {
                    psCheck.setInt(1, producto.getIdProducto());
                    try (ResultSet rs = psCheck.executeQuery()) {
                        // rs.getInt(1) = COUNT(*) de imágenes existentes
                        if (rs.next() && rs.getInt(1) > 0) hasImage = true;
                    }
                }

                if (hasImage) {
                    // Ya existe una imagen → UPDATE (reemplazar la imagen principal)
                    try (PreparedStatement psUpd = conn.prepareStatement(sqlImgUpdate)) {
                        psUpd.setString(1, rutaImagen);              // Nueva ruta
                        psUpd.setInt(2, producto.getIdProducto());   // FK del producto
                        psUpd.executeUpdate();
                    }
                } else {
                    // No existe imagen aún → INSERT (primera imagen del producto)
                    try (PreparedStatement psIns = conn.prepareStatement(sqlImgInsert)) {
                        psIns.setInt(1, producto.getIdProducto()); // FK del producto
                        psIns.setString(2, rutaImagen);            // Ruta de la imagen
                        psIns.executeUpdate();
                    }
                }
            }
            // Si rutaImagen es null → no se tocó la imagen (se mantiene la existente)

            conn.commit(); // COMMIT: confirmar todos los cambios
            return true;

        } catch (SQLException e) {
            // ROLLBACK si algo falló
            if (conn != null) try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            e.printStackTrace();
            return false;
        } finally {
            // Restaurar autocommit y devolver la conexión al pool
            if (conn != null) try { conn.setAutoCommit(true); conn.close(); } catch (SQLException e) { e.printStackTrace(); }
        }
    }

    /**
     * Obtiene todos los productos con estado PENDIENTE para que el admin los revise.
     * Incluye el id_vendedor para que la vista de revisión muestre quién lo publicó.
     *
     * Llamada por: AdminDashboardServlet.doGet()
     *
     * @return Lista de productos pendientes de revisión (puede ser vacía)
     */
    public List<Producto> obtenerProductosPendientes() {
        List<Producto> lista = new ArrayList<>();

        // Similar a obtenerProductosAprobados() pero filtra por estado='PENDIENTE'
        // Incluye id_vendedor (no en el catálogo público pero sí para el admin)
        String sql = "SELECT p.*, vp.id_vendedor, COALESCE(vp.stock_local, 0) AS stock_real, " +
                     "(SELECT ruta FROM producto_imagenes pi WHERE pi.id_producto = p.id_producto AND pi.es_principal = 1 LIMIT 1) AS imagen_principal " +
                     "FROM productos p " +
                     "LEFT JOIN vendedor_producto vp ON p.id_producto = vp.id_producto " +
                     "WHERE p.estado = 'PENDIENTE'"; // Filtrar solo los que necesitan revisión

        try (Connection       conn = DBConnection.getConnection();
             PreparedStatement ps  = conn.prepareStatement(sql);
             ResultSet         rs  = ps.executeQuery()) {

            while (rs.next()) {
                lista.add(mapResultSetToProducto(rs));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return lista;
    }

    /**
     * Actualiza el estado de un producto a cualquier estado válido.
     * Método genérico usado para aprobar, rechazar, suspender o eliminar.
     *
     * Llamada por:
     *   - RevisionProductoServlet.doPost() → "APROBADO" o "RECHAZADO"
     *   - AdminProductosServlet.doPost()   → "SUSPENDIDO" o "ELIMINADO"
     *
     * @param idProducto  ID del producto a actualizar
     * @param nuevoEstado Nuevo estado: "APROBADO", "RECHAZADO", "SUSPENDIDO" o "ELIMINADO"
     * @return true si el UPDATE afectó 1 fila; false si no encontró el producto o hubo error
     */
    public boolean actualizarEstado(int idProducto, String nuevoEstado) {
        // Simple UPDATE de solo la columna estado
        String sql = "UPDATE productos SET estado = ? WHERE id_producto = ?";

        try (Connection       conn = DBConnection.getConnection();
             PreparedStatement ps  = conn.prepareStatement(sql)) {

            ps.setString(1, nuevoEstado); // Nuevo estado
            ps.setInt(2, idProducto);     // PK del producto a actualizar

            int rows = ps.executeUpdate(); // Número de filas afectadas
            return rows > 0;              // true si se actualizó correctamente

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Busca productos APROBADOS cuyo nombre contenga el texto de búsqueda.
     * Usa LIKE con wildcards (%) para búsqueda parcial (no requiere coincidencia exacta).
     *
     * Llamada por: CatalogoServlet.doGet() cuando el usuario escribe en el buscador
     *
     * @param query Texto a buscar (ej: "sombrero" → busca productos con "sombrero" en el nombre)
     * @return Lista de productos aprobados cuyo nombre contiene el texto buscado
     */
    public List<Producto> buscarProductosPorNombre(String query) {
        List<Producto> lista = new ArrayList<>();

        // LIKE ? con el parámetro "%" + query + "%" → búsqueda parcial
        // La concatenación se hace en setString (no en el SQL) para evitar SQL injection
        String sql = "SELECT p.*, COALESCE(SUM(vp.stock_local), 0) AS stock_real, " +
                     "(SELECT ruta FROM producto_imagenes pi WHERE pi.id_producto = p.id_producto AND pi.es_principal = 1 LIMIT 1) AS imagen_principal " +
                     "FROM productos p " +
                     "LEFT JOIN vendedor_producto vp ON p.id_producto = vp.id_producto " +
                     "WHERE p.estado = 'APROBADO' AND p.nombre LIKE ? GROUP BY p.id_producto";

        try (Connection       conn = DBConnection.getConnection();
             PreparedStatement ps  = conn.prepareStatement(sql)) {

            // "%" es el wildcard SQL: "%" + query + "%" busca el texto en CUALQUIER posición del nombre
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

    /**
     * Método PRIVADO de mapeo: convierte una fila del ResultSet en un objeto Producto.
     * Es el punto central de mapeo; todos los métodos de lectura lo llaman.
     * Maneja de forma robusta la ausencia de columnas opcionales (imagenPrincipal, id_vendedor)
     * usando bloques try-catch individuales para evitar que la ausencia de un campo opcional
     * provoque un error en todo el mapeo.
     *
     * Columnas que siempre estarán presentes en el ResultSet:
     *   id_producto, nombre, descripcion, precio, id_categoria, estado, vistas, fechas
     *
     * Columnas opcionales (presentes solo en algunos queries):
     *   imagen_principal → subconsulta en la mayoría de queries
     *   id_vendedor → presente en queries con JOIN a vendedor_producto
     *   stock_real → alias COALESCE(SUM(stock_local)) o COALESCE(stock_local)
     *   stock → columna directa de la tabla productos (fallback si stock_real no existe)
     *
     * @param rs ResultSet posicionado en la fila a leer (rs.next() ya fue llamado)
     * @return Objeto Producto con todos los campos disponibles llenos
     * @throws SQLException si hay error al leer una columna obligatoria
     */
    private Producto mapResultSetToProducto(ResultSet rs) throws SQLException {
        Producto p = new Producto(); // Crear instancia vacía

        // Columnas siempre presentes (leer sin try-catch individual; si fallan es un error real)
        p.setIdProducto(rs.getInt("id_producto"));         // PK de la tabla productos
        p.setNombre(rs.getString("nombre"));               // Nombre del producto
        p.setDescripcion(rs.getString("descripcion"));     // Descripción del producto
        p.setPrecio(rs.getBigDecimal("precio"));           // Precio DECIMAL de la BD
        p.setIdCategoria(rs.getInt("id_categoria"));       // FK a categorias
        p.setEstado(rs.getString("estado"));               // Estado del producto
        p.setVistas(rs.getInt("vistas"));                  // Contador de vistas
        p.setFechaCreacion(rs.getTimestamp("fecha_creacion"));           // Fecha de creación
        p.setFechaActualizacion(rs.getTimestamp("fecha_actualizacion")); // Última actualización

        // ── Stock: columna opcional con fallback ──
        // Intentar leer "stock_real" (alias del COALESCE(SUM/stock_local))
        // Si no existe (la query no lo incluye), intentar leer "stock" como columna directa
        try {
            p.setStock(rs.getInt("stock_real")); // Alias del JOIN con vendedor_producto
        } catch (SQLException e1) {
            try { p.setStock(rs.getInt("stock")); } catch (SQLException e2) {
                /* Si tampoco existe "stock", el stock queda en 0 (valor por defecto del int) */
            }
        }

        // ── Imagen principal: columna opcional (subconsulta no presente en todos los queries) ──
        // Si la query no incluyó la subconsulta de imagen, este bloque simplemente la ignora
        try { p.setImagenPrincipal(rs.getString("imagen_principal")); } catch (SQLException e) {}

        // ── ID del vendedor: columna opcional (presente solo en queries con JOIN a vendedor_producto) ──
        // Necesario para verificar en ProductoVendedorServlet que el vendedor es el dueño
        try {
            p.setIdVendedor(rs.getInt("id_vendedor")); // FK a usuarios.id_usuario del vendedor
        } catch (SQLException e) {
            // Si no hay id_vendedor en el ResultSet, idVendedor queda en 0 (valor por defecto)
        }

        return p;
    }

    /**
     * Verifica si un producto tiene al menos una compra registrada en la BD.
     * Usada por AdminProductosServlet antes de intentar eliminar un producto.
     *
     * Si tiene compras, el producto NO se puede eliminar (los datos históricos son importantes).
     * El admin debe suspenderlo en lugar de eliminarlo.
     *
     * @param idProducto ID del producto a verificar
     * @return true si COUNT(*) en orden_detalle es > 0; false si no tiene compras registradas
     */
    public boolean tieneCompras(int idProducto) {
        // COUNT(*) cuenta las líneas en orden_detalle que referencian a este producto
        String sql = "SELECT COUNT(*) FROM orden_detalle WHERE id_producto = ?";

        try (Connection       conn = DBConnection.getConnection();
             PreparedStatement ps  = conn.prepareStatement(sql)) {

            ps.setInt(1, idProducto); // FK del producto a verificar

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0; // true si tiene al menos 1 compra
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Obtiene todos los productos para el panel de administración, con filtros opcionales.
     * Construye una query dinámica con hasta 3 filtros opcionales (estado, vendedor, fecha).
     *
     * Patrón de query dinámica con StringBuilder:
     * La base del SQL siempre se incluye. Los WHERE adicionales se agregan si el parámetro
     * correspondiente no es null. Se lleva un índice de parámetros (paramIndex) para mapear
     * correctamente los ? al valor correcto con ps.setXxx(paramIndex++, valor).
     *
     * Incluye SIEMPRE: p.estado != 'ELIMINADO' (borrado lógico; los eliminados no se ven)
     *
     * Llamada por: AdminProductosServlet.doGet()
     *
     * @param estadoFiltro    Filtrar por estado ("APROBADO", "PENDIENTE", etc.) o null para todos
     * @param fechaFiltro     Filtrar por fecha de creación (formato "YYYY-MM-DD") o null para todas
     * @param idVendedorFiltro Filtrar por vendedor específico (ID) o null para todos los vendedores
     * @return Lista de productos del sistema que cumplen los filtros, ordenados por fecha desc
     */
    public List<Producto> obtenerTodosAdmin(String estadoFiltro, String fechaFiltro, Integer idVendedorFiltro) {
        List<Producto> lista = new ArrayList<>();

        // ── Base de la query: siempre se incluye ──
        // COALESCE(SUM(vp.stock_local)) da el stock total del producto entre todos sus vendedores
        // Se excluyen los ELIMINADOS (borrado lógico; no deben aparecer en el listado admin)
        StringBuilder sql = new StringBuilder(
            "SELECT p.*, COALESCE(SUM(vp.stock_local), 0) AS stock_real, " +
            "(SELECT ruta FROM producto_imagenes pi WHERE pi.id_producto = p.id_producto AND pi.es_principal = 1 LIMIT 1) AS imagen_principal "
        );
        sql.append("FROM productos p ");
        sql.append("LEFT JOIN vendedor_producto vp ON p.id_producto = vp.id_producto "); // JOIN para stock y filtro de vendedor
        sql.append("WHERE p.estado != 'ELIMINADO' "); // Excluir borrados lógicos siempre

        // ── Añadir filtros opcionales si fueron proporcionados ──

        if (estadoFiltro != null && !estadoFiltro.isEmpty()) {
            // Filtrar por estado específico (PENDIENTE, APROBADO, RECHAZADO, SUSPENDIDO)
            sql.append("AND p.estado = ? ");
        }
        if (idVendedorFiltro != null && idVendedorFiltro > 0) {
            // Filtrar el JOIN al vendedor elegido; SUM() solo suma su stock_local
            // Esto muestra solo los productos de ese vendedor en el panel de admin
            sql.append("AND vp.id_vendedor = ? ");
        }
        if (fechaFiltro != null && !fechaFiltro.isEmpty()) {
            // Filtrar por fecha de creación usando DATE() para comparar solo la parte de fecha
            // (ignora la hora en la columna TIMESTAMP fecha_creacion)
            sql.append("AND DATE(p.fecha_creacion) = ? ");
        }

        // GROUP BY obligatorio al usar SUM() (función de agregación)
        sql.append("GROUP BY p.id_producto ");

        // Ordenar por más recientes primero
        sql.append("ORDER BY p.fecha_creacion DESC");

        try (Connection       conn = DBConnection.getConnection();
             PreparedStatement ps  = conn.prepareStatement(sql.toString())) {

            // Índice de parámetro que se incrementa dinámicamente según los filtros activos
            int paramIndex = 1;

            // Establecer cada parámetro ? en el mismo orden en que se añadieron al SQL
            if (estadoFiltro != null && !estadoFiltro.isEmpty()) {
                ps.setString(paramIndex++, estadoFiltro); // Parámetro de estado
            }
            if (idVendedorFiltro != null && idVendedorFiltro > 0) {
                ps.setInt(paramIndex++, idVendedorFiltro); // Parámetro de id del vendedor
            }
            if (fechaFiltro != null && !fechaFiltro.isEmpty()) {
                ps.setString(paramIndex++, fechaFiltro); // Parámetro de fecha (formato YYYY-MM-DD)
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
