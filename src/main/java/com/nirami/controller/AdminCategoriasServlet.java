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

@WebServlet("/admin/categorias")
public class AdminCategoriasServlet extends HttpServlet {
    private CategoriaDAO categoriaDAO = new CategoriaDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        List<Categoria> categorias = categoriaDAO.obtenerTodas();
        request.setAttribute("categorias", categorias);
        request.getRequestDispatcher("/admin_categorias.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String accion = request.getParameter("accion");
        
        try {
            if ("crear".equals(accion)) {
                Categoria c = new Categoria();
                c.setNombre(request.getParameter("nombre"));
                c.setDescripcion(request.getParameter("descripcion"));
                categoriaDAO.agregarCategoria(c);
            } else if ("editar".equals(accion)) {
                Categoria c = new Categoria();
                c.setIdCategoria(Integer.parseInt(request.getParameter("idCategoria")));
                c.setNombre(request.getParameter("nombre"));
                c.setDescripcion(request.getParameter("descripcion"));
                categoriaDAO.actualizarCategoria(c);
            } else if ("estado".equals(accion)) {
                int id = Integer.parseInt(request.getParameter("idCategoria"));
                String estado = request.getParameter("nuevoEstado");
                
                if ("ELIMINADA".equals(estado)) {
                    if (categoriaDAO.tieneProductos(id)) {
                        request.setAttribute("error", "No se puede eliminar la categoría porque ya tiene productos asociados.");
                        doGet(request, response);
                        return;
                    }
                }
                categoriaDAO.cambiarEstado(id, estado);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        response.sendRedirect(request.getContextPath() + "/admin/categorias");
    }
}
