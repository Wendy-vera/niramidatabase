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

@WebServlet("/admin/dashboard")
public class AdminDashboardServlet extends HttpServlet {
    private ProductoDAO productoDAO = new ProductoDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession();
        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");

        if (usuario == null || !"ADMIN".equals(usuario.getTipoUsuario())) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        // Obtener solo productos pendientes
        List<Producto> productosPendientes = productoDAO.obtenerProductosPendientes();
        request.setAttribute("productosPendientes", productosPendientes);

        request.getRequestDispatcher("/admin_dashboard.jsp").forward(request, response);
    }
}
