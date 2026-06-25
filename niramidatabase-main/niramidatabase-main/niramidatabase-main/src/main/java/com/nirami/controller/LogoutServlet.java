package com.nirami.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

/**
 * Servlet que gestiona el cierre de sesión del usuario.
 * URL: /logout
 * GET → invalida la sesión HTTP y redirige al login
 *
 * Este servlet es el único punto de salida de la sesión.
 * Destruye TODOS los atributos de sesión en una sola llamada: usuarioLogueado, rol, carrito, etc.
 *
 * Conexiones:
 *   → HttpSession.invalidate() (elimina toda la sesión del servidor)
 *   → /login (redirección tras logout)
 *   → LoginFilter (después del logout, el filtro redirigirá nuevamente al login en cualquier acceso)
 */
@WebServlet("/logout") // Responde a la URL /logout (botón "Salir" en el header)
public class LogoutServlet extends HttpServlet {

    /**
     * GET /logout → Destruye la sesión del usuario y lo redirige al login.
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // Obtener la sesión actual SIN crear una nueva (false = no crear si no existe)
        // Si el usuario ya no tiene sesión, session será null
        HttpSession session = request.getSession(false);

        if (session != null) {
            // Invalidar la sesión: elimina TODOS los atributos guardados en ella.
            // Atributos eliminados: "usuarioLogueado", "rol", "carrito", y cualquier otro.
            // El servidor también destruye la cookie JSESSIONID del cliente.
            session.invalidate();
        }

        // Redirigir al login para que el usuario pueda ingresar nuevamente
        // Después de esta redirección, LoginFilter bloqueará cualquier URL protegida
        response.sendRedirect(request.getContextPath() + "/login");
    }
}
