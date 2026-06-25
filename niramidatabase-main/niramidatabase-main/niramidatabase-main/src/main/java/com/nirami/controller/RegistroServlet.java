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

/**
 * Servlet que gestiona el registro de nuevos usuarios (Clientes y Vendedores).
 * URL: /registro
 * GET → muestra el formulario de registro (registro.jsp)
 * POST → valida y guarda el nuevo usuario en la BD
 *
 * Conexiones:
 *   → UsuarioDAO.registrarUsuario() (INSERT en usuarios + clientes o vendedores)
 *   → PasswordUtil.hashPassword()   (convierte contraseña plana a hash bcrypt)
 *   → registro.jsp                  (formulario de registro)
 *   → /login                        (redirección tras registro exitoso)
 *
 * Seguridad: Este servlet bloquea el registro con tipo "ADMIN" desde el formulario público.
 */
@WebServlet("/registro") // Responde a la URL /registro
public class RegistroServlet extends HttpServlet {

    // DAO para insertar el nuevo usuario en la BD (tabla usuarios + subtipo)
    private UsuarioDAO usuarioDAO = new UsuarioDAO();

    /**
     * GET /registro → simplemente muestra el formulario de registro.
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // Delegar la renderización del formulario a registro.jsp
        request.getRequestDispatcher("/registro.jsp").forward(request, response);
    }

    /**
     * POST /registro → procesa el formulario y crea el usuario si los datos son válidos.
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // ── Paso 1: Leer todos los campos del formulario HTML ──
        String nombre              = request.getParameter("nombre");              // Nombre completo
        String correo              = request.getParameter("correo");              // Email (será el usuario)
        String telefono            = request.getParameter("telefono");            // Teléfono (opcional)
        String contrasena          = request.getParameter("contrasena");          // Contraseña en texto plano
        String confirmarContrasena = request.getParameter("confirmar_contrasena");// Confirmación
        String tipoUsuario         = request.getParameter("tipoUsuario");         // "CLIENTE" o "VENDEDOR"

        // ── Paso 2: Validar que los campos obligatorios no estén vacíos ──
        if (nombre == null || correo == null || contrasena == null || confirmarContrasena == null
                || tipoUsuario == null || nombre.trim().isEmpty() || correo.trim().isEmpty()
                || contrasena.trim().isEmpty() || confirmarContrasena.trim().isEmpty()) {
            // Poner el mensaje de error en el request para que registro.jsp lo muestre
            request.setAttribute("error", "Todos los campos obligatorios deben ser completados.");
            request.getRequestDispatcher("/registro.jsp").forward(request, response);
            return; // Detener aquí, no continuar con el registro
        }

        // ── Paso 3: Validar longitud mínima del nombre ──
        if (nombre.trim().length() < 3) {
            request.setAttribute("error", "El nombre debe tener al menos 3 caracteres.");
            request.getRequestDispatcher("/registro.jsp").forward(request, response);
            return;
        }

        // ── Paso 4: Verificar que ambas contraseñas sean idénticas ──
        if (!contrasena.equals(confirmarContrasena)) {
            request.setAttribute("error", "Las contraseñas no coinciden.");
            request.getRequestDispatcher("/registro.jsp").forward(request, response);
            return;
        }

        // ── Paso 5: Validar formato del correo con regex ──
        // Patrón: al menos un carácter, @, al menos un carácter, punto, 2+ letras
        if (!correo.matches("^[^@]+@[^@]+\\.[a-zA-Z]{2,}$")) {
            request.setAttribute("error", "El correo ingresado no es válido.");
            request.getRequestDispatcher("/registro.jsp").forward(request, response);
            return;
        }

        // ── Paso 6: Validar que la contraseña cumpla la política de seguridad ──
        // Regex: mínimo 8 caracteres, al menos 1 dígito, 1 minúscula, 1 mayúscula
        if (!contrasena.matches("^(?=.*\\d)(?=.*[a-z])(?=.*[A-Z]).{8,}$")) {
            request.setAttribute("error", "La contraseña debe tener mínimo 8 caracteres, una mayúscula, una minúscula y un número.");
            request.getRequestDispatcher("/registro.jsp").forward(request, response);
            return;
        }

        // ── Paso 7: Validar que el teléfono (si se provee) sea exactamente 10 dígitos ──
        if (telefono != null && !telefono.trim().isEmpty() && !telefono.matches("^\\d{10}$")) {
            request.setAttribute("error", "El teléfono debe tener exactamente 10 dígitos numéricos.");
            request.getRequestDispatcher("/registro.jsp").forward(request, response);
            return;
        }

        // ── Paso 8: Construir el objeto Usuario con los datos validados ──
        Usuario nuevoUsuario = new Usuario();
        nuevoUsuario.setNombre(nombre.trim());
        nuevoUsuario.setCorreo(correo.trim());
        nuevoUsuario.setTelefono(telefono != null ? telefono.trim() : null);

        // NUNCA guardar contraseñas en texto plano → convertir a hash bcrypt
        // PasswordUtil.hashPassword() llama BCrypt.hashpw() con un salt aleatorio
        nuevoUsuario.setContrasenaHash(PasswordUtil.hashPassword(contrasena));

        // ── Paso 9: Bloqueo de seguridad — no permitir que nadie se registre como ADMIN ──
        // Si alguien manipula el formulario para enviar tipoUsuario=ADMIN, se ignora
        if ("ADMIN".equals(tipoUsuario)) {
            tipoUsuario = "CLIENTE"; // Forzar a CLIENTE
        }
        nuevoUsuario.setTipoUsuario(tipoUsuario);

        // ── Paso 10: Procesar datos específicos del subtipo ──
        String extraInfo = ""; // Campo extra que va a la tabla de subtipo (clientes o vendedores)

        if ("VENDEDOR".equals(tipoUsuario)) {
            // Para vendedores se requiere la cuenta bancaria (va a vendedores.cuenta_bancaria)
            extraInfo = request.getParameter("cuentaBancaria");
            // Validar que la cuenta bancaria sea exactamente 10 dígitos numéricos
            if (extraInfo == null || !extraInfo.matches("^\\d{10}$")) {
                request.setAttribute("error",
                    "La cuenta bancaria es requerida para vendedores y debe tener exactamente 10 dígitos numéricos.");
                request.getRequestDispatcher("/registro.jsp").forward(request, response);
                return;
            }
        }
        // Para CLIENTE, extraInfo queda "" (la dirección se puede agregar después en el perfil)

        // ── Paso 11: Intentar guardar en la BD ──
        // registrarUsuario() hace un INSERT transaccional:
        //   → INSERT INTO usuarios (...)
        //   → INSERT INTO clientes (id_usuario, direccion_envio) o vendedores (id_usuario, cuenta_bancaria)
        boolean exito = usuarioDAO.registrarUsuario(nuevoUsuario, extraInfo);

        if (exito) {
            // Registro exitoso → redirigir al login con parámetro para mostrar mensaje de éxito
            response.sendRedirect(request.getContextPath() + "/login?registrado=true");
        } else {
            // Fallo (probablemente el correo ya existe por la restricción UNIQUE de la BD)
            request.setAttribute("error", "Error al registrar. Es posible que el correo ya exista.");
            request.getRequestDispatcher("/registro.jsp").forward(request, response);
        }
    }
}
