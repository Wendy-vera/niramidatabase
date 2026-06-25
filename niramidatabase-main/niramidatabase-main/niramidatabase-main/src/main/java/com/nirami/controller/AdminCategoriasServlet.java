package com.nirami.controller;

import com.nirami.dao.CategoriaDAO;
import com.nirami.model.Categoria;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

/**
 * Servlet que gestiona la administración de categorías de productos.
 * URL: /admin/categorias
 * GET  → muestra la lista de todas las categorías (admin_categorias.jsp)
 * POST → crea, edita o cambia el estado de una categoría
 *
 * Conexiones:
 *   → CategoriaDAO (CRUD sobre la tabla `categorias`)
 *   → admin_categorias.jsp (vista de administración de categorías)
 *   → ProductoVendedorServlet (las categorías activas aparecen en el formulario de producto)
 *   → CatalogoServlet (las categorías activas aparecen como filtros en el catálogo)
 */
@WebServlet("/admin/categorias") // Responde a la URL /admin/categorias
public class AdminCategoriasServlet extends HttpServlet {

    // DAO para todas las operaciones sobre la tabla `categorias`
    private CategoriaDAO categoriaDAO = new CategoriaDAO();

    /**
     * GET /admin/categorias → Carga y muestra todas las categorías (activas, inactivas, eliminadas).
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // Obtener TODAS las categorías sin filtrar estado (el admin ve todas)
        // Conecta con: SELECT * FROM categorias ORDER BY nombre ASC
        List<Categoria> categorias = categoriaDAO.obtenerTodas();

        // Pasar la lista al request para que admin_categorias.jsp la renderice
        request.setAttribute("categorias", categorias);

        // Delegar la respuesta a la vista JSP de administración de categorías
        request.getRequestDispatcher("/admin_categorias.jsp").forward(request, response);
    }

    /**
     * POST /admin/categorias → Procesa la acción indicada por el formulario.
     * Parámetro "accion" define qué hacer: "crear", "editar" o "estado"
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // Leer la acción del campo hidden del formulario
        String accion = request.getParameter("accion"); // "crear", "editar" o "estado"

        try {
            if ("crear".equals(accion)) {
                /* ── Crear nueva categoría ──
                 * Recibe nombre y descripción del formulario
                 * Llama al DAO para INSERT INTO categorias (nombre, descripcion)
                 */
                Categoria c = new Categoria();
                c.setNombre(request.getParameter("nombre"));           // Nombre de la nueva categoría
                c.setDescripcion(request.getParameter("descripcion")); // Descripción opcional
                categoriaDAO.agregarCategoria(c); // INSERT en la tabla categorias

            } else if ("editar".equals(accion)) {
                /* ── Editar una categoría existente ──
                 * Recibe idCategoria, nombre y descripción nuevos
                 * Llama al DAO para UPDATE categorias SET ... WHERE id_categoria = ?
                 */
                Categoria c = new Categoria();
                c.setIdCategoria(Integer.parseInt(request.getParameter("idCategoria"))); // PK de la categoría
                c.setNombre(request.getParameter("nombre"));
                c.setDescripcion(request.getParameter("descripcion"));
                categoriaDAO.actualizarCategoria(c); // UPDATE en la tabla categorias

            } else if ("estado".equals(accion)) {
                /* ── Cambiar el estado de una categoría (ACTIVA, INACTIVA, ELIMINADA) ──
                 * ACTIVA   → aparece en el catálogo y en los formularios de productos
                 * INACTIVA → no aparece en el catálogo pero mantiene sus productos
                 * ELIMINADA → como borrado lógico, solo permitido si no tiene productos
                 */
                int    id     = Integer.parseInt(request.getParameter("idCategoria"));
                String estado = request.getParameter("nuevoEstado"); // Estado destino

                // PROTECCIÓN: Si van a eliminar la categoría, verificar que no tenga productos
                // tieneProductos() hace: SELECT COUNT(*) FROM productos WHERE id_categoria = ?
                if ("ELIMINADA".equals(estado)) {
                    if (categoriaDAO.tieneProductos(id)) {
                        // Hay productos en esta categoría → no se puede eliminar
                        request.setAttribute("error",
                            "No se puede eliminar la categoría porque ya tiene productos asociados.");
                        doGet(request, response); // Volver al listado con el error
                        return; // Detener aquí, no ejecutar el cambio de estado
                    }
                }

                // Cambiar el estado en la BD
                // UPDATE categorias SET estado = ? WHERE id_categoria = ?
                categoriaDAO.cambiarEstado(id, estado);
            }

        } catch (Exception e) {
            // Capturar cualquier error (p.ej. NumberFormatException si el ID es inválido)
            e.printStackTrace();
        }

        // En todos los casos exitosos, redirigir al GET para recargar la lista actualizada
        // Patrón POST-Redirect-GET: evita reenvío del formulario al refrescar
        response.sendRedirect(request.getContextPath() + "/admin/categorias");
    }
}
