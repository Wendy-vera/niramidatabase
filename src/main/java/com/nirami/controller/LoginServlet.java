package com.nirami.controller;

import com.nirami.dao.UsuarioDAO;
import com.nirami.model.Usuario;
import com.nirami.util.PasswordUtil;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

@WebServlet("/login")
public class LoginServlet extends HttpServlet {
    private UsuarioDAO usuarioDAO = new UsuarioDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.getRequestDispatcher("/login.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String correo = request.getParameter("correo");
        String contrasena = request.getParameter("contrasena");

        Usuario usuario = usuarioDAO.obtenerPorCorreo(correo);

        if (usuario != null && "ACTIVO".equals(usuario.getEstado())) {
            if (PasswordUtil.checkPassword(contrasena, usuario.getContrasenaHash())) {
                HttpSession session = request.getSession();
                session.setAttribute("usuarioLogueado", usuario);
                session.setAttribute("rol", usuario.getTipoUsuario());
                
                // Redirigir según el rol
                if ("ADMIN".equals(usuario.getTipoUsuario())) {
                    response.sendRedirect(request.getContextPath() + "/admin/dashboard");
                } else if ("VENDEDOR".equals(usuario.getTipoUsuario())) {
                    response.sendRedirect(request.getContextPath() + "/vendedor/dashboard");
                } else {
                    response.sendRedirect(request.getContextPath() + "/");
                }
                return;
            }
        }
        
        request.setAttribute("error", "Credenciales inválidas o cuenta inactiva");
        request.getRequestDispatcher("/login.jsp").forward(request, response);
    }
}
