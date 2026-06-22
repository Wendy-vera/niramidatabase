package com.nirami.controller;

import com.nirami.dao.CategoriaDAO;
import com.nirami.dao.ProductoDAO;
import com.nirami.dao.FavoritoDAO;
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

@WebServlet({"", "/catalogo", "/index"})
public class CatalogoServlet extends HttpServlet {
    private ProductoDAO productoDAO = new ProductoDAO();
    private CategoriaDAO categoriaDAO = new CategoriaDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String action = request.getParameter("action");

        if ("detalle".equals(action)) {
            // Mostrar detalle del producto
            String idParam = request.getParameter("id");
            if (idParam != null) {
                try {
                    int id = Integer.parseInt(idParam);
                    Producto producto = productoDAO.obtenerPorId(id);
                    if (producto != null) {
                        request.setAttribute("producto", producto);
                        request.getRequestDispatcher("/producto_detalle.jsp").forward(request, response);
                        return;
                    }
                } catch (NumberFormatException e) {
                    // Ignorar y volver al catálogo
                }
            }
        }

        // Mostrar listado de catálogo por defecto o por búsqueda/categoría
        String q = request.getParameter("q");
        String catIdStr = request.getParameter("categoria");
        List<Producto> productos;

        if (q != null && !q.trim().isEmpty()) {
            productos = productoDAO.buscarProductosPorNombre(q.trim());
            request.setAttribute("busqueda", q.trim());
        } else if (catIdStr != null && !catIdStr.trim().isEmpty()) {
            try {
                int catId = Integer.parseInt(catIdStr);
                productos = productoDAO.obtenerPorCategoria(catId);
                request.setAttribute("categoriaSel", catId);
            } catch (NumberFormatException e) {
                productos = productoDAO.obtenerProductosAprobados();
            }
        } else {
            productos = productoDAO.obtenerProductosAprobados();
        }
        List<Categoria> categorias = categoriaDAO.obtenerTodasActivas();
        
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
        
        request.setAttribute("productos", productos);
        request.setAttribute("categorias", categorias);
        request.setAttribute("favIds", favIds);
        
        request.getRequestDispatcher("/index.jsp").forward(request, response);
    }
}
