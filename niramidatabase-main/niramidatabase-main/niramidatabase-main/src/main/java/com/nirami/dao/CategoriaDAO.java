package com.nirami.dao;

import com.nirami.model.Categoria;
import com.nirami.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO (Data Access Object) que gestiona todas las operaciones sobre la tabla {@code categorias}.
 *
 * Tabla destino: {@code categorias}
 * Columnas: id_categoria (PK, AUTO_INCREMENT), nombre, descripcion, imagen, estado, fecha_creacion
 *
 * Estados posibles de una categoría:
 *   - ACTIVA   → aparece en el catálogo y en los formularios de productos
 *   - INACTIVA → oculta en el catálogo pero mantiene sus productos
 *   - ELIMINADA → borrado lógico, solo cuando no tiene productos asociados
 *
 * Clases que usan este DAO:
 *   → AdminCategoriasServlet (crear, editar, cambiar estado)
 *   → ProductoVendedorServlet (cargar categorías activas en el formulario de producto)
 *   → CatalogoServlet (cargar categorías activas para el filtro lateral del catálogo)
 */
public class CategoriaDAO {

    /**
     * Obtiene SOLO las categorías en estado ACTIVA.
     * Usada por ProductoVendedorServlet y CatalogoServlet para los selectores de categoría.
     *
     * @return Lista de categorías activas ordenadas por defecto (por fecha_creacion ASC).
     */
    public List<Categoria> obtenerTodasActivas() {
        List<Categoria> lista = new ArrayList<>(); // Lista que se llenará y devolverá

        // Solo categorías ACTIVAS → las que aparecen en el catálogo y en los formularios
        String sql = "SELECT * FROM categorias WHERE estado = 'ACTIVA'";

        // try-with-resources: cierra automáticamente Connection, PreparedStatement y ResultSet
        // Evita fugas de conexión incluso si ocurre una excepción
        try (Connection       conn = DBConnection.getConnection();  // Abrir conexión desde el pool
             PreparedStatement ps  = conn.prepareStatement(sql);    // Preparar la query
             ResultSet         rs  = ps.executeQuery()) {           // Ejecutar y obtener resultados

            // Iterar cada fila del ResultSet → una fila = una categoría
            while (rs.next()) {
                Categoria c = new Categoria();
                c.setIdCategoria(rs.getInt("id_categoria"));        // PK de la tabla categorias
                c.setNombre(rs.getString("nombre"));                // Nombre visible en el catálogo
                c.setDescripcion(rs.getString("descripcion"));      // Descripción opcional
                c.setImagen(rs.getString("imagen"));                // Ruta de imagen de la categoría (puede ser null)
                c.setEstado(rs.getString("estado"));                // Siempre "ACTIVA" en este método
                c.setFechaCreacion(rs.getTimestamp("fecha_creacion")); // Cuándo fue creada
                lista.add(c); // Agregar el objeto mapeado a la lista resultado
            }

        } catch (SQLException e) {
            e.printStackTrace(); // Registrar el error (BD no disponible, SQL inválido, etc.)
        }
        return lista; // Lista vacía si hubo error o no hay categorías activas
    }

