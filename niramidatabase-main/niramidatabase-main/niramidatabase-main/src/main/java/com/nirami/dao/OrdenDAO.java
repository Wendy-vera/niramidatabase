package com.nirami.dao;

import com.nirami.model.CartItem;
import com.nirami.model.VentaAdminDTO;
import com.nirami.util.DBConnection;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import com.nirami.model.Orden;

/**
 * DAO (Data Access Object) que gestiona todas las operaciones relacionadas con
 * órdenes de compra, detalles de orden y pagos a vendedores.
 *
 * <p>Este DAO interactúa con las tablas:</p>
 * <ul>
 *   <li>{@code ordenes} — Cabecera de cada orden de compra</li>
 *   <li>{@code orden_detalle} — Líneas de producto dentro de una orden</li>
 *   <li>{@code pagos_vendedor} — Registro de pagos pendientes/completados para cada vendedor</li>
 *   <li>{@code vendedor_producto} — Inventario local de cada vendedor (stock)</li>
 * </ul>
 *
 * <p><b>Conexión a BD:</b> Usa {@link com.nirami.util.DBConnection#getConnection()}.
 * Las operaciones que modifican múltiples tablas usan transacciones explícitas
 * ({@code setAutoCommit(false)} + {@code commit()} / {@code rollback()}).</p>
 *
 * <p><b>Servlets que usan este DAO:</b>
 * <ul>
 *   <li>{@link com.nirami.controller.CheckoutServlet} — {@code crearOrden()}</li>
 *   <li>{@link com.nirami.controller.AdminVentasServlet} — {@code actualizarEstadoPago()}, {@code obtenerVentasAdmin()}</li>
 *   <li>{@link com.nirami.controller.AdminPagosVendedorServlet} — {@code obtenerResumenPagosPorVendedor()}, {@code ejecutarPagoVendedor()}</li>
 *   <li>{@link com.nirami.controller.VendedorPagosServlet} — {@code obtenerPagosPorVendedor()}</li>
 *   <li>{@link com.nirami.controller.VendedorVentasServlet} — {@code obtenerVentasAdmin()}</li>
 * </ul>
 * </p>
 *
 * @author Nirami Dev Team
 * @version 1.1
 * @see com.nirami.model.Orden
 * @see com.nirami.model.OrdenDetalle
 * @see com.nirami.model.VentaAdminDTO
 */
public class OrdenDAO {

    /**
     * Crea una nueva orden de compra de forma transaccional.
     *
     * <p>Este método realiza las siguientes operaciones en una única transacción ACID:</p>
     * <ol>
     *   <li>Inserta un registro en {@code ordenes} con estado {@code PENDIENTE}</li>
     *   <li>Para cada ítem del carrito:
     *     <ul>
     *       <li>Busca el vendedor responsable del producto en {@code vendedor_producto}</li>
     *       <li>Reduce el {@code stock_local} del vendedor (con verificación de stock suficiente)</li>
     *       <li>Inserta el detalle en {@code orden_detalle}</li>
     *       <li>Crea un registro en {@code pagos_vendedor} con estado {@code PENDIENTE}
     *           y comisión 0.00 (el admin la puede ajustar después)</li>
     *     </ul>
     *   </li>
     * </ol>
     *
     * <p><b>Rollback automático:</b> Si el stock es insuficiente para algún ítem,
     * o si ocurre cualquier error SQL, se hace rollback completo de la transacción.</p>
     *
     * @param idCliente ID del cliente comprador (FK a {@code clientes.id_usuario})
     * @param subtotal  Suma de subtotales de todos los ítems, sin IVA
     * @param iva       Monto del IVA calculado (19% del subtotal)
     * @param total     Total final (subtotal + iva); debe cumplir la restricción {@code chk_total} de la BD
     * @param direccion Dirección de envío ingresada por el cliente en el checkout
     * @param carrito   Lista de {@link CartItem} del carrito de compras de la sesión
     * @return {@code true} si la transacción fue exitosa; {@code false} si hubo error o stock insuficiente
     */
    public boolean crearOrden(int idCliente, BigDecimal subtotal, BigDecimal iva, BigDecimal total,
                               String direccion, List<CartItem> carrito) {
        String sqlOrden        = "INSERT INTO ordenes (id_cliente, subtotal, iva, total, direccion, estado_pago) VALUES (?, ?, ?, ?, ?, 'PENDIENTE')";
        String sqlDetalle      = "INSERT INTO orden_detalle (id_orden, id_producto, id_vendedor, cantidad, precio_unitario, subtotal_linea) VALUES (?, ?, ?, ?, ?, ?)";
        String sqlFindVendedor = "SELECT id_vendedor FROM vendedor_producto WHERE id_producto = ? LIMIT 1";
        // Reduce stock del vendedor; si stock_local < cantidad, la cláusula AND evita que se ejecute (filasAfectadas = 0)
        String sqlReduceStock  = "UPDATE vendedor_producto SET stock_local = stock_local - ? WHERE id_producto = ? AND id_vendedor = ? AND stock_local >= ?";
        // Crea el pago pendiente; comisión 0 → monto_neto = monto (el admin puede ajustar después)
        String sqlPago         = "INSERT INTO pagos_vendedor (id_vendedor, id_detalle, monto, porcentaje_comision, monto_comision, monto_neto, estado) VALUES (?, ?, ?, 0.00, 0.00, ?, 'PENDIENTE')";

        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false); // ── Inicio de transacción ──

