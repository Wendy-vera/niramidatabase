package com.nirami.controller;

import com.nirami.dao.OrdenDAO;
import com.nirami.model.Usuario;
import com.nirami.model.VentaAdminDTO;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

/**
 * Servlet que muestra el reporte de pagos del vendedor autenticado.
 *
 * <p><b>URL mapeada:</b> {@code /vendedor/pagos}</p>
 *
 * <p><b>Acceso:</b> Solo usuarios con rol {@code VENDEDOR} (controlado por {@link LoginFilter}).</p>
 *
 * <p><b>Método GET:</b><br>
 * Obtiene el detalle de todos los pagos (pendientes y completados) del vendedor logueado
 * usando {@link OrdenDAO#obtenerPagosPorVendedor(int)}, calcula los totales y los pasa
 * a la vista {@code vendedor_pagos.jsp}.</p>
 *
 * <p><b>Totales calculados:</b>
 * <ul>
 *   <li>{@code totalGenerado} — Suma de todos los {@code subtotalLinea} (ingresos brutos totales)</li>
 *   <li>{@code pagosPendientes} — Suma de {@code montoNeto} donde {@code estadoPagoVendedor = 'PENDIENTE'}</li>
 *   <li>{@code pagosCompletados} — Suma de {@code montoNeto} donde {@code estadoPagoVendedor = 'PAGADO'}</li>
 * </ul>
 * </p>
 *
 * <p><b>Nota:</b> El estado correcto en la BD es {@code 'PAGADO'} (no {@code 'COMPLETADO'}).
 * Esta versión corrige el bug de la versión anterior que comparaba con {@code 'COMPLETADO'}.</p>
 *
 * <p><b>Atributos de request que se pasan a la vista:</b>
 * <ul>
 *   <li>{@code ventas} — Lista de {@link VentaAdminDTO} con todos los pagos del vendedor</li>
 *   <li>{@code totalGenerado} — {@link BigDecimal} con el total bruto generado</li>
 *   <li>{@code pagosPendientes} — {@link BigDecimal} con el monto pendiente de pago</li>
 *   <li>{@code pagosCompletados} — {@link BigDecimal} con el monto ya pagado al vendedor</li>
 * </ul>
 * </p>
 *
 * @author Nirami Dev Team
 * @version 1.1
 * @see OrdenDAO#obtenerPagosPorVendedor(int)
 */
@WebServlet("/vendedor/pagos")
public class VendedorPagosServlet extends HttpServlet {

    /**
     * DAO para consultar los pagos del vendedor en la BD.
     * Se instancia una vez al cargar el servlet.
     */
    private OrdenDAO ordenDAO = new OrdenDAO();

    /**
     * Muestra el reporte de pagos del vendedor autenticado.
     *
     * <p>Lee el ID del vendedor de la sesión HTTP y consulta sus pagos.
     * Los totales se calculan iterando la lista para evitar una consulta adicional.</p>
     *
     * @param request  Solicitud HTTP
     * @param response Respuesta HTTP
     * @throws ServletException si hay error al delegar a la JSP
     * @throws IOException      si hay error de I/O
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session  = request.getSession(false);
        Usuario     vendedor = (Usuario) session.getAttribute("usuarioLogueado");

        // Obtener el detalle completo de pagos del vendedor desde la BD
        List<VentaAdminDTO> pagos = ordenDAO.obtenerPagosPorVendedor(vendedor.getIdUsuario());

        // Calcular los totales iterando la lista (evita consulta adicional a la BD)
        BigDecimal totalGenerado    = BigDecimal.ZERO;
        BigDecimal pagosPendientes  = BigDecimal.ZERO;
        BigDecimal pagosCompletados = BigDecimal.ZERO;

        for (VentaAdminDTO p : pagos) {
            BigDecimal monto = p.getMontoNeto() != null ? p.getMontoNeto() : BigDecimal.ZERO;
            totalGenerado = totalGenerado.add(p.getSubtotalLinea() != null ? p.getSubtotalLinea() : BigDecimal.ZERO);

            // ── Bug fix: comparar con 'PAGADO' (valor real en BD), NO 'COMPLETADO' ──
            if ("PAGADO".equals(p.getEstadoPagoVendedor())) {
                pagosCompletados = pagosCompletados.add(monto);
            } else {
                // PENDIENTE u otro estado no reconocido → se trata como pendiente
                pagosPendientes = pagosPendientes.add(monto);
            }
        }

        request.setAttribute("ventas",           pagos);
        request.setAttribute("totalGenerado",     totalGenerado);
        request.setAttribute("pagosPendientes",   pagosPendientes);
        request.setAttribute("pagosCompletados",  pagosCompletados);

        request.getRequestDispatcher("/vendedor_pagos.jsp").forward(request, response);
    }
}
