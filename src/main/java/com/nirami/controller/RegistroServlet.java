package com.nirami.controller;

import com.nirami.dao.UsuarioDAO;
import com.nirami.model.Usuario;
import com.nirami.util.PasswordUtil;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet("/registro")
public class RegistroServlet extends HttpServlet {
    private UsuarioDAO usuarioDAO = new UsuarioDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.getRequestDispatcher("/registro.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String nombre = request.getParameter("nombre");
        String correo = request.getParameter("correo");
        String telefono = request.getParameter("telefono");
        String contrasena = request.getParameter("contrasena");
        String confirmarContrasena = request.getParameter("confirmar_contrasena");
        String tipoUsuario = request.getParameter("tipoUsuario"); // CLIENTE o VENDEDOR
        
        // Validación básica
        if(nombre == null || correo == null || contrasena == null || confirmarContrasena == null || tipoUsuario == null ||
           nombre.trim().isEmpty() || correo.trim().isEmpty() || contrasena.trim().isEmpty() || confirmarContrasena.trim().isEmpty()) {
            request.setAttribute("error", "Todos los campos obligatorios deben ser completados.");
            request.getRequestDispatcher("/registro.jsp").forward(request, response);
            return;
        }

        // Validación estricta con Expresiones Regulares
        if (nombre.trim().length() < 3) {
            request.setAttribute("error", "El nombre debe tener al menos 3 caracteres.");
            request.getRequestDispatcher("/registro.jsp").forward(request, response);
            return;
        }
        
        if (!contrasena.equals(confirmarContrasena)) {
            request.setAttribute("error", "Las contraseñas no coinciden.");
            request.getRequestDispatcher("/registro.jsp").forward(request, response);
            return;
        }
        if (!correo.matches("^[^@]+@[^@]+\\.[a-zA-Z]{2,}$")) {
            request.setAttribute("error", "El correo ingresado no es válido.");
            request.getRequestDispatcher("/registro.jsp").forward(request, response);
            return;
        }
        if (!contrasena.matches("^(?=.*\\d)(?=.*[a-z])(?=.*[A-Z]).{8,}$")) {
            request.setAttribute("error", "La contraseña debe tener mínimo 8 caracteres, una mayúscula, una minúscula y un número.");
            request.getRequestDispatcher("/registro.jsp").forward(request, response);
            return;
        }
        if (telefono != null && !telefono.trim().isEmpty() && !telefono.matches("^\\d{10}$")) {
            request.setAttribute("error", "El teléfono debe tener exactamente 10 dígitos numéricos.");
            request.getRequestDispatcher("/registro.jsp").forward(request, response);
            return;
        }

        Usuario nuevoUsuario = new Usuario();
        nuevoUsuario.setNombre(nombre.trim());
        nuevoUsuario.setCorreo(correo.trim());
        nuevoUsuario.setTelefono(telefono != null ? telefono.trim() : null);
        nuevoUsuario.setContrasenaHash(PasswordUtil.hashPassword(contrasena));
        
        // Evitar que cualquiera se registre como ADMIN desde el form público
        if("ADMIN".equals(tipoUsuario)) {
            tipoUsuario = "CLIENTE";
        }
        nuevoUsuario.setTipoUsuario(tipoUsuario);

        String extraInfo = "";
        if("VENDEDOR".equals(tipoUsuario)) {
            extraInfo = request.getParameter("cuentaBancaria");
            if(extraInfo == null || !extraInfo.matches("^\\d{10}$")) {
                request.setAttribute("error", "La cuenta bancaria es requerida para vendedores y debe tener exactamente 10 dígitos numéricos.");
                request.getRequestDispatcher("/registro.jsp").forward(request, response);
                return;
            }
        }

        boolean exito = usuarioDAO.registrarUsuario(nuevoUsuario, extraInfo);

        if (exito) {
            response.sendRedirect(request.getContextPath() + "/login?registrado=true");
        } else {
            request.setAttribute("error", "Error al registrar. Es posible que el correo ya exista.");
            request.getRequestDispatcher("/registro.jsp").forward(request, response);
        }
    }
}
