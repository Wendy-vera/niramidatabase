package com.nirami.model;

import java.sql.Timestamp;

/**
 * POJO (Plain Old Java Object) que representa una fila de la tabla {@code categorias}.
 *
 * Tabla mapeada: {@code categorias}
 * Columnas: id_categoria (PK), nombre, descripcion, imagen, estado, fecha_creacion
 *
 * Estados posibles (columna {@code estado}):
 *   - "ACTIVA"   → La categoría aparece en el catálogo y en el formulario de productos
 *   - "INACTIVA" → No aparece en el catálogo pero sus productos existen
 *   - "ELIMINADA"→ Borrado lógico; solo permitido si no tiene productos asociados
 *
 * Relación con otras tablas:
 *   - {@code productos.id_categoria} → FK que referencia a {@code categorias.id_categoria}
 *   - Un producto pertenece a exactamente una categoría
 *
 * Clases que usan este modelo:
 *   → CategoriaDAO  (leer y escribir en la tabla)
 *   → AdminCategoriasServlet (CRUD de categorías)
 *   → ProductoVendedorServlet (seleccionar categoría al crear producto)
 *   → CatalogoServlet (filtrar productos por categoría)
 *   → Múltiples JSPs (admin_categorias.jsp, producto_form.jsp, etc.)
 */
public class Categoria {

    // ── Columnas de la tabla categorias ──

    /** PK de la tabla categorias. Generado automáticamente por AUTO_INCREMENT de MySQL. */
    private int idCategoria;

    /** Nombre de la categoría (ej: "Tejidos", "Cerámica"). Columna: nombre (VARCHAR NOT NULL). */
    private String nombre;

    /** Descripción opcional de la categoría. Columna: descripcion (TEXT, nullable). */
    private String descripcion;

    /**
     * Ruta de la imagen representativa de la categoría.
     * Sigue el mismo patrón que Producto.imagenPrincipal: "uploads/nombrearchivo.jpg".
     * Columna: imagen (VARCHAR, nullable).
     */
    private String imagen;

    /**
     * Estado actual de la categoría.
     * Valores: "ACTIVA", "INACTIVA", "ELIMINADA".
     * Columna: estado (VARCHAR DEFAULT 'ACTIVA').
     */
    private String estado;

    /**
     * Fecha y hora de creación de la categoría.
     * Columna: fecha_creacion (TIMESTAMP DEFAULT CURRENT_TIMESTAMP).
     */
    private Timestamp fechaCreacion;

    /** Constructor vacío requerido por JDBC y frameworks de inyección. */
    public Categoria() {}

    // ── Getters y Setters ──

    /** @return ID único de la categoría (PK de la tabla categorias) */
    public int getIdCategoria() { return idCategoria; }
    /** @param idCategoria ID asignado por AUTO_INCREMENT al insertar */
    public void setIdCategoria(int idCategoria) { this.idCategoria = idCategoria; }

    /** @return Nombre de la categoría para mostrar en el catálogo y formularios */
    public String getNombre() { return nombre; }
    /** @param nombre Texto del nombre de la categoría */
    public void setNombre(String nombre) { this.nombre = nombre; }

    /** @return Descripción de la categoría (puede ser null si no se proporcionó) */
    public String getDescripcion() { return descripcion; }
    /** @param descripcion Texto descriptivo de la categoría */
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    /** @return Ruta relativa de la imagen ("uploads/xxx.jpg") o null si no tiene imagen */
    public String getImagen() { return imagen; }
    /** @param imagen Ruta relativa guardada en BD; servida por ImageServlet */
    public void setImagen(String imagen) { this.imagen = imagen; }

    /** @return Estado actual: "ACTIVA", "INACTIVA" o "ELIMINADA" */
    public String getEstado() { return estado; }
    /** @param estado Nuevo estado. Cambiado por CategoriaDAO.cambiarEstado() */
    public void setEstado(String estado) { this.estado = estado; }

    /** @return Timestamp de cuándo fue creada la categoría en la BD */
    public Timestamp getFechaCreacion() { return fechaCreacion; }
    /** @param fechaCreacion Leído de la BD, corresponde a TIMESTAMP DEFAULT CURRENT_TIMESTAMP */
    public void setFechaCreacion(Timestamp fechaCreacion) { this.fechaCreacion = fechaCreacion; }
}
