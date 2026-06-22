<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.nirami.model.VentaAdminDTO" %>
<%@ page import="com.nirami.model.Producto" %>
<%@ page import="java.util.List" %>
<%
    List<VentaAdminDTO> ventas = (List<VentaAdminDTO>) request.getAttribute("ventas");
    List<Producto> productos = (List<Producto>) request.getAttribute("productos");
%>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Mis Ventas - Nirami Vendedor</title>
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
        .filter-form { display: flex; gap: 1rem; margin-bottom: 1rem; align-items: flex-end; }
    </style>
</head>
<body>
    <header>
        <a href="${pageContext.request.contextPath}/" class="logo">Nirami Vendedor</a>
        <nav>
            <ul>
                <li><a href="${pageContext.request.contextPath}/vendedor/dashboard">Mi Inventario</a></li>
                <li><a href="${pageContext.request.contextPath}/vendedor/ventas" style="color: var(--naranja)">Ventas</a></li>
                <li><a href="${pageContext.request.contextPath}/vendedor/pagos">Pagos</a></li>
                <li><a href="${pageContext.request.contextPath}/perfil">Perfil</a></li>
                <li><a href="${pageContext.request.contextPath}/logout" class="btn btn-secondary">Salir</a></li>
            </ul>
        </nav>
    </header>

    <main class="container">
        <h2>Historial de Mis Ventas</h2>
        
        <form class="filter-form" action="${pageContext.request.contextPath}/vendedor/ventas" method="get">
            <div class="form-group" style="margin-bottom: 0;">
                <label>Filtrar por Producto:</label>
                <select name="idProducto" class="form-control">
                    <option value="">Todos mis productos</option>
                    <% if(productos != null) { for(Producto p : productos) { %>
                        <option value="<%= p.getIdProducto() %>" <%= String.valueOf(p.getIdProducto()).equals(request.getParameter("idProducto")) ? "selected" : "" %>>
                            <%= p.getNombre() %>
                        </option>
                    <% } } %>
                </select>
            </div>
            <button type="submit" class="btn btn-primary">Filtrar</button>
            <a href="${pageContext.request.contextPath}/vendedor/ventas" class="btn btn-secondary">Limpiar</a>
        </form>

        <table class="table">
            <thead>
                <tr>
                    <th>Orden #</th>
                    <th>Fecha</th>
                    <th>Producto</th>
                    <th>Cant.</th>
                    <th>Precio U.</th>
                    <th>Subtotal (Ingreso)</th>
                    <th>Estado Pago</th>
                </tr>
            </thead>
            <tbody>
                <% if(ventas != null && !ventas.isEmpty()) { 
                    for(VentaAdminDTO v : ventas) { %>
                <tr>
                    <td><%= v.getIdOrden() %></td>
                    <td><%= v.getFechaOrden() %></td>
                    <td><%= v.getNombreProducto() %></td>
                    <td><%= v.getCantidad() %></td>
                    <td>$<%= v.getPrecioUnitario() %></td>
                    <td>$<%= v.getSubtotalLinea() %></td>
                    <td>
                        <span class="badge <%= v.getEstadoPago().equals("COMPLETADO") ? "badge-success" : "badge-warning" %>">
                            <%= v.getEstadoPago() %>
                        </span>
                    </td>
                </tr>
                <%  }
                   } else { %>
                <tr>
                    <td colspan="7" style="text-align: center;">Aún no tienes ventas de este producto o en general.</td>
                </tr>
                <% } %>
            </tbody>
        </table>
    </main>
</body>
</html>
