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

@WebServlet("/admin/productos")
public class AdminProductosServlet extends HttpServlet {
    private ProductoDAO productoDAO = new ProductoDAO();
    private UsuarioDAO usuarioDAO = new UsuarioDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String estadoFiltro = request.getParameter("estado");
        String fechaFiltro = request.getParameter("fecha");
        String idVendedorStr = request.getParameter("idVendedor");
        
        Integer idVendedorFiltro = null;
        if (idVendedorStr != null && !idVendedorStr.isEmpty()) {
            idVendedorFiltro = Integer.parseInt(idVendedorStr);
        }

        List<Producto> productos = productoDAO.obtenerTodosAdmin(estadoFiltro, fechaFiltro, idVendedorFiltro);
        List<Usuario> vendedores = usuarioDAO.obtenerUsuariosPorTipo("VENDEDOR");

        request.setAttribute("productos", productos);
        request.setAttribute("vendedores", vendedores);
        request.getRequestDispatcher("/admin_productos.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String accion = request.getParameter("accion");
        int idProducto = Integer.parseInt(request.getParameter("idProducto"));

        if ("eliminar".equals(accion)) {
            if (productoDAO.tieneCompras(idProducto)) {
                request.setAttribute("error", "No se puede eliminar el producto porque tiene compras. Puedes suspenderlo en su lugar.");
                doGet(request, response);
                return;
            } else {
                productoDAO.actualizarEstado(idProducto, "ELIMINADO");
            }
        } else if ("suspender".equals(accion)) {
            productoDAO.actualizarEstado(idProducto, "SUSPENDIDO");
        }
        
        response.sendRedirect(request.getContextPath() + "/admin/productos");
    }
}
