package com.nirami.controller;

import com.nirami.dao.ProductoDAO;
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
 * Servlet que gestiona el panel principal (dashboard) del vendedor.
 * URL: /vendedor/dashboard
 * GET → muestra el inventario de productos del vendedor autenticado
 *
 * Este es el "home" del vendedor. Desde aquí puede ver sus productos,
 * filtrarlos por estado y acceder a las acciones de crear/editar.
 *
 * Conexiones:
 *   → session.getAttribute("usuarioLogueado") (leer el vendedor logueado; puesto por LoginServlet)
 *   → ProductoDAO.obtenerPorVendedor()         (consulta productos del vendedor en la BD)
 *   → vendedor_dashboard.jsp                   (vista del panel del vendedor)
 *   → ProductoVendedorServlet                  (al hacer clic en "Nuevo Producto" o "Editar")
 */
@WebServlet("/vendedor/dashboard") // Responde a /vendedor/dashboard
public class VendedorDashboardServlet extends HttpServlet {

    // DAO para consultar los productos del vendedor en la BD
    // Conecta con tabla: productos + vendedor_producto (JOIN para obtener stock_local)
    private ProductoDAO productoDAO = new ProductoDAO();

    /**
     * GET /vendedor/dashboard → Carga y muestra los productos del vendedor.
     * Permite filtrar por estado: PENDIENTE, APROBADO, RECHAZADO, SUSPENDIDO
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // Leer la sesión para identificar quién es el vendedor que está navegando
        HttpSession session = request.getSession();

        // Obtener el objeto Usuario de la sesión (fue guardado por LoginServlet al hacer login)
        // Contiene: id_usuario, nombre, tipo_usuario, etc.
        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");

        // Verificar que sea un vendedor autenticado (doble verificación; LoginFilter ya filtra)
        if (usuario == null || !"VENDEDOR".equals(usuario.getTipoUsuario())) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        // Leer el filtro de estado de la URL (?estado=APROBADO, ?estado=PENDIENTE, etc.)
        // Si no se provee el parámetro, estado será null → se traen TODOS los productos
        String estado = request.getParameter("estado");

        /* Consultar los productos del vendedor específico
         * La query en ProductoDAO filtra por id_vendedor = usuario.getIdUsuario()
         * y opcionalmente filtra por estado si no es null
         * Tablas consultadas: productos JOIN vendedor_producto WHERE id_vendedor = ?
         */
        List<Producto> misProductos = productoDAO.obtenerPorVendedor(usuario.getIdUsuario(), estado);

        // Pasar los productos al request para que vendedor_dashboard.jsp los muestre en una tabla
        request.setAttribute("misProductos", misProductos);

        // Pasar el filtro activo para que la JSP marque el botón correcto como activo
        request.setAttribute("estadoFiltro", estado);

        // Delegar la renderización al panel del vendedor
        request.getRequestDispatcher("/vendedor_dashboard.jsp").forward(request, response);
    }
}
