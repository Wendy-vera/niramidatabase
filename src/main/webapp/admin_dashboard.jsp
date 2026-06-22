<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.nirami.model.Producto" %>
<%@ page import="java.util.List" %>
<%@ page import="com.nirami.model.Usuario" %>
<%
    Usuario usuarioLogueado = (Usuario) session.getAttribute("usuarioLogueado");
    List<Producto> productosPendientes = (List<Producto>) request.getAttribute("productosPendientes");
    String mensaje = request.getParameter("mensaje");
    String error = request.getParameter("error");
%>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Panel de Administrador - Nirami</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/styles.css">
    <style>
        .table {
            width: 100%;
            border-collapse: collapse;
            background: var(--blanco-puro);
            border-radius: 8px;
            overflow: hidden;
            box-shadow: 0 4px 10px rgba(0,0,0,0.05);
            margin-top: 2rem;
        }
        .table th, .table td {
            padding: 1rem;
            text-align: left;
            border-bottom: 1px solid var(--gris);
        }
        .table th {
            background-color: var(--oscuro);
            color: var(--blanco);
        }
        .img-thumbnail {
            width: 50px;
            height: 50px;
            object-fit: cover;
            border-radius: 4px;
        }
        .btn-small {
            padding: 6px 12px;
            font-size: 0.9rem;
        }
    </style>
</head>
<body>
    <header>
        <a href="${pageContext.request.contextPath}/" class="logo">Nirami Admin</a>
        <nav>
            <ul>
                <li><a href="${pageContext.request.contextPath}/admin/vendedores">Vendedores</a></li>
                <li><a href="${pageContext.request.contextPath}/admin/clientes">Clientes</a></li>
                <li><a href="${pageContext.request.contextPath}/admin/dashboard" style="color: var(--naranja)">Revisiones</a></li>
                <li><a href="${pageContext.request.contextPath}/admin/categorias">Categorías</a></li>
                <li><a href="${pageContext.request.contextPath}/admin/productos">Productos</a></li>
                <li><a href="${pageContext.request.contextPath}/admin/ventas">Ventas</a></li>
                <li><a href="${pageContext.request.contextPath}/logout" class="btn btn-secondary">Salir</a></li>
            </ul>
        </nav>
    </header>

    <main class="container">
        <h2>Panel de Revisión de Productos</h2>
        <p style="color: #555;">Aquí aparecen los productos sugeridos por los vendedores que esperan ser aprobados para el catálogo global.</p>

        <% if (mensaje != null) { %>
            <div class="alert alert-success" style="margin-top: 1rem;"><%= mensaje %></div>
        <% } %>
        <% if (error != null) { %>
            <div class="alert alert-error" style="margin-top: 1rem;"><%= error %></div>
        <% } %>

        <table class="table">
            <thead>
                <tr>
                    <th>Imagen</th>
                    <th>Nombre</th>
                    <th>Precio Sugerido</th>
                    <th>Fecha de Solicitud</th>
                    <th>Acción</th>
                </tr>
            </thead>
            <tbody>
                <% if (productosPendientes != null && !productosPendientes.isEmpty()) { 
                    for (Producto p : productosPendientes) { 
                %>
                <tr>
                    <td>
                        <% if(p.getImagenPrincipal() != null) { %>
                            <img src="${pageContext.request.contextPath}/<%= p.getImagenPrincipal() %>" class="img-thumbnail">
                        <% } else { %>
                            <div class="img-thumbnail" style="background:#eee; display:flex; align-items:center; justify-content:center; font-size:0.8rem; color:#888;">Sin Img</div>
                        <% } %>
                    </td>
                    <td><strong><%= p.getNombre() %></strong></td>
                    <td>$<%= p.getPrecio() %></td>
                    <td><%= p.getFechaCreacion() %></td>
                    <td>
                        <a href="${pageContext.request.contextPath}/admin/revision?id=<%= p.getIdProducto() %>" class="btn btn-primary btn-small">Evaluar</a>
                    </td>
                </tr>
                <%  } 
                   } else { %>
                <tr>
                    <td colspan="5" style="text-align:center; padding:2rem;">No hay productos pendientes de revisión. ¡Todo está al día!</td>
                </tr>
                <% } %>
            </tbody>
        </table>
    </main>
</body>
</html>