            // 1. Insertar la cabecera de la orden
            PreparedStatement psOrden = conn.prepareStatement(sqlOrden, Statement.RETURN_GENERATED_KEYS);
            psOrden.setInt(1, idCliente);
            psOrden.setBigDecimal(2, subtotal);
            psOrden.setBigDecimal(3, iva);
            psOrden.setBigDecimal(4, total);
            psOrden.setString(5, direccion);
            psOrden.executeUpdate();

            // Recuperar el ID de la orden recién creada
            ResultSet rs = psOrden.getGeneratedKeys();
            int idOrden = 0;
            if (rs.next()) {
                idOrden = rs.getInt(1);
            }

            // 2. Procesar cada línea del carrito
            PreparedStatement psDetalle     = conn.prepareStatement(sqlDetalle, Statement.RETURN_GENERATED_KEYS);
            PreparedStatement psFindVendedor = conn.prepareStatement(sqlFindVendedor);
            PreparedStatement psReduceStock  = conn.prepareStatement(sqlReduceStock);
            PreparedStatement psPago         = conn.prepareStatement(sqlPago);

            for (CartItem item : carrito) {
                // 2a. Encontrar al vendedor responsable de este producto
                psFindVendedor.setInt(1, item.getProducto().getIdProducto());
                ResultSet rsVendedor = psFindVendedor.executeQuery();
                int idVendedor = 0;
                if (rsVendedor.next()) {
                    idVendedor = rsVendedor.getInt("id_vendedor");
                }
                rsVendedor.close();

                // Si no hay vendedor asignado, el producto no puede venderse → rollback
                if (idVendedor == 0) {
                    conn.rollback();
                    return false;
                }

                // 2b. Reducir stock_local; si stock insuficiente, filasAfectadas = 0 → rollback
                psReduceStock.setInt(1, item.getCantidad());
                psReduceStock.setInt(2, item.getProducto().getIdProducto());
                psReduceStock.setInt(3, idVendedor);
                psReduceStock.setInt(4, item.getCantidad());
                int filasAfectadas = psReduceStock.executeUpdate();
                if (filasAfectadas == 0) {
                    conn.rollback();
                    return false;
                }

                // 2c. Insertar la línea de detalle
                psDetalle.setInt(1, idOrden);
                psDetalle.setInt(2, item.getProducto().getIdProducto());
                psDetalle.setInt(3, idVendedor);
                psDetalle.setInt(4, item.getCantidad());
                psDetalle.setBigDecimal(5, item.getProducto().getPrecio());
                psDetalle.setBigDecimal(6, item.getSubtotal());
                psDetalle.executeUpdate();

                // Obtener el ID del detalle recién insertado (necesario para la FK de pagos_vendedor)
                ResultSet rsDetalle = psDetalle.getGeneratedKeys();
                int idDetalle = 0;
                if (rsDetalle.next()) {
                    idDetalle = rsDetalle.getInt(1);
                }
                rsDetalle.close();

                // 2d. Crear registro de pago pendiente para el vendedor
                psPago.setInt(1, idVendedor);
                psPago.setInt(2, idDetalle);
                psPago.setBigDecimal(3, item.getSubtotal());  // monto = subtotal_linea
                psPago.setBigDecimal(4, item.getSubtotal());  // monto_neto = monto (comisión 0)
                psPago.executeUpdate();
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
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (SQLException e) { e.printStackTrace(); }
            }
        }
    }

    /**
     * Actualiza el estado de pago de una orden completa (aprobación o rechazo).
     *
     * <p>Operación transaccional que ejecuta en paralelo:</p>
     * <ul>
     *   <li>Actualiza {@code ordenes.estado_pago} y {@code ordenes.estado_orden}</li>
     *   <li>Actualiza {@code pagos_vendedor.estado} y registra {@code fecha_pago} si se aprueba</li>
     *   <li>Si se rechaza: restaura el stock en {@code vendedor_producto} para todos los ítems de la orden</li>
     * </ul>
     *
     * <p><b>Reglas de negocio:</b>
     * <ul>
     *   <li>APROBADO → {@code estado_pago = APROBADO}, {@code estado_orden = COMPLETADA},
     *       {@code pagos_vendedor.estado = PAGADO}, {@code fecha_pago = NOW()}</li>
     *   <li>RECHAZADO → {@code estado_pago = RECHAZADO}, {@code estado_orden = CANCELADA},
     *       {@code pagos_vendedor.estado = PENDIENTE} (sin fecha_pago), stock restaurado</li>
     * </ul>
     * </p>
     *
     * @param idOrden      ID de la orden a actualizar
     * @param nuevoEstado  {@code "APROBADO"} o {@code "RECHAZADO"}
     * @return {@code true} si la transacción fue exitosa; {@code false} en caso de error SQL
     */
    public boolean actualizarEstadoPago(int idOrden, String nuevoEstado) {
        String sqlOrdenUpdate  = "UPDATE ordenes SET estado_pago = ?, estado_orden = ? WHERE id_orden = ?";
        // JOIN implícito para actualizar todos los pagos de los detalles de esta orden
        String sqlPagoUpdate   = "UPDATE pagos_vendedor pv " +
                                 "JOIN orden_detalle od ON pv.id_detalle = od.id_detalle " +
                                 "SET pv.estado = ?, pv.fecha_pago = IF(? = 'APROBADO', NOW(), NULL) " +
                                 "WHERE od.id_orden = ?";
        // Restaurar stock solo si se rechaza (devolver unidades al vendedor)
        String sqlRestoreStock = "UPDATE vendedor_producto vp " +
                                 "JOIN orden_detalle od ON vp.id_vendedor = od.id_vendedor AND vp.id_producto = od.id_producto " +
                                 "SET vp.stock_local = vp.stock_local + od.cantidad " +
                                 "WHERE od.id_orden = ?";

        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);

            // Determinar los estados resultantes según la acción
            String estadoOrden = "APROBADO".equals(nuevoEstado) ? "COMPLETADA" : "CANCELADA";
            String estadoPago  = "APROBADO".equals(nuevoEstado) ? "PAGADO"     : "PENDIENTE";

            // 1. Actualizar la cabecera de la orden
            try (PreparedStatement ps = conn.prepareStatement(sqlOrdenUpdate)) {
                ps.setString(1, nuevoEstado);
                ps.setString(2, estadoOrden);
                ps.setInt(3, idOrden);
                ps.executeUpdate();
            }

            // 2. Actualizar los pagos a vendedores asociados a esta orden
            try (PreparedStatement ps = conn.prepareStatement(sqlPagoUpdate)) {
                ps.setString(1, estadoPago);
                ps.setString(2, nuevoEstado);
                ps.setInt(3, idOrden);
                ps.executeUpdate();
            }

            // 3. Si se rechaza, devolver el stock a los vendedores
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

    /**
     * Obtiene todas las órdenes de un cliente específico, incluyendo sus detalles.
     *
     * <p>Cada {@link Orden} en la lista incluye su lista de {@link com.nirami.model.OrdenDetalle}
     * (obtenida mediante {@link #obtenerDetallesPorOrden(int, Connection)}).</p>
     *
     * @param idCliente ID del cliente cuyas órdenes se desean obtener
     * @return Lista de {@link Orden} ordenadas por fecha descendente; lista vacía si no hay órdenes
     */
    public List<Orden> obtenerOrdenesPorCliente(int idCliente) {
        List<Orden> ordenes = new ArrayList<>();
        String sql = "SELECT * FROM ordenes WHERE id_cliente = ? ORDER BY fecha_orden DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idCliente);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Orden orden = mapearOrden(rs);
                    orden.setDetalles(obtenerDetallesPorOrden(orden.getIdOrden(), conn));
                    ordenes.add(orden);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return ordenes;
    }

    /**
     * Obtiene una única orden por su ID primario, incluyendo sus detalles.
     *
     * @param idOrden ID de la orden a buscar
     * @return Objeto {@link Orden} con sus detalles, o {@code null} si no existe
     */
    public Orden obtenerOrdenPorId(int idOrden) {
        String sql = "SELECT * FROM ordenes WHERE id_orden = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idOrden);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Orden orden = mapearOrden(rs);
                    orden.setDetalles(obtenerDetallesPorOrden(orden.getIdOrden(), conn));
                    return orden;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Obtiene ventas con filtros opcionales para las vistas de administrador y vendedor.
     *
     * <p>Construye dinámicamente la cláusula WHERE según los filtros proporcionados.
     * Cualquier filtro puede ser {@code null} para omitirlo.</p>
     *
     * <p>Este método es usado por:
     * <ul>
     *   <li>{@link com.nirami.controller.AdminVentasServlet} — con cualquier combinación de filtros</li>
     *   <li>{@link com.nirami.controller.VendedorVentasServlet} — siempre con {@code idVendedor} fijo</li>
     *   <li>{@link com.nirami.controller.VendedorPagosServlet} — siempre con {@code idVendedor} fijo</li>
     * </ul>
     * </p>
     *
     * <p>Los datos del pago al vendedor ({@code pagos_vendedor}) se incluyen via LEFT JOIN,
     * lo que permite ver el estado del pago individual por línea de detalle.</p>
     *
     * @param idProducto Filtrar por producto (FK a {@code productos}); {@code null} = todos
     * @param idVendedor Filtrar por vendedor (FK a {@code vendedores}); {@code null} = todos
     * @param idCliente  Filtrar por cliente (FK a {@code clientes}); {@code null} = todos
     * @return Lista de {@link VentaAdminDTO} con datos agregados de ventas y pagos,
     *         ordenada por fecha de orden descendente.
     */
    public List<VentaAdminDTO> obtenerVentasAdmin(Integer idProducto, Integer idVendedor, Integer idCliente) {
        List<VentaAdminDTO> ventas = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
            "SELECT o.id_orden, o.fecha_orden, o.estado_pago, " +
            "c.nombre AS cliente, p.nombre AS producto, " +
            "v.nombre AS vendedor, od.id_vendedor, " +
            "od.id_detalle, od.cantidad, od.precio_unitario, od.subtotal_linea, " +
            "pv.id_pago, pv.monto_neto, pv.estado AS estado_pago_vendedor, " +
            "vend.cuenta_bancaria " +
            "FROM orden_detalle od " +
            "JOIN ordenes o ON od.id_orden = o.id_orden " +
            "JOIN usuarios c ON o.id_cliente = c.id_usuario " +
            "JOIN productos p ON od.id_producto = p.id_producto " +
            "JOIN usuarios v ON od.id_vendedor = v.id_usuario " +
            "JOIN vendedores vend ON od.id_vendedor = vend.id_usuario " +
            "LEFT JOIN pagos_vendedor pv ON pv.id_detalle = od.id_detalle " +
            "WHERE 1=1 "
        );

        // Añadir cláusulas WHERE dinámicamente según los filtros activos
        if (idProducto != null && idProducto > 0) sql.append("AND od.id_producto = ? ");
        if (idVendedor != null && idVendedor > 0) sql.append("AND od.id_vendedor = ? ");
        if (idCliente  != null && idCliente  > 0) sql.append("AND o.id_cliente = ? ");

        sql.append("ORDER BY o.fecha_orden DESC");

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            // Asignar parámetros en el mismo orden en que se añadieron al WHERE
            int paramIndex = 1;
            if (idProducto != null && idProducto > 0) ps.setInt(paramIndex++, idProducto);
            if (idVendedor != null && idVendedor > 0) ps.setInt(paramIndex++, idVendedor);
            if (idCliente  != null && idCliente  > 0) ps.setInt(paramIndex++, idCliente);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    VentaAdminDTO venta = new VentaAdminDTO();
                    venta.setIdOrden(rs.getInt("id_orden"));
                    venta.setIdDetalle(rs.getInt("id_detalle"));
                    venta.setFechaOrden(rs.getTimestamp("fecha_orden"));
                    venta.setEstadoPago(rs.getString("estado_pago"));
                    venta.setNombreCliente(rs.getString("cliente"));
                    venta.setNombreProducto(rs.getString("producto"));
                    venta.setNombreVendedor(rs.getString("vendedor"));
                    venta.setIdVendedor(rs.getInt("id_vendedor"));
                    venta.setCantidad(rs.getInt("cantidad"));
                    venta.setPrecioUnitario(rs.getBigDecimal("precio_unitario"));
                    venta.setSubtotalLinea(rs.getBigDecimal("subtotal_linea"));
                    venta.setIdPago(rs.getInt("id_pago"));
                    venta.setMontoNeto(rs.getBigDecimal("monto_neto"));
                    venta.setEstadoPagoVendedor(rs.getString("estado_pago_vendedor"));
                    venta.setCuentaBancaria(rs.getString("cuenta_bancaria"));
                    ventas.add(venta);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return ventas;
    }

    /**
     * Obtiene un resumen de pagos pendientes agrupado por vendedor.
     *
     * <p>Esta consulta es usada por la vista de administrador de pagos
     * ({@code admin_pagos_vendedores.jsp}) para mostrar una fila por vendedor
     * con el monto total que se le debe pagar.</p>
     *
     * <p>Solo incluye vendedores que tienen pagos con estado {@code PENDIENTE}.</p>
     *
     * <p><b>SQL agrupado:</b> GROUP BY id_vendedor, con SUM del monto_neto pendiente.</p>
     *
     * @return Lista de {@link VentaAdminDTO} donde cada elemento representa un vendedor
     *         con su monto total pendiente ({@code subtotalLinea} = suma de {@code monto_neto}),
     *         cuenta bancaria y nombre. Un elemento por vendedor con pagos pendientes.
     */
    public List<VentaAdminDTO> obtenerResumenPagosPorVendedor() {
        List<VentaAdminDTO> resumen = new ArrayList<>();
        String sql = "SELECT od.id_vendedor, u.nombre AS vendedor, vend.cuenta_bancaria, " +
                     "SUM(pv.monto_neto) AS total_pendiente, COUNT(pv.id_pago) AS num_pagos " +
                     "FROM pagos_vendedor pv " +
                     "JOIN orden_detalle od ON pv.id_detalle = od.id_detalle " +
                     "JOIN usuarios u ON od.id_vendedor = u.id_usuario " +
                     "JOIN vendedores vend ON od.id_vendedor = vend.id_usuario " +
                     "WHERE pv.estado = 'PENDIENTE' " +
                     "GROUP BY od.id_vendedor, u.nombre, vend.cuenta_bancaria " +
                     "ORDER BY total_pendiente DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                VentaAdminDTO dto = new VentaAdminDTO();
                dto.setIdVendedor(rs.getInt("id_vendedor"));
                dto.setNombreVendedor(rs.getString("vendedor"));
                dto.setCuentaBancaria(rs.getString("cuenta_bancaria"));
                dto.setSubtotalLinea(rs.getBigDecimal("total_pendiente")); // monto total pendiente
                dto.setCantidad(rs.getInt("num_pagos")); // cantidad de transacciones pendientes
                resumen.add(dto);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return resumen;
    }

    /**
     * Obtiene el detalle de todos los pagos (pendientes y pagados) de un vendedor específico.
     *
     * <p>Usado por {@link com.nirami.controller.VendedorPagosServlet} para mostrar al
     * vendedor su historial de pagos con el estado correcto ({@code PENDIENTE} o {@code PAGADO}).</p>
     *
     * @param idVendedor ID del vendedor cuyo historial de pagos se desea obtener
     * @return Lista de {@link VentaAdminDTO} con estado de pago del vendedor,
     *         ordenada por fecha descendente.
     */
    public List<VentaAdminDTO> obtenerPagosPorVendedor(int idVendedor) {
        List<VentaAdminDTO> pagos = new ArrayList<>();
        String sql = "SELECT pv.id_pago, pv.monto_neto, pv.estado AS estado_pago_vendedor, " +
                     "pv.fecha_pago, o.id_orden, o.fecha_orden, o.estado_pago, " +
                     "p.nombre AS producto, od.cantidad, od.precio_unitario, od.subtotal_linea, " +
                     "od.id_detalle, od.id_vendedor, u.nombre AS vendedor, vend.cuenta_bancaria " +
                     "FROM pagos_vendedor pv " +
                     "JOIN orden_detalle od ON pv.id_detalle = od.id_detalle " +
                     "JOIN ordenes o ON od.id_orden = o.id_orden " +
                     "JOIN productos p ON od.id_producto = p.id_producto " +
                     "JOIN usuarios u ON od.id_vendedor = u.id_usuario " +
                     "JOIN vendedores vend ON od.id_vendedor = vend.id_usuario " +
                     "WHERE od.id_vendedor = ? " +
                     "ORDER BY o.fecha_orden DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, idVendedor);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    VentaAdminDTO dto = new VentaAdminDTO();
                    dto.setIdPago(rs.getInt("id_pago"));
                    dto.setMontoNeto(rs.getBigDecimal("monto_neto"));
                    dto.setEstadoPagoVendedor(rs.getString("estado_pago_vendedor"));
                    dto.setIdOrden(rs.getInt("id_orden"));
                    dto.setFechaOrden(rs.getTimestamp("fecha_orden"));
                    dto.setEstadoPago(rs.getString("estado_pago"));
                    dto.setNombreProducto(rs.getString("producto"));
                    dto.setCantidad(rs.getInt("cantidad"));
                    dto.setPrecioUnitario(rs.getBigDecimal("precio_unitario"));
                    dto.setSubtotalLinea(rs.getBigDecimal("subtotal_linea"));
                    dto.setIdDetalle(rs.getInt("id_detalle"));
                    dto.setIdVendedor(rs.getInt("id_vendedor"));
                    dto.setNombreVendedor(rs.getString("vendedor"));
                    dto.setCuentaBancaria(rs.getString("cuenta_bancaria"));
                    pagos.add(dto);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return pagos;
    }

    /**
     * Ejecuta el pago a un vendedor: marca todos sus pagos PENDIENTE como PAGADO.
     *
     * <p>Este método es llamado por el administrador al presionar el botón "Pagar"
     * en {@code admin_pagos_vendedores.jsp}. Actualiza TODOS los registros
     * {@code PENDIENTE} del vendedor en {@code pagos_vendedor} de una sola vez,
     * registrando la {@code fecha_pago} actual.</p>
     *
     * <p><b>Precondición:</b> El administrador debe haber verificado la cuenta bancaria
     * antes de ejecutar el pago, ya que este sistema no realiza transferencias reales
     * — solo registra que el pago fue realizado.</p>
     *
     * @param idVendedor ID del vendedor al que se le realizará el pago
     * @return {@code true} si se actualizaron registros; {@code false} si hubo error o no había pendientes
     */
    public boolean ejecutarPagoVendedor(int idVendedor) {
        // Actualiza solo los pagos PENDIENTE del vendedor; registra fecha_pago = NOW()
        String sql = "UPDATE pagos_vendedor pv " +
                     "JOIN orden_detalle od ON pv.id_detalle = od.id_detalle " +
                     "SET pv.estado = 'PAGADO', pv.fecha_pago = NOW() " +
                     "WHERE od.id_vendedor = ? AND pv.estado = 'PENDIENTE'";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, idVendedor);
            int filasAfectadas = ps.executeUpdate();
            // Devuelve true solo si se actualizó al menos un pago
            return filasAfectadas > 0;

        } catch (SQLException e) {
            System.err.println("[OrdenDAO] Error al ejecutar pago para vendedor " + idVendedor);
            e.printStackTrace();
            return false;
        }
    }

    // ─── Métodos privados de apoyo ───────────────────────────────────────────

    /**
     * Obtiene los detalles de una orden usando una conexión existente (reutilización de conexión).
     *
     * <p>Este método es llamado dentro de {@link #obtenerOrdenesPorCliente(int)} y
     * {@link #obtenerOrdenPorId(int)} para evitar abrir una nueva conexión por cada orden
     * (optimización N+1).</p>
     *
     * @param idOrden ID de la orden cuyos detalles se buscan
     * @param conn    Conexión JDBC activa (NO se cierra aquí; el llamador es responsable)
     * @return Lista de {@link com.nirami.model.OrdenDetalle} con datos del producto incluidos
     * @throws SQLException si hay error en la consulta SQL
     */
    private List<com.nirami.model.OrdenDetalle> obtenerDetallesPorOrden(int idOrden, Connection conn) throws SQLException {
        List<com.nirami.model.OrdenDetalle> detalles = new ArrayList<>();
        String sql = "SELECT od.*, p.nombre, p.descripcion, " +
                     "(SELECT ruta FROM producto_imagenes pi " +
                     " WHERE pi.id_producto = p.id_producto AND pi.es_principal = 1 LIMIT 1) AS imagen " +
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

    /**
     * Mapea los campos comunes de una orden desde un {@link ResultSet}.
     *
     * @param rs ResultSet posicionado en la fila actual de la tabla {@code ordenes}
     * @return Objeto {@link Orden} con los campos básicos mapeados (sin detalles)
     * @throws SQLException si hay error al leer las columnas
     */
    private Orden mapearOrden(ResultSet rs) throws SQLException {
        Orden orden = new Orden();
        orden.setIdOrden(rs.getInt("id_orden"));
        orden.setIdCliente(rs.getInt("id_cliente"));
        orden.setSubtotal(rs.getBigDecimal("subtotal"));
        orden.setIva(rs.getBigDecimal("iva"));
        orden.setTotal(rs.getBigDecimal("total"));
        orden.setDireccion(rs.getString("direccion"));
        orden.setEstadoPago(rs.getString("estado_pago"));
        orden.setFechaOrden(rs.getTimestamp("fecha_orden"));
        return orden;
    }
}
