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

/**
 * Servlet que gestiona la visualización y actualización del perfil de usuario.
 * URL: /perfil
 * GET  → muestra los datos del usuario: info personal, favoritos e historial de órdenes
 * POST → actualiza los datos del perfil (nombre, correo, foto, contraseña, extra info)
 *
 * @MultipartConfig: Necesario para recibir la foto de perfil (formulario multipart/form-data)
 *
 * Conexiones:
 *   → session.getAttribute("usuarioLogueado")   (leer el usuario logueado; puesto por LoginServlet)
 *   → FavoritoDAO.obtenerFavoritosPorCliente()  (lista de productos favoritos del cliente)
 *   → OrdenDAO.obtenerOrdenesPorCliente()       (historial de órdenes del cliente)
 *   → UsuarioDAO.obtenerExtraInfo()             (cuenta bancaria o dirección de envío)
 *   → UsuarioDAO.actualizarPerfilCompleto()     (UPDATE en usuarios + subtabla)
 *   → PasswordUtil / BCrypt                     (hashear nueva contraseña si se cambia)
 *   → perfil.jsp                                (vista del perfil con tabs)
 *   → C:\NiramiUploads\                         (directorio donde se guarda la foto de perfil)
 */
@WebServlet("/perfil") // Responde a la URL /perfil
@MultipartConfig(
    fileSizeThreshold = 1024 * 1024 * 2,  // 2MB en memoria antes de escribir a disco
    maxFileSize       = 1024 * 1024 * 10, // Foto de perfil: máximo 10MB
    maxRequestSize    = 1024 * 1024 * 50  // Todo el formulario: máximo 50MB
)
public class PerfilServlet extends HttpServlet {

    // DAO para obtener la lista de productos guardados en favoritos del cliente
    // Conecta con: SELECT p.* FROM favoritos f JOIN productos p WHERE f.id_cliente = ?
    private FavoritoDAO favoritoDAO = new FavoritoDAO();

    // DAO para obtener el historial de órdenes del cliente
    // Conecta con: SELECT * FROM ordenes WHERE id_cliente = ? ORDER BY fecha_orden DESC
    private OrdenDAO ordenDAO = new OrdenDAO();

