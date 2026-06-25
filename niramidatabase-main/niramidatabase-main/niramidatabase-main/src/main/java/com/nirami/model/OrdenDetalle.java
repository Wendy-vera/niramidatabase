package com.nirami.model;

import java.math.BigDecimal;

/**
 * POJO que representa una línea de detalle dentro de una orden de compra.
 * Mapea una fila de la tabla {@code orden_detalle}.
 *
 * Tabla mapeada: {@code orden_detalle}
 * Columnas: id_detalle (PK), id_orden (FK→ordenes), id_producto (FK→productos),
 *           id_vendedor (FK→usuarios), cantidad, precio_unitario, subtotal_linea
 *
 * Relación con otras tablas:
 *   - {@code orden_detalle.id_orden}    → FK a {@code ordenes} (la orden a la que pertenece)
 *   - {@code orden_detalle.id_producto} → FK a {@code productos} (qué producto se compró)
 *   - {@code orden_detalle.id_vendedor} → FK a {@code usuarios} (qué vendedor vendió)
 *   - Una Orden puede tener muchos OrdenDetalle (relación 1:N)
 *
 * En este POJO, el campo {@code producto} es el objeto completo (no solo el ID),
 * cargado mediante JOIN en OrdenDAO para acceder directamente al nombre, precio, etc.
 *
 * Clases que usan este modelo:
 *   → OrdenDAO         (crear detalles en crearOrden(); leer en obtenerOrdenPorId())
 *   → CheckoutServlet  (convierta CartItem → OrdenDetalle al finalizar la compra)
 *   → ReciboPDFServlet (itera los detalles para construir la tabla del recibo PDF)
 *   → admin_ventas.jsp (muestra las líneas de venta)
 */
public class OrdenDetalle {

    /** PK de la tabla orden_detalle. AUTO_INCREMENT. */
    private int idDetalle;

    /**
     * FK a ordenes.id_orden → indica a qué orden pertenece esta línea.
     * Permite reconstruir la orden completa con todas sus líneas.
     */
    private int idOrden;

    /**
     * Objeto Producto completo en lugar de solo el ID.
     * Cargado en OrdenDAO con un JOIN para que ReciboPDFServlet pueda leer od.getProducto().getNombre()
     * sin una consulta extra.
     */
    private Producto producto;

    /**
     * Cantidad de unidades compradas de este producto en esta orden.
     * Columna: cantidad (INT NOT NULL).
     * Viene del CartItem.getCantidad() al finalizar la compra.
     */
    private int cantidad;

    /**
     * Precio unitario del producto AL MOMENTO de la compra.
     * Se congela aquí para que si el vendedor cambia el precio después,
     * el recibo histórico siga mostrando el precio original pagado.
     * Columna: precio_unitario (DECIMAL(10,2)).
     */
    private BigDecimal precioUnitario;

    /**
     * Subtotal de esta línea: precioUnitario × cantidad.
     * Columna: subtotal_linea (DECIMAL(10,2)).
     * Calculado y guardado al crear la orden en OrdenDAO.crearOrden().
     * En VentaAdminDTO este mismo campo aparece como subtotalLinea.
     */
    private BigDecimal subtotalLinea;

    /** Constructor vacío requerido por JDBC. */
    public OrdenDetalle() {}

    // ── Getters y Setters ──

    /** @return ID de la línea de detalle (PK AUTO_INCREMENT) */
    public int getIdDetalle() { return idDetalle; }
    public void setIdDetalle(int idDetalle) { this.idDetalle = idDetalle; }

    /** @return ID de la orden a la que pertenece esta línea (FK a ordenes) */
    public int getIdOrden() { return idOrden; }
    public void setIdOrden(int idOrden) { this.idOrden = idOrden; }

    /**
     * @return El objeto Producto completo (con nombre, precio, imagen).
     *         Llenado por OrdenDAO al hacer JOIN con la tabla productos.
     */
    public Producto getProducto() { return producto; }
    public void setProducto(Producto producto) { this.producto = producto; }

    /** @return Número de unidades compradas (del CartItem) */
    public int getCantidad() { return cantidad; }
    public void setCantidad(int cantidad) { this.cantidad = cantidad; }

    /**
     * @return Precio unitario congelado al momento de la compra.
     *         No cambia aunque el vendedor actualice el precio del producto después.
     */
    public BigDecimal getPrecioUnitario() { return precioUnitario; }
    public void setPrecioUnitario(BigDecimal precioUnitario) { this.precioUnitario = precioUnitario; }

    /** @return Subtotal de la línea = precioUnitario × cantidad */
    public BigDecimal getSubtotalLinea() { return subtotalLinea; }
    public void setSubtotalLinea(BigDecimal subtotalLinea) { this.subtotalLinea = subtotalLinea; }
}
