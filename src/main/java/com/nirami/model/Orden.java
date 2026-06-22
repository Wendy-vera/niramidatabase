package com.nirami.model;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;

public class Orden {
    private int idOrden;
    private int idCliente;
    private BigDecimal subtotal;
    private BigDecimal iva;
    private BigDecimal total;
    private String direccion;
    private String estadoPago;
    private Timestamp fechaOrden;
    private List<OrdenDetalle> detalles;

    public Orden() {}

    public int getIdOrden() { return idOrden; }
    public void setIdOrden(int idOrden) { this.idOrden = idOrden; }
    public int getIdCliente() { return idCliente; }
    public void setIdCliente(int idCliente) { this.idCliente = idCliente; }
    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }
    public BigDecimal getIva() { return iva; }
    public void setIva(BigDecimal iva) { this.iva = iva; }
    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }
    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }
    public String getEstadoPago() { return estadoPago; }
    public void setEstadoPago(String estadoPago) { this.estadoPago = estadoPago; }
    public Timestamp getFechaOrden() { return fechaOrden; }
    public void setFechaOrden(Timestamp fechaOrden) { this.fechaOrden = fechaOrden; }
    public List<OrdenDetalle> getDetalles() { return detalles; }
    public void setDetalles(List<OrdenDetalle> detalles) { this.detalles = detalles; }
}
