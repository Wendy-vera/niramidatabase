package com.nirami.controller;

import com.nirami.dao.UsuarioDAO;
import com.nirami.model.Usuario;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

@WebServlet({"/admin/vendedores", "/admin/clientes"})
public class AdminUsuariosServlet extends HttpServlet {
    private UsuarioDAO usuarioDAO = new UsuarioDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String uri = request.getRequestURI();
        String tipo = uri.endsWith("vendedores") ? "VENDEDOR" : "CLIENTE";
        
        List<Usuario> usuarios = usuarioDAO.obtenerUsuariosPorTipo(tipo);
        
        request.setAttribute("usuarios", usuarios);
        request.setAttribute("tipoVista", tipo);
        
        request.getRequestDispatcher("/admin_usuarios.jsp").forward(request, response);
    }
}
