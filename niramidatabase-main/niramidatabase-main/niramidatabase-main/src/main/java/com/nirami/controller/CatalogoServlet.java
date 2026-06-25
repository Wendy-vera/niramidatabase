package com.nirami.controller;

import com.nirami.dao.CategoriaDAO;
import com.nirami.dao.FavoritoDAO;
import com.nirami.dao.HistorialDAO;
import com.nirami.dao.ProductoDAO;
import com.nirami.model.Categoria;
import com.nirami.model.Producto;
import com.nirami.model.Usuario;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Servlet que gestiona el catálogo de productos visibles al público.
 *
 * <p><b>URLs mapeadas:</b> {@code ""} (raíz), {@code /catalogo}, {@code /index}</p>
 *
 * <p><b>Modos de operación (parámetro {@code action}):</b></p>
 * <ul>
 *   <li>{@code action=detalle&id=X} — Muestra el detalle de un producto específico.
 *       Si el usuario es CLIENTE, registra la acción {@code VER_PRODUCTO} en el historial.</li>
 *   <li>Sin action (listado) — Muestra todos los productos APROBADOS con filtros opcionales:
 *     <ul>
 *       <li>{@code q=texto} — Búsqueda por nombre (registra {@code BUSQUEDA} en historial si es CLIENTE)</li>
 *       <li>{@code categoria=X} — Filtrar por categoría</li>
 *     </ul>
 *   </li>
 * </ul>
 *
 * <p><b>Historial de usuario:</b> Este servlet registra acciones en {@code historial_usuario}
 * usando {@link HistorialDAO} cuando el usuario logueado es de tipo {@code CLIENTE}.
 * Los errores de historial NO interrumpen el flujo normal del usuario.</p>
 *
 * <p><b>Atributos de request que se pasan a la vista:</b>
 * <ul>
 *   <li>{@code productos} — Lista de {@link Producto} filtrados</li>
 *   <li>{@code categorias} — Lista de {@link Categoria} activas (para el menú lateral)</li>
 *   <li>{@code favIds} — Lista de IDs de productos favoritos del usuario logueado</li>
 *   <li>{@code busqueda} — Término de búsqueda (si aplica)</li>
 *   <li>{@code categoriaSel} — ID de categoría seleccionada (si aplica)</li>
 *   <li>{@code producto} — Objeto {@link Producto} específico (en modo detalle)</li>
 * </ul>
 * </p>
 *
 * @author Nirami Dev Team
 * @version 1.1
 * @see HistorialDAO
 * @see ProductoDAO
 */
@WebServlet({"", "/catalogo", "/index"})
public class CatalogoServlet extends HttpServlet {

    /** DAO para consultar el catálogo de productos de la BD. */
    private ProductoDAO productoDAO = new ProductoDAO();

    /** DAO para obtener las categorías activas del sistema. */
    private CategoriaDAO categoriaDAO = new CategoriaDAO();

    /** DAO para registrar acciones del usuario en el historial. */
    private HistorialDAO historialDAO = new HistorialDAO();

    /**
     * Gestiona todas las solicitudes GET al catálogo.
     *
     * <p>Determina el modo de operación según el parámetro {@code action}:
     * si es {@code "detalle"}, muestra un producto individual; en caso contrario,
     * muestra el listado filtrado del catálogo.</p>
     *
     * @param request  Solicitud HTTP con parámetros opcionales: {@code action}, {@code id}, {@code q}, {@code categoria}
     * @param response Respuesta HTTP
     * @throws ServletException si hay error al delegar a la JSP
     * @throws IOException      si hay error de I/O
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String action = request.getParameter("action");

        if ("detalle".equals(action)) {
            mostrarDetalleProducto(request, response);
        } else {
            mostrarCatalogo(request, response);
        }
    }

    /**
     * Muestra el detalle de un producto y registra la acción {@code VER_PRODUCTO}
     * en el historial si el visitante es un CLIENTE autenticado.
     *
     * @param request  Request con parámetro {@code id} (ID del producto)
     * @param response Response HTTP
     * @throws ServletException si hay error al delegar a la JSP
     * @throws IOException      si hay error de I/O o si el producto no existe
     */
    private void mostrarDetalleProducto(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String idParam = request.getParameter("id");
        if (idParam != null) {
            try {
                int id = Integer.parseInt(idParam);
                Producto producto = productoDAO.obtenerPorId(id);
                if (producto != null) {
                    // ── Registrar historial ──
                    registrarHistorialSiEsCliente(request, id, "VER_PRODUCTO",
                            null, "Vio el producto: " + producto.getNombre());

                    request.setAttribute("producto", producto);
                    request.getRequestDispatcher("/producto_detalle.jsp").forward(request, response);
                    return;
                }
            } catch (NumberFormatException e) {
                // ID inválido → cae al catálogo general
            }
        }
        // Si el producto no existe o el ID es inválido, redirigir al catálogo
        response.sendRedirect(request.getContextPath() + "/catalogo");
    }

