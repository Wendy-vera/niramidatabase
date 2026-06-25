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

/**
 * Servlet que gestiona el proceso de autenticación de usuarios (login/logout).
 *
 * <p><b>URL mapeada:</b> {@code /login}</p>
 *
 * <p><b>Métodos HTTP:</b></p>
 * <ul>
 *   <li>{@code GET /login} — Muestra el formulario de inicio de sesión ({@code login.jsp})</li>
 *   <li>{@code POST /login} — Procesa las credenciales enviadas por el formulario</li>
 * </ul>
 *
 * <p><b>Flujo de autenticación (POST):</b></p>
 * <ol>
 *   <li>Recibe {@code correo} y {@code contrasena} del formulario</li>
 *   <li>Busca el usuario en la BD con {@link UsuarioDAO#obtenerPorCorreo(String)}</li>
 *   <li>Verifica que el usuario exista y esté en estado {@code ACTIVO}</li>
 *   <li>Compara la contraseña con el hash bcrypt usando {@link PasswordUtil#checkPassword}</li>
 *   <li>Si es válido: crea la sesión, registra la última sesión y redirige según el rol</li>
 *   <li>Si es inválido: reenvía al formulario con mensaje de error</li>
 * </ol>
 *
 * <p><b>Redirección por rol:</b>
 * <ul>
 *   <li>{@code ADMIN} → {@code /admin/dashboard}</li>
 *   <li>{@code VENDEDOR} → {@code /vendedor/dashboard}</li>
 *   <li>{@code CLIENTE} → {@code /} (catálogo principal)</li>
 * </ul>
 * </p>
 *
 * <p><b>Atributos de sesión creados:</b>
 * <ul>
 *   <li>{@code usuarioLogueado} — Objeto {@link Usuario} completo</li>
 *   <li>{@code rol} — String con el tipo de usuario ({@code "ADMIN"}, {@code "VENDEDOR"}, {@code "CLIENTE"})</li>
 * </ul>
 * </p>
 *
 * @author Nirami Dev Team
 * @version 1.1
 * @see UsuarioDAO
 * @see PasswordUtil
 */
@WebServlet("/login")
public class LoginServlet extends HttpServlet {

    /**
     * DAO para consultar usuarios en la base de datos.
     * Inicializado una vez al cargar el servlet (singleton por servlet).
     */
    private UsuarioDAO usuarioDAO = new UsuarioDAO();

    /**
     * Muestra el formulario de inicio de sesión.
     *
     * @param request  Solicitud HTTP
     * @param response Respuesta HTTP
     * @throws ServletException si hay error al delegar a la JSP
     * @throws IOException      si hay error de I/O
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.getRequestDispatcher("/login.jsp").forward(request, response);
    }

    /**
     * Procesa las credenciales del formulario de login.
     *
     * <p>Parámetros esperados del formulario:
     * <ul>
     *   <li>{@code correo} — Email del usuario</li>
     *   <li>{@code contrasena} — Contraseña en texto plano (se compara con hash en BD)</li>
     * </ul>
     * </p>
     *
     * @param request  Solicitud HTTP con parámetros del formulario
     * @param response Respuesta HTTP para redirección o reenvío
     * @throws ServletException si hay error al delegar a la JSP
     * @throws IOException      si hay error de I/O en la redirección
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String correo     = request.getParameter("correo");
        String contrasena = request.getParameter("contrasena");

        // Buscar usuario por correo en la BD
        Usuario usuario = usuarioDAO.obtenerPorCorreo(correo);

        if (usuario != null && "ACTIVO".equals(usuario.getEstado())) {
            if (PasswordUtil.checkPassword(contrasena, usuario.getContrasenaHash())) {

                // ── Autenticación exitosa ──

                // Registrar la sesión HTTP con datos del usuario
                HttpSession session = request.getSession();
                session.setAttribute("usuarioLogueado", usuario);
                session.setAttribute("rol", usuario.getTipoUsuario());

                // Actualizar campo ultima_sesion en la BD para auditoría
                usuarioDAO.actualizarUltimaSesion(usuario.getIdUsuario());

                // Redirigir al panel correspondiente según el rol
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

        // ── Autenticación fallida ──
        request.setAttribute("error", "Credenciales inválidas o cuenta inactiva");
        request.getRequestDispatcher("/login.jsp").forward(request, response);
    }
}
