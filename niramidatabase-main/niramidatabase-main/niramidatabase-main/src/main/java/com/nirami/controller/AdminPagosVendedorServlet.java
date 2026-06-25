package com.nirami.controller;

import com.nirami.dao.OrdenDAO;
import com.nirami.model.VentaAdminDTO;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

/**
 * Servlet que gestiona la vista de pagos pendientes a vendedores para el administrador.
 *
 * <p><b>URL mapeada:</b> {@code /admin/pagos-vendedores}</p>
 *
 * <p><b>Acceso:</b> Solo administradores (controlado por {@link LoginFilter}).</p>
 *
 * <p><b>Método GET:</b><br>
 * Obtiene el resumen de pagos pendientes agrupado por vendedor usando
 * {@link OrdenDAO#obtenerResumenPagosPorVendedor()} y lo pasa a la vista
 * {@code admin_pagos_vendedores.jsp}. Muestra para cada vendedor:
 * <ul>
 *   <li>Nombre del vendedor</li>
 *   <li>Número de transacciones pendientes</li>
 *   <li>Monto total pendiente a pagar</li>
 *   <li>Cuenta bancaria de destino</li>
 *   <li>Botón "Pagar" que hace POST a este servlet</li>
 * </ul>
 * </p>
 *
 * <p><b>Método POST:</b><br>
 * Ejecuta el pago a un vendedor específico llamando a
 * {@link OrdenDAO#ejecutarPagoVendedor(int)}, que marca todos los pagos
 * {@code PENDIENTE} del vendedor como {@code PAGADO} con la fecha actual.
 * Redirige de vuelta al listado con un mensaje de éxito o error.</p>
 *
 * <p><b>Parámetros POST esperados:</b>
 * <ul>
 *   <li>{@code idVendedor} — ID del vendedor al que se realizará el pago</li>
 *   <li>{@code nombreVendedor} — Nombre del vendedor (para el mensaje de confirmación)</li>
 * </ul>
 * </p>
 *
 * <p><b>Conexión entre componentes:</b>
 * <pre>
 * admin_pagos_vendedores.jsp
 *   → POST → AdminPagosVendedorServlet.doPost()
 *   → OrdenDAO.ejecutarPagoVendedor(idVendedor)
 *   → UPDATE pagos_vendedor SET estado='PAGADO', fecha_pago=NOW()
 *   → El vendedor ve el cambio en vendedor_pagos.jsp
 * </pre>
 * </p>
 *
 * @author Nirami Dev Team
 * @version 1.0
 * @see OrdenDAO#obtenerResumenPagosPorVendedor()
 * @see OrdenDAO#ejecutarPagoVendedor(int)
 */
@WebServlet("/admin/pagos-vendedores")
public class AdminPagosVendedorServlet extends HttpServlet {

    /**
     * DAO para consultar y actualizar pagos a vendedores en la BD.
     * Se instancia una vez al cargar el servlet.
     */
    private OrdenDAO ordenDAO = new OrdenDAO();

    /**
     * Muestra el panel de pagos pendientes a vendedores.
     *
     * <p>Consulta la vista agrupada de pagos pendientes (un registro por vendedor)
     * y la pasa a la JSP. Si existe un mensaje de resultado de operación anterior
     * (parámetro {@code mensaje}), también se pasa a la vista para mostrarlo.</p>
     *
     * @param request  Solicitud HTTP (parámetro opcional: {@code mensaje})
     * @param response Respuesta HTTP
     * @throws ServletException si hay error al delegar a la JSP
     * @throws IOException      si hay error de I/O
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // Obtener resumen agrupado: un item por vendedor con pagos pendientes
        List<VentaAdminDTO> resumen = ordenDAO.obtenerResumenPagosPorVendedor();

        request.setAttribute("resumenPagos", resumen);

        // Pasar el mensaje de resultado de operaciones previas (si existe)
        String mensaje = request.getParameter("mensaje");
        if (mensaje != null && !mensaje.isEmpty()) {
            request.setAttribute("mensaje", mensaje);
        }

        request.getRequestDispatcher("/admin_pagos_vendedores.jsp").forward(request, response);
    }

    /**
     * Ejecuta el pago a un vendedor específico.
     *
     * <p>Llama a {@link OrdenDAO#ejecutarPagoVendedor(int)} que actualiza
     * TODOS los registros {@code PENDIENTE} del vendedor en {@code pagos_vendedor}
     * a estado {@code PAGADO} con {@code fecha_pago = NOW()}.</p>
     *
     * <p>Este cambio se refleja inmediatamente en el panel del vendedor
     * ({@code vendedor_pagos.jsp}) porque ambas vistas leen de la misma tabla.</p>
     *
     * <p>Redirige a {@code GET /admin/pagos-vendedores} con el mensaje de resultado.</p>
     *
     * @param request  Solicitud HTTP con parámetros {@code idVendedor} y {@code nombreVendedor}
     * @param response Respuesta HTTP
     * @throws ServletException si hay error de Servlet
     * @throws IOException      si hay error de I/O en la redirección
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String idVendedorStr   = request.getParameter("idVendedor");
        String nombreVendedor  = request.getParameter("nombreVendedor");

        // Validar que se recibió un ID de vendedor válido
        if (idVendedorStr == null || idVendedorStr.trim().isEmpty()) {
            response.sendRedirect(request.getContextPath() +
                    "/admin/pagos-vendedores?mensaje=Error:+ID+de+vendedor+invalido");
            return;
        }

        int idVendedor;
        try {
            idVendedor = Integer.parseInt(idVendedorStr);
        } catch (NumberFormatException e) {
            response.sendRedirect(request.getContextPath() +
                    "/admin/pagos-vendedores?mensaje=Error:+ID+de+vendedor+invalido");
            return;
        }

        // Ejecutar el pago: marca todos los pagos PENDIENTE del vendedor como PAGADO
        boolean exito = ordenDAO.ejecutarPagoVendedor(idVendedor);

        String msg;
        if (exito) {
            // URL-encode el nombre del vendedor para el mensaje
            String nombre = (nombreVendedor != null) ? nombreVendedor.replace(" ", "+") : "Vendedor";
            msg = "Pago+realizado+exitosamente+a+" + nombre;
        } else {
            msg = "Error:+No+se+pudo+realizar+el+pago+(sin+pagos+pendientes+o+error+interno)";
        }

        response.sendRedirect(request.getContextPath() + "/admin/pagos-vendedores?mensaje=" + msg);
    }
}
