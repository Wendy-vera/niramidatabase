<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.nirami.model.HistorialUsuario" %>
<%@ page import="java.util.List" %>
<%@ page import="java.text.SimpleDateFormat" %>
<%
    List<HistorialUsuario> historial = (List<HistorialUsuario>) request.getAttribute("historial");
    String nombreCliente = (String) request.getAttribute("nombreCliente");
    Object idClienteObj  = request.getAttribute("idCliente");
    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
%>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Historial de Cliente - Nirami Admin</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/styles.css">
    <style>
        /* ── Tabla de historial ── */
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
            padding: 0.85rem 1rem;
            text-align: left;
            border-bottom: 1px solid var(--gris);
            font-size: 0.9rem;
        }
        .table th {
            background-color: var(--oscuro);
            color: var(--blanco);
            font-weight: 600;
            font-size: 0.85rem;
            text-transform: uppercase;
            letter-spacing: 0.05em;
        }
        .table tr:hover td { background: #f8f9fa; }
        .table tr:last-child td { border-bottom: none; }

        /* ── Badges de acción ── */
        .accion-badge {
            padding: 4px 12px;
            border-radius: 20px;
            font-size: 0.8rem;
            font-weight: 600;
            white-space: nowrap;
        }
        .accion-VER_PRODUCTO    { background: #e3f2fd; color: #1565c0; }
        .accion-BUSQUEDA        { background: #fff3e0; color: #e65100; }
        .accion-AGREGAR_FAVORITO { background: #fce4ec; color: #c62828; }
        .accion-QUITAR_FAVORITO { background: #f3e5f5; color: #6a1b9a; }
        .accion-COMPRAR         { background: #e8f5e9; color: #2e7d32; }

        /* ── Iconos de acción ── */
        .accion-icon { margin-right: 4px; }

        /* ── Card de info del cliente ── */
        .cliente-info {
            background: linear-gradient(135deg, var(--naranja), #e55a00);
            color: white;
            border-radius: 12px;
            padding: 1.5rem 2rem;
            margin-bottom: 2rem;
            display: flex;
            justify-content: space-between;
            align-items: center;
        }
        .cliente-info h3 { margin: 0; font-size: 1.5rem; }
        .cliente-info p  { margin: 0.25rem 0 0; opacity: 0.85; }

        /* ── Estadísticas de acciones ── */
        .stats-row {
            display: flex;
            gap: 1rem;
            margin-bottom: 1.5rem;
            flex-wrap: wrap;
        }
        .stat-mini {
            background: var(--blanco-puro);
            border-radius: 8px;
            padding: 0.75rem 1.25rem;
            box-shadow: 0 2px 8px rgba(0,0,0,0.06);
            text-align: center;
            flex: 1;
            min-width: 120px;
        }
        .stat-mini .num   { font-size: 1.5rem; font-weight: 700; color: var(--oscuro); }
        .stat-mini .label { font-size: 0.75rem; color: #888; text-transform: uppercase; }

        /* ── Estado vacío ── */
        .empty-state {
            text-align: center;
            padding: 3rem;
            color: #aaa;
        }

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
                <li><a href="${pageContext.request.contextPath}/admin/clientes" style="color: var(--naranja)">← Clientes</a></li>
                <li><a href="${pageContext.request.contextPath}/admin/vendedores">Vendedores</a></li>
                <li><a href="${pageContext.request.contextPath}/admin/ventas">Ventas</a></li>
                <li><a href="${pageContext.request.contextPath}/admin/pagos-vendedores">Pagos</a></li>
                <li><a href="${pageContext.request.contextPath}/logout" class="btn btn-secondary">Salir</a></li>
            </ul>
        </nav>
    </header>

    <main class="container">
        <%-- ── Tarjeta de info del cliente ── --%>
        <div class="cliente-info">
            <div>
                <h3>👤 <%= nombreCliente != null ? nombreCliente : "Cliente #" + idClienteObj %></h3>
                <p>Historial de actividad en la plataforma</p>
            </div>
            <div style="text-align: right;">
                <strong style="font-size: 2rem;"><%= historial != null ? historial.size() : 0 %></strong>
                <p>acciones registradas</p>
            </div>
        </div>

        <%-- ── Estadísticas rápidas de acciones ── --%>
        <%
            int cntVer = 0, cntBusqueda = 0, cntFav = 0, cntCompras = 0;
            if (historial != null) {
                for (HistorialUsuario h : historial) {
                    switch (h.getAccion()) {
                        case "VER_PRODUCTO":     cntVer++;      break;
                        case "BUSQUEDA":         cntBusqueda++; break;
                        case "AGREGAR_FAVORITO":
                        case "QUITAR_FAVORITO":  cntFav++;      break;
                        case "COMPRAR":          cntCompras++;  break;
                    }
                }
            }
        %>
        <div class="stats-row">
            <div class="stat-mini">
                <div class="num"><%= cntVer %></div>
                <div class="label">👁️ Vistas</div>
            </div>
            <div class="stat-mini">
                <div class="num"><%= cntBusqueda %></div>
                <div class="label">🔍 Búsquedas</div>
            </div>
            <div class="stat-mini">
                <div class="num"><%= cntFav %></div>
                <div class="label">❤️ Favoritos</div>
            </div>
            <div class="stat-mini">
                <div class="num"><%= cntCompras %></div>
                <div class="label">🛒 Compras</div>
            </div>
        </div>

        <%-- ── Tabla de historial ── --%>
        <h3 style="margin-bottom: 0.5rem;">Registro de Actividad</h3>
        <p style="color: #777; font-size: 0.9rem; margin-top: 0;">
            Ordenado de más reciente a más antiguo. Máximo 200 registros.
        </p>

        <% if (historial != null && !historial.isEmpty()) { %>
        <table class="table">
            <thead>
                <tr>
                    <th>#</th>
                    <th>Fecha y Hora</th>
                    <th>Acción</th>
                    <th>Producto</th>
                    <th>Detalle / Búsqueda</th>
                    <th>IP</th>
                </tr>
            </thead>
            <tbody>
                <% int idx = 0;
                   for (HistorialUsuario h : historial) {
                       idx++;
                       String accionClass = "accion-" + h.getAccion();
                       String accionIcon;
                       String accionLabel;
                       switch (h.getAccion()) {
                           case "VER_PRODUCTO":      accionIcon = "👁️"; accionLabel = "Ver Producto";  break;
                           case "BUSQUEDA":          accionIcon = "🔍"; accionLabel = "Búsqueda";       break;
                           case "AGREGAR_FAVORITO":  accionIcon = "❤️"; accionLabel = "Fav +";          break;
                           case "QUITAR_FAVORITO":   accionIcon = "💔"; accionLabel = "Fav -";          break;
                           case "COMPRAR":           accionIcon = "🛒"; accionLabel = "Compra";         break;
                           default:                  accionIcon = "•";  accionLabel = h.getAccion();    break;
                       }
                %>
                <tr>
                    <td style="color: #aaa;"><%= idx %></td>
                    <td style="font-size: 0.85rem; white-space: nowrap;">
                        <%= h.getFecha() != null ? sdf.format(h.getFecha()) : "—" %>
                    </td>
                    <td>
                        <span class="accion-badge <%= accionClass %>">
                            <span class="accion-icon"><%= accionIcon %></span><%= accionLabel %>
                        </span>
                    </td>
                    <td>
                        <% if (h.getNombreProducto() != null) { %>
                            <em><%= h.getNombreProducto() %></em>
                        <% } else if (h.getIdProducto() != null) { %>
                            <em>Producto #<%= h.getIdProducto() %> (eliminado)</em>
                        <% } else { %>
                            <span style="color: #ccc;">—</span>
                        <% } %>
                    </td>
                    <td style="max-width: 200px;">
                        <% if (h.getTerminoBusqueda() != null) { %>
                            🔍 "<strong><%= h.getTerminoBusqueda() %></strong>"
                        <% } else if (h.getDetalle() != null) { %>
                            <%= h.getDetalle() %>
                        <% } else { %>
                            <span style="color: #ccc;">—</span>
                        <% } %>
                    </td>
                    <td style="font-family: monospace; font-size: 0.8rem; color: #888;">
                        <%= h.getIpOrigen() != null ? h.getIpOrigen() : "—" %>
                    </td>
                </tr>
                <% } %>
            </tbody>
        </table>
        <% } else { %>
        <div class="empty-state">
            <p style="font-size: 2.5rem;">📭</p>
            <p>Este cliente no tiene actividad registrada todavía.</p>
        </div>
        <% } %>
    </main>
</body>
</html>
