package com.nirami.controller;

import com.nirami.dao.HistorialDAO;
import com.nirami.dao.OrdenDAO;
import com.nirami.model.CartItem;
import com.nirami.model.Usuario;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Servlet que gestiona el proceso de pago (checkout) de una compra.
 *
 * <p><b>URL mapeada:</b> {@code /checkout}</p>
 *
 * <p><b>Acceso:</b> Requiere usuario autenticado con carrito no vacío.</p>
 *
 * <p><b>Método GET:</b><br>
 * Muestra el formulario de confirmación de compra ({@code checkout.jsp}).
 * Verifica que el usuario esté logueado y tenga productos en el carrito;
 * si no, redirige a {@code /login} o {@code /carrito} respectivamente.</p>
 *
 * <p><b>Método POST:</b><br>
 * Procesa la compra usando {@link OrdenDAO#crearOrden}. Si es exitosa:
 * <ol>
 *   <li>Crea la orden en la BD (transaccional: orden + detalles + pagos_vendedor + reduce stock)</li>
 *   <li>Registra la acción {@code COMPRAR} en el historial para cada producto comprado</li>
 *   <li>Vacía el carrito de la sesión</li>
 *   <li>Muestra la página de éxito ({@code checkout_success.jsp})</li>
 * </ol>
 * </p>
 *
 * <p><b>Parámetros POST esperados:</b>
 * <ul>
 *   <li>{@code direccion} — Dirección de entrega ingresada por el cliente</li>
 * </ul>
 * </p>
 *
 * <p><b>Cálculo de totales:</b> Se recalcula en el servidor (no se confía en valores del cliente):
 * <ul>
 *   <li>Subtotal = suma de (precio × cantidad) de cada item del carrito</li>
 *   <li>IVA = 19% del subtotal</li>
 *   <li>Total = subtotal + IVA</li>
 * </ul>
 * </p>
 *
 * @author Nirami Dev Team
 * @version 1.1
 * @see OrdenDAO
 * @see HistorialDAO
 */
@WebServlet("/checkout")
public class CheckoutServlet extends HttpServlet {

    /** DAO para crear órdenes y pagos pendientes en la BD. */
    private OrdenDAO    ordenDAO    = new OrdenDAO();

    /** DAO para registrar la acción COMPRAR en el historial del cliente. */
    private HistorialDAO historialDAO = new HistorialDAO();

    /**
     * Muestra el formulario de confirmación de compra.
     *
     * <p>Redirige a {@code /login} si no hay usuario logueado,
     * o a {@code /carrito} si el carrito está vacío.</p>
     *
     * @param request  Solicitud HTTP
     * @param response Respuesta HTTP
     * @throws ServletException si hay error al delegar a la JSP
     * @throws IOException      si hay error de I/O en la redirección
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession    session  = request.getSession();
        Usuario        usuario  = (Usuario)        session.getAttribute("usuarioLogueado");
        List<CartItem> carrito  = (List<CartItem>) session.getAttribute("carrito");

        if (usuario == null) {
            response.sendRedirect(request.getContextPath() + "/login?mensaje=Inicia+sesion+para+comprar");
            return;
        }

        if (carrito == null || carrito.isEmpty()) {
            response.sendRedirect(request.getContextPath() + "/carrito");
            return;
        }

        request.getRequestDispatcher("/checkout.jsp").forward(request, response);
    }

    /**
     * Procesa el pago del carrito y crea la orden en la base de datos.
     *
     * <p>Los totales se recalculan en el servidor para evitar manipulación.
     * Si {@link OrdenDAO#crearOrden} falla (stock insuficiente u otro error),
     * se muestra un mensaje de error en el formulario de checkout.</p>
     *
     * <p>Al éxito, registra {@code COMPRAR} en el historial por cada producto
     * del carrito, luego elimina el carrito de la sesión.</p>
     *
     * @param request  Solicitud HTTP con parámetro {@code direccion}
     * @param response Respuesta HTTP
     * @throws ServletException si hay error al delegar a la JSP
     * @throws IOException      si hay error de I/O
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession    session  = request.getSession();
        Usuario        usuario  = (Usuario)        session.getAttribute("usuarioLogueado");
        List<CartItem> carrito  = (List<CartItem>) session.getAttribute("carrito");

        if (usuario == null || carrito == null || carrito.isEmpty()) {
            response.sendRedirect(request.getContextPath() + "/carrito");
            return;
        }

        String direccion = request.getParameter("direccion");

        // ── Recalcular totales en el servidor (nunca confiar en el cliente) ──
        BigDecimal subtotal = BigDecimal.ZERO;
        for (CartItem item : carrito) {
            subtotal = subtotal.add(item.getSubtotal());
        }
        BigDecimal iva   = subtotal.multiply(new BigDecimal("0.19")).setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = subtotal.add(iva);

        // ── Crear la orden (transaccional) ──
        boolean exito = ordenDAO.crearOrden(usuario.getIdUsuario(), subtotal, iva, total, direccion, carrito);

        if (exito) {
            // ── Registrar COMPRAR en historial para cada producto ──
            if ("CLIENTE".equals(usuario.getTipoUsuario())) {
                String ip = request.getHeader("X-Forwarded-For");
                if (ip == null || ip.isEmpty()) ip = request.getRemoteAddr();

                for (CartItem item : carrito) {
                    historialDAO.registrarAccion(
                        usuario.getIdUsuario(),
                        item.getProducto().getIdProducto(),
                        "COMPRAR",
                        null,
                        "Compró " + item.getCantidad() + "x " + item.getProducto().getNombre(),
                        ip
                    );
                }
            }

            // ── Vaciar carrito de la sesión ──
            session.removeAttribute("carrito");

            request.getRequestDispatcher("/checkout_success.jsp").forward(request, response);
        } else {
            request.setAttribute("error", "Hubo un problema al procesar tu orden (stock insuficiente o error interno). Inténtalo más tarde.");
            request.getRequestDispatcher("/checkout.jsp").forward(request, response);
        }
    }
}
