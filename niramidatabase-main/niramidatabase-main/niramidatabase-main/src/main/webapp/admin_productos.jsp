<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.nirami.model.Producto" %>
<%@ page import="com.nirami.model.Usuario" %>
<%@ page import="java.util.List" %>
<%
    List<Producto> productos = (List<Producto>) request.getAttribute("productos");
    List<Usuario> vendedores = (List<Usuario>) request.getAttribute("vendedores");
    String error = (String) request.getAttribute("error");
%>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Gestión de Productos - Nirami</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/styles.css">
    <style>
        .table {
            width: 100%;
            border-collapse: collapse;
            background: var(--blanco-puro);
            border-radius: 8px;
            overflow: hidden;
            box-shadow: 0 4px 10px rgba(0,0,0,0.05);
            margin-top: 1rem;
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
        .img-thumbnail { width: 50px; height: 50px; object-fit: cover; border-radius: 4px; }
        .filter-form { display: flex; gap: 1rem; margin-bottom: 1rem; align-items: flex-end; }
    </style>
</head>
<body>
    <header>
        <a href="${pageContext.request.contextPath}/" class="logo">Nirami Admin</a>
        <nav>
            <ul>
                <li><a href="${pageContext.request.contextPath}/admin/vendedores">Vendedores</a></li>
                <li><a href="${pageContext.request.contextPath}/admin/clientes">Clientes</a></li>
                <li><a href="${pageContext.request.contextPath}/admin/dashboard">Revisiones</a></li>
                <li><a href="${pageContext.request.contextPath}/admin/categorias">Categorías</a></li>
                <li><a href="${pageContext.request.contextPath}/admin/productos" style="color: var(--naranja)">Productos</a></li>
                <li><a href="${pageContext.request.contextPath}/admin/ventas">Ventas</a></li>
                <li><a href="${pageContext.request.contextPath}/logout" class="btn btn-secondary">Salir</a></li>
            </ul>
        </nav>
    </header>

    <main class="container">
        <h2>Catálogo Global de Productos</h2>
        
        <form class="filter-form" action="${pageContext.request.contextPath}/admin/productos" method="get">
            <div class="form-group" style="margin-bottom: 0;">
                <label>Estado:</label>
                <select name="estado" class="form-control">
                    <option value="">Todos</option>
                    <option value="APROBADO" <%= "APROBADO".equals(request.getParameter("estado")) ? "selected" : "" %>>Aprobados</option>
                    <option value="PENDIENTE" <%= "PENDIENTE".equals(request.getParameter("estado")) ? "selected" : "" %>>Pendientes</option>
                    <option value="RECHAZADO" <%= "RECHAZADO".equals(request.getParameter("estado")) ? "selected" : "" %>>Rechazados</option>
                </select>
            </div>
            <div class="form-group" style="margin-bottom: 0;">
                <label>Vendedor:</label>
                <select name="idVendedor" class="form-control">
                    <option value="">Todos</option>
                    <% if(vendedores != null) { for(Usuario v : vendedores) { %>
                        <option value="<%= v.getIdUsuario() %>" <%= String.valueOf(v.getIdUsuario()).equals(request.getParameter("idVendedor")) ? "selected" : "" %>>
                            <%= v.getNombre() %>
                        </option>
                    <% } } %>
                </select>
            </div>
            <div class="form-group" style="margin-bottom: 0;">
                <label>Fecha (Creación):</label>
                <input type="date" name="fecha" class="form-control" value="<%= request.getParameter("fecha") != null ? request.getParameter("fecha") : "" %>">
            </div>
            <button type="submit" class="btn btn-primary">Filtrar</button>
            <a href="${pageContext.request.contextPath}/admin/productos" class="btn btn-secondary">Limpiar</a>
        </form>

        <% if(error != null) { %>
            <div class="alert alert-error" style="margin-bottom: 1rem;"><%= error %></div>
        <% } %>

        <table class="table">
            <thead>
                <tr>
                    <th>Imagen</th>
                    <th>Nombre</th>
                    <th>Precio</th>
                    <th>Stock</th>
                    <th>Estado</th>
                    <th>Fecha Creación</th>
                    <th>Acciones</th>
                </tr>
            </thead>
            <tbody>
                <% if(productos != null && !productos.isEmpty()) { 
                    for(Producto p : productos) { %>
                <tr>
                    <td>
                        <% if(p.getImagenPrincipal() != null) { %>
                            <img src="${pageContext.request.contextPath}/<%= p.getImagenPrincipal() %>" class="img-thumbnail" alt="<%= p.getNombre() %>">
                        <% } else { %>
                            <div class="img-thumbnail" style="background:#eee; display:flex; align-items:center; justify-content:center;">Sin img</div>
                        <% } %>
                    </td>
                    <td><%= p.getNombre() %></td>
                    <td>$<%= p.getPrecio() %></td>
                    <td><%= p.getStock() %></td>
                    <td>
                        <span class="badge 
                            <%= p.getEstado().equals("APROBADO") ? "badge-success" : 
                                p.getEstado().equals("PENDIENTE") ? "badge-warning" : "badge-error" %>">
                            <%= p.getEstado() %>
                        </span>
                    </td>
                    <td><%= p.getFechaCreacion() %></td>
                    <td>
                        <form action="${pageContext.request.contextPath}/admin/productos" method="post" style="display:flex; gap: 5px; flex-wrap: wrap; justify-content: center;">
                            <input type="hidden" name="idProducto" value="<%= p.getIdProducto() %>">
                            <% if (!"SUSPENDIDO".equals(p.getEstado())) { %>
                                <button type="submit" name="accion" value="suspender" class="btn btn-warning btn-small" onclick="return confirm('¿Seguro que deseas suspender el producto?');">Suspender</button>
                            <% } %>
                            <button type="submit" name="accion" value="eliminar" class="btn btn-error btn-small" onclick="return confirm('¿Seguro que deseas eliminar el producto?');">Eliminar</button>
                        </form>
                    </td>
                </tr>
                <%  }
                   } else { %>
                <tr>
                    <td colspan="7" style="text-align: center;">No se encontraron productos.</td>
                </tr>
                <% } %>
            </tbody>
        </table>
    </main>
</body>
</html>
