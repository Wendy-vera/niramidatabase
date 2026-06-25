package com.nirami.controller;

import com.nirami.dao.OrdenDAO;
import com.nirami.dao.ProductoDAO;
import com.nirami.dao.UsuarioDAO;
import com.nirami.model.Producto;
import com.nirami.model.Usuario;
import com.nirami.model.VentaAdminDTO;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

/**
 * Servlet que gestiona la vista y acciones de Ventas para el Administrador.
 * URL: /admin/ventas
 * GET → muestra listado de ventas con filtros
 * POST → aprueba o rechaza una orden de compra
 *
 * Conexiones:
 *   → OrdenDAO  (leer ventas, actualizar estado de orden y pagos_vendedor)
 *   → ProductoDAO (obtener lista de productos para el filtro)
 *   → UsuarioDAO  (obtener vendedores y clientes para los filtros)
 *   → admin_ventas.jsp (vista que muestra los resultados)
 */
@WebServlet("/admin/ventas") // Responde a la URL /admin/ventas
public class AdminVentasServlet extends HttpServlet {

    // DAO para consultar órdenes y actualizar estados de pago
    // Conecta con tablas: ordenes, orden_detalle, pagos_vendedor
    private OrdenDAO ordenDAO = new OrdenDAO();

    // DAO para traer lista de productos (usado en el filtro del formulario)
    // Conecta con tabla: productos
    private ProductoDAO productoDAO = new ProductoDAO();

    // DAO para traer listas de vendedores y clientes (usados en los filtros)
    // Conecta con tabla: usuarios
    private UsuarioDAO usuarioDAO = new UsuarioDAO();

    /**
     * GET /admin/ventas — Muestra todas las ventas con filtros opcionales.
     * Los filtros vienen como parámetros en la URL: ?idProducto=X&idVendedor=Y&idCliente=Z
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // Leer parámetros de filtro de la URL (pueden ser null si no se filtra)
        String idProductoStr = request.getParameter("idProducto"); // Filtro por producto
        String idVendedorStr = request.getParameter("idVendedor"); // Filtro por vendedor
        String idClienteStr  = request.getParameter("idCliente");  // Filtro por cliente

        // Convertir a Integer o null si el parámetro está vacío o ausente
        // Un valor null en OrdenDAO.obtenerVentasAdmin significa "sin filtro = todos"
        Integer idProducto = (idProductoStr != null && !idProductoStr.isEmpty())
                             ? Integer.parseInt(idProductoStr) : null;
        Integer idVendedor = (idVendedorStr != null && !idVendedorStr.isEmpty())
                             ? Integer.parseInt(idVendedorStr) : null;
        Integer idCliente  = (idClienteStr  != null && !idClienteStr.isEmpty())
                             ? Integer.parseInt(idClienteStr)  : null;

        // Consultar ventas con los filtros aplicados
        // Cada VentaAdminDTO incluye datos de: ordenes + orden_detalle + pagos_vendedor + usuarios + productos
        List<VentaAdminDTO> ventas = ordenDAO.obtenerVentasAdmin(idProducto, idVendedor, idCliente);

        // Traer todos los productos para poblar el <select> de filtro en la JSP
        List<Producto> productos  = productoDAO.obtenerTodosAdmin(null, null, null);

        // Traer lista de vendedores para el <select> de filtro de vendedor
        List<Usuario>  vendedores = usuarioDAO.obtenerUsuariosPorTipo("VENDEDOR");

        // Traer lista de clientes para el <select> de filtro de cliente
        List<Usuario>  clientes   = usuarioDAO.obtenerUsuariosPorTipo("CLIENTE");

        // Pasar los datos al request para que la JSP los lea con request.getAttribute(...)
        request.setAttribute("ventas",     ventas);     // Lista de ventas → tabla principal
        request.setAttribute("productos",  productos);  // Lista de productos → <select>
        request.setAttribute("vendedores", vendedores); // Lista de vendedores → <select>
        request.setAttribute("clientes",   clientes);   // Lista de clientes → <select>

        // Si viene un mensaje de resultado de una operación POST anterior (en la URL)
        // p.ej. ?mensaje=Orden+aprobada → se pasa a la JSP para mostrar el alert
        String mensaje = request.getParameter("mensaje");
        if (mensaje != null) request.setAttribute("mensaje", mensaje);

        // Delegar la renderización a la vista admin_ventas.jsp
        request.getRequestDispatcher("/admin_ventas.jsp").forward(request, response);
    }

    /**
     * POST /admin/ventas — El administrador aprueba o rechaza el pago de una orden.
     * Parámetros de formulario: idOrden (int), accion ("APROBADO" | "RECHAZADO")
     *
     * Flujo de APROBADO:
     *   ordenes.estado_pago → APROBADO, estado_orden → COMPLETADA
     *   pagos_vendedor.estado → PAGADO, fecha_pago → NOW()
     *
     * Flujo de RECHAZADO:
     *   ordenes.estado_pago → RECHAZADO, estado_orden → CANCELADA
     *   vendedor_producto.stock_local → restaurado por cada ítem de la orden
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // Leer el ID de la orden y la acción desde el formulario HTML
        String idOrdenStr = request.getParameter("idOrden"); // ID de la orden a procesar
        String accion     = request.getParameter("accion");  // "APROBADO" o "RECHAZADO"

        // Validar que los parámetros llegaron y que la acción es válida
        if (idOrdenStr == null || accion == null ||
                (!accion.equals("APROBADO") && !accion.equals("RECHAZADO"))) {
            // Parámetros inválidos → redirigir con mensaje de error
            response.sendRedirect(request.getContextPath() + "/admin/ventas?mensaje=Accion+invalida");
            return;
        }

        // Convertir el ID de String a int
        int idOrden = Integer.parseInt(idOrdenStr);

        // Llamar al DAO para actualizar el estado de la orden y los pagos del vendedor
        // actualizarEstadoPago es transaccional: actualiza ordenes + pagos_vendedor en una sola operación
        boolean ok = ordenDAO.actualizarEstadoPago(idOrden, accion);

        // Construir el mensaje de resultado para mostrar en el GET siguiente
        String msg = ok
                ? (accion.equals("APROBADO")
                    ? "Orden+%23" + idOrden + "+aprobada+exitosamente"
                    : "Orden+%23" + idOrden + "+rechazada")
                : "Error+al+procesar+la+orden";

        // Redirigir de vuelta al listado de ventas con el mensaje de resultado
        // El patrón POST-Redirect-GET evita reenvío del formulario al recargar
        response.sendRedirect(request.getContextPath() + "/admin/ventas?mensaje=" + msg);
    }
}
