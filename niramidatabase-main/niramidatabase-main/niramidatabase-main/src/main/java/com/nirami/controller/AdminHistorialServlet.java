package com.nirami.controller;

import com.nirami.dao.HistorialDAO;
import com.nirami.dao.UsuarioDAO;
import com.nirami.model.HistorialUsuario;
import com.nirami.model.Usuario;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

/**
 * Servlet que muestra el historial de acciones de un cliente específico al administrador.
 *
 * <p><b>URL mapeada:</b> {@code /admin/historial-usuario}</p>
 *
 * <p><b>Acceso:</b> Solo administradores (controlado por {@link LoginFilter}).</p>
 *
 * <p><b>Parámetros GET esperados:</b>
 * <ul>
 *   <li>{@code idCliente} — ID del cliente cuyo historial se desea ver</li>
 * </ul>
 * </p>
 *
 * <p><b>Flujo:</b>
 * <ol>
 *   <li>Verifica que se proporcionó un {@code idCliente} válido</li>
 *   <li>Consulta los datos del cliente con {@link UsuarioDAO#obtenerPorCorreo} (no directamente por ID;
 *       se obtiene el objeto a través del correo del historial)</li>
 *   <li>Consulta el historial de acciones con {@link HistorialDAO#obtenerHistorialPorCliente(int)}</li>
 *   <li>Pasa los datos a {@code admin_historial_usuario.jsp}</li>
 * </ol>
 * </p>
 *
 * @author Nirami Dev Team
 * @version 1.0
 * @see HistorialDAO
 */
@WebServlet("/admin/historial-usuario")
public class AdminHistorialServlet extends HttpServlet {

    /** DAO para obtener registros del historial de usuario. */
    private HistorialDAO historialDAO = new HistorialDAO();

    /**
     * Muestra el historial de acciones de un cliente.
     *
     * @param request  Solicitud HTTP con parámetro {@code idCliente}
     * @param response Respuesta HTTP
     * @throws ServletException si hay error al delegar a la JSP
     * @throws IOException      si hay error de I/O o redirección
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String idClienteStr = request.getParameter("idCliente");

        // Validar el parámetro obligatorio
        if (idClienteStr == null || idClienteStr.trim().isEmpty()) {
            response.sendRedirect(request.getContextPath() + "/admin/clientes");
            return;
        }

        int idCliente;
        try {
            idCliente = Integer.parseInt(idClienteStr);
        } catch (NumberFormatException e) {
            response.sendRedirect(request.getContextPath() + "/admin/clientes");
            return;
        }

        // Obtener el historial del cliente desde la BD
        List<HistorialUsuario> historial = historialDAO.obtenerHistorialPorCliente(idCliente);

        request.setAttribute("historial", historial);
        request.setAttribute("idCliente", idCliente);

        // Si hay registros, extraer el nombre del cliente del primer registro
        if (!historial.isEmpty()) {
            request.setAttribute("nombreCliente", historial.get(0).getNombreCliente());
        }

        request.getRequestDispatcher("/admin_historial_usuario.jsp").forward(request, response);
    }
}
