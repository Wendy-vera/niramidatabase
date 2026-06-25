<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.nirami.model.CartItem" %>
<%@ page import="java.util.List" %>
<%@ page import="java.math.BigDecimal" %>
<%@ page import="com.nirami.model.Usuario" %>
<%
    List<CartItem> carrito = (List<CartItem>) session.getAttribute("carrito");
    BigDecimal subtotal = (BigDecimal) request.getAttribute("subtotal");
    BigDecimal iva = (BigDecimal) request.getAttribute("iva");
    BigDecimal total = (BigDecimal) request.getAttribute("total");
    Usuario usuarioLogueado = (Usuario) session.getAttribute("usuarioLogueado");
%>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Carrito de Compras - Nirami</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/styles.css">
    <style>
        .cart-container {
            display: flex;
            gap: 2rem;
            margin-top: 2rem;
        }
        .cart-items {
            flex: 2;
            background: var(--blanco-puro);
            padding: 2rem;
            border-radius: 12px;
            box-shadow: 0 4px 15px rgba(0,0,0,0.05);
        }
        .cart-summary {
            flex: 1;
            background: var(--blanco-puro);
            padding: 2rem;
            border-radius: 12px;
            box-shadow: 0 4px 15px rgba(0,0,0,0.05);
            height: fit-content;
        }
        .cart-item {
            display: flex;
            gap: 1rem;
            border-bottom: 1px solid var(--gris);
            padding-bottom: 1rem;
            margin-bottom: 1rem;
            align-items: center;
        }
        .cart-item:last-child {
            border-bottom: none;
            margin-bottom: 0;
            padding-bottom: 0;
        }
        .cart-item img {
            width: 80px;
            height: 80px;
            object-fit: cover;
            border-radius: 8px;
        }
        .cart-item-details {
            flex: 1;
        }
        .cart-item-price {
            font-weight: 600;
            color: var(--azul);
        }
        .qty-input {
            width: 60px;
            padding: 5px;
            border: 1px solid var(--gris);
            border-radius: 4px;
        }
        .summary-row {
            display: flex;
            justify-content: space-between;
            margin-bottom: 1rem;
        }
        .summary-total {
            font-size: 1.5rem;
            font-weight: 700;
            color: var(--azul);
            border-top: 2px solid var(--gris);
            padding-top: 1rem;
        }
    </style>
</head>
<body>
    <header>
        <a href="${pageContext.request.contextPath}/" class="logo">Nirami</a>
        <nav>
            <ul>
                <li><a href="${pageContext.request.contextPath}/">Inicio / Catálogo</a></li>
                <% if (usuarioLogueado == null) { %>
                    <li><a href="${pageContext.request.contextPath}/login" class="btn btn-primary">Iniciar Sesión</a></li>
                <% } else { %>
                    <li><a href="${pageContext.request.contextPath}/favoritos">Favoritos</a></li>
                    <li><a href="${pageContext.request.contextPath}/carrito" style="color: var(--naranja)">Carrito</a></li>
                    <li><a href="${pageContext.request.contextPath}/perfil" style="display:inline-flex; align-items:center; gap:5px;">
                        <% if(usuarioLogueado.getFotoPerfil() != null && !usuarioLogueado.getFotoPerfil().isEmpty()) { %>
                            <img src="${pageContext.request.contextPath}/<%= usuarioLogueado.getFotoPerfil() %>" alt="Perfil" style="width: 25px; height: 25px; border-radius: 50%; object-fit: cover;">
                        <% } %>
                        Perfil
                    </a></li>
                    <li><a href="${pageContext.request.contextPath}/logout" class="btn btn-secondary">Salir</a></li>
                <% } %>
            </ul>
        </nav>
    </header>

    <main class="container">
        <h2>Tu Carrito de Compras</h2>
        
        <div class="cart-container">
            <div class="cart-items">
                <% if (carrito != null && !carrito.isEmpty()) { 
                    for(CartItem item : carrito) {
                %>
                    <div class="cart-item">
                        <% if(item.getProducto().getImagenPrincipal() != null) { %>
                            <img src="${pageContext.request.contextPath}/<%= item.getProducto().getImagenPrincipal() %>" alt="<%= item.getProducto().getNombre() %>">
                        <% } else { %>
                            <div style="width:80px; height:80px; background:#eee; display:flex; align-items:center; justify-content:center; color:#888; border-radius:8px; font-size:0.8rem;">Sin Img</div>
                        <% } %>
                        
                        <div class="cart-item-details">
                            <h4 style="margin:0;"><%= item.getProducto().getNombre() %></h4>
                            <p class="cart-item-price">$<%= item.getProducto().getPrecio() %> c/u</p>
                        </div>
                        
                        <div>
                            <form action="${pageContext.request.contextPath}/carrito" method="post" style="display:flex; align-items:center; gap:0.5rem;">
                                <input type="hidden" name="action" value="actualizar">
                                <input type="hidden" name="idProducto" value="<%= item.getProducto().getIdProducto() %>">
                                <input type="number" name="cantidad" value="<%= item.getCantidad() %>" min="1" max="<%= item.getProducto().getStock() %>" class="qty-input">
                                <button type="submit" class="btn" style="padding: 5px 10px; background:var(--gris);">↻</button>
                            </form>
                        </div>
                        
                        <div style="font-weight:600; width:80px; text-align:right;">
                            $<%= item.getSubtotal() %>
                        </div>
                        
                        <div>
                            <form action="${pageContext.request.contextPath}/carrito" method="post">
                                <input type="hidden" name="action" value="quitar">
                                <input type="hidden" name="idProducto" value="<%= item.getProducto().getIdProducto() %>">
                                <button type="submit" class="btn" style="background:none; color:#e74c3c; font-size:1.2rem; border:none; cursor:pointer;">&times;</button>
                            </form>
                        </div>
                    </div>
                <%  }
                   } else { %>
                    <p style="text-align:center; padding: 2rem;">Tu carrito está vacío. <a href="${pageContext.request.contextPath}/catalogo" style="color:var(--azul);">Explorar catálogo</a></p>
                <% } %>
            </div>
            
            <div class="cart-summary">
                <h3 style="margin-bottom: 1.5rem;">Resumen de Orden</h3>
                <div class="summary-row">
                    <span>Subtotal</span>
                    <span>$<%= subtotal != null ? subtotal : "0.00" %></span>
                </div>
                <div class="summary-row">
                    <span>IVA (19%)</span>
                    <span>$<%= iva != null ? iva : "0.00" %></span>
                </div>
                <div class="summary-row summary-total">
                    <span>Total</span>
                    <span>$<%= total != null ? total : "0.00" %></span>
                </div>
                
                <% if (carrito != null && !carrito.isEmpty()) { %>
                    <a href="${pageContext.request.contextPath}/checkout" class="btn btn-primary btn-block" style="text-align:center; margin-top:1.5rem;">Proceder al Pago</a>
                <% } %>
            </div>
        </div>
    </main>
</body>
</html>
