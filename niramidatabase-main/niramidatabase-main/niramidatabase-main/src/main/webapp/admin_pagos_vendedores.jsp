<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.nirami.model.VentaAdminDTO" %>
<%@ page import="java.util.List" %>
<%@ page import="com.nirami.model.Usuario" %>
<%
    Usuario usuarioLogueado = (Usuario) session.getAttribute("usuarioLogueado");
    List<VentaAdminDTO> resumenPagos = (List<VentaAdminDTO>) request.getAttribute("resumenPagos");
    String mensaje = (String) request.getAttribute("mensaje");
%>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Pagos a Vendedores - Nirami Admin</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/styles.css">
    <style>
        /* ── Tabla de pagos ── */
        .table {
            width: 100%;
            border-collapse: collapse;
            background: var(--blanco-puro);
            border-radius: 12px;
            overflow: hidden;
            box-shadow: 0 4px 15px rgba(0,0,0,0.08);
            margin-top: 1.5rem;
        }
        .table th, .table td {
            padding: 1rem 1.2rem;
            text-align: left;
            border-bottom: 1px solid var(--gris);
        }
        .table th {
            background-color: var(--oscuro);
            color: var(--blanco);
            font-weight: 600;
            font-size: 0.9rem;
            text-transform: uppercase;
            letter-spacing: 0.05em;
        }
        .table tr:hover td { background: #f8f9fa; }
        .table tr:last-child td { border-bottom: none; }

        /* ── Alertas ── */
        .alert-success {
            background: linear-gradient(135deg, #d4edda, #c3e6cb);
            border-left: 4px solid #28a745;
            color: #155724;
            padding: 1rem 1.2rem;
            border-radius: 8px;
            margin-bottom: 1.5rem;
            display: flex;
            align-items: center;
            gap: 0.75rem;
            font-weight: 500;
        }
        .alert-danger {
            background: linear-gradient(135deg, #f8d7da, #f5c6cb);
            border-left: 4px solid #dc3545;
            color: #721c24;
            padding: 1rem 1.2rem;
            border-radius: 8px;
            margin-bottom: 1.5rem;
            display: flex;
            align-items: center;
            gap: 0.75rem;
            font-weight: 500;
        }

        /* ── Cards de resumen ── */
        .summary-cards {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
            gap: 1.5rem;
            margin-bottom: 2rem;
        }
        .summary-card {
            background: var(--blanco-puro);
            border-radius: 12px;
            padding: 1.5rem;
            box-shadow: 0 4px 15px rgba(0,0,0,0.06);
            text-align: center;
            border-top: 4px solid var(--naranja);
        }
        .summary-card h4 {
            color: #666;
            font-size: 0.9rem;
            text-transform: uppercase;
            letter-spacing: 0.05em;
            margin-bottom: 0.5rem;
        }
        .summary-card .amount {
            font-size: 2rem;
            font-weight: 700;
            color: var(--oscuro);
        }

        /* ── Botón de pago ── */
        .btn-pagar {
            background: linear-gradient(135deg, #28a745, #20963b);
            color: #fff;
            border: none;
            padding: 0.55rem 1.2rem;
            border-radius: 8px;
            cursor: pointer;
            font-size: 0.875rem;
            font-weight: 600;
            display: inline-flex;
            align-items: center;
            gap: 0.4rem;
            transition: all 0.2s ease;
            box-shadow: 0 2px 6px rgba(40,167,69,0.3);
        }
        .btn-pagar:hover {
            transform: translateY(-1px);
            box-shadow: 0 4px 12px rgba(40,167,69,0.4);
        }
        .btn-pagar:active { transform: translateY(0); }

        /* ── Badge de cuenta bancaria ── */
        .cuenta-badge {
            background: #e9ecef;
            color: #495057;
            padding: 3px 10px;
            border-radius: 20px;
            font-family: monospace;
            font-size: 0.85rem;
        }

        /* ── Estado vacío ── */
        .empty-state {
            text-align: center;
            padding: 3rem;
            color: #aaa;
        }
        .empty-state .icon { font-size: 3rem; margin-bottom: 1rem; }
        .empty-state p { font-size: 1.1rem; }

        /* ── Animación de entrada ── */
        @keyframes fadeInUp {
            from { opacity: 0; transform: translateY(20px); }
            to   { opacity: 1; transform: translateY(0); }
        }
        main { animation: fadeInUp 0.4s ease; }
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
                <li><a href="${pageContext.request.contextPath}/admin/ventas">Ventas</a></li>
                <li><a href="${pageContext.request.contextPath}/admin/pagos-vendedores" style="color: var(--naranja)">💰 Pagos</a></li>
                <li><a href="${pageContext.request.contextPath}/logout" class="btn btn-secondary">Salir</a></li>
            </ul>
        </nav>
    </header>

    <main class="container">
        <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 1.5rem;">
            <div>
                <h2 style="margin: 0;">Pagos a Vendedores</h2>
                <p style="color: #666; margin: 0.25rem 0 0;">Administra los pagos pendientes de cada vendedor</p>
            </div>
            <a href="${pageContext.request.contextPath}/admin/ventas" class="btn btn-secondary">← Ver Todas las Ventas</a>
        </div>

        <%-- ── Mensaje de resultado ── --%>
        <% if (mensaje != null && !mensaje.isEmpty()) {
               boolean esError = mensaje.toLowerCase().contains("error"); %>
        <div class="<%= esError ? "alert-danger" : "alert-success" %>">
            <span><%= esError ? "❌" : "✅" %></span>
            <span><%= mensaje.replace("+", " ") %></span>
        </div>
        <% } %>

        <%-- ── Tarjetas de resumen ── --%>
        <%
            java.math.BigDecimal totalPendiente = java.math.BigDecimal.ZERO;
            int totalVendedores = 0;
            int totalTransacciones = 0;
            if (resumenPagos != null) {
                totalVendedores = resumenPagos.size();
                for (VentaAdminDTO r : resumenPagos) {
                    if (r.getSubtotalLinea() != null) {
                        totalPendiente = totalPendiente.add(r.getSubtotalLinea());
                    }
                    totalTransacciones += r.getCantidad();
                }
            }
        %>
        <div class="summary-cards">
            <div class="summary-card">
                <h4>Vendedores con pagos pendientes</h4>
                <div class="amount"><%= totalVendedores %></div>
            </div>
            <div class="summary-card" style="border-top-color: #e74c3c;">
                <h4>Total pendiente de pago</h4>
                <div class="amount" style="color: #e74c3c;">$<%= totalPendiente %></div>
            </div>
            <div class="summary-card" style="border-top-color: #3498db;">
                <h4>Transacciones pendientes</h4>
                <div class="amount" style="color: #3498db;"><%= totalTransacciones %></div>
            </div>
        </div>

        <%-- ── Tabla de vendedores con pagos pendientes ── --%>
        <h3 style="margin-bottom: 0.5rem;">Detalle por Vendedor</h3>
        <p style="color: #777; margin-top: 0; margin-bottom: 1rem; font-size: 0.9rem;">
            Al presionar <strong>Pagar</strong>, se marcarán como PAGADAS todas las transacciones pendientes del vendedor.
        </p>

        <table class="table">
            <thead>
                <tr>
                    <th>Vendedor</th>
                    <th>Cuenta Bancaria</th>
                    <th>Transacciones Pendientes</th>
                    <th>Monto Total a Pagar</th>
                    <th>Acción</th>
                </tr>
            </thead>
            <tbody>
                <% if (resumenPagos != null && !resumenPagos.isEmpty()) {
                       for (VentaAdminDTO r : resumenPagos) { %>
                <tr>
                    <td>
                        <strong><%= r.getNombreVendedor() %></strong>
                        <br><small style="color: #888;">ID: <%= r.getIdVendedor() %></small>
                    </td>
                    <td>
                        <span class="cuenta-badge"><%= r.getCuentaBancaria() != null ? r.getCuentaBancaria() : "No registrada" %></span>
                    </td>
                    <td style="text-align: center;">
                        <strong><%= r.getCantidad() %></strong> ventas
                    </td>
                    <td>
                        <strong style="font-size: 1.2rem; color: #e74c3c;">
                            $<%= r.getSubtotalLinea() %>
                        </strong>
                    </td>
                    <td>
                        <form method="post" action="${pageContext.request.contextPath}/admin/pagos-vendedores"
                              style="display: inline;"
                              onsubmit="return confirmarPago('<%= r.getNombreVendedor() %>', '<%= r.getSubtotalLinea() %>', '<%= r.getCuentaBancaria() != null ? r.getCuentaBancaria() : "No registrada" %>')">
                            <input type="hidden" name="idVendedor"    value="<%= r.getIdVendedor() %>">
                            <input type="hidden" name="nombreVendedor" value="<%= r.getNombreVendedor() %>">
                            <button type="submit" class="btn-pagar" id="btn-pagar-<%= r.getIdVendedor() %>">
                                💸 Pagar
                            </button>
                        </form>
                    </td>
                </tr>
                <% }
                   } else { %>
                <tr>
                    <td colspan="5">
                        <div class="empty-state">
                            <div class="icon">🎉</div>
                            <p><strong>¡Todo al día!</strong> No hay pagos pendientes a ningún vendedor.</p>
                        </div>
                    </td>
                </tr>
                <% } %>
            </tbody>
        </table>
    </main>

    <script>
        /**
         * Muestra un diálogo de confirmación detallado antes de ejecutar el pago.
         * @param {string} nombre - Nombre del vendedor
         * @param {string} monto  - Monto total a pagar
         * @param {string} cuenta - Cuenta bancaria de destino
         * @returns {boolean} true si el usuario confirma, false si cancela
         */
        function confirmarPago(nombre, monto, cuenta) {
            return confirm(
                '⚠️ CONFIRMAR PAGO\n\n' +
                'Vendedor: ' + nombre + '\n' +
                'Cuenta bancaria: ' + cuenta + '\n' +
                'Monto total: $' + monto + '\n\n' +
                '¿Confirmas que realizaste la transferencia?\n' +
                'Esta acción marcará todas las ventas como PAGADAS.'
            );
        }
    </script>
</body>
</html>
