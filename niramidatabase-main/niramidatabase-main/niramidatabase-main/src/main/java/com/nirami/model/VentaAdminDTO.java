package com.nirami.model;

import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * DTO (Data Transfer Object) que transporta datos combinados de venta
 * entre el backend (DAO) y las vistas JSP (admin y vendedor).
 *
 * <p>Esta clase no corresponde a una sola tabla, sino que agrega datos de:
 * <ul>
 *   <li>{@code ordenes} — id_orden, fecha_orden, estado_pago</li>
 *   <li>{@code orden_detalle} — id_detalle, cantidad, precio_unitario, subtotal_linea, id_vendedor</li>
 *   <li>{@code pagos_vendedor} — id_pago, monto_neto, estado (PENDIENTE/PAGADO)</li>
 *   <li>{@code usuarios} (cliente y vendedor) — nombre</li>
 *   <li>{@code productos} — nombre</li>
 *   <li>{@code vendedores} — cuenta_bancaria</li>
 * </ul>
 * </p>
 *
 * <p><b>Uso principal:</b>
 * <ul>
 *   <li>Vista de admin de ventas ({@code admin_ventas.jsp})</li>
 *   <li>Vista de admin de pagos a vendedores ({@code admin_pagos_vendedores.jsp})</li>
 *   <li>Vista de ventas del vendedor ({@code vendedor_ventas.jsp})</li>
 *   <li>Vista de pagos del vendedor ({@code vendedor_pagos.jsp})</li>
 * </ul>
 * </p>
 *
 * @author Nirami Dev Team
 * @version 1.1
 */
public class VentaAdminDTO {

    // ─── Datos de la Orden ──────────────────────────────────────────────────

    /**
     * ID de la orden padre.
     * Corresponde a {@code ordenes.id_orden}.
     */
    private int idOrden;

    /**
     * Fecha y hora en que se creó la orden.
     * Corresponde a {@code ordenes.fecha_orden}.
     */
    private Timestamp fechaOrden;

    /**
     * Estado del pago de la orden completa.
     * Valores posibles: {@code PENDIENTE}, {@code APROBADO}, {@code RECHAZADO}, {@code REEMBOLSADO}.
     * Corresponde a {@code ordenes.estado_pago}.
     */
    private String estadoPago;

    // ─── Datos del Detalle de Orden ─────────────────────────────────────────

    /**
     * ID del detalle de orden específico.
     * Corresponde a {@code orden_detalle.id_detalle}.
     * Usado para referenciar el pago individual en {@code pagos_vendedor}.
     */
    private int idDetalle;

    /**
     * Cantidad de unidades del producto en este detalle.
     * Corresponde a {@code orden_detalle.cantidad}.
     */
    private int cantidad;

    /**
     * Precio unitario del producto al momento de la venta.
     * Corresponde a {@code orden_detalle.precio_unitario}.
     */
    private BigDecimal precioUnitario;

    /**
     * Subtotal de esta línea de detalle (cantidad × precio_unitario).
     * Corresponde a {@code orden_detalle.subtotal_linea}.
     * Este es el monto bruto que le corresponde al vendedor antes de comisiones.
     */
    private BigDecimal subtotalLinea;

    /**
     * ID del vendedor responsable de este detalle de venta.
     * Corresponde a {@code orden_detalle.id_vendedor}.
     * Usado para agrupar pagos por vendedor en la vista de pagos.
     */
    private int idVendedor;

    // ─── Datos de Nombres (resueltos por JOIN) ──────────────────────────────

    /**
     * Nombre completo del cliente que realizó la orden.
     * Resuelto por JOIN con {@code usuarios} usando {@code ordenes.id_cliente}.
     */
    private String nombreCliente;

    /**
     * Nombre del producto vendido.
     * Resuelto por JOIN con {@code productos}.
     */
    private String nombreProducto;

    /**
     * Nombre completo del vendedor responsable.
     * Resuelto por JOIN con {@code usuarios} usando {@code orden_detalle.id_vendedor}.
     */
    private String nombreVendedor;

    // ─── Datos del Pago al Vendedor ─────────────────────────────────────────

    /**
     * ID del registro de pago en la tabla {@code pagos_vendedor}.
     * Puede ser 0 si aún no existe el registro de pago (raro, ya que se crea al hacer checkout).
     */
    private int idPago;

    /**
     * Monto neto a pagar al vendedor después de descontar comisiones.
     * Corresponde a {@code pagos_vendedor.monto_neto}.
     * Actualmente la comisión es 0.00, por lo que monto_neto = subtotal_linea.
     */
    private BigDecimal montoNeto;

