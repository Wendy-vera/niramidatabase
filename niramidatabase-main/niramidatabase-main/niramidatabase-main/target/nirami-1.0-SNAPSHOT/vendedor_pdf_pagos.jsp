<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.nirami.model.Usuario" %>
<%@ page import="com.nirami.model.VentaAdminDTO" %>
<%@ page import="java.util.List" %>
<%@ page import="java.math.BigDecimal" %>
<%@ page import="java.util.Date" %>
<%@ page import="java.text.SimpleDateFormat" %>
<%
    Usuario vendedor = (Usuario) session.getAttribute("usuarioLogueado");
    if (vendedor == null || !"VENDEDOR".equals(vendedor.getTipoUsuario())) {
        response.sendRedirect(request.getContextPath() + "/login");
        return;
    }
    List<VentaAdminDTO> completados = (List<VentaAdminDTO>) request.getAttribute("completados");
    BigDecimal totalCompletados = (BigDecimal) request.getAttribute("totalCompletados");
    
    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
    String fechaActual = sdf.format(new Date());
%>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <title>Reporte PDF - Pagos Completados</title>
    <style>
        body {
            font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif;
            margin: 0;
            padding: 40px;
            color: #333;
        }
        .header {
            text-align: center;
            margin-bottom: 30px;
            border-bottom: 2px solid #333;
            padding-bottom: 20px;
        }
        .header h1 {
            margin: 0 0 10px 0;
            font-size: 24px;
            color: #1a1a1a;
        }
        .header p {
            margin: 5px 0;
            font-size: 14px;
            color: #666;
        }
        .info-section {
            margin-bottom: 30px;
            font-size: 14px;
        }
        .info-section div {
            margin-bottom: 5px;
        }
        table {
            width: 100%;
            border-collapse: collapse;
            margin-bottom: 30px;
            font-size: 14px;
        }
        th, td {
            padding: 12px;
            text-align: left;
            border-bottom: 1px solid #ddd;
        }
        th {
            background-color: #f5f5f5;
            font-weight: bold;
            color: #333;
        }
        .total-section {
            text-align: right;
            font-size: 18px;
            font-weight: bold;
        }
        .footer {
            margin-top: 50px;
            text-align: center;
            font-size: 12px;
            color: #999;
        }
    </style>
</head>
<body onload="window.print(); setTimeout(function(){ window.close(); }, 1000);">
    <div class="header">
        <h1>Reporte de Pagos Completados</h1>
        <p>Generado el: <%= fechaActual %></p>
    </div>

    <div class="info-section">
        <div><strong>Vendedor:</strong> <%= vendedor.getNombre() %></div>
        <div><strong>Correo:</strong> <%= vendedor.getCorreo() %></div>
        <div><strong>Teléfono:</strong> <%= vendedor.getTelefono() != null ? vendedor.getTelefono() : "No registrado" %></div>
    </div>

    <table>
        <thead>
            <tr>
                <th>Fecha de Transacción</th>
                <th>Nombre del Producto</th>
                <th>Pago Generado (Ingreso)</th>
            </tr>
        </thead>
        <tbody>
            <% if(completados != null && !completados.isEmpty()) { 
                for(VentaAdminDTO v : completados) { %>
            <tr>
                <td><%= v.getFechaOrden() %></td>
                <td><%= v.getNombreProducto() %></td>
                <td>$<%= v.getSubtotalLinea() %></td>
            </tr>
            <%  }
               } else { %>
            <tr>
                <td colspan="3" style="text-align: center; padding: 20px;">No hay pagos completados registrados.</td>
            </tr>
            <% } %>
        </tbody>
    </table>

    <div class="total-section">
        Total de Pagos Completados: $<%= totalCompletados %>
    </div>

    <div class="footer">
        <p>Documento generado por el sistema automatizado de Nirami.</p>
        <p>Este reporte sirve como comprobante de los pagos procesados y completados exitosamente.</p>
    </div>
</body>
</html>
