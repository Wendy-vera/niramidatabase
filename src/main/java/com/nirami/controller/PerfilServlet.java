package com.nirami.controller;

import com.nirami.dao.FavoritoDAO;
import com.nirami.dao.OrdenDAO;
import com.nirami.dao.UsuarioDAO;
import com.nirami.model.Orden;
import com.nirami.model.Producto;
import com.nirami.model.Usuario;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.Part;

import java.io.File;
import java.io.IOException;
import java.util.List;
import org.mindrot.jbcrypt.BCrypt;

@WebServlet("/perfil")
@MultipartConfig(
    fileSizeThreshold = 1024 * 1024 * 2,  // 2MB
    maxFileSize = 1024 * 1024 * 10,       // 10MB
    maxRequestSize = 1024 * 1024 * 50     // 50MB
)
public class PerfilServlet extends HttpServlet {
    private FavoritoDAO favoritoDAO = new FavoritoDAO();
    private OrdenDAO ordenDAO = new OrdenDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession();
        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");

        if (usuario == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        List<Producto> favoritos = favoritoDAO.obtenerFavoritosPorCliente(usuario.getIdUsuario());
        List<Orden> historial = ordenDAO.obtenerOrdenesPorCliente(usuario.getIdUsuario());

        UsuarioDAO usuarioDAO = new UsuarioDAO();
        String extraInfo = usuarioDAO.obtenerExtraInfo(usuario.getIdUsuario(), usuario.getTipoUsuario());
        
        request.setAttribute("favoritos", favoritos);
        request.setAttribute("historial", historial);
        request.setAttribute("extraInfo", extraInfo);

        request.getRequestDispatcher("/perfil.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession();
        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
        if (usuario == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        String nombre = request.getParameter("nombre");
        String telefono = request.getParameter("telefono");
        String correo = request.getParameter("correo");
        String contrasena = request.getParameter("contrasena");
        String extraInfo = request.getParameter("extraInfo"); // cuenta bancaria o direccion

        // Validaciones (similar al registro)
        if (nombre == null || nombre.trim().length() < 3) {
            request.setAttribute("error", "El nombre debe tener al menos 3 caracteres.");
            doGet(request, response);
            return;
        }
        if (correo == null || !correo.contains("@")) {
            request.setAttribute("error", "Correo electrónico no válido.");
            doGet(request, response);
            return;
        }
        if (telefono != null && !telefono.isEmpty() && !telefono.matches("\\d{10}")) {
            request.setAttribute("error", "El teléfono debe tener 10 dígitos numéricos.");
            doGet(request, response);
            return;
        }
        if ("VENDEDOR".equals(usuario.getTipoUsuario()) && (extraInfo == null || !extraInfo.matches("\\d{10}"))) {
            request.setAttribute("error", "La cuenta bancaria debe tener 10 dígitos numéricos.");
            doGet(request, response);
            return;
        }
        if (contrasena != null && !contrasena.isEmpty()) {
            if (contrasena.length() < 8 || !contrasena.matches(".*[A-Z].*") || !contrasena.matches(".*\\d.*")) {
                request.setAttribute("error", "La contraseña debe tener al menos 8 caracteres, una mayúscula y un número.");
                doGet(request, response);
                return;
            }
            usuario.setContrasenaHash(BCrypt.hashpw(contrasena, BCrypt.gensalt()));
        }

        // Manejo de la foto de perfil
        Part filePart = request.getPart("fotoPerfil");
        if (filePart != null && filePart.getSize() > 0) {
            String fileName = System.currentTimeMillis() + "_" + filePart.getSubmittedFileName();
            String uploadPath = "C:\\NiramiUploads";
            File uploadDir = new File(uploadPath);
            if (!uploadDir.exists()) uploadDir.mkdirs();
            filePart.write(uploadPath + File.separator + fileName);
            usuario.setFotoPerfil("uploads/" + fileName);
        }

        usuario.setNombre(nombre);
        usuario.setTelefono(telefono);
        usuario.setCorreo(correo);

        UsuarioDAO usuarioDAO = new UsuarioDAO();
        if (usuarioDAO.actualizarPerfilCompleto(usuario, extraInfo)) {
            session.setAttribute("usuarioLogueado", usuario); // Actualizar en sesion
            request.setAttribute("mensaje", "Perfil actualizado exitosamente.");
        } else {
            request.setAttribute("error", "Error al actualizar el perfil (tal vez el correo ya está en uso).");
        }
        
        doGet(request, response);
    }
}
