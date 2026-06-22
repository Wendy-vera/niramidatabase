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

@WebFilter("/*")
public class LoginFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {}

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;
        HttpSession session = req.getSession(false);

        String loginURI = req.getContextPath() + "/login";
        String registroURI = req.getContextPath() + "/registro";
        String cssURI = req.getContextPath() + "/css/";
        String uploadsURI = req.getContextPath() + "/uploads/";

        boolean loggedIn = session != null && session.getAttribute("usuarioLogueado") != null;
        boolean loginRequest = req.getRequestURI().equals(loginURI);
        boolean registroRequest = req.getRequestURI().equals(registroURI);
        boolean isStaticResource = req.getRequestURI().startsWith(cssURI) || req.getRequestURI().startsWith(uploadsURI);

        if (loggedIn || loginRequest || registroRequest || isStaticResource) {
            // Si el usuario ya está logueado y trata de acceder al index raíz "/", dejarlo pasar
            if(loggedIn && req.getRequestURI().equals(req.getContextPath() + "/")) {
                res.sendRedirect(req.getContextPath() + "/catalogo");
                return;
            }
            chain.doFilter(request, response);
        } else {
            res.sendRedirect(loginURI);
        }
    }

    @Override
    public void destroy() {}
}
