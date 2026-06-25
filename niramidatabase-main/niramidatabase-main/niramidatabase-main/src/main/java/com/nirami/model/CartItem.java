package com.nirami.model;

import java.math.BigDecimal;

/**
 * Representa un ítem (producto + cantidad) dentro del carrito de compras.
 *
 * IMPORTANTE: El carrito NO vive en la base de datos. Vive en la SESIÓN HTTP del usuario.
 * Clave de sesión: "carrito" → List&lt;CartItem&gt;
 * Quién lo gestiona: CarritoServlet (agregar, quitar, actualizar)
 * Quién lo consume: CheckoutServlet (para crear la orden), carrito.jsp (para mostrar)
 *
 * Relación con otras clases:
 *   - Contiene un objeto {@link Producto} completo (cargado desde BD por ProductoDAO)
 *   - Su método getSubtotal() calcula precio × cantidad sin llamar a la BD
 *   - Al finalizar la compra, CheckoutServlet convierte cada CartItem en un OrdenDetalle en la BD
 */
public class CartItem {

    // El producto que el cliente quiere comprar
    // Viene de ProductoDAO.obtenerPorId() cuando se agrega al carrito en CarritoServlet
    private Producto producto;

    // Cuántas unidades del producto quiere el cliente
    // Se incrementa si el mismo producto se agrega múltiples veces (no se duplican filas)
    private int cantidad;

    /**
     * Constructor vacío requerido por algunos frameworks de serialización.
     */
    public CartItem() {}

    /**
     * Constructor principal usado en CarritoServlet cuando se agrega un producto nuevo.
     *
     * @param producto Objeto Producto completo cargado desde ProductoDAO
     * @param cantidad Cantidad inicial (normalmente 1 al agregar por primera vez)
     */
    public CartItem(Producto producto, int cantidad) {
        this.producto = producto; // Guardar la referencia al producto (con nombre, precio, etc.)
        this.cantidad = cantidad; // Cantidad inicial
    }

    /**
     * Devuelve el producto asociado a este ítem del carrito.
     * Usado por: carrito.jsp (mostrar nombre, imagen, precio), CheckoutServlet (crear orden)
     */
    public Producto getProducto() { return producto; }

    /**
     * Establece el producto de este ítem.
     */
    public void setProducto(Producto producto) { this.producto = producto; }

    /**
     * Devuelve la cantidad de unidades solicitadas para este producto.
     * Usado por: carrito.jsp (mostrar cantidad), CarritoServlet (incrementar al agregar)
     * CheckoutServlet (verificar stock antes de crear la orden)
     */
    public int getCantidad() { return cantidad; }

    /**
     * Actualiza la cantidad de este ítem.
     * Llamado por CarritoServlet cuando:
     *   - Se agrega el mismo producto (cantidad += 1)
     *   - El usuario escribe una cantidad manualmente (action="actualizar")
     */
    public void setCantidad(int cantidad) { this.cantidad = cantidad; }

    /**
     * Calcula el subtotal de este ítem: precio × cantidad.
     *
     * Se usa en:
     *   - CarritoServlet.doGet() para sumar el total del carrito
     *   - carrito.jsp para mostrar el subtotal por fila
     *
     * Usa BigDecimal para precisión decimal exacta en operaciones monetarias
     * (evita errores de redondeo de double/float con precios como $9.99)
     *
     * @return BigDecimal con el valor precio_unitario × cantidad (ej: $25.00 × 3 = $75.00)
     */
    public BigDecimal getSubtotal() {
        // producto.getPrecio() → BigDecimal con el precio unitario (viene de la BD como DECIMAL)
        // new BigDecimal(cantidad) → convierte el int a BigDecimal para la multiplicación
        // multiply() → operación exacta de multiplicación de decimales
        return producto.getPrecio().multiply(new BigDecimal(cantidad));
    }
}
