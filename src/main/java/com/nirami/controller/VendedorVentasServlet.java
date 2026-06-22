package com.nirami.controller;

import com.nirami.dao.OrdenDAO;
import com.nirami.dao.ProductoDAO;
import com.nirami.model.Producto;
import com.nirami.model.Usuario;
import com.nirami.model.VentaAdminDTO;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.List;

@WebServlet("/vendedor/ventas")
public class VendedorVentasServlet extends HttpServlet {
    private OrdenDAO ordenDAO = new OrdenDAO();
    private ProductoDAO productoDAO = new ProductoDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        Usuario vendedor = (Usuario) session.getAttribute("usuarioLogueado");

        String idProductoStr = request.getParameter("idProducto");
        Integer idProducto = (idProductoStr != null && !idProductoStr.isEmpty()) ? Integer.parseInt(idProductoStr) : null;

        List<VentaAdminDTO> ventas = ordenDAO.obtenerVentasAdmin(idProducto, vendedor.getIdUsuario(), null);
        List<Producto> productos = productoDAO.obtenerPorVendedor(vendedor.getIdUsuario());

        request.setAttribute("ventas", ventas);
        request.setAttribute("productos", productos);
        request.getRequestDispatcher("/vendedor_ventas.jsp").forward(request, response);
    }
}
