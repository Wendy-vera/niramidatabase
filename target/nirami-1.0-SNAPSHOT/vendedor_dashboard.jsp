<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.nirami.model.Producto" %>
<%@ page import="java.util.List" %>
<%@ page import="com.nirami.model.Usuario" %>
<%
    Usuario usuarioLogueado = (Usuario) session.getAttribute("usuarioLogueado");
    List<Producto> misProductos = (List<Producto>) request.getAttribute("misProductos");
    String mensaje = request.getParameter("mensaje");
%>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Dashboard Vendedor - Nirami</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/styles.css">
    <style>
        .dashboard-header {
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-top: 2rem;
            margin-bottom: 2rem;
        }
        .table {
            width: 100%;
            border-collapse: collapse;
            background: var(--blanco-puro);
            border-radius: 8px;
            overflow: hidden;
            box-shadow: 0 4px 10px rgba(0,0,0,0.05);
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
        .status-badge {
            padding: 4px 8px;
            border-radius: 4px;
            font-size: 0.85rem;
            font-weight: 600;
        }
        .status-pendiente { background: #ffeaa7; color: #d68910; }
        .status-aprobado { background: #e2fcf9; color: var(--turquesa); }
        .status-rechazado { background: #ffc9c9; color: #e74c3c; }
        
        .img-thumbnail {
            width: 50px;
            height: 50px;
            object-fit: cover;
            border-radius: 4px;
        }
    </style>
</head>
<body>
    <header>
        <a href="${pageContext.request.contextPath}/" class="logo">Nirami Vendedor</a>
        <nav>
            <ul>
                <li><a href="${pageContext.request.contextPath}/vendedor/dashboard" style="color: var(--naranja)">Mi Inventario</a></li>
                <li><a href="${pageContext.request.contextPath}/vendedor/ventas">Ventas</a></li>
                <li><a href="${pageContext.request.contextPath}/vendedor/pagos">Pagos</a></li>
                <li><a href="${pageContext.request.contextPath}/perfil" style="display:inline-flex; align-items:center; gap:5px;">
                    <% if(usuarioLogueado.getFotoPerfil() != null && !usuarioLogueado.getFotoPerfil().isEmpty()) { %>
                        <img src="${pageContext.request.contextPath}/<%= usuarioLogueado.getFotoPerfil() %>" alt="Perfil" style="width: 25px; height: 25px; border-radius: 50%; object-fit: cover;">
                    <% } %>
                    Perfil
                </a></li>
                <li><a href="${pageContext.request.contextPath}/logout" class="btn btn-secondary">Salir</a></li>
            </ul>
        </nav>
    </header>

    <main class="container">
        <% if (mensaje != null) { %>
            <div class="alert alert-success"><%= mensaje %></div>
        <% } %>

        <div class="dashboard-header">
            <h2>Panel de Vendedor: Hola, <%= usuarioLogueado.getNombre() %></h2>
            <a href="${pageContext.request.contextPath}/vendedor/producto?action=crear" class="btn btn-primary">+ Nuevo Producto</a>
        </div>

        <h3>Tus Productos (Inventario Local)</h3>
        
        <% String estadoFiltro = (String) request.getAttribute("estadoFiltro"); %>
        <form action="${pageContext.request.contextPath}/vendedor/dashboard" method="get" class="filtros" style="display: flex; gap: 10px; align-items: center; margin-bottom: 1rem;">
            <label style="font-weight: bold;">Filtrar por Estado:</label>
            <select name="estado" class="form-control" style="width: auto;">
                <option value="">Todos</option>
                <option value="PENDIENTE" <%= "PENDIENTE".equals(estadoFiltro) ? "selected" : "" %>>Pendiente</option>
                <option value="APROBADO" <%= "APROBADO".equals(estadoFiltro) ? "selected" : "" %>>Aprobado</option>
                <option value="RECHAZADO" <%= "RECHAZADO".equals(estadoFiltro) ? "selected" : "" %>>Rechazado</option>
            </select>
            <button type="submit" class="btn btn-primary" style="padding: 5px 15px;">Filtrar</button>
            <a href="${pageContext.request.contextPath}/vendedor/dashboard" class="btn btn-secondary" style="padding: 5px 15px;">Limpiar</a>
        </form>

        <table class="table">
            <thead>
                <tr>
                    <th>Imagen</th>
                    <th>Nombre</th>
                    <th>Precio</th>
                    <th>Stock (Tuyo)</th>
                    <th>Estado en Catálogo</th>
                    <th>Acciones</th>
                </tr>
            </thead>
            <tbody>
                <% if (misProductos != null && !misProductos.isEmpty()) { 
                    for (Producto p : misProductos) { 
                        String statusClass = "";
                        if("PENDIENTE".equals(p.getEstado()) || "EN_CORRECCION".equals(p.getEstado())) statusClass = "status-pendiente";
                        else if("APROBADO".equals(p.getEstado())) statusClass = "status-aprobado";
                        else statusClass = "status-rechazado";
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
                    <td><%= p.getStock() %></td>
                    <td><span class="status-badge <%= statusClass %>"><%= p.getEstado() %></span></td>
                    <td>
                        <a href="${pageContext.request.contextPath}/vendedor/producto?action=editar&id=<%= p.getIdProducto() %>" class="btn btn-secondary" style="padding: 5px 10px; font-size:0.9rem;">Editar Producto</a>
                    </td>
                </tr>
                <%  } 
                   } else { %>
                <tr>
                    <td colspan="6" style="text-align:center; padding:2rem;">No tienes productos vinculados todavía. Crea uno nuevo o busca en el catálogo para venderlo.</td>
                </tr>
                <% } %>
            </tbody>
        </table>
    </main>
</body>
</html>
