<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.nirami.model.VentaAdminDTO" %>
<%@ page import="com.nirami.model.Producto" %>
<%@ page import="com.nirami.model.Usuario" %>
<%@ page import="java.util.List" %>
<%
    List<VentaAdminDTO> ventas    = (List<VentaAdminDTO>) request.getAttribute("ventas");
    List<Producto>      productos = (List<Producto>)      request.getAttribute("productos");
    List<Usuario>       vendedores= (List<Usuario>)       request.getAttribute("vendedores");
    List<Usuario>       clientes  = (List<Usuario>)       request.getAttribute("clientes");
    String              mensaje   = (String)              request.getAttribute("mensaje");
%>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Historial de Ventas - Nirami Admin</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/styles.css">
    <style>
        .table { width:100%; border-collapse:collapse; background:var(--blanco-puro); border-radius:8px; overflow:hidden; box-shadow:0 4px 10px rgba(0,0,0,0.05); margin-top:1rem; }
        .table th, .table td { padding:1rem; text-align:left; border-bottom:1px solid var(--gris); }
        .table th { background-color:var(--oscuro); color:var(--blanco); }
        .filter-form { display:flex; gap:1rem; margin-bottom:1rem; align-items:flex-end; flex-wrap:wrap; }
        .badge-success  { background:#28a745; color:#fff; padding:3px 8px; border-radius:4px; font-size:.82em; }
        .badge-danger   { background:#dc3545; color:#fff; padding:3px 8px; border-radius:4px; font-size:.82em; }
        .badge-warning  { background:#ffc107; color:#333; padding:3px 8px; border-radius:4px; font-size:.82em; }
        .action-btns    { display:flex; gap:6px; }
        .btn-approve    { background:#28a745; color:#fff; border:none; padding:5px 10px; border-radius:4px; cursor:pointer; font-size:.82em; }
        .btn-reject     { background:#dc3545; color:#fff; border:none; padding:5px 10px; border-radius:4px; cursor:pointer; font-size:.82em; }
        .alert-success  { background:#d4edda; border:1px solid #c3e6cb; color:#155724; padding:.75rem 1rem; border-radius:4px; margin-bottom:1rem; }
        .alert-danger   { background:#f8d7da; border:1px solid #f5c6cb; color:#721c24; padding:.75rem 1rem; border-radius:4px; margin-bottom:1rem; }
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
                <li><a href="${pageContext.request.contextPath}/admin/productos">Productos</a></li>
                <li><a href="${pageContext.request.contextPath}/admin/ventas" style="color:var(--naranja)">Ventas</a></li>
                <li><a href="${pageContext.request.contextPath}/admin/pagos-vendedores">💰 Pagos</a></li>
                <li><a href="${pageContext.request.contextPath}/logout" class="btn btn-secondary">Salir</a></li>
            </ul>
        </nav>
    </header>

    <main class="container">
        <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 1rem;">
            <h2 style="margin: 0;">Historial Global de Ventas</h2>
            <a href="${pageContext.request.contextPath}/admin/pagos-vendedores" class="btn btn-primary">💸 Gestionar Pagos a Vendedores</a>
        </div>

        <% if (mensaje != null && !mensaje.isEmpty()) {
               boolean esError = mensaje.toLowerCase().contains("error"); %>
        <div class="<%= esError ? "alert-danger" : "alert-success" %>">
            <%= mensaje %>
        </div>
        <% } %>

        <form class="filter-form" action="${pageContext.request.contextPath}/admin/ventas" method="get">
            <div class="form-group" style="margin-bottom:0;">
                <label>Producto:</label>
                <select name="idProducto" class="form-control">
                    <option value="">Todos</option>
                    <% if (productos != null) { for (Producto p : productos) { %>
                        <option value="<%= p.getIdProducto() %>" <%= String.valueOf(p.getIdProducto()).equals(request.getParameter("idProducto")) ? "selected" : "" %>>
                            <%= p.getNombre() %>
                        </option>
                    <% } } %>
                </select>
            </div>
            <div class="form-group" style="margin-bottom:0;">
                <label>Vendedor:</label>
                <select name="idVendedor" class="form-control">
                    <option value="">Todos</option>
                    <% if (vendedores != null) { for (Usuario v : vendedores) { %>
                        <option value="<%= v.getIdUsuario() %>" <%= String.valueOf(v.getIdUsuario()).equals(request.getParameter("idVendedor")) ? "selected" : "" %>>
                            <%= v.getNombre() %>
                        </option>
                    <% } } %>
                </select>
            </div>
            <div class="form-group" style="margin-bottom:0;">
                <label>Cliente:</label>
                <select name="idCliente" class="form-control">
                    <option value="">Todos</option>
                    <% if (clientes != null) { for (Usuario c : clientes) { %>
                        <option value="<%= c.getIdUsuario() %>" <%= String.valueOf(c.getIdUsuario()).equals(request.getParameter("idCliente")) ? "selected" : "" %>>
                            <%= c.getNombre() %>
                        </option>
                    <% } } %>
                </select>
            </div>
            <button type="submit" class="btn btn-primary">Filtrar</button>
            <a href="${pageContext.request.contextPath}/admin/ventas" class="btn btn-secondary">Limpiar</a>
        </form>

        <table class="table">
            <thead>
                <tr>
                    <th>Orden #</th>
                    <th>Fecha</th>
                    <th>Cliente</th>
                    <th>Producto</th>
                    <th>Vendedor</th>
                    <th>Cant.</th>
                    <th>Precio U.</th>
                    <th>Subtotal</th>
                    <th>Estado Pago</th>
                    <th>Acciones</th>
                </tr>
            </thead>
            <tbody>
                <% if (ventas != null && !ventas.isEmpty()) {
                       for (VentaAdminDTO v : ventas) {
                           boolean pendiente = "PENDIENTE".equals(v.getEstadoPago());
                           boolean aprobado  = "APROBADO".equals(v.getEstadoPago());
                           boolean rechazado = "RECHAZADO".equals(v.getEstadoPago()); %>
                <tr>
                    <td><%= v.getIdOrden() %></td>
                    <td><%= v.getFechaOrden() %></td>
                    <td><%= v.getNombreCliente() %></td>
                    <td><%= v.getNombreProducto() %></td>
                    <td><%= v.getNombreVendedor() %></td>
                    <td><%= v.getCantidad() %></td>
                    <td>$<%= v.getPrecioUnitario() %></td>
                    <td>$<%= v.getSubtotalLinea() %></td>
                    <td>
                        <span class="badge <%= aprobado ? "badge-success" : rechazado ? "badge-danger" : "badge-warning" %>">
                            <%= v.getEstadoPago() %>
                        </span>
                    </td>
                    <td>
                        <% if (pendiente) { %>
                        <div class="action-btns">
                            <form method="post" action="${pageContext.request.contextPath}/admin/ventas" style="display:inline;">
                                <input type="hidden" name="idOrden" value="<%= v.getIdOrden() %>">
                                <input type="hidden" name="accion"  value="APROBADO">
                                <button type="submit" class="btn-approve"
                                        onclick="return confirm('¿Aprobar el pago de la Orden #<%= v.getIdOrden() %>?')">
                                    ✓ Aprobar
                                </button>
                            </form>
                            <form method="post" action="${pageContext.request.contextPath}/admin/ventas" style="display:inline;">
                                <input type="hidden" name="idOrden" value="<%= v.getIdOrden() %>">
                                <input type="hidden" name="accion"  value="RECHAZADO">
                                <button type="submit" class="btn-reject"
                                        onclick="return confirm('¿Rechazar el pago de la Orden #<%= v.getIdOrden() %>? El stock se restaurará.')">
                                    ✗ Rechazar
                                </button>
                            </form>
                        </div>
                        <% } else { %>
                        <span style="color:#999; font-size:.85em;">Sin acciones</span>
                        <% } %>
                    </td>
                </tr>
                <%  }
                   } else { %>
                <tr>
                    <td colspan="10" style="text-align:center;">No hay registros de ventas con estos filtros.</td>
                </tr>
                <% } %>
            </tbody>
        </table>
    </main>
</body>
</html>
