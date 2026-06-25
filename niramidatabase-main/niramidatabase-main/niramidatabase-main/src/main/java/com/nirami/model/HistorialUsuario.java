package com.nirami.model;

import java.sql.Timestamp;

/**
 * Modelo POJO que representa un registro en la tabla {@code historial_usuario}.
 *
 * <p>Cada instancia de esta clase corresponde a una acción realizada por un cliente
 * dentro del sistema (ver producto, buscar, agregar/quitar favorito, comprar).
 * El historial es de solo lectura para el cliente y de consulta para el administrador.</p>
 *
 * <p><b>Tabla asociada:</b> {@code historial_usuario}</p>
 *
 * <p><b>Relaciones:</b>
 * <ul>
 *   <li>Pertenece a un {@code cliente} (referencia {@code clientes.id_usuario})</li>
 *   <li>Opcionalmente referencia a un {@code producto} (puede ser NULL si la acción es BUSQUEDA)</li>
 * </ul>
 * </p>
 *
 * @author Nirami Dev Team
 * @version 1.0
 */
public class HistorialUsuario {

    /**
     * Clave primaria autoincremental del registro de historial.
     * Corresponde a la columna {@code id_historial}.
     */
    private int idHistorial;

    /**
     * ID del cliente que realizó la acción.
     * Corresponde a la columna {@code id_cliente} (FK → {@code clientes.id_usuario}).
     */
    private int idCliente;

    /**
     * Nombre del cliente (se obtiene mediante JOIN al consultar el historial).
     * No es una columna directa de la tabla; se resuelve en el DAO.
     */
    private String nombreCliente;

    /**
     * ID del producto relacionado con la acción. Puede ser {@code null} cuando
     * la acción es {@code BUSQUEDA} (el cliente buscó un término sin ver un producto específico).
     * Corresponde a la columna {@code id_producto} (FK → {@code productos.id_producto}).
     */
    private Integer idProducto;

    /**
     * Nombre del producto (se resuelve mediante JOIN). Puede ser {@code null}
     * si el producto fue eliminado (FK ON DELETE SET NULL) o si la acción es BUSQUEDA.
     */
    private String nombreProducto;

    /**
     * Tipo de acción realizada por el cliente. Valores posibles (ENUM en BD):
     * <ul>
     *   <li>{@code VER_PRODUCTO} — El cliente abrió la página de detalle de un producto</li>
     *   <li>{@code AGREGAR_FAVORITO} — El cliente añadió un producto a sus favoritos</li>
     *   <li>{@code QUITAR_FAVORITO} — El cliente eliminó un producto de sus favoritos</li>
     *   <li>{@code COMPRAR} — El cliente completó una compra (checkout exitoso)</li>
     *   <li>{@code BUSQUEDA} — El cliente realizó una búsqueda en el catálogo</li>
     * </ul>
     * Corresponde a la columna {@code accion}.
     */
    private String accion;

    /**
     * Término de búsqueda ingresado por el cliente. Solo se llena cuando
     * {@code accion} es {@code BUSQUEDA}. En otros casos es {@code null}.
     * Corresponde a la columna {@code termino_busqueda}.
     */
    private String terminoBusqueda;

    /**
     * Texto descriptivo adicional de la acción. Por ejemplo, el nombre del producto
     * visto, la cantidad comprada, etc. Máximo 255 caracteres.
     * Corresponde a la columna {@code detalle}.
     */
    private String detalle;

    /**
     * Dirección IP desde la cual el cliente realizó la acción (IPv4 o IPv6).
     * Se usa para auditoría y detección de comportamiento anómalo.
     * Corresponde a la columna {@code ip_origen}.
     */
    private String ipOrigen;

    /**
     * Fecha y hora exacta en que se registró la acción.
     * Corresponde a la columna {@code fecha} (DEFAULT CURRENT_TIMESTAMP en BD).
     */
    private Timestamp fecha;

    /** Constructor por defecto requerido por el framework. */
    public HistorialUsuario() {}

    // ─── Getters y Setters ─────────────────────────────────────────────────

    /** @return ID único del registro de historial */
    public int getIdHistorial() { return idHistorial; }
    /** @param idHistorial ID único del registro */
    public void setIdHistorial(int idHistorial) { this.idHistorial = idHistorial; }

    /** @return ID del cliente que realizó la acción */
    public int getIdCliente() { return idCliente; }
    /** @param idCliente ID del cliente (FK a clientes) */
    public void setIdCliente(int idCliente) { this.idCliente = idCliente; }

    /** @return Nombre del cliente (resuelto por JOIN en DAO) */
    public String getNombreCliente() { return nombreCliente; }
    /** @param nombreCliente Nombre del cliente */
    public void setNombreCliente(String nombreCliente) { this.nombreCliente = nombreCliente; }

    /** @return ID del producto relacionado (puede ser null) */
    public Integer getIdProducto() { return idProducto; }
    /** @param idProducto ID del producto (FK a productos, nullable) */
    public void setIdProducto(Integer idProducto) { this.idProducto = idProducto; }

    /** @return Nombre del producto (resuelto por JOIN, puede ser null) */
    public String getNombreProducto() { return nombreProducto; }
    /** @param nombreProducto Nombre del producto */
    public void setNombreProducto(String nombreProducto) { this.nombreProducto = nombreProducto; }

    /** @return Tipo de acción (VER_PRODUCTO, AGREGAR_FAVORITO, QUITAR_FAVORITO, COMPRAR, BUSQUEDA) */
    public String getAccion() { return accion; }
    /** @param accion Tipo de acción realizada */
    public void setAccion(String accion) { this.accion = accion; }

    /** @return Término de búsqueda (solo para acción BUSQUEDA, null en otros casos) */
    public String getTerminoBusqueda() { return terminoBusqueda; }
    /** @param terminoBusqueda Texto buscado por el cliente */
    public void setTerminoBusqueda(String terminoBusqueda) { this.terminoBusqueda = terminoBusqueda; }

    /** @return Detalle adicional de la acción */
    public String getDetalle() { return detalle; }
    /** @param detalle Texto descriptivo de la acción */
    public void setDetalle(String detalle) { this.detalle = detalle; }

    /** @return IP de origen de la solicitud */
    public String getIpOrigen() { return ipOrigen; }
    /** @param ipOrigen Dirección IP del cliente */
    public void setIpOrigen(String ipOrigen) { this.ipOrigen = ipOrigen; }

    /** @return Fecha y hora del registro */
    public Timestamp getFecha() { return fecha; }
    /** @param fecha Timestamp del momento de la acción */
    public void setFecha(Timestamp fecha) { this.fecha = fecha; }
}
