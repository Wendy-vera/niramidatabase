package com.nirami.controller;

import com.nirami.dao.ProductoDAO;
import com.nirami.model.CartItem;
import com.nirami.model.Producto;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Servlet que gestiona el carrito de compras del cliente.
 * URL: /carrito
 * GET → muestra los items del carrito con totales calculados
 * POST → agrega, quita o actualiza items en el carrito
 *
 * IMPORTANTE: El carrito se almacena en la SESIÓN HTTP, no en la base de datos.
 * Esto significa que si el usuario cierra el navegador, el carrito se pierde.
 * Clave de sesión: "carrito" → List<CartItem>
 *
 * Conexiones:
 *   → session.getAttribute("carrito")  (lee el carrito desde la sesión HTTP)
 *   → ProductoDAO.obtenerPorId()        (trae datos del producto al agregarlo)
 *   → carrito.jsp                       (vista que muestra el carrito)
 *   → CheckoutServlet                   (destino del botón "Pagar")
 */
@WebServlet("/carrito") // Responde a la URL /carrito
public class CarritoServlet extends HttpServlet {

    // DAO para consultar la BD cuando el usuario agrega un producto por primera vez
    // Se consulta para verificar que el producto existe y tiene stock
    private ProductoDAO productoDAO = new ProductoDAO();

    /**
     * GET /carrito → Calcula los totales y muestra el carrito al cliente.
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // Obtener la sesión del usuario (o crearla si no existe)
        HttpSession session = request.getSession();

        // Leer el carrito guardado en la sesión (puede ser null si es primera visita)
        // Se hace cast a List<CartItem> porque así lo guardó este mismo servlet
        List<CartItem> carrito = (List<CartItem>) session.getAttribute("carrito");

        // Si no existe carrito en sesión, crearlo vacío y guardarlo
        if (carrito == null) {
            carrito = new ArrayList<>();
            session.setAttribute("carrito", carrito); // Guardar el carrito vacío en sesión
        }

        // ── Calcular los totales sumando todos los items del carrito ──
        BigDecimal subtotal = BigDecimal.ZERO; // Iniciar en cero
        for (CartItem item : carrito) {
            // item.getSubtotal() → precio × cantidad (calculado en CartItem)
            subtotal = subtotal.add(item.getSubtotal());
        }

        // IVA = 19% del subtotal, redondeado a 2 decimales (HALF_UP = redondeo estándar)
        BigDecimal iva   = subtotal.multiply(new BigDecimal("0.19")).setScale(2, RoundingMode.HALF_UP);

        // Total = subtotal + IVA
        BigDecimal total = subtotal.add(iva);

        // Pasar los valores calculados al request para que carrito.jsp los muestre
        request.setAttribute("subtotal", subtotal); // Suma sin IVA
        request.setAttribute("iva",      iva);      // Monto del IVA (19%)
        request.setAttribute("total",    total);    // Total a pagar

        // El carrito en sí ya está en la sesión; carrito.jsp lo lee directamente
        request.getRequestDispatcher("/carrito.jsp").forward(request, response);
    }

    /**
     * POST /carrito → Modifica el carrito (agregar, quitar, actualizar cantidad).
     * Parámetros: action ("agregar"|"quitar"|"actualizar"), idProducto, cantidad (solo para actualizar)
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // Obtener la sesión y el carrito actual (igual que en GET)
        HttpSession session = request.getSession();
        List<CartItem> carrito = (List<CartItem>) session.getAttribute("carrito");

        // Crear carrito si no existe aún
        if (carrito == null) {
            carrito = new ArrayList<>();
            session.setAttribute("carrito", carrito);
        }

        // Leer la acción y el ID del producto del formulario HTML
        String action  = request.getParameter("action");    // "agregar", "quitar" o "actualizar"
        String idParam = request.getParameter("idProducto"); // ID del producto a operar

        if (idParam != null) {
            int idProducto = Integer.parseInt(idParam);

            if ("agregar".equals(action)) {
                /* ── Agregar producto al carrito ──
                 * Si el producto YA está en el carrito → incrementar cantidad en 1
                 * Si NO está → consultar la BD, verificar stock y agregar item nuevo
                 */
                boolean existe = false; // Bandera para saber si ya existe en el carrito

                // Buscar si el producto ya está en el carrito (comparar por ID)
                for (CartItem item : carrito) {
                    if (item.getProducto().getIdProducto() == idProducto) {
                        // Ya existe → solo sumar 1 a la cantidad actual
                        item.setCantidad(item.getCantidad() + 1);
                        existe = true;
                        break; // Salir del loop, ya encontramos el item
                    }
                }

                if (!existe) {
                    // Producto nuevo en el carrito → buscar sus datos en la BD
                    // obtenerPorId hace SELECT * FROM productos WHERE id_producto = ?
                    Producto p = productoDAO.obtenerPorId(idProducto);

                    // Solo agregar si el producto existe Y tiene stock disponible
                    if (p != null && p.getStock() > 0) {
                        carrito.add(new CartItem(p, 1)); // Agregar con cantidad inicial 1
                    }
                    // Si no hay stock, simplemente no se agrega (sin mensaje de error)
                }

            } else if ("quitar".equals(action)) {
                /* ── Quitar producto del carrito ──
                 * removeIf itera la lista y elimina el item cuyo producto coincida con el ID
                 */
                carrito.removeIf(item -> item.getProducto().getIdProducto() == idProducto);

            } else if ("actualizar".equals(action)) {
                /* ── Actualizar la cantidad de un producto en el carrito ──
                 * Se usa cuando el usuario escribe directamente la cantidad deseada
                 */
                int nuevaCantidad = Integer.parseInt(request.getParameter("cantidad"));

                // Solo actualizar si la nueva cantidad es positiva (no permitir 0 o negativo)
                if (nuevaCantidad > 0) {
                    for (CartItem item : carrito) {
                        if (item.getProducto().getIdProducto() == idProducto) {
                            item.setCantidad(nuevaCantidad); // Reemplazar la cantidad
                            break;
                        }
                    }
                }
                // Si nuevaCantidad <= 0, no se hace nada (el usuario debería usar "quitar")
            }
        }

        // Después de cualquier modificación, redirigir al GET del carrito
        // Esto aplica el patrón POST-Redirect-GET para evitar doble envío del formulario
        response.sendRedirect(request.getContextPath() + "/carrito");
    }
}
