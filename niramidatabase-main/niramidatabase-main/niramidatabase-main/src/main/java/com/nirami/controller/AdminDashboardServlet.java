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
 * Servlet que gestiona el panel de administración principal (revisión de productos).
 * URL: /admin/dashboard
 * GET → muestra la lista de productos en estado PENDIENTE para que el admin los revise
 *
 * Conexiones:
 *   → session.getAttribute("usuarioLogueado") (verifica que sea un ADMIN)
 *   → ProductoDAO.obtenerProductosPendientes() (trae productos en estado PENDIENTE de BD)
 *   → admin_dashboard.jsp                      (vista del panel del administrador)
 *   → RevisionProductoServlet                  (cuando el admin clica en un producto para revisarlo)
 */
@WebServlet("/admin/dashboard") // Responde a la URL /admin/dashboard
public class AdminDashboardServlet extends HttpServlet {

    // DAO para consultar los productos pendientes de revisión
    // Conecta con: SELECT * FROM productos WHERE estado = 'PENDIENTE'
    private ProductoDAO productoDAO = new ProductoDAO();

    /**
     * GET /admin/dashboard → Carga los productos pendientes de aprobación.
     * Solo usuarios ADMIN pueden acceder; otros son redirigidos al login.
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // Leer la sesión actual para identificar al usuario logueado
        HttpSession session = request.getSession();

        // Obtener el objeto Usuario de la sesión (guardado por LoginServlet al autenticar)
        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");

        // SEGURIDAD: Verificar que existe sesión y que el usuario es ADMIN
        // LoginFilter ya filtra usuarios no logueados, pero esta verificación de rol
        // protege contra clientes o vendedores que conozcan la URL /admin/dashboard
        if (usuario == null || !"ADMIN".equals(usuario.getTipoUsuario())) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        // Consultar todos los productos en estado PENDIENTE
        // Estos son los que los vendedores publicaron y aún no han sido aprobados o rechazados
        // La query en ProductoDAO: SELECT * FROM productos WHERE estado = 'PENDIENTE'
        List<Producto> productosPendientes = productoDAO.obtenerProductosPendientes();

        // Pasar la lista de pendientes al request para que admin_dashboard.jsp los liste
        // La JSP mostrará un link "Revisar" por cada producto, que lleva a RevisionProductoServlet
        request.setAttribute("productosPendientes", productosPendientes);

        // Delegar la renderización al panel de administración
        request.getRequestDispatcher("/admin_dashboard.jsp").forward(request, response);
    }
}
