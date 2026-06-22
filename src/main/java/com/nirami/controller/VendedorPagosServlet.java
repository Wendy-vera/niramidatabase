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

@WebServlet("/vendedor/pagos")
public class VendedorPagosServlet extends HttpServlet {
    private OrdenDAO ordenDAO = new OrdenDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        Usuario vendedor = (Usuario) session.getAttribute("usuarioLogueado");

        List<VentaAdminDTO> ventas = ordenDAO.obtenerVentasAdmin(null, vendedor.getIdUsuario(), null);
        
        BigDecimal totalGenerado = BigDecimal.ZERO;
        BigDecimal pagosPendientes = BigDecimal.ZERO;
        BigDecimal pagosCompletados = BigDecimal.ZERO;

        for (VentaAdminDTO v : ventas) {
            totalGenerado = totalGenerado.add(v.getSubtotalLinea());
            if ("COMPLETADO".equals(v.getEstadoPago())) {
                pagosCompletados = pagosCompletados.add(v.getSubtotalLinea());
            } else {
                pagosPendientes = pagosPendientes.add(v.getSubtotalLinea());
            }
        }

        request.setAttribute("totalGenerado", totalGenerado);
        request.setAttribute("pagosPendientes", pagosPendientes);
        request.setAttribute("pagosCompletados", pagosCompletados);
        request.setAttribute("ventas", ventas);
        
        request.getRequestDispatcher("/vendedor_pagos.jsp").forward(request, response);
    }
}
