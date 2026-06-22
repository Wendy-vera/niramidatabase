package com.nirami.controller;

import com.nirami.dao.FavoritoDAO;
import com.nirami.model.Usuario;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

import java.util.List;
import com.nirami.model.Producto;

@WebServlet({"/favoritos/toggle", "/favoritos"})
public class FavoritosServlet extends HttpServlet {
    private FavoritoDAO favoritoDAO = new FavoritoDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
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

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession();
        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");

        if (usuario == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        int idProducto = Integer.parseInt(request.getParameter("idProducto"));
        String action = request.getParameter("action"); // agregar o quitar
        String referer = request.getHeader("Referer");

        if ("agregar".equals(action)) {
            favoritoDAO.agregarFavorito(usuario.getIdUsuario(), idProducto);
        } else if ("quitar".equals(action)) {
            favoritoDAO.quitarFavorito(usuario.getIdUsuario(), idProducto);
        }

        // Volver a la página desde la que se hizo la petición
        if (referer != null) {
            response.sendRedirect(referer);
        } else {
            response.sendRedirect(request.getContextPath() + "/catalogo");
        }
    }
}
