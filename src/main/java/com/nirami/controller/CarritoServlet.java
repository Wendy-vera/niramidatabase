package com.nirami.controller;

import com.nirami.dao.ProductoDAO;
import com.nirami.model.CartItem;
import com.nirami.model.Producto;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@WebServlet("/carrito")
public class CarritoServlet extends HttpServlet {
    private ProductoDAO productoDAO = new ProductoDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession();
        List<CartItem> carrito = (List<CartItem>) session.getAttribute("carrito");

        if (carrito == null) {
            carrito = new ArrayList<>();
            session.setAttribute("carrito", carrito);
        }

        BigDecimal subtotal = BigDecimal.ZERO;
        for (CartItem item : carrito) {
            subtotal = subtotal.add(item.getSubtotal());
        }

        BigDecimal iva = subtotal.multiply(new BigDecimal("0.19")).setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = subtotal.add(iva);

        request.setAttribute("subtotal", subtotal);
        request.setAttribute("iva", iva);
        request.setAttribute("total", total);

        request.getRequestDispatcher("/carrito.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession();
        List<CartItem> carrito = (List<CartItem>) session.getAttribute("carrito");

        if (carrito == null) {
            carrito = new ArrayList<>();
            session.setAttribute("carrito", carrito);
        }

        String action = request.getParameter("action");
        String idParam = request.getParameter("idProducto");

        if (idParam != null) {
            int idProducto = Integer.parseInt(idParam);

            if ("agregar".equals(action)) {
                boolean existe = false;
                for (CartItem item : carrito) {
                    if (item.getProducto().getIdProducto() == idProducto) {
                        item.setCantidad(item.getCantidad() + 1);
                        existe = true;
                        break;
                    }
                }
                if (!existe) {
                    Producto p = productoDAO.obtenerPorId(idProducto);
                    if (p != null && p.getStock() > 0) {
                        carrito.add(new CartItem(p, 1));
                    }
                }
            } else if ("quitar".equals(action)) {
                carrito.removeIf(item -> item.getProducto().getIdProducto() == idProducto);
            } else if ("actualizar".equals(action)) {
                int nuevaCantidad = Integer.parseInt(request.getParameter("cantidad"));
                if(nuevaCantidad > 0) {
                    for (CartItem item : carrito) {
                        if (item.getProducto().getIdProducto() == idProducto) {
                            item.setCantidad(nuevaCantidad);
                            break;
                        }
                    }
                }
            }
        }

        response.sendRedirect(request.getContextPath() + "/carrito");
    }
}
