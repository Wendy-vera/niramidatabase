package com.nirami.controller;

import com.nirami.dao.OrdenDAO;
import com.nirami.dao.ProductoDAO;
import com.nirami.model.Producto;
import com.nirami.model.Usuario;
import com.nirami.model.VentaAdminDTO;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.List;

/**
 * Servlet que muestra el historial de ventas del VENDEDOR autenticado.
 * URL: /vendedor/ventas
 * GET → lista todas las ventas del vendedor, con filtro opcional por producto
 *
 * La diferencia con AdminVentasServlet:
 *   - AdminVentasServlet: ve TODAS las ventas de TODOS los vendedores
 *   - VendedorVentasServlet: ve SOLO sus propias ventas (filtra por id_vendedor automáticamente)
 *
 * Conexiones:
 *   → session.getAttribute("usuarioLogueado") (identificar el vendedor; puesto por LoginServlet)
 *   → OrdenDAO.obtenerVentasAdmin()            (reutiliza el método con filtro de vendedor fijo)
 *   → ProductoDAO.obtenerPorVendedor()         (lista de productos del vendedor para el filtro)
 *   → vendedor_ventas.jsp                      (vista que muestra el historial de ventas)
 */
@WebServlet("/vendedor/ventas") // Responde a /vendedor/ventas
public class VendedorVentasServlet extends HttpServlet {

    // DAO para consultar ventas desde la BD (tablas: ordenes, orden_detalle, pagos_vendedor)
    private OrdenDAO ordenDAO = new OrdenDAO();

    // DAO para traer la lista de productos del vendedor (para el filtro del formulario)
    // Conecta con: SELECT * FROM productos JOIN vendedor_producto WHERE id_vendedor = ?
    private ProductoDAO productoDAO = new ProductoDAO();

    /**
     * GET /vendedor/ventas → Carga y muestra las ventas del vendedor logueado.
     * Parámetro opcional: ?idProducto=X para filtrar por un producto específico
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // Obtener la sesión SIN crear una nueva (false = no crear si no existe)
        HttpSession session = request.getSession(false);

        // Obtener el vendedor logueado desde la sesión
        // Este objeto fue guardado por LoginServlet con session.setAttribute("usuarioLogueado", usuario)
        Usuario vendedor = (Usuario) session.getAttribute("usuarioLogueado");

        // Leer el filtro de producto opcional (?idProducto=X en la URL)
        String idProductoStr = request.getParameter("idProducto");

        // Convertir a Integer o null si no se proporcionó el filtro
        Integer idProducto = (idProductoStr != null && !idProductoStr.isEmpty())
                             ? Integer.parseInt(idProductoStr) : null;

        /* Consultar las ventas usando el método compartido con AdminVentasServlet.
         * Parámetros:
         *   idProducto              = filtro de producto (null = todos)
         *   vendedor.getIdUsuario() = ID del vendedor logueado (SIEMPRE fijo, no puede ver los de otro)
         *   null                    = sin filtro de cliente (ve todos sus clientes)
         *
         * La query SQL incluye un WHERE od.id_vendedor = vendedor.getIdUsuario()
         * lo que garantiza que el vendedor SOLO ve sus propias ventas
         */
        List<VentaAdminDTO> ventas = ordenDAO.obtenerVentasAdmin(
            idProducto,                  // Filtro por producto (null = todos)
            vendedor.getIdUsuario(),     // SIEMPRE el vendedor logueado (seguridad)
            null                         // Sin filtro de cliente
        );

        // Traer los productos del vendedor para el <select> de filtro en la JSP
        // Permite al vendedor filtrar sus ventas por un producto específico
        List<Producto> productos = productoDAO.obtenerPorVendedor(vendedor.getIdUsuario());

        // Pasar los datos a la vista para que los renderice en la tabla
        request.setAttribute("ventas",    ventas);    // Lista de ventas → tabla principal
        request.setAttribute("productos", productos); // Lista de productos → <select> de filtro

        // Delegar la renderización a la vista de ventas del vendedor
        request.getRequestDispatcher("/vendedor_ventas.jsp").forward(request, response);
    }
}
