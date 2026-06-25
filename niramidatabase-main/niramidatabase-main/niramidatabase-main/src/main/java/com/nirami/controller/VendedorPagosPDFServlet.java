package com.nirami.controller;

import com.nirami.dao.OrdenDAO;
import com.nirami.model.VentaAdminDTO;
import com.nirami.model.Usuario;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Servlet que prepara los datos de pagos del vendedor para impresión/PDF.
 * URL: /vendedor/pagos/pdf
 * GET → filtra solo las ventas con pago COMPLETADO y delega el render a una JSP imprimible
 *
 * Este servlet NO genera un PDF con iText. En cambio, carga datos y reenvía a una JSP
 * que usa CSS @media print para que el navegador imprima la página como PDF.
 *
 * Nota de mantenimiento: El filtro actualmente usa "COMPLETADO" como estado de comparación,
 * pero el campo correcto es estadoPagoVendedor con valor "PAGADO" (ver VendedorPagosServlet).
 * Se mantiene por compatibilidad hasta unificar el campo en una refactorización.
 *
 * Conexiones:
 *   → session.getAttribute("usuarioLogueado") (verificar que es un VENDEDOR)
 *   → OrdenDAO.obtenerVentasAdmin()           (trae todas las ventas del vendedor)
 *   → vendedor_pdf_pagos.jsp                  (vista optimizada para impresión)
 */
@WebServlet("/vendedor/pagos/pdf") // URL para la vista imprimible de pagos
public class VendedorPagosPDFServlet extends HttpServlet {

    // DAO para consultar ventas del vendedor (tablas: ordenes, orden_detalle, pagos_vendedor)
    private OrdenDAO ordenDAO = new OrdenDAO();

    /**
     * GET /vendedor/pagos/pdf → Prepara el resumen de pagos completados para impresión.
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // Obtener la sesión SIN crear una nueva (si no existe → el usuario no está logueado)
        HttpSession session = request.getSession(false);

        // Leer el vendedor logueado (guardado por LoginServlet)
        Usuario vendedor = (Usuario) session.getAttribute("usuarioLogueado");

        // Protección de rol: solo vendedores pueden acceder a esta vista
        if (vendedor == null || !"VENDEDOR".equals(vendedor.getTipoUsuario())) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        /* Consultar TODAS las ventas del vendedor (sin filtros de producto o cliente)
         * Parámetros:
         *   null                    → sin filtro de producto
         *   vendedor.getIdUsuario() → filtrado por el vendedor logueado (SIEMPRE)
         *   null                    → sin filtro de cliente
         *
         * Devuelve List<VentaAdminDTO>, cada uno representa una línea de venta
         */
        List<VentaAdminDTO> ventas = ordenDAO.obtenerVentasAdmin(null, vendedor.getIdUsuario(), null);

        /* Filtrar SOLO las ventas cuyo pago ya fue COMPLETADO/PAGADO
         * stream() → convierte la List en un stream para operaciones funcionales
         * .filter() → retiene solo los elementos que cumplen la condición
         * "COMPLETADO".equals(...) → comparación segura que no lanza NullPointerException
         * .collect(Collectors.toList()) → recopila los filtrados en una nueva List
         */
        List<VentaAdminDTO> completados = ventas.stream()
                .filter(v -> "COMPLETADO".equals(v.getEstadoPago()))
                .collect(Collectors.toList());

        // Calcular el total acumulado de todos los pagos completados
        BigDecimal totalCompletados = BigDecimal.ZERO; // Iniciar en cero
        for (VentaAdminDTO v : completados) {
            // Sumar el subtotal de cada línea de venta completada
            totalCompletados = totalCompletados.add(v.getSubtotalLinea());
        }

        // Pasar los datos filtrados al request para que la JSP los muestre en la vista imprimible
        request.setAttribute("completados",      completados);      // Lista de ventas pagadas
        request.setAttribute("totalCompletados", totalCompletados); // Total acumulado

        // Delegar la renderización a la JSP optimizada para impresión
        // vendedor_pdf_pagos.jsp usa @media print para ocultar el nav y mostrar solo el contenido
        request.getRequestDispatcher("/vendedor_pdf_pagos.jsp").forward(request, response);
    }
}
