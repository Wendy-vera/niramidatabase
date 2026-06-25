package com.nirami.dao;

import com.nirami.model.Producto;
import com.nirami.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO (Data Access Object) que gestiona las operaciones sobre la tabla {@code favoritos}.
 *
 * Tabla destino: {@code favoritos}
 * Columnas: id_favorito (PK, AUTO_INCREMENT), id_cliente (FK→usuarios), id_producto (FK→productos), fecha_agregado
 * Restricción única: (id_cliente, id_producto) → un cliente no puede guardar el mismo producto dos veces
 *
 * Clases que usan este DAO:
 *   → FavoritosServlet (agregar, quitar, verificar si ya es favorito)
 *   → PerfilServlet    (obtener la lista de favoritos del cliente para su perfil)
 *   → CatalogoServlet  (verificar si un producto ya es favorito al mostrar el catálogo)
 */
public class FavoritoDAO {

    /**
     * Agrega un producto a la lista de favoritos de un cliente.
     *
     * Llamada por: FavoritosServlet.doPost() cuando action="agregar"
     *
     * @param idCliente  ID del cliente que agrega el favorito (usuarios.id_usuario con tipo CLIENTE)
     * @param idProducto ID del producto que se agrega (productos.id_producto)
     * @return true si el INSERT fue exitoso; false si ya existía (UNIQUE violation) o hubo error
     */
    public boolean agregarFavorito(int idCliente, int idProducto) {
        // INSERT en la tabla de relación muchos-a-muchos entre clientes y productos
        // Si el par (id_cliente, id_producto) ya existe, la BD lanzará SQLException por UNIQUE constraint
        String sql = "INSERT INTO favoritos (id_cliente, id_producto) VALUES (?, ?)";

        try (Connection       conn = DBConnection.getConnection();
             PreparedStatement ps  = conn.prepareStatement(sql)) {

            ps.setInt(1, idCliente);   // FK referenciando a usuarios.id_usuario del cliente
            ps.setInt(2, idProducto);  // FK referenciando a productos.id_producto

            return ps.executeUpdate() > 0; // true si se insertó 1 fila

        } catch (SQLException e) {
            // SQLException aquí generalmente significa que el favorito ya existe (UNIQUE constraint)
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Elimina un producto de la lista de favoritos de un cliente.
     *
     * Llamada por: FavoritosServlet.doPost() cuando action="quitar"
     *
     * @param idCliente  ID del cliente que quita el favorito
     * @param idProducto ID del producto que se quita de favoritos
     * @return true si se eliminó 1 fila; false si el favorito no existía o hubo error
     */
    public boolean quitarFavorito(int idCliente, int idProducto) {
        // DELETE identificando la fila por las dos FKs (que juntas son UNIQUE)
        String sql = "DELETE FROM favoritos WHERE id_cliente = ? AND id_producto = ?";

        try (Connection       conn = DBConnection.getConnection();
             PreparedStatement ps  = conn.prepareStatement(sql)) {

            ps.setInt(1, idCliente);   // FK: el cliente que quita el favorito
            ps.setInt(2, idProducto);  // FK: el producto que se quita

            return ps.executeUpdate() > 0; // true si eliminó exactamente 1 fila

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Verifica si un producto específico ya está en los favoritos de un cliente.
     * Usada para mostrar el botón "♥ Quitar" (si ya es favorito) o "♡ Agregar" (si no lo es).
     *
     * Llamada por: CatalogoServlet (para cada producto del listado) y FavoritosServlet
     *
     * @param idCliente  ID del cliente a verificar
     * @param idProducto ID del producto a verificar
     * @return true si el par existe en la tabla favoritos; false si no está guardado
     */
    public boolean esFavorito(int idCliente, int idProducto) {
        // "SELECT 1" es más eficiente que "SELECT *" porque solo verifica existencia, no trae datos
        // Si el par (id_cliente, id_producto) existe → el ResultSet tendrá 1 fila
        // Si no existe → el ResultSet estará vacío
        String sql = "SELECT 1 FROM favoritos WHERE id_cliente = ? AND id_producto = ?";

        try (Connection       conn = DBConnection.getConnection();
             PreparedStatement ps  = conn.prepareStatement(sql)) {

            ps.setInt(1, idCliente);
            ps.setInt(2, idProducto);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next(); // true si hay al menos 1 fila (= existe el favorito)
            }

        } catch (SQLException e) {
            e.printStackTrace();
            return false; // En caso de error, asumir que no es favorito
        }
    }

    /**
     * Obtiene la lista completa de productos favoritos de un cliente.
     * Hace JOIN con la tabla productos para traer todos los datos del producto en un solo query.
     * También trae la imagen principal de cada producto con una subconsulta.
     *
     * Llamada por: PerfilServlet.doGet() (tab "Mis Favoritos" del perfil)
     *
     * Query:
     * SELECT p.*, (subconsulta imagen) AS imagen_principal
     * FROM favoritos f
     * JOIN productos p ON f.id_producto = p.id_producto
     * WHERE f.id_cliente = ?
     *
     * @param idCliente ID del cliente cuyos favoritos se quieren listar
     * @return Lista de objetos Producto (con imagen_principal) que el cliente tiene como favoritos
     */
    public List<Producto> obtenerFavoritosPorCliente(int idCliente) {
        List<Producto> lista = new ArrayList<>();

        // JOIN con productos para obtener todos los datos del producto en un solo query
        // Subconsulta: busca la imagen marcada como principal (es_principal=1) en producto_imagenes
        // LIMIT 1 garantiza que la subconsulta devuelva solo 1 valor aunque haya múltiples imágenes
        String sql = "SELECT p.*, " +
                     "(SELECT ruta FROM producto_imagenes pi " +
                     " WHERE pi.id_producto = p.id_producto AND pi.es_principal = 1 LIMIT 1) AS imagen_principal " +
                     "FROM favoritos f " +
                     "JOIN productos p ON f.id_producto = p.id_producto " + // JOIN: relaciona favoritos con datos del producto
                     "WHERE f.id_cliente = ?"; // Solo los favoritos de este cliente específico

        try (Connection       conn = DBConnection.getConnection();
             PreparedStatement ps  = conn.prepareStatement(sql)) {

            ps.setInt(1, idCliente); // ID del cliente para el WHERE

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    // Mapear cada fila del ResultSet a un objeto Producto
                    Producto p = new Producto();
                    p.setIdProducto(rs.getInt("id_producto"));       // PK del producto
                    p.setNombre(rs.getString("nombre"));             // Nombre del artesanía
                    p.setDescripcion(rs.getString("descripcion"));   // Descripción larga
                    p.setPrecio(rs.getBigDecimal("precio"));         // Precio de venta
                    p.setStock(rs.getInt("stock"));                  // Stock disponible
                    p.setIdCategoria(rs.getInt("id_categoria"));     // FK a tabla categorias
                    p.setEstado(rs.getString("estado"));             // APROBADO/PENDIENTE/etc.
                    p.setImagenPrincipal(rs.getString("imagen_principal")); // Ruta de la imagen (puede ser null)
                    lista.add(p); // Agregar el producto mapeado a la lista
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return lista; // Lista vacía si el cliente no tiene favoritos o hubo error
    }
}