    /**
     * Obtiene TODAS las categorías excepto las ELIMINADAS.
     * Usada por AdminCategoriasServlet para mostrar el listado completo (activas + inactivas).
     *
     * @return Lista de todas las categorías no eliminadas.
     */
    public List<Categoria> obtenerTodas() {
        List<Categoria> lista = new ArrayList<>();

        // Incluir ACTIVAS e INACTIVAS pero excluir ELIMINADAS (borrado lógico)
        String sql = "SELECT * FROM categorias WHERE estado != 'ELIMINADA'";

        try (Connection       conn = DBConnection.getConnection();
             PreparedStatement ps  = conn.prepareStatement(sql);
             ResultSet         rs  = ps.executeQuery()) {

            while (rs.next()) {
                Categoria c = new Categoria();
                c.setIdCategoria(rs.getInt("id_categoria"));
                c.setNombre(rs.getString("nombre"));
                c.setDescripcion(rs.getString("descripcion"));
                c.setImagen(rs.getString("imagen"));
                c.setEstado(rs.getString("estado"));           // Puede ser ACTIVA o INACTIVA
                c.setFechaCreacion(rs.getTimestamp("fecha_creacion"));
                lista.add(c);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return lista;
    }

    /**
     * Inserta una nueva categoría en la BD.
     * El estado se fija automáticamente en ACTIVA por el DEFAULT de la columna en la BD.
     *
     * Llamada por: AdminCategoriasServlet.doPost() cuando accion="crear"
     *
     * @param c Objeto Categoria con nombre y descripción completados.
     * @return true si el INSERT fue exitoso; false si hubo error.
     */
    public boolean agregarCategoria(Categoria c) {
        // Solo se insertan nombre y descripción; id_categoria es AUTO_INCREMENT y estado DEFAULT 'ACTIVA'
        String sql = "INSERT INTO categorias (nombre, descripcion) VALUES (?, ?)";

        try (Connection       conn = DBConnection.getConnection();
             PreparedStatement ps  = conn.prepareStatement(sql)) {

            ps.setString(1, c.getNombre());      // Parámetro 1: nombre de la categoría
            ps.setString(2, c.getDescripcion()); // Parámetro 2: descripción opcional

            // executeUpdate() devuelve el número de filas afectadas (1 si fue exitoso)
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Actualiza el nombre y descripción de una categoría existente.
     *
     * Llamada por: AdminCategoriasServlet.doPost() cuando accion="editar"
     *
     * @param c Objeto Categoria con idCategoria, nombre y descripción actualizados.
     * @return true si el UPDATE afectó al menos 1 fila; false si no encontró la categoría o hubo error.
     */
    public boolean actualizarCategoria(Categoria c) {
        // Actualizar solo nombre y descripción; el estado no se toca aquí (se cambia con cambiarEstado)
        String sql = "UPDATE categorias SET nombre = ?, descripcion = ? WHERE id_categoria = ?";

        try (Connection       conn = DBConnection.getConnection();
             PreparedStatement ps  = conn.prepareStatement(sql)) {

            ps.setString(1, c.getNombre());       // Nuevo nombre
            ps.setString(2, c.getDescripcion());  // Nueva descripción
            ps.setInt(3, c.getIdCategoria());     // PK para el WHERE (qué fila actualizar)
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Cambia el estado de una categoría (ACTIVA → INACTIVA, INACTIVA → ACTIVA, etc.).
     *
     * Llamada por: AdminCategoriasServlet.doPost() cuando accion="estado".
     * Antes de llamar con estado="ELIMINADA", el servlet verifica con tieneProductos().
     *
     * @param idCategoria ID de la categoría a modificar.
     * @param estado      Nuevo estado: "ACTIVA", "INACTIVA" o "ELIMINADA".
     * @return true si el UPDATE fue exitoso; false si no encontró la categoría o hubo error.
     */
    public boolean cambiarEstado(int idCategoria, String estado) {
        // Simple UPDATE de una sola columna identificada por PK
        String sql = "UPDATE categorias SET estado = ? WHERE id_categoria = ?";

        try (Connection       conn = DBConnection.getConnection();
             PreparedStatement ps  = conn.prepareStatement(sql)) {

            ps.setString(1, estado);       // Nuevo estado ("ACTIVA", "INACTIVA" o "ELIMINADA")
            ps.setInt(2, idCategoria);     // PK de la categoría a modificar
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Verifica si una categoría tiene productos asociados.
     * Usada por AdminCategoriasServlet antes de intentar eliminar una categoría.
     *
     * Si tiene productos, no se puede eliminar (integridad referencial: productos.id_categoria FK).
     *
     * @param idCategoria ID de la categoría a verificar.
     * @return true si hay al menos 1 producto en esta categoría; false si está vacía.
     */
    public boolean tieneProductos(int idCategoria) {
        // COUNT(*) devuelve el número de productos cuya FK id_categoria = idCategoria
        String sql = "SELECT COUNT(*) FROM productos WHERE id_categoria = ?";

        try (Connection       conn = DBConnection.getConnection();
             PreparedStatement ps  = conn.prepareStatement(sql)) {

            ps.setInt(1, idCategoria); // FK de los productos que pertenecen a esta categoría

            // El ResultSet tiene exactamente 1 fila con 1 columna: el número de productos
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0; // true si el conteo es mayor a cero
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false; // En caso de error, asumir que no tiene (más seguro para la operación)
    }
}