    /**
     * Estado del pago al vendedor.
     * Valores posibles: {@code PENDIENTE}, {@code PAGADO}.
     * Corresponde a {@code pagos_vendedor.estado}.
     * <strong>Nota:</strong> Este campo es distinto de {@code estadoPago} (que es el estado de la orden).
     */
    private String estadoPagoVendedor;

    /**
     * Número de cuenta bancaria del vendedor.
     * Resuelto por JOIN con {@code vendedores}.
     * Se usa en la vista de admin para saber a qué cuenta transferir.
     */
    private String cuentaBancaria;

    // ─── Getters y Setters ──────────────────────────────────────────────────

    /** @return ID de la orden padre */
    public int getIdOrden() { return idOrden; }
    /** @param idOrden ID de la orden */
    public void setIdOrden(int idOrden) { this.idOrden = idOrden; }

    /** @return Fecha y hora de la orden */
    public Timestamp getFechaOrden() { return fechaOrden; }
    /** @param fechaOrden Timestamp de la orden */
    public void setFechaOrden(Timestamp fechaOrden) { this.fechaOrden = fechaOrden; }

    /** @return Estado de pago de la orden (PENDIENTE/APROBADO/RECHAZADO) */
    public String getEstadoPago() { return estadoPago; }
    /** @param estadoPago Estado del pago de la orden */
    public void setEstadoPago(String estadoPago) { this.estadoPago = estadoPago; }

    /** @return ID del detalle de orden */
    public int getIdDetalle() { return idDetalle; }
    /** @param idDetalle ID del detalle */
    public void setIdDetalle(int idDetalle) { this.idDetalle = idDetalle; }

    /** @return Cantidad de unidades */
    public int getCantidad() { return cantidad; }
    /** @param cantidad Unidades vendidas */
    public void setCantidad(int cantidad) { this.cantidad = cantidad; }

    /** @return Precio unitario al momento de la venta */
    public BigDecimal getPrecioUnitario() { return precioUnitario; }
    /** @param precioUnitario Precio por unidad */
    public void setPrecioUnitario(BigDecimal precioUnitario) { this.precioUnitario = precioUnitario; }

    /** @return Subtotal de la línea (cantidad × precio) */
    public BigDecimal getSubtotalLinea() { return subtotalLinea; }
    /** @param subtotalLinea Monto total de la línea */
    public void setSubtotalLinea(BigDecimal subtotalLinea) { this.subtotalLinea = subtotalLinea; }

    /** @return ID del vendedor responsable del detalle */
    public int getIdVendedor() { return idVendedor; }
    /** @param idVendedor ID del vendedor */
    public void setIdVendedor(int idVendedor) { this.idVendedor = idVendedor; }

    /** @return Nombre del cliente */
    public String getNombreCliente() { return nombreCliente; }
    /** @param nombreCliente Nombre completo del cliente */
    public void setNombreCliente(String nombreCliente) { this.nombreCliente = nombreCliente; }

    /** @return Nombre del producto */
    public String getNombreProducto() { return nombreProducto; }
    /** @param nombreProducto Nombre del producto vendido */
    public void setNombreProducto(String nombreProducto) { this.nombreProducto = nombreProducto; }

    /** @return Nombre del vendedor */
    public String getNombreVendedor() { return nombreVendedor; }
    /** @param nombreVendedor Nombre completo del vendedor */
    public void setNombreVendedor(String nombreVendedor) { this.nombreVendedor = nombreVendedor; }

    /** @return ID del registro en pagos_vendedor */
    public int getIdPago() { return idPago; }
    /** @param idPago ID del pago al vendedor */
    public void setIdPago(int idPago) { this.idPago = idPago; }

    /** @return Monto neto a pagar al vendedor (tras comisiones) */
    public BigDecimal getMontoNeto() { return montoNeto; }
    /** @param montoNeto Monto neto del pago */
    public void setMontoNeto(BigDecimal montoNeto) { this.montoNeto = montoNeto; }

    /** @return Estado del pago al vendedor (PENDIENTE o PAGADO) */
    public String getEstadoPagoVendedor() { return estadoPagoVendedor; }
    /** @param estadoPagoVendedor Estado del pago (PENDIENTE/PAGADO) */
    public void setEstadoPagoVendedor(String estadoPagoVendedor) { this.estadoPagoVendedor = estadoPagoVendedor; }

    /** @return Número de cuenta bancaria del vendedor */
    public String getCuentaBancaria() { return cuentaBancaria; }
    /** @param cuentaBancaria Cuenta bancaria del vendedor */
    public void setCuentaBancaria(String cuentaBancaria) { this.cuentaBancaria = cuentaBancaria; }
}
