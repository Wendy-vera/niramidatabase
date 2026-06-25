package com.nirami.model;

import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * POJO que representa un artículo/producto del catálogo de Nirami.
 * Mapea (principalmente) la tabla {@code productos}, más campos calculados de JOINs.
 *
 * Tabla principal mapeada: {@code productos}
 * Columnas: id_producto (PK), nombre, descripcion, precio, stock (suma), id_categoria (FK),
 *           estado, vistas, fecha_creacion, fecha_actualizacion
 *
 * Tablas relacionadas:
 *   - {@code vendedor_producto}: cada vendedor tiene su propio stock_local por producto.
 *     El campo {@code stock} en este POJO puede representar el stock_local de un vendedor
 *     (cuando se consulta con ProductoDAO.obtenerPorVendedor) o el stock total sumado
 *     (cuando se usa COALESCE(SUM(vp.stock_local)) en otros métodos de ProductoDAO).
 *   - {@code producto_imagenes}: el campo {@code imagenPrincipal} viene de un JOIN/subconsulta
 *     que busca la imagen marcada como es_principal=1.
 *   - {@code categorias}: el campo {@code idCategoria} es FK a esa tabla.
 *
 * Estados posibles (columna estado):
 *   - "PENDIENTE"  → Recién creado por el vendedor, esperando revisión del admin
 *   - "APROBADO"   → Visible en el catálogo, puede ser comprado
 *   - "RECHAZADO"  → El admin lo rechazó; el vendedor debe corregirlo
 *   - "SUSPENDIDO" → El admin lo suspendió; no visible en el catálogo
 *   - "ELIMINADO"  → Borrado lógico por el admin
 *
 * Clases que usan este modelo:
 *   → ProductoDAO           (leer/escribir productos en la BD)
 *   → CatalogoServlet       (mostrar el catálogo de productos aprobados)
 *   → VendedorDashboardServlet (mostrar los productos del vendedor)
 *   → CarritoServlet        (guardar el producto en la sesión del carrito)
 *   → CheckoutServlet       (verificar stock al comprar)
 *   → FavoritoDAO           (la lista de favoritos contiene objetos Producto)
 *   → Múltiples JSPs        (catalogo.jsp, vendedor_dashboard.jsp, carrito.jsp, etc.)
 */
public class Producto {

    // ── Columnas directas de la tabla productos ──

    /** PK de la tabla productos. AUTO_INCREMENT. */
    private int idProducto;

    /** Nombre del producto artesanal. Columna: nombre (VARCHAR NOT NULL). */
    private String nombre;

    /** Descripción detallada del producto. Columna: descripcion (TEXT). */
    private String descripcion;

    /**
     * Precio de venta unitario. Columna: precio (DECIMAL(10,2) NOT NULL).
     * Se usa BigDecimal (no double) para evitar errores de precisión en cálculos monetarios.
     */
    private BigDecimal precio;

    /**
     * Stock disponible. Puede venir de diferentes fuentes según el query:
     *   - En ProductoDAO.obtenerPorVendedor(): viene de vendedor_producto.stock_local
     *     (stock del vendedor específico)
     *   - En otros métodos: viene de COALESCE(SUM(vp.stock_local)) (stock total del producto)
     * Columna de referencia: vendedor_producto.stock_local (INT NOT NULL).
     */
    private int stock;

    /** FK a categorias.id_categoria. Columna: id_categoria (INT FK NOT NULL). */
    private int idCategoria;

    /**
     * Estado actual del producto.
     * Valores: "PENDIENTE", "APROBADO", "RECHAZADO", "SUSPENDIDO", "ELIMINADO".
     * Columna: estado (VARCHAR DEFAULT 'PENDIENTE').
     */
    private String estado;

    /**
     * Número de veces que el producto fue visto en el catálogo.
     * Columna: vistas (INT DEFAULT 0).
     * Incrementado por: CatalogoServlet cuando un cliente abre el detalle del producto.
     */
    private int vistas;

    /** Fecha de creación del producto. Columna: fecha_creacion (TIMESTAMP DEFAULT CURRENT_TIMESTAMP). */
    private Timestamp fechaCreacion;

