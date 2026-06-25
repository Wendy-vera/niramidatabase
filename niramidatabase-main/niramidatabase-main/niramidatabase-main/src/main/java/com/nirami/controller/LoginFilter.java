package com.nirami.controller;

import com.nirami.model.Usuario;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

/**
 * Filtro de seguridad global que intercepta TODAS las peticiones HTTP antes
 * de que lleguen a cualquier Servlet o JSP.
 *
 * Funciona como un portero: si el usuario no está autenticado y la URL no es
 * pública (login, registro, CSS, uploads), lo redirige al login.
 *
 * Flujo: Petición HTTP → LoginFilter.doFilter() → [¿permitido?] → Servlet destino
 *                                                              ↓ NO → /login
 */
@WebFilter("/*") // Intercepta ABSOLUTAMENTE TODAS las URLs del proyecto
public class LoginFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // Método requerido por la interfaz Filter; no se necesita inicialización
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        // Convertir los tipos genéricos a HTTP para acceder a sesión y URLs
        HttpServletRequest  req     = (HttpServletRequest)  request;
        HttpServletResponse res     = (HttpServletResponse) response;

        // Intentar obtener la sesión existente sin crear una nueva (false = no crear)
        // La sesión contiene el atributo "usuarioLogueado" si el usuario inició sesión
        HttpSession session = req.getSession(false);

        // Construir las URLs públicas que NO requieren autenticación
        String loginURI      = req.getContextPath() + "/login";      // Página de ingreso
        String registroURI   = req.getContextPath() + "/registro";   // Página de registro
        String cssURI        = req.getContextPath() + "/css/";       // Archivos CSS (estilos)
        String uploadsURI    = req.getContextPath() + "/uploads/";   // Imágenes subidas por vendedores

        // Verificar si el usuario ya inició sesión correctamente
        // La sesión existe Y contiene el objeto Usuario (puesto por LoginServlet)
        boolean loggedIn         = session != null && session.getAttribute("usuarioLogueado") != null;

        // Verificar si la URL actual es una de las permitidas sin autenticación
        boolean loginRequest     = req.getRequestURI().equals(loginURI);     // Va al login
        boolean registroRequest  = req.getRequestURI().equals(registroURI);  // Va al registro
        boolean isStaticResource = req.getRequestURI().startsWith(cssURI)    // Es un CSS
                                || req.getRequestURI().startsWith(uploadsURI); // Es una imagen

        /* Decisión del filtro:
         * - Si está logueado, o va al login/registro, o es un recurso estático → DEJAR PASAR
         * - En cualquier otro caso → REDIRIGIR AL LOGIN
         */
        if (loggedIn || loginRequest || registroRequest || isStaticResource) {

            // Caso especial: usuario logueado que accede a la raíz "/" → redirigir al catálogo
            // Esto evita mostrar una página en blanco o el index.html de Tomcat
            if (loggedIn && req.getRequestURI().equals(req.getContextPath() + "/")) {
                res.sendRedirect(req.getContextPath() + "/catalogo");
                return; // Terminar aquí, no continuar la cadena de filtros
            }

            // Dejar que la petición continúe hacia el Servlet o JSP destino
            chain.doFilter(request, response);

        } else {
            // Usuario no autenticado intentando acceder a una URL protegida → login
            res.sendRedirect(loginURI);
        }
    }

    @Override
    public void destroy() {
        // Método requerido por la interfaz Filter; no hay recursos que liberar
    }
}
