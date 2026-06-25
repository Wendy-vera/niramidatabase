package com.nirami.controller;

import com.nirami.dao.FavoritoDAO;
import com.nirami.dao.HistorialDAO;
import com.nirami.model.Producto;
import com.nirami.model.Usuario;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.List;

/**
 * Servlet que gestiona la lista de favoritos de un cliente.
 *
 * <p><b>URLs mapeadas:</b> {@code /favoritos/toggle}, {@code /favoritos}</p>
 *
 * <p><b>Acceso:</b> Restringido a usuarios autenticados. Los no autenticados
 * son redirigidos a {@code /login}.</p>
 *
 * <p><b>Método GET ({@code /favoritos}):</b><br>
 * Muestra la lista de productos favoritos del usuario logueado.</p>
 *
 * <p><b>Método POST ({@code /favoritos/toggle}):</b><br>
 * Agrega o quita un producto de favoritos. Registra la acción en el historial
 * ({@code AGREGAR_FAVORITO} o {@code QUITAR_FAVORITO}) si el usuario es CLIENTE.
 * Al terminar, redirige de vuelta a la página anterior (usando el header {@code Referer}).</p>
 *
 * <p><b>Parámetros POST esperados:</b>
 * <ul>
 *   <li>{@code idProducto} — ID del producto a agregar/quitar</li>
 *   <li>{@code action} — {@code "agregar"} o {@code "quitar"}</li>
 * </ul>
 * </p>
 *
 * @author Nirami Dev Team
 * @version 1.1
 * @see FavoritoDAO
 * @see HistorialDAO
 */
@WebServlet({"/favoritos/toggle", "/favoritos"})
public class FavoritosServlet extends HttpServlet {

    /** DAO para operaciones de favoritos en la BD. */
    private FavoritoDAO favoritoDAO = new FavoritoDAO();

    /** DAO para registrar acciones de favoritos en el historial del usuario. */
    private HistorialDAO historialDAO = new HistorialDAO();

    /**
     * Muestra la lista de favoritos del usuario autenticado.
     *
     * @param request  Solicitud HTTP
     * @param response Respuesta HTTP
     * @throws ServletException si hay error al delegar a la JSP
     * @throws IOException      si hay error de I/O
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession();
        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");

        if (usuario == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        List<Producto> favoritos = favoritoDAO.obtenerFavoritosPorCliente(usuario.getIdUsuario());
        request.setAttribute("favoritos", favoritos);
        request.getRequestDispatcher("/favoritos.jsp").forward(request, response);
    }

    /**
     * Agrega o quita un producto de la lista de favoritos del usuario.
     *
     * <p>Registra la acción en {@code historial_usuario} via {@link HistorialDAO}
     * si el usuario es de tipo {@code CLIENTE}.</p>
     *
     * <p><b>Parámetros requeridos:</b>
     * <ul>
     *   <li>{@code idProducto} — ID del producto a operar</li>
     *   <li>{@code action} — {@code "agregar"} para añadir, {@code "quitar"} para eliminar</li>
     * </ul>
     * </p>
     *
     * <p>Redirige de vuelta a la página anterior usando el header {@code Referer}.
     * Si no hay Referer, redirige al catálogo.</p>
     *
     * @param request  Solicitud HTTP con parámetros del formulario
     * @param response Respuesta HTTP
     * @throws ServletException si hay error de Servlet
     * @throws IOException      si hay error de I/O en la redirección
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession();
        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");

        if (usuario == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        int    idProducto = Integer.parseInt(request.getParameter("idProducto"));
        String action     = request.getParameter("action"); // "agregar" o "quitar"
        String referer    = request.getHeader("Referer");

        // Obtener IP real (considera proxy reverso)
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) {
            ip = request.getRemoteAddr();
        }

        if ("agregar".equals(action)) {
            favoritoDAO.agregarFavorito(usuario.getIdUsuario(), idProducto);

            // ── Registrar AGREGAR_FAVORITO en historial ──
            if ("CLIENTE".equals(usuario.getTipoUsuario())) {
                historialDAO.registrarAccion(usuario.getIdUsuario(), idProducto,
                        "AGREGAR_FAVORITO", null, "Agregó a favoritos el producto #" + idProducto, ip);
            }

        } else if ("quitar".equals(action)) {
            favoritoDAO.quitarFavorito(usuario.getIdUsuario(), idProducto);

            // ── Registrar QUITAR_FAVORITO en historial ──
            if ("CLIENTE".equals(usuario.getTipoUsuario())) {
                historialDAO.registrarAccion(usuario.getIdUsuario(), idProducto,
                        "QUITAR_FAVORITO", null, "Quitó de favoritos el producto #" + idProducto, ip);
            }
        }

        // Volver a la página de origen; si no hay Referer, ir al catálogo
        if (referer != null) {
            response.sendRedirect(referer);
        } else {
            response.sendRedirect(request.getContextPath() + "/catalogo");
        }
    }
}
