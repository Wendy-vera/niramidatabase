package com.nirami.dao;

import com.nirami.model.HistorialUsuario;
import com.nirami.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO (Data Access Object) para gestionar el historial de acciones de los clientes.
 *
 * <p>Este DAO interactúa exclusivamente con la tabla {@code historial_usuario} de la base
 * de datos {@code nirami_prueba}. Permite registrar acciones realizadas por clientes
 * (ver productos, buscar, agregar/quitar favoritos, comprar) y consultarlas
 * tanto por cliente individual como de forma global para el administrador.</p>
 *
 * <p><b>Tabla asociada:</b> {@code historial_usuario}</p>
 *
 * <p><b>Relaciones:</b>
 * <ul>
 *   <li>{@code historial_usuario.id_cliente} → {@code clientes.id_usuario} → {@code usuarios.id_usuario}</li>
 *   <li>{@code historial_usuario.id_producto} → {@code productos.id_producto} (ON DELETE SET NULL)</li>
 * </ul>
 * </p>
 *
 * <p><b>Conexión a BD:</b> Usa {@link com.nirami.util.DBConnection#getConnection()} para
 * obtener una conexión JDBC a MySQL.</p>
 *
 * @author Nirami Dev Team
 * @version 1.0
 * @see com.nirami.model.HistorialUsuario
 * @see com.nirami.util.DBConnection
 */
public class HistorialDAO {

    /**
     * Registra una nueva acción del cliente en la tabla {@code historial_usuario}.
     *
     * <p>Este método es llamado por varios servlets para trazar el comportamiento
     * del cliente:</p>
     * <ul>
     *   <li>{@link com.nirami.controller.CatalogoServlet} — para acciones {@code VER_PRODUCTO} y {@code BUSQUEDA}</li>
     *   <li>{@link com.nirami.controller.FavoritosServlet} — para {@code AGREGAR_FAVORITO} y {@code QUITAR_FAVORITO}</li>
     *   <li>{@link com.nirami.controller.CheckoutServlet} — para {@code COMPRAR}</li>
     * </ul>
     *
     * <p>Si ocurre un error SQL, se imprime el stack trace pero NO se lanza la excepción,
     * para evitar interrumpir el flujo normal del usuario.</p>
     *
     * @param idCliente       ID del cliente que realiza la acción (FK a {@code clientes.id_usuario})
     * @param idProducto      ID del producto involucrado; puede ser {@code null} para acción {@code BUSQUEDA}
     * @param accion          Tipo de acción: {@code VER_PRODUCTO}, {@code AGREGAR_FAVORITO},
     *                        {@code QUITAR_FAVORITO}, {@code COMPRAR}, {@code BUSQUEDA}
     * @param terminoBusqueda Texto buscado (solo cuando {@code accion = BUSQUEDA}); en otros casos pasar {@code null}
     * @param detalle         Texto descriptivo adicional (ej: nombre del producto, cantidad); máx. 255 chars
     * @param ipOrigen        Dirección IP del cliente (IPv4/IPv6); se obtiene de {@code request.getRemoteAddr()}
     */
    public void registrarAccion(int idCliente, Integer idProducto, String accion,
                                String terminoBusqueda, String detalle, String ipOrigen) {
        // La IP real puede estar detrás de un proxy; intentar obtenerla del header X-Forwarded-For
        String sql = "INSERT INTO historial_usuario " +
                     "(id_cliente, id_producto, accion, termino_busqueda, detalle, ip_origen) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, idCliente);
            // id_producto es nullable en la BD; usar setNull si es null
            if (idProducto != null) {
                ps.setInt(2, idProducto);
            } else {
                ps.setNull(2, java.sql.Types.INTEGER);
            }
            ps.setString(3, accion);
            ps.setString(4, terminoBusqueda);
            ps.setString(5, detalle);
            ps.setString(6, ipOrigen);

            ps.executeUpdate();

        } catch (SQLException e) {
            // El error de historial no debe detener el flujo del usuario
            System.err.println("[HistorialDAO] Error al registrar acción '" + accion + "' para cliente " + idCliente);
            e.printStackTrace();
        }
    }

    /**
     * Obtiene el historial completo de acciones de un cliente específico,
     * ordenado de más reciente a más antiguo.
     *
     * <p>Realiza un JOIN con {@code usuarios} (para el nombre del cliente) y un
     * LEFT JOIN con {@code productos} (para el nombre del producto, que puede ser NULL
     * si el producto fue eliminado o si la acción es BUSQUEDA).</p>
     *
     * <p><b>Conexión:</b> Abre y cierra su propia conexión (try-with-resources).</p>
     *
     * @param idCliente ID del cliente cuyo historial se desea consultar
     * @return Lista de {@link HistorialUsuario} ordenada por {@code fecha DESC};
     *         lista vacía si no hay registros o si ocurre un error SQL.
     */
    public List<HistorialUsuario> obtenerHistorialPorCliente(int idCliente) {
        List<HistorialUsuario> lista = new ArrayList<>();
        String sql = "SELECT h.*, u.nombre AS nombre_cliente, p.nombre AS nombre_producto " +
                     "FROM historial_usuario h " +
                     "JOIN usuarios u ON h.id_cliente = u.id_usuario " +
                     "LEFT JOIN productos p ON h.id_producto = p.id_producto " +
                     "WHERE h.id_cliente = ? " +
                     "ORDER BY h.fecha DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, idCliente);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapearResultSet(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[HistorialDAO] Error al obtener historial del cliente " + idCliente);
            e.printStackTrace();
        }
        return lista;
    }

    /**
     * Obtiene el historial global de acciones de TODOS los clientes,
     * ordenado de más reciente a más antiguo.
     *
     * <p>Esta consulta está diseñada para la vista de administrador, donde se puede
     * ver toda la actividad de la plataforma.</p>
     *
     * <p><b>Advertencia de rendimiento:</b> En producción con muchos registros,
     * se recomienda paginar esta consulta (LIMIT/OFFSET).</p>
     *
     * @param limite Número máximo de registros a devolver (recomendado: 100-500).
     *               Si se pasa 0 o negativo, se usa 200 como valor por defecto.
     * @return Lista de {@link HistorialUsuario} con los últimos {@code limite} registros
     *         de todos los clientes; lista vacía si no hay datos.
     */
    public List<HistorialUsuario> obtenerHistorialGlobal(int limite) {
        List<HistorialUsuario> lista = new ArrayList<>();
        if (limite <= 0) limite = 200;

        String sql = "SELECT h.*, u.nombre AS nombre_cliente, p.nombre AS nombre_producto " +
                     "FROM historial_usuario h " +
                     "JOIN usuarios u ON h.id_cliente = u.id_usuario " +
                     "LEFT JOIN productos p ON h.id_producto = p.id_producto " +
                     "ORDER BY h.fecha DESC " +
                     "LIMIT ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, limite);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapearResultSet(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[HistorialDAO] Error al obtener historial global");
            e.printStackTrace();
        }
        return lista;
    }

    /**
     * Método privado de apoyo que mapea una fila del {@link ResultSet}
     * a un objeto {@link HistorialUsuario}.
     *
     * <p>Se asume que el ResultSet proviene de una consulta que incluye las columnas:
     * {@code id_historial, id_cliente, id_producto, accion, termino_busqueda, detalle,
     * ip_origen, fecha, nombre_cliente, nombre_producto}.</p>
     *
     * @param rs ResultSet posicionado en la fila actual (no se llama a {@code rs.next()} aquí)
     * @return Objeto {@link HistorialUsuario} con todos los campos mapeados
     * @throws SQLException si alguna columna no existe o hay error de tipo
     */
    private HistorialUsuario mapearResultSet(ResultSet rs) throws SQLException {
        HistorialUsuario h = new HistorialUsuario();
        h.setIdHistorial(rs.getInt("id_historial"));
        h.setIdCliente(rs.getInt("id_cliente"));

        // id_producto es nullable: getInt devuelve 0 si es NULL en BD
        int idProd = rs.getInt("id_producto");
        h.setIdProducto(rs.wasNull() ? null : idProd);

        h.setAccion(rs.getString("accion"));
        h.setTerminoBusqueda(rs.getString("termino_busqueda"));
        h.setDetalle(rs.getString("detalle"));
        h.setIpOrigen(rs.getString("ip_origen"));
        h.setFecha(rs.getTimestamp("fecha"));
        h.setNombreCliente(rs.getString("nombre_cliente"));
        h.setNombreProducto(rs.getString("nombre_producto"));
        return h;
    }
}
