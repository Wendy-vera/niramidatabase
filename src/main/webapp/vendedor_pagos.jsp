<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.nirami.model.VentaAdminDTO" %>
<%@ page import="java.math.BigDecimal" %>
<%@ page import="java.util.List" %>
<%
    BigDecimal totalGenerado = (BigDecimal) request.getAttribute("totalGenerado");
    BigDecimal pagosPendientes = (BigDecimal) request.getAttribute("pagosPendientes");
    BigDecimal pagosCompletados = (BigDecimal) request.getAttribute("pagosCompletados");
    List<VentaAdminDTO> ventas = (List<VentaAdminDTO>) request.getAttribute("ventas");
%>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Reporte de Pagos - Nirami Vendedor</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/styles.css">
    <style>
        .report-cards {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
            gap: 1.5rem;
            margin-bottom: 2rem;
        }
        .card {
            background: var(--blanco-puro);
            padding: 1.5rem;
            border-radius: 12px;
            box-shadow: 0 4px 15px rgba(0,0,0,0.05);
            text-align: center;
        }
        .card h3 { color: var(--gris); font-size: 1.1rem; margin-bottom: 0.5rem; }
        .card .amount { font-size: 2rem; font-weight: 700; color: var(--oscuro); }
        .card.pending .amount { color: #d68910; }
        .card.completed .amount { color: var(--turquesa); }
        
        .table { width: 100%; border-collapse: collapse; background: var(--blanco-puro); border-radius: 8px; overflow: hidden; box-shadow: 0 4px 10px rgba(0,0,0,0.05); }
        .table th, .table td { padding: 1rem; text-align: left; border-bottom: 1px solid var(--gris); }
        .table th { background-color: var(--oscuro); color: var(--blanco); }
    </style>
</head>
<body>
    <header>
        <a href="${pageContext.request.contextPath}/" class="logo">Nirami Vendedor</a>
        <nav>
            <ul>
                <li><a href="${pageContext.request.contextPath}/vendedor/dashboard">Mi Inventario</a></li>
                <li><a href="${pageContext.request.contextPath}/vendedor/ventas">Ventas</a></li>
                <li><a href="${pageContext.request.contextPath}/vendedor/pagos" style="color: var(--naranja)">Pagos</a></li>
                <li><a href="${pageContext.request.contextPath}/perfil">Perfil</a></li>
                <li><a href="${pageContext.request.contextPath}/logout" class="btn btn-secondary">Salir</a></li>
            </ul>
        </nav>
    </header>

    <main class="container">
        <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 2rem;">
            <h2>Reporte de Pagos</h2>
            <button class="btn btn-primary" onclick="window.open('${pageContext.request.contextPath}/vendedor/pagos/pdf', '_blank')">Descargar PDF</button>
        </div>
        
        <div class="report-cards">
            <div class="card">
                <h3>Ingresos Totales (Ventas)</h3>
                <div class="amount">$<%= totalGenerado %></div>
            </div>
            <div class="card completed">
                <h3>Pagos Completados</h3>
                <div class="amount">$<%= pagosCompletados %></div>
            </div>
            <div class="card pending hide-on-print">
                <h3>Pagos Pendientes (Por Transferir)</h3>
                <div class="amount">$<%= pagosPendientes %></div>
            </div>
        </div>

        <h3>Detalle de Transacciones</h3>
        <table class="table">
            <thead>
                <tr>
                    <th>Fecha</th>
                    <th>Producto</th>
                    <th>Subtotal (Ingreso)</th>
                    <th>Estado de Pago</th>
                </tr>
            </thead>
            <tbody>
                <% if(ventas != null && !ventas.isEmpty()) { 
                    for(VentaAdminDTO v : ventas) { 
                        boolean esCompletado = v.getEstadoPago().equals("COMPLETADO");
                %>
                <tr class="<%= esCompletado ? "" : "hide-on-print" %>">
                    <td><%= v.getFechaOrden() %></td>
                    <td><%= v.getNombreProducto() %></td>
                    <td>$<%= v.getSubtotalLinea() %></td>
                    <td>
                        <span class="badge <%= esCompletado ? "badge-success" : "badge-warning" %>">
                            <%= v.getEstadoPago() %>
                        </span>
                    </td>
                </tr>
                <%  }
                   } else { %>
                <tr>
                    <td colspan="4" style="text-align: center;">No hay transacciones registradas.</td>
                </tr>
                <% } %>
            </tbody>
        </table>
    </main>
</body>
</html>