    /** Fecha de última actualización. Columna: fecha_actualizacion (TIMESTAMP ON UPDATE CURRENT_TIMESTAMP). */
    private Timestamp fechaActualizacion;

    // ── Campos adicionales calculados por JOIN (no existen como columnas directas en productos) ──

    /**
     * Ruta de la imagen principal del producto.
     * No es una columna de la tabla productos. Se obtiene con una subconsulta a producto_imagenes:
     *   (SELECT ruta FROM producto_imagenes WHERE id_producto = p.id_producto AND es_principal = 1 LIMIT 1)
     * Servida por ImageServlet cuando la JSP usa: {@code <img src="${p.imagenPrincipal}">}
     */
    private String imagenPrincipal;

    /**
     * ID del vendedor dueño del producto.
     * No es una columna de la tabla productos. Se obtiene con JOIN a vendedor_producto:
     *   vp.id_vendedor (FK a usuarios.id_usuario del vendedor)
     * Usado por ProductoVendedorServlet para verificar que el vendedor logueado
     * es el dueño antes de permitirle editar el producto.
     */
    private int idVendedor;

    /** Constructor vacío requerido por JDBC. */
    public Producto() {}

    // ── Getters y Setters ──

    /** @return ID único del producto (PK AUTO_INCREMENT) */
    public int getIdVendedor() { return idVendedor; }
    /** @param idVendedor FK a vendedor_producto.id_vendedor; se llena con JOIN en ProductoDAO */
    public void setIdVendedor(int idVendedor) { this.idVendedor = idVendedor; }

    /** @return ID del producto (PK, clave primaria de la tabla productos) */
    public int getIdProducto() { return idProducto; }
    public void setIdProducto(int idProducto) { this.idProducto = idProducto; }

    /** @return Nombre del producto para mostrar en el catálogo */
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    /** @return Descripción larga (para la página de detalle del producto) */
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    /**
     * @return Precio unitario de venta.
     *         Usado en: CartItem.getSubtotal(), CheckoutServlet, carrito.jsp, catalogo.jsp
     */
    public BigDecimal getPrecio() { return precio; }
    public void setPrecio(BigDecimal precio) { this.precio = precio; }

    /**
     * @return Stock disponible. Puede ser el stock_local del vendedor o el total sumado,
     *         dependiendo del método de ProductoDAO que lo cargó.
     */
    public int getStock() { return stock; }
    /** @param stock Nuevo valor de stock (actualizado en vendedor_producto.stock_local) */
    public void setStock(int stock) { this.stock = stock; }

    /** @return FK a categorias.id_categoria */
    public int getIdCategoria() { return idCategoria; }
    public void setIdCategoria(int idCategoria) { this.idCategoria = idCategoria; }

    /** @return Estado: "PENDIENTE", "APROBADO", "RECHAZADO", "SUSPENDIDO" o "ELIMINADO" */
    public String getEstado() { return estado; }
    /** @param estado Cambiado por ProductoDAO.actualizarEstado() o crearProducto() */
    public void setEstado(String estado) { this.estado = estado; }

    /** @return Conteo de vistas del producto en el catálogo */
    public int getVistas() { return vistas; }
    public void setVistas(int vistas) { this.vistas = vistas; }

    /** @return Timestamp de cuándo fue publicado el producto */
    public Timestamp getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(Timestamp fechaCreacion) { this.fechaCreacion = fechaCreacion; }

    /** @return Timestamp de la última actualización del producto */
    public Timestamp getFechaActualizacion() { return fechaActualizacion; }
    public void setFechaActualizacion(Timestamp fechaActualizacion) { this.fechaActualizacion = fechaActualizacion; }

    /**
     * @return Ruta relativa de la imagen principal ("uploads/xxx.jpg").
     *         Null si el producto no tiene imagen asociada.
     *         Servida por ImageServlet: GET /uploads/xxx.jpg
     */
    public String getImagenPrincipal() { return imagenPrincipal; }
    /** @param imagenPrincipal Ruta leída de la subconsulta en ProductoDAO */
    public void setImagenPrincipal(String imagenPrincipal) { this.imagenPrincipal = imagenPrincipal; }
}
