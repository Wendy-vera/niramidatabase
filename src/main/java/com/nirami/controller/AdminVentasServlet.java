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

@WebServlet("/admin/ventas")
public class AdminVentasServlet extends HttpServlet {
    private OrdenDAO ordenDAO = new OrdenDAO();
    private ProductoDAO productoDAO = new ProductoDAO();
    private UsuarioDAO usuarioDAO = new UsuarioDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String idProductoStr = request.getParameter("idProducto");
        String idVendedorStr = request.getParameter("idVendedor");
        String idClienteStr  = request.getParameter("idCliente");

        Integer idProducto = (idProductoStr != null && !idProductoStr.isEmpty()) ? Integer.parseInt(idProductoStr) : null;
        Integer idVendedor = (idVendedorStr != null && !idVendedorStr.isEmpty()) ? Integer.parseInt(idVendedorStr) : null;
        Integer idCliente  = (idClienteStr  != null && !idClienteStr.isEmpty())  ? Integer.parseInt(idClienteStr)  : null;

        List<VentaAdminDTO> ventas = ordenDAO.obtenerVentasAdmin(idProducto, idVendedor, idCliente);

        List<Producto> productos  = productoDAO.obtenerTodosAdmin(null, null, null);
        List<Usuario>  vendedores = usuarioDAO.obtenerUsuariosPorTipo("VENDEDOR");
        List<Usuario>  clientes   = usuarioDAO.obtenerUsuariosPorTipo("CLIENTE");

        request.setAttribute("ventas",     ventas);
        request.setAttribute("productos",  productos);
        request.setAttribute("vendedores", vendedores);
        request.setAttribute("clientes",   clientes);

        String mensaje = request.getParameter("mensaje");
        if (mensaje != null) request.setAttribute("mensaje", mensaje);

        request.getRequestDispatcher("/admin_ventas.jsp").forward(request, response);
    }

    /**
     * POST: el administrador aprueba o rechaza el pago de una orden.
     * Parámetros esperados: idOrden (int), accion ("APROBADO" | "RECHAZADO")
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String idOrdenStr = request.getParameter("idOrden");
        String accion     = request.getParameter("accion");

        if (idOrdenStr == null || accion == null ||
                (!accion.equals("APROBADO") && !accion.equals("RECHAZADO"))) {
            response.sendRedirect(request.getContextPath() + "/admin/ventas?mensaje=Accion+invalida");
            return;
        }

        int idOrden = Integer.parseInt(idOrdenStr);
        boolean ok  = ordenDAO.actualizarEstadoPago(idOrden, accion);

        String msg = ok
                ? (accion.equals("APROBADO") ? "Orden+%23" + idOrden + "+aprobada+exitosamente" : "Orden+%23" + idOrden + "+rechazada")
                : "Error+al+procesar+la+orden";

        response.sendRedirect(request.getContextPath() + "/admin/ventas?mensaje=" + msg);
    }
}
