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

/**
 * Servlet que gestiona la vista de usuarios (Vendedores o Clientes) para el Administrador.
 * URLs:
 *   /admin/vendedores → lista todos los vendedores
 *   /admin/clientes   → lista todos los clientes
 *
 * El mismo servlet sirve ambas URLs. La diferencia la determina la URL en sí.
 *
 * Conexiones:
 *   → UsuarioDAO.obtenerUsuariosPorTipo() (consulta usuarios por tipo en la BD)
 *   → admin_usuarios.jsp (vista compartida para ambos tipos de usuarios)
 *   → AdminHistorialServlet (enlace "Ver Historial" visible solo para clientes)
 */
@WebServlet({"/admin/vendedores", "/admin/clientes"}) // Atiende ambas URLs
public class AdminUsuariosServlet extends HttpServlet {

    // DAO para consultar usuarios de la BD filtrados por tipo (VENDEDOR o CLIENTE)
    // Conecta con: SELECT * FROM usuarios WHERE tipo_usuario = ? ORDER BY fecha_registro DESC
    private UsuarioDAO usuarioDAO = new UsuarioDAO();

    /**
     * GET → Determina si mostrar vendedores o clientes según la URL solicitada,
     * consulta la BD y delega la renderización a la vista compartida.
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // Leer la URI completa de la petición para determinar qué tipo de usuarios mostrar
        // Ejemplo: /nirami/admin/vendedores → uri termina en "vendedores"
        //          /nirami/admin/clientes   → uri termina en "clientes"
        String uri  = request.getRequestURI();

        // Determinar el tipo de usuario según la URL
        // Si la URL termina con "vendedores" → buscar VENDEDOR, de lo contrario CLIENTE
        String tipo = uri.endsWith("vendedores") ? "VENDEDOR" : "CLIENTE";

        // Consultar todos los usuarios del tipo determinado en la BD
        // La query: SELECT * FROM usuarios WHERE tipo_usuario = ? ORDER BY fecha_registro DESC
        List<Usuario> usuarios = usuarioDAO.obtenerUsuariosPorTipo(tipo);

        // Pasar la lista de usuarios para que admin_usuarios.jsp los muestre en la tabla
        request.setAttribute("usuarios", usuarios);

        // Pasar el tipo de vista para que admin_usuarios.jsp pueda:
        //   - Mostrar "Vendedores" o "Clientes" como título
        //   - Mostrar/ocultar columna de historial (solo para CLIENTE)
        request.setAttribute("tipoVista", tipo);

        // Ambas URLs usan la misma vista, que adapta su contenido según tipoVista
        request.getRequestDispatcher("/admin_usuarios.jsp").forward(request, response);
    }
}
