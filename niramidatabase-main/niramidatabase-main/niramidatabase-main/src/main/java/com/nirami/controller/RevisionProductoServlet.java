package com.nirami.controller;

import com.nirami.dao.ProductoDAO;
import com.nirami.dao.RevisionDAO;
import com.nirami.model.Producto;
import com.nirami.model.Usuario;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

/**
 * Servlet que gestiona la revisión (aprobación/rechazo) de un producto por el Administrador.
 * URL: /admin/revision
 * GET  → muestra el detalle del producto para que el admin lo revise
 * POST → ejecuta la decisión (APROBADO o RECHAZADO) y registra la revisión
 *
 * Flujo:
 *   AdminDashboardServlet → clic en "Revisar" → GET /admin/revision?id=X
 *   Admin ve el producto → toma decisión → POST /admin/revision
 *   POST actualiza productos.estado y registra en revisiones (tabla de auditoría)
 *
 * Conexiones:
 *   → ProductoDAO.obtenerPorId()    (leer el producto a revisar)
 *   → ProductoDAO.actualizarEstado() (cambiar estado: PENDIENTE → APROBADO o RECHAZADO)
 *   → RevisionDAO.registrarRevision() (registrar en tabla `revisiones` para auditoría)
 *   → admin_revision_producto.jsp    (formulario de decisión del admin)
 *   → admin/dashboard                (redirección después de la revisión)
 */
@WebServlet("/admin/revision") // Responde a /admin/revision
public class RevisionProductoServlet extends HttpServlet {

    // DAO para leer el producto y actualizar su estado en la tabla `productos`
    private ProductoDAO productoDAO = new ProductoDAO();

    // DAO para registrar la revisión en la tabla `revisiones` (historial de auditoría)
    // Almacena quién revisó, cuándo, qué decisión tomó y el motivo
    private RevisionDAO revisionDAO = new RevisionDAO();

    /**
     * GET /admin/revision?id=X → Muestra el detalle del producto para revisión.
     * Solo productos en estado PENDIENTE pueden ser revisados.
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // Verificar que el usuario sea ADMIN (protección de rol adicional)
        HttpSession session = request.getSession();
        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");

        if (usuario == null || !"ADMIN".equals(usuario.getTipoUsuario())) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        // Leer el parámetro id del producto a revisar (?id=X en la URL)
        String idParam = request.getParameter("id");

        if (idParam != null) {
            try {
                int idProducto = Integer.parseInt(idParam); // Convertir String a int

                // Buscar el producto en la BD por su ID
                // SELECT * FROM productos p JOIN ... WHERE p.id_producto = ?
                Producto p = productoDAO.obtenerPorId(idProducto);

                // Solo mostrar si el producto existe Y está en estado PENDIENTE
                // Un admin no debe poder re-revisar un producto ya aprobado o rechazado
                if (p != null && "PENDIENTE".equals(p.getEstado())) {
                    // Pasar el producto a la JSP para mostrarlo con todos sus detalles
                    request.setAttribute("producto", p);
                    request.getRequestDispatcher("/admin_revision_producto.jsp").forward(request, response);
                    return; // Terminar aquí si encontramos el producto
                }

            } catch (NumberFormatException e) {
                // Si el parámetro id no es un número válido, ignorar y caer al dashboard
            }
        }

        // Si no hay id, el id es inválido, o el producto no existe/no está PENDIENTE
        // → redirigir al dashboard para que el admin elija otro producto
        response.sendRedirect(request.getContextPath() + "/admin/dashboard");
    }

    /**
     * POST /admin/revision → Procesa la decisión del admin sobre el producto.
     * Parámetros: idProducto (int), accion ("APROBADO"|"RECHAZADO"), observacion (String)
     *
     * Si accion = "RECHAZADO", la observación es obligatoria (motivo del rechazo).
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // Verificar rol de ADMIN nuevamente para el POST
        HttpSession session = request.getSession();
        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");

        if (usuario == null || !"ADMIN".equals(usuario.getTipoUsuario())) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        // Leer los parámetros del formulario de revisión
        int    idProducto  = Integer.parseInt(request.getParameter("idProducto")); // ID del producto revisado
        String accion      = request.getParameter("accion");       // "APROBADO" o "RECHAZADO"
        String observacion = request.getParameter("observacion");   // Motivo (obligatorio si RECHAZADO)

        // VALIDACIÓN: Si se rechaza, debe haber un motivo escrito
        // El motivo se guarda en la tabla `revisiones` para que el vendedor sepa qué corregir
        if ("RECHAZADO".equals(accion) && (observacion == null || observacion.trim().isEmpty())) {
            // Redirigir de vuelta al formulario de revisión con un error
            response.sendRedirect(request.getContextPath()
                + "/admin/revision?id=" + idProducto
                + "&error=Debe+ingresar+un+motivo+para+el+rechazo");
            return;
        }

        // Solo procesar si la acción es válida (APROBADO o RECHAZADO)
        if ("APROBADO".equals(accion) || "RECHAZADO".equals(accion)) {

            // Paso 1: Actualizar el estado del producto en la tabla `productos`
            // UPDATE productos SET estado = ? WHERE id_producto = ?
            boolean actualizado = productoDAO.actualizarEstado(idProducto, accion);

            if (actualizado) {
                // Paso 2: Registrar la revisión en la tabla `revisiones` (auditoría)
                // INSERT INTO revisiones (id_producto, id_admin, resultado, observacion, fecha)
                // Esto permite rastrear quién aprobó/rechazó, cuándo y por qué
                revisionDAO.registrarRevision(idProducto, usuario.getIdUsuario(), accion, observacion);

                // Paso 3: Redirigir al dashboard con mensaje de éxito
                response.sendRedirect(request.getContextPath()
                    + "/admin/dashboard?mensaje=Producto+" + accion.toLowerCase());
                return;
            }
        }

        // Si algo falló (acción inválida o error al actualizar) → ir al dashboard con error
        response.sendRedirect(request.getContextPath() + "/admin/dashboard?error=Error+al+procesar+la+revision");
    }
}
