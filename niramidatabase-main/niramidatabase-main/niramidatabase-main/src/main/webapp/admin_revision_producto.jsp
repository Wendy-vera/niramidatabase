<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.nirami.model.Producto" %>
<%
    Producto producto = (Producto) request.getAttribute("producto");
    if(producto == null) {
        response.sendRedirect(request.getContextPath() + "/admin/dashboard");
        return;
    }
%>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Evaluar Producto - Nirami Admin</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/styles.css">
    <style>
        .eval-container {
            display: flex;
            gap: 2rem;
            background: var(--blanco-puro);
            padding: 2rem;
            border-radius: 12px;
            box-shadow: 0 4px 15px rgba(0,0,0,0.05);
            margin-top: 2rem;
        }
        .eval-img {
            flex: 1;
            text-align: center;
        }
        .eval-img img {
            max-width: 100%;
            border-radius: 8px;
        }
        .eval-details {
            flex: 2;
        }
        .action-box {
            margin-top: 2rem;
            padding-top: 2rem;
            border-top: 1px solid var(--gris);
        }
        .action-box textarea {
            width: 100%;
            padding: 10px;
            border-radius: 8px;
            border: 1px solid #ccc;
            margin-bottom: 1rem;
            font-family: var(--font-body);
        }
        .btn-success { background-color: var(--turquesa); color: var(--oscuro); }
        .btn-danger { background-color: #e74c3c; color: white; }
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
                <li><a href="${pageContext.request.contextPath}/logout" class="btn btn-secondary">Salir</a></li>
            </ul>
        </nav>
    </header>

    <main class="container">
        <a href="${pageContext.request.contextPath}/admin/dashboard" style="color:var(--azul); text-decoration:none;">&larr; Volver a revisiones</a>
        
        <div class="eval-container">
            <div class="eval-img">
                <% if(producto.getImagenPrincipal() != null) { %>
                    <img src="${pageContext.request.contextPath}/<%= producto.getImagenPrincipal() %>" alt="<%= producto.getNombre() %>">
                <% } else { %>
                    <div style="background:#eee; height:300px; display:flex; align-items:center; justify-content:center; color:#888; border-radius:8px;">Sin Imagen</div>
                <% } %>
            </div>
            <div class="eval-details">
                <h2><%= producto.getNombre() %></h2>
                <p><strong>Precio sugerido:</strong> $<%= producto.getPrecio() %></p>
                <p><strong>Descripción:</strong></p>
                <p style="background: var(--gris); padding: 1rem; border-radius: 8px;"><%= producto.getDescripcion() %></p>
                
                <div class="action-box">
                    <h3>Veredicto</h3>
                    <form action="${pageContext.request.contextPath}/admin/revision" method="post">
                        <input type="hidden" name="idProducto" value="<%= producto.getIdProducto() %>">
                        
                        <label for="observacion">Observaciones o motivo (visible para el vendedor):</label>
                        <textarea id="observacion" name="observacion" rows="3" placeholder="Ej. Excelente artesanía / La imagen no es clara..."></textarea>
                        
                        <div style="display:flex; gap:1rem;">
                            <button type="submit" name="accion" value="APROBADO" class="btn btn-success" style="flex:1;" onclick="document.getElementById('observacion').required = false;">Aprobar Producto</button>
                            <button type="submit" name="accion" value="RECHAZADO" class="btn btn-danger" style="flex:1;" onclick="document.getElementById('observacion').required = true;">Rechazar Producto</button>
                        </div>
                    </form>
                </div>
            </div>
        </div>
    </main>
</body>
</html>
