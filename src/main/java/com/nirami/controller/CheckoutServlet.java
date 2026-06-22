package com.nirami.controller;

import com.nirami.dao.OrdenDAO;
import com.nirami.model.CartItem;
import com.nirami.model.Usuario;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@WebServlet("/checkout")
public class CheckoutServlet extends HttpServlet {
    private OrdenDAO ordenDAO = new OrdenDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession();
        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
        List<CartItem> carrito = (List<CartItem>) session.getAttribute("carrito");

        if (usuario == null) {
            response.sendRedirect(request.getContextPath() + "/login?mensaje=Inicia+sesion+para+comprar");
            return;
        }
        
        if (carrito == null || carrito.isEmpty()) {
            response.sendRedirect(request.getContextPath() + "/carrito");
            return;
        }

        request.getRequestDispatcher("/checkout.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession();
        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
        List<CartItem> carrito = (List<CartItem>) session.getAttribute("carrito");

        if (usuario == null || carrito == null || carrito.isEmpty()) {
            response.sendRedirect(request.getContextPath() + "/carrito");
            return;
        }

        String direccion = request.getParameter("direccion");
        
        // Recalcular totales por seguridad
        BigDecimal subtotal = BigDecimal.ZERO;
        for (CartItem item : carrito) {
            subtotal = subtotal.add(item.getSubtotal());
        }
        BigDecimal iva = subtotal.multiply(new BigDecimal("0.19")).setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = subtotal.add(iva);

        boolean exito = ordenDAO.crearOrden(usuario.getIdUsuario(), subtotal, iva, total, direccion, carrito);

        if (exito) {
            // Limpiar carrito
            session.removeAttribute("carrito");
            request.getRequestDispatcher("/checkout_success.jsp").forward(request, response);
        } else {
            request.setAttribute("error", "Hubo un problema al procesar tu orden. Inténtalo más tarde.");
            request.getRequestDispatcher("/checkout.jsp").forward(request, response);
        }
    }
}
