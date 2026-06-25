<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.nirami.model.Producto" %>
<%@ page import="com.nirami.model.Usuario" %>
<%
    Producto producto = (Producto) request.getAttribute("producto");
    Usuario usuarioLogueado = (Usuario) session.getAttribute("usuarioLogueado");
    String rol = (String) session.getAttribute("rol");
%>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><%= producto.getNombre() %> - Nirami</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/styles.css">
    <style>
        .detalle-container {
            display: flex;
            gap: 3rem;
            background: var(--blanco-puro);
            padding: 2rem;
            border-radius: 12px;
            box-shadow: 0 4px 15px rgba(0,0,0,0.05);
            margin-top: 2rem;
        }
        .detalle-imagen {
            flex: 1;
            background: var(--gris);
            border-radius: 8px;
            min-height: 400px;
            display: flex;
            align-items: center;
            justify-content: center;
            overflow: hidden;
        }
        .detalle-imagen img {
            width: 100%;
            height: auto;
            object-fit: cover;
        }
        .detalle-info {
            flex: 1;
        }
        .detalle-precio {
            font-size: 2.5rem;
            color: var(--azul);
            font-weight: 700;
            margin: 1rem 0;
        }
        .detalle-descripcion {
            font-size: 1.1rem;
            color: #555;
            margin-bottom: 2rem;
        }
        .acciones {
            display: flex;
            gap: 1rem;
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
                    <li><a href="${pageContext.request.contextPath}/carrito">Carrito</a></li>
                    <li><a href="${pageContext.request.contextPath}/perfil">Perfil</a></li>
                    <li><a href="${pageContext.request.contextPath}/logout" class="btn btn-secondary">Salir</a></li>
                <% } %>
            </ul>
        </nav>
    </header>

    <main class="container">
        <a href="${pageContext.request.contextPath}/catalogo" style="color:var(--azul); text-decoration:none;">&larr; Volver al catálogo</a>
        
        <div class="detalle-container">
            <div class="detalle-imagen">
                <% if(producto.getImagenPrincipal() != null) { %>
                    <img src="${pageContext.request.contextPath}/<%= producto.getImagenPrincipal() %>" alt="<%= producto.getNombre() %>">
                <% } else { %>
                    <span style="color:#888;">Sin Imagen</span>
                <% } %>
            </div>
            <div class="detalle-info">
                <h1 style="font-size: 2.5rem; margin-bottom: 0.5rem;"><%= producto.getNombre() %></h1>
                <p style="color: #888;">Vistas: <%= producto.getVistas() %></p>
                
                <div class="detalle-precio">$<%= producto.getPrecio() %></div>
                
                <div class="detalle-descripcion">
                    <%= producto.getDescripcion() %>
                </div>
                
                <p><strong>Disponibles:</strong> <%= producto.getStock() %></p>
                
                <div class="acciones" style="margin-top: 2rem;">
                    <% if("CLIENTE".equals(rol) || usuarioLogueado == null) { %>
                        <div style="display: flex; gap: 1rem; align-items: center;">
                        <form action="${pageContext.request.contextPath}/carrito" method="post" style="flex: 1;">
                            <input type="hidden" name="action" value="agregar">
                            <input type="hidden" name="idProducto" value="<%= producto.getIdProducto() %>">
                            <button type="submit" class="btn btn-primary" style="width: 100%; padding: 15px; font-size: 1.1rem;">🛒 Añadir al Carrito</button>
                        </form>
                        
                        <form action="${pageContext.request.contextPath}/favoritos/toggle" method="post">
                            <input type="hidden" name="idProducto" value="<%= producto.getIdProducto() %>">
                            <input type="hidden" name="action" value="agregar">
                            <button type="submit" class="btn btn-secondary" style="padding: 15px; font-size: 1.1rem;" title="Añadir a favoritos">🤍</button>
                        </form>
                    </div>
                    <% } %>
                </div>
            </div>
        </div>
    </main>
</body>
</html>
