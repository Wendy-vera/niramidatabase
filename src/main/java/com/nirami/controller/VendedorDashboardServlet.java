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

@WebServlet("/vendedor/dashboard")
public class VendedorDashboardServlet extends HttpServlet {
    private ProductoDAO productoDAO = new ProductoDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession();
        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");

        if (usuario == null || !"VENDEDOR".equals(usuario.getTipoUsuario())) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        String estado = request.getParameter("estado");

        // Obtener productos del vendedor (filtrados por estado opcionalmente)
        List<Producto> misProductos = productoDAO.obtenerPorVendedor(usuario.getIdUsuario(), estado);
        request.setAttribute("misProductos", misProductos);
        request.setAttribute("estadoFiltro", estado);

        request.getRequestDispatcher("/vendedor_dashboard.jsp").forward(request, response);
    }
}
