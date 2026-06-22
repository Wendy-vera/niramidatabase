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

@WebServlet("/vendedor/pagos/pdf")
public class VendedorPagosPDFServlet extends HttpServlet {

    private OrdenDAO ordenDAO = new OrdenDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        Usuario vendedor = (Usuario) session.getAttribute("usuarioLogueado");

        if (vendedor == null || !"VENDEDOR".equals(vendedor.getTipoUsuario())) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        List<VentaAdminDTO> ventas = ordenDAO.obtenerVentasAdmin(null, vendedor.getIdUsuario(), null);
        
        // Filtrar SOLO completados
        List<VentaAdminDTO> completados = ventas.stream()
                .filter(v -> "COMPLETADO".equals(v.getEstadoPago()))
                .collect(Collectors.toList());
        
        BigDecimal totalCompletados = BigDecimal.ZERO;
        for (VentaAdminDTO v : completados) {
            totalCompletados = totalCompletados.add(v.getSubtotalLinea());
        }

        request.setAttribute("completados", completados);
        request.setAttribute("totalCompletados", totalCompletados);
        
        request.getRequestDispatcher("/vendedor_pdf_pagos.jsp").forward(request, response);
    }
}