    /**
     * GET /perfil → Carga los datos del usuario y los envía a la vista del perfil.
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // Obtener la sesión y verificar que haya un usuario logueado
        HttpSession session = request.getSession();
        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado"); // Lee el objeto completo del usuario

        if (usuario == null) {
            // Sin sesión → redirigir al login
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        // ── Cargar la lista de productos favoritos del usuario (si es CLIENTE) ──
        // Los favoritos se muestran en el tab "Mis Favoritos" del perfil
        List<Producto> favoritos = favoritoDAO.obtenerFavoritosPorCliente(usuario.getIdUsuario());

        // ── Cargar el historial de órdenes del usuario ──
        // Muestra el historial de compras con su estado (PENDIENTE, APROBADO, RECHAZADO)
        List<Orden> historial = ordenDAO.obtenerOrdenesPorCliente(usuario.getIdUsuario());

        // ── Cargar información extra del subtipo de usuario ──
        // Para CLIENTE → dirección de envío (tabla clientes.direccion_envio)
        // Para VENDEDOR → cuenta bancaria (tabla vendedores.cuenta_bancaria)
        UsuarioDAO usuarioDAO = new UsuarioDAO();
        String extraInfo = usuarioDAO.obtenerExtraInfo(usuario.getIdUsuario(), usuario.getTipoUsuario());

        // Pasar los datos al request para que perfil.jsp los muestre en los diferentes tabs
        request.setAttribute("favoritos",  favoritos);  // Tab "Mis Favoritos"
        request.setAttribute("historial",  historial);  // Tab "Mis Pedidos"
        request.setAttribute("extraInfo",  extraInfo);  // Campo de dirección o cuenta bancaria

        // Delegar la renderización a la vista del perfil
        request.getRequestDispatcher("/perfil.jsp").forward(request, response);
    }

    /**
     * POST /perfil → Procesa el formulario de actualización del perfil.
     * Puede actualizar: nombre, teléfono, correo, contraseña, foto y extraInfo
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // Obtener el usuario de la sesión para verificar autenticación y obtener su ID
        HttpSession session = request.getSession();
        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");

        if (usuario == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        // ── Leer todos los campos del formulario de perfil ──
        String nombre     = request.getParameter("nombre");     // Nombre completo del usuario
        String telefono   = request.getParameter("telefono");   // Teléfono (opcional)
        String correo     = request.getParameter("correo");     // Email (también es el login)
        String contrasena = request.getParameter("contrasena"); // Nueva contraseña (vacío = no cambiar)
        String extraInfo  = request.getParameter("extraInfo");  // Dirección (Cliente) o cuenta bancaria (Vendedor)

        // ── Validaciones de los campos editables ──

        // Validar nombre: mínimo 3 caracteres
        if (nombre == null || nombre.trim().length() < 3) {
            request.setAttribute("error", "El nombre debe tener al menos 3 caracteres.");
            doGet(request, response); // Re-usar GET para mostrar el formulario con el error
            return;
        }

        // Validar correo: debe contener al menos un "@"
        if (correo == null || !correo.contains("@")) {
            request.setAttribute("error", "Correo electrónico no válido.");
            doGet(request, response);
            return;
        }

        // Validar teléfono: solo si no está vacío, debe tener exactamente 10 dígitos
        if (telefono != null && !telefono.isEmpty() && !telefono.matches("\\d{10}")) {
            request.setAttribute("error", "El teléfono debe tener 10 dígitos numéricos.");
            doGet(request, response);
            return;
        }

        // Validar cuenta bancaria para vendedores: debe tener exactamente 10 dígitos
        if ("VENDEDOR".equals(usuario.getTipoUsuario()) && (extraInfo == null || !extraInfo.matches("\\d{10}"))) {
            request.setAttribute("error", "La cuenta bancaria debe tener 10 dígitos numéricos.");
            doGet(request, response);
            return;
        }

        // ── Actualizar contraseña SOLO si se proporcionó una nueva ──
        if (contrasena != null && !contrasena.isEmpty()) {
            // Validar política de contraseña: mínimo 8 chars, al menos 1 mayúscula y 1 número
            if (contrasena.length() < 8 || !contrasena.matches(".*[A-Z].*") || !contrasena.matches(".*\\d.*")) {
                request.setAttribute("error",
                    "La contraseña debe tener al menos 8 caracteres, una mayúscula y un número.");
                doGet(request, response);
                return;
            }
            // Generar un nuevo hash bcrypt y actualizar el campo en el objeto usuario
            // Este hash reemplazará el anterior en la BD al llamar actualizarPerfilCompleto()
            usuario.setContrasenaHash(BCrypt.hashpw(contrasena, BCrypt.gensalt()));
        }
        // Si contrasena está vacío, usuario.getContrasenaHash() mantiene el valor actual → no cambia en BD

        // ── Manejar la foto de perfil (campo de tipo file) ──
        Part filePart = request.getPart("fotoPerfil"); // Parte del formulario multipart con la imagen

        if (filePart != null && filePart.getSize() > 0) {
            // Se subió una nueva foto de perfil
            // Usar timestamp como prefijo para nombre único (alternativa a UUID)
            String fileName   = System.currentTimeMillis() + "_" + filePart.getSubmittedFileName();
            String uploadPath = "C:\\NiramiUploads"; // Mismo directorio que ProductoVendedorServlet

            // Crear el directorio si no existe
            File uploadDir = new File(uploadPath);
            if (!uploadDir.exists()) uploadDir.mkdirs();

            // Guardar el archivo en disco
            filePart.write(uploadPath + File.separator + fileName);

            // Actualizar la ruta de la foto en el objeto usuario (para guardar en BD y en sesión)
            // La ruta relativa permite que ImageServlet la sirva: GET /uploads/timestamp_foto.jpg
            usuario.setFotoPerfil("uploads/" + fileName);
        }
        // Si no se subió foto, usuario.getFotoPerfil() mantiene el valor anterior

        // ── Aplicar los cambios de texto al objeto usuario ──
        usuario.setNombre(nombre);
        usuario.setTelefono(telefono);
        usuario.setCorreo(correo);

        // ── Persistir los cambios en la BD ──
        // actualizarPerfilCompleto() hace en una sola transacción:
        //   UPDATE usuarios SET nombre=?, correo=?, telefono=?, contrasena_hash=?, foto_perfil=? WHERE id_usuario=?
        //   UPDATE clientes/vendedores SET direccion_envio/cuenta_bancaria=? WHERE id_usuario=?
        UsuarioDAO usuarioDAO = new UsuarioDAO();
        if (usuarioDAO.actualizarPerfilCompleto(usuario, extraInfo)) {
            // Actualizar también el objeto en la sesión HTTP para reflejar los cambios inmediatamente
            // (nombre, foto, correo ya fueron actualizados en el objeto; la sesión apunta al mismo objeto)
            session.setAttribute("usuarioLogueado", usuario);
            request.setAttribute("mensaje", "Perfil actualizado exitosamente.");
        } else {
            // Fallo → posiblemente el correo ya está en uso por otro usuario (UNIQUE constraint)
            request.setAttribute("error", "Error al actualizar el perfil (tal vez el correo ya está en uso).");
        }

        // Volver a renderizar el perfil (GET) con el mensaje de éxito o error
        doGet(request, response);
    }
}
