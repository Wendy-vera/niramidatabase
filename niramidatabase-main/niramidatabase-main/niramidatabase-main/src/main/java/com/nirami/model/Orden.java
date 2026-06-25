package com.nirami.model;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;

/**
 * POJO que representa una orden de compra completa de un cliente.
 * Mapea la fila de la tabla {@code ordenes} más sus líneas de detalle.
 *
 * Tabla mapeada: {@code ordenes}
 * Columnas: id_orden (PK), id_cliente (FK→usuarios), subtotal, iva, total,
 *           direccion_envio, estado_pago, estado_orden, fecha_orden
 *
 * Estado del pago (columna estado_pago):
 *   - "PENDIENTE"  → Recién creada, esperando revisión del administrador
 *   - "APROBADO"   → Aprobada por el admin; el pago al vendedor está registrado
 *   - "RECHAZADO"  → Rechazada por el admin; el stock fue restaurado
 *
 * Relación con otras tablas:
 *   - {@code ordenes.id_cliente} → FK a {@code usuarios} (el comprador)
 *   - {@code orden_detalle.id_orden} → FK a esta tabla (las líneas de la compra)
 *   - {@code pagos_vendedor.id_orden} → FK a esta tabla (los pagos generados para cada vendedor)
 *
 * Clases que usan este modelo:
 *   → OrdenDAO    (crear, leer órdenes; la devuelve en obtenerOrdenPorId)
 *   → CheckoutServlet (crear la orden al comprar)
 *   → PerfilServlet   (listar historial de órdenes del cliente)
 *   → ReciboPDFServlet (generar PDF del recibo; accede a detalles y totales)
 */
public class Orden {

    /** PK de la tabla ordenes. Generado por AUTO_INCREMENT al insertar. */
    private int idOrden;

    /** FK a usuarios.id_usuario → identifica al cliente comprador. */
    private int idCliente;

    /**
     * Subtotal de la orden ANTES del IVA (suma de todos los subtotales de línea).
     * Columna: subtotal (DECIMAL(10,2)).
     * Calculado en CheckoutServlet antes de crear la orden.
     */
    private BigDecimal subtotal;

    /**
     * Monto del IVA (19% del subtotal).
     * Columna: iva (DECIMAL(10,2)).
     * Calculado en CheckoutServlet: subtotal × 0.19
     */
    private BigDecimal iva;

    /**
     * Total a pagar (subtotal + IVA).
     * Columna: total (DECIMAL(10,2)).
     * Calculado en CheckoutServlet: subtotal + iva
     */
    private BigDecimal total;

    /**
     * Dirección de envío donde se entregará la orden.
     * Copiada del perfil del cliente al momento de la compra (clientes.direccion_envio).
     * Columna: direccion_envio (TEXT).
     */
    private String direccion;

    /**
     * Estado actual del pago/proceso de la orden.
     * Valores: "PENDIENTE", "APROBADO", "RECHAZADO".
     * Columna: estado_pago (VARCHAR DEFAULT 'PENDIENTE').
     * Modificado por: AdminVentasServlet → OrdenDAO.actualizarEstadoPago()
     */
    private String estadoPago;

    /**
     * Fecha y hora en que el cliente realizó la compra.
     * Columna: fecha_orden (TIMESTAMP DEFAULT CURRENT_TIMESTAMP).
     */
    private Timestamp fechaOrden;

    /**
     * Lista de los ítems comprados en esta orden.
     * No es una columna de la BD; es una relación 1:N cargada por OrdenDAO.
     * Se carga con: SELECT * FROM orden_detalle WHERE id_orden = ?
     * Usado por: ReciboPDFServlet (iterar para generar la tabla del recibo)
     */
    private List<OrdenDetalle> detalles;

    /** Constructor vacío requerido por JDBC. */
    public Orden() {}

    // ── Getters y Setters ──

    /** @return ID único de la orden (PK AUTO_INCREMENT de la tabla ordenes) */
    public int getIdOrden() { return idOrden; }
    public void setIdOrden(int idOrden) { this.idOrden = idOrden; }

    /** @return ID del cliente comprador (FK a usuarios.id_usuario) */
    public int getIdCliente() { return idCliente; }
    public void setIdCliente(int idCliente) { this.idCliente = idCliente; }

    /** @return Suma de subtotales de todas las líneas, antes de IVA */
    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }

    /** @return Monto del IVA (19% del subtotal) */
    public BigDecimal getIva() { return iva; }
    public void setIva(BigDecimal iva) { this.iva = iva; }

    /** @return Total final (subtotal + IVA) que pagó el cliente */
    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }

    /** @return Dirección de envío copiada del perfil del cliente al momento de la compra */
    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }

    /** @return Estado: "PENDIENTE", "APROBADO" o "RECHAZADO" */
    public String getEstadoPago() { return estadoPago; }
    public void setEstadoPago(String estadoPago) { this.estadoPago = estadoPago; }

    /** @return Fecha y hora de creación de la orden */
    public Timestamp getFechaOrden() { return fechaOrden; }
    public void setFechaOrden(Timestamp fechaOrden) { this.fechaOrden = fechaOrden; }

    /** @return Lista de OrdenDetalle; cargada por OrdenDAO.obtenerOrdenPorId() */
    public List<OrdenDetalle> getDetalles() { return detalles; }
    public void setDetalles(List<OrdenDetalle> detalles) { this.detalles = detalles; }
}
