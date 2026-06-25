package com.nirami.controller;

import com.nirami.dao.ProductoDAO;
import com.nirami.dao.UsuarioDAO;
import com.nirami.model.Producto;
import com.nirami.model.Usuario;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

/**
 * Servlet de administración que lista TODOS los productos del sistema con filtros.
 * URL: /admin/productos
 * GET  → lista productos con filtros de estado, fecha y vendedor
 * POST → suspende o elimina (lógicamente) un producto
 *
 * Diferencia con VendedorDashboardServlet:
 *   - VendedorDashboardServlet: ve solo los productos del vendedor logueado
 *   - AdminProductosServlet: ve los productos de TODOS los vendedores con más filtros
 *
 * Conexiones:
 *   → ProductoDAO.obtenerTodosAdmin()  (consulta productos con filtros opcionales)
 *   → ProductoDAO.tieneCompras()       (verificar antes de eliminar)
 *   → ProductoDAO.actualizarEstado()   (SUSPENDIDO o ELIMINADO)
 *   → UsuarioDAO.obtenerUsuariosPorTipo() (lista de vendedores para el filtro)
 *   → admin_productos.jsp              (vista de gestión de productos)
 */
@WebServlet("/admin/productos") // Responde a /admin/productos
public class AdminProductosServlet extends HttpServlet {

    // DAO para consultar y actualizar productos en la BD
    // Conecta con tablas: productos, vendedor_producto, producto_imagenes, categorias
    private ProductoDAO productoDAO = new ProductoDAO();

    // DAO para obtener la lista de vendedores (usada en el <select> de filtro)
    // Conecta con: SELECT * FROM usuarios WHERE tipo_usuario = 'VENDEDOR'
    private UsuarioDAO usuarioDAO = new UsuarioDAO();

    /**
     * GET /admin/productos → Lista productos con filtros opcionales.
     * Parámetros opcionales: ?estado=X&fecha=YYYY-MM-DD&idVendedor=Y
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // Leer los filtros opcionales de la URL (null si no se proporcionan)
        String estadoFiltro  = request.getParameter("estado");     // PENDIENTE, APROBADO, RECHAZADO...
        String fechaFiltro   = request.getParameter("fecha");      // Filtro por fecha (YYYY-MM-DD)
        String idVendedorStr = request.getParameter("idVendedor"); // Filtrar por vendedor específico

        // Convertir idVendedor de String a Integer (o null si no se filtra)
        Integer idVendedorFiltro = null;
        if (idVendedorStr != null && !idVendedorStr.isEmpty()) {
            idVendedorFiltro = Integer.parseInt(idVendedorStr);
        }

        /* Consultar productos con los filtros aplicados.
         * obtenerTodosAdmin() acepta null en cualquier parámetro para "sin filtro"
         * La query base: SELECT p.*, u.nombre as vendedor_nombre, c.nombre as categoria_nombre
         *                FROM productos p JOIN vendedor_producto vp JOIN usuarios u JOIN categorias c
         *                WHERE [filtros opcionales con AND]
         *                ORDER BY p.fecha_creacion DESC
         */
        List<Producto> productos = productoDAO.obtenerTodosAdmin(estadoFiltro, fechaFiltro, idVendedorFiltro);

        // Traer todos los vendedores para el <select> de filtro por vendedor en la JSP
        List<Usuario> vendedores = usuarioDAO.obtenerUsuariosPorTipo("VENDEDOR");

        // Pasar los datos al request para que la JSP los renderice
        request.setAttribute("productos",  productos);  // Lista de productos → tabla
        request.setAttribute("vendedores", vendedores); // Lista de vendedores → <select>

        // Delegar la renderización a la vista de admin de productos
        request.getRequestDispatcher("/admin_productos.jsp").forward(request, response);
    }

    /**
     * POST /admin/productos → Suspende o elimina un producto.
     * Parámetros: idProducto (int), accion ("suspender" | "eliminar")
     *
     * Regla de negocio IMPORTANTE:
     *   - SUSPENDIDO: oculta el producto del catálogo sin eliminar nada
     *   - ELIMINADO: solo permitido si el producto NO tiene compras registradas
     *               Si tiene compras, el admin debe suspenderlo en su lugar
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // Leer la acción y el ID del producto del formulario
        String accion    = request.getParameter("accion");                          // "suspender" o "eliminar"
        int idProducto   = Integer.parseInt(request.getParameter("idProducto"));    // ID del producto

        if ("eliminar".equals(accion)) {
            /* ── Eliminar producto (borrado lógico) ──
             * Antes de eliminar, verificar que el producto no tenga compras registradas.
             * tieneCompras() ejecuta: SELECT COUNT(*) FROM orden_detalle WHERE id_producto = ?
             * Si tiene compras, los datos históricos no se pueden borrar (integridad referencial)
             */
            if (productoDAO.tieneCompras(idProducto)) {
                // Tiene compras → no se puede eliminar; mostrar error y recargar la lista
                request.setAttribute("error",
                    "No se puede eliminar el producto porque tiene compras. Puedes suspenderlo en su lugar.");
                doGet(request, response); // Re-usar el GET para mostrar el listado con el error
                return; // Detener el flujo, no ejecutar la eliminación
            } else {
                // Sin compras → eliminar lógicamente (UPDATE productos SET estado = 'ELIMINADO')
                // No se elimina el registro físicamente para mantener la trazabilidad
                productoDAO.actualizarEstado(idProducto, "ELIMINADO");
            }

        } else if ("suspender".equals(accion)) {
            /* ── Suspender producto ──
             * El producto deja de aparecer en el catálogo pero mantiene todos sus datos.
             * Los pedidos anteriores de este producto no se ven afectados.
             * UPDATE productos SET estado = 'SUSPENDIDO' WHERE id_producto = ?
             */
            productoDAO.actualizarEstado(idProducto, "SUSPENDIDO");
        }

        // Redirigir al GET para mostrar la lista actualizada (patrón POST-Redirect-GET)
        response.sendRedirect(request.getContextPath() + "/admin/productos");
    }
}
