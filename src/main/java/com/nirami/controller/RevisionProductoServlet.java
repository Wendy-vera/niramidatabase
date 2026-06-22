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

@WebServlet("/admin/revision")
public class RevisionProductoServlet extends HttpServlet {
    private ProductoDAO productoDAO = new ProductoDAO();
    private RevisionDAO revisionDAO = new RevisionDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession();
        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");

        if (usuario == null || !"ADMIN".equals(usuario.getTipoUsuario())) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        String idParam = request.getParameter("id");
        if (idParam != null) {
            try {
                int idProducto = Integer.parseInt(idParam);
                Producto p = productoDAO.obtenerPorId(idProducto);
                if (p != null && "PENDIENTE".equals(p.getEstado())) {
                    request.setAttribute("producto", p);
                    request.getRequestDispatcher("/admin_revision_producto.jsp").forward(request, response);
                    return;
                }
            } catch (NumberFormatException e) {
                // Ignorar
            }
        }
        response.sendRedirect(request.getContextPath() + "/admin/dashboard");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession();
        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");

        if (usuario == null || !"ADMIN".equals(usuario.getTipoUsuario())) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        int idProducto = Integer.parseInt(request.getParameter("idProducto"));
        String accion = request.getParameter("accion"); // APROBADO o RECHAZADO
        String observacion = request.getParameter("observacion");

        if ("RECHAZADO".equals(accion) && (observacion == null || observacion.trim().isEmpty())) {
            response.sendRedirect(request.getContextPath() + "/admin/revision?id=" + idProducto + "&error=Debe+ingresar+un+motivo+para+el+rechazo");
            return;
        }

        if ("APROBADO".equals(accion) || "RECHAZADO".equals(accion)) {
            boolean actualizado = productoDAO.actualizarEstado(idProducto, accion);
            if (actualizado) {
                revisionDAO.registrarRevision(idProducto, usuario.getIdUsuario(), accion, observacion);
                response.sendRedirect(request.getContextPath() + "/admin/dashboard?mensaje=Producto+" + accion.toLowerCase());
                return;
            }
        }
        
        response.sendRedirect(request.getContextPath() + "/admin/dashboard?error=Error+al+procesar+la+revision");
    }
}
