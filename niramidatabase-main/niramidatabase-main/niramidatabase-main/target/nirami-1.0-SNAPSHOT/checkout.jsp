<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.nirami.model.CartItem" %>
<%@ page import="java.util.List" %>
<%@ page import="java.math.BigDecimal" %>
<%@ page import="java.math.RoundingMode" %>
<%
    List<CartItem> carrito = (List<CartItem>) session.getAttribute("carrito");
    BigDecimal subtotal = BigDecimal.ZERO;
    if(carrito != null) {
        for(CartItem item : carrito) {
            subtotal = subtotal.add(item.getSubtotal());
        }
    }
    BigDecimal iva = subtotal.multiply(new BigDecimal("0.19")).setScale(2, RoundingMode.HALF_UP);
    BigDecimal total = subtotal.add(iva);
    String error = (String) request.getAttribute("error");
%>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Finalizar Compra - Nirami</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/styles.css">
    <style>
        .checkout-container {
            display: flex;
            gap: 2rem;
            margin-top: 2rem;
        }
        .checkout-form {
            flex: 2;
            background: var(--blanco-puro);
            padding: 2rem;
            border-radius: 12px;
            box-shadow: 0 4px 15px rgba(0,0,0,0.05);
        }
        .checkout-summary {
            flex: 1;
            background: var(--blanco-puro);
            padding: 2rem;
            border-radius: 12px;
            box-shadow: 0 4px 15px rgba(0,0,0,0.05);
            height: fit-content;
        }
        .summary-row {
            display: flex;
            justify-content: space-between;
            margin-bottom: 0.5rem;
            font-size: 0.9rem;
        }
        .summary-total {
            font-size: 1.3rem;
            font-weight: 700;
            color: var(--azul);
            border-top: 2px solid var(--gris);
            padding-top: 1rem;
            margin-top: 1rem;
        }
    </style>
</head>
<body>
    <header>
        <a href="${pageContext.request.contextPath}/" class="logo">Nirami</a>
    </header>

    <main class="container">
        <h2>Detalles de Envío y Pago</h2>
        
        <% if (error != null) { %>
            <div class="alert alert-error"><%= error %></div>
        <% } %>

        <div class="checkout-container">
            <div class="checkout-form">
                <form action="${pageContext.request.contextPath}/checkout" method="post">
                    <h3>Dirección de Envío</h3>
                    <div class="form-group" style="margin-top:1rem;">
                        <label for="direccion">Dirección Completa</label>
                        <textarea id="direccion" name="direccion" class="form-control" required rows="3" placeholder="Ej. Calle 123 #45-67, Apartamento 8A, Ciudad"></textarea>
                    </div>

                    <h3 style="margin-top:2rem;">Método de Pago</h3>
                    <p style="color:#555; margin-bottom:1rem;">Por ahora estamos usando un entorno de prueba. El cobro se simulará exitosamente.</p>
                    
                    <div class="form-group">
                        <label>Nombre en la Tarjeta</label>
                        <input type="text" class="form-control" required value="TEST USER">
                    </div>
                    <div class="form-group">
                        <label>Número de Tarjeta</label>
                        <input type="text" class="form-control" required value="**** **** **** 1234">
                    </div>

                    <button type="submit" class="btn btn-primary" style="margin-top: 2rem; padding: 12px 30px; font-size: 1.1rem;">Confirmar y Pagar</button>
                    <a href="${pageContext.request.contextPath}/carrito" style="margin-left: 1rem; color: var(--azul);">Volver al carrito</a>
                </form>
            </div>
            
            <div class="checkout-summary">
                <h3>Resumen de la Orden</h3>
                <hr style="border:none; border-top:1px solid var(--gris); margin:1rem 0;">
                
                <% if(carrito != null) {
                    for(CartItem item : carrito) { %>
                    <div class="summary-row">
                        <span><%= item.getCantidad() %>x <%= item.getProducto().getNombre() %></span>
                        <span>$<%= item.getSubtotal() %></span>
                    </div>
                <%  }
                   } %>
                
                <hr style="border:none; border-top:1px solid var(--gris); margin:1rem 0;">
                <div class="summary-row">
                    <span>Subtotal</span>
                    <span>$<%= subtotal %></span>
                </div>
                <div class="summary-row">
                    <span>IVA (19%)</span>
                    <span>$<%= iva %></span>
                </div>
                <div class="summary-row summary-total">
                    <span>Total a Pagar</span>
                    <span>$<%= total %></span>
                </div>
            </div>
        </div>
    </main>
</body>
</html>