    /**
     * Muestra el listado del catálogo con filtros opcionales por búsqueda o categoría.
     * Registra {@code BUSQUEDA} en el historial si el usuario busca un término.
     *
     * @param request  Request con parámetros opcionales: {@code q} (búsqueda), {@code categoria}
     * @param response Response HTTP
     * @throws ServletException si hay error al delegar a la JSP
     * @throws IOException      si hay error de I/O
     */
    private void mostrarCatalogo(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String q         = request.getParameter("q");
        String catIdStr  = request.getParameter("categoria");
        List<Producto> productos;

        if (q != null && !q.trim().isEmpty()) {
            // Modo búsqueda por texto
            productos = productoDAO.buscarProductosPorNombre(q.trim());
            request.setAttribute("busqueda", q.trim());

            // ── Registrar BUSQUEDA en historial ──
            registrarHistorialSiEsCliente(request, null, "BUSQUEDA",
                    q.trim(), "Buscó: " + q.trim());

        } else if (catIdStr != null && !catIdStr.trim().isEmpty()) {
            // Modo filtro por categoría
            try {
                int catId = Integer.parseInt(catIdStr);
                productos = productoDAO.obtenerPorCategoria(catId);
                request.setAttribute("categoriaSel", catId);
            } catch (NumberFormatException e) {
                productos = productoDAO.obtenerProductosAprobados();
            }
        } else {
            // Sin filtros: mostrar todos los productos aprobados
            productos = productoDAO.obtenerProductosAprobados();
        }

        // Obtener categorías para el panel de filtrado lateral
        List<Categoria> categorias = categoriaDAO.obtenerTodasActivas();

        // Obtener IDs de favoritos del usuario logueado (para marcar el ♥ en los cards)
        List<Integer> favIds = new ArrayList<>();
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute("usuarioLogueado") != null) {
            Usuario u = (Usuario) session.getAttribute("usuarioLogueado");
            FavoritoDAO favDAO = new FavoritoDAO();
            List<Producto> favs = favDAO.obtenerFavoritosPorCliente(u.getIdUsuario());
            for (Producto f : favs) {
                favIds.add(f.getIdProducto());
            }
        }

        request.setAttribute("productos",  productos);
        request.setAttribute("categorias", categorias);
        request.setAttribute("favIds",     favIds);

        request.getRequestDispatcher("/index.jsp").forward(request, response);
    }

    /**
     * Método auxiliar que registra una acción en el historial SOLO si el usuario
     * logueado es de tipo {@code CLIENTE}. Los VENDEDORES y ADMINS no generan historial.
     *
     * <p>Los errores al registrar historial son silenciosos (no interrumpen el flujo).</p>
     *
     * @param request          Solicitud HTTP (para leer sesión e IP)
     * @param idProducto       ID del producto involucrado (null para BUSQUEDA)
     * @param accion           Tipo de acción: {@code VER_PRODUCTO}, {@code BUSQUEDA}, etc.
     * @param terminoBusqueda  Texto buscado (solo para BUSQUEDA, null en otros casos)
     * @param detalle          Texto descriptivo adicional
     */
    private void registrarHistorialSiEsCliente(HttpServletRequest request, Integer idProducto,
                                                String accion, String terminoBusqueda, String detalle) {
        HttpSession session = request.getSession(false);
        if (session == null) return;

        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
        // Solo registrar si es CLIENTE (vendedores y admins no generan historial)
        if (usuario == null || !"CLIENTE".equals(usuario.getTipoUsuario())) return;

        // Obtener IP real (considera proxy reverso)
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) {
            ip = request.getRemoteAddr();
        }

        historialDAO.registrarAccion(usuario.getIdUsuario(), idProducto,
                accion, terminoBusqueda, detalle, ip);
    }
}
