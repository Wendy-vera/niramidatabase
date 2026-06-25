<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.nirami.model.Usuario" %>
<%@ page import="java.util.List" %>
<%
    Usuario usuarioLogueado = (Usuario) session.getAttribute("usuarioLogueado");
    List<Usuario> usuarios = (List<Usuario>) request.getAttribute("usuarios");
    String tipoVista = (String) request.getAttribute("tipoVista");
%>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Gestión de <%= tipoVista.equals("VENDEDOR") ? "Vendedores" : "Clientes" %> - Nirami</title>
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
    </style>
    <script>
        function filtrarUsuarios() {
            var input, filter, table, tr, td, i, txtValue;
            input = document.getElementById("searchInput");
            filter = input.value.toUpperCase();
            table = document.getElementById("usuariosTable");
            tr = table.getElementsByTagName("tr");
            for (i = 1; i < tr.length; i++) {
                tdNombre = tr[i].getElementsByTagName("td")[0];
                tdCorreo = tr[i].getElementsByTagName("td")[1];
                if (tdNombre || tdCorreo) {
                    txtValueNombre = tdNombre.textContent || tdNombre.innerText;
                    txtValueCorreo = tdCorreo.textContent || tdCorreo.innerText;
                    if (txtValueNombre.toUpperCase().indexOf(filter) > -1 || txtValueCorreo.toUpperCase().indexOf(filter) > -1) {
                        tr[i].style.display = "";
                    } else {
                        tr[i].style.display = "none";
                    }
                }
            }
        }
    </script>
</head>
<body>
    <header>
        <a href="${pageContext.request.contextPath}/" class="logo">Nirami Admin</a>
        <nav>
            <ul>
                <li><a href="${pageContext.request.contextPath}/admin/vendedores" <%= tipoVista.equals("VENDEDOR") ? "style='color: var(--naranja)'" : "" %>>Vendedores</a></li>
                <li><a href="${pageContext.request.contextPath}/admin/clientes" <%= tipoVista.equals("CLIENTE") ? "style='color: var(--naranja)'" : "" %>>Clientes</a></li>
                <li><a href="${pageContext.request.contextPath}/admin/dashboard">Revisiones</a></li>
                <li><a href="${pageContext.request.contextPath}/admin/categorias">Categorías</a></li>
                <li><a href="${pageContext.request.contextPath}/admin/productos">Productos</a></li>
                <li><a href="${pageContext.request.contextPath}/admin/ventas">Ventas</a></li>
                <li><a href="${pageContext.request.contextPath}/admin/pagos-vendedores">💰 Pagos</a></li>
                <li><a href="${pageContext.request.contextPath}/logout" class="btn btn-secondary">Salir</a></li>
            </ul>
        </nav>
    </header>

    <main class="container">
        <h2>Listado de <%= tipoVista.equals("VENDEDOR") ? "Vendedores" : "Clientes" %></h2>
        
        <input type="text" id="searchInput" onkeyup="filtrarUsuarios()" placeholder="Buscar por nombre o correo..." class="form-control" style="max-width: 400px; margin-bottom: 1rem;">
        
        <table class="table" id="usuariosTable">
            <thead>
                <tr>
                    <th>Nombre</th>
                    <th>Correo</th>
                    <th>Teléfono</th>
                    <th>Estado</th>
                    <th>Fecha Registro</th>
                    <% if("CLIENTE".equals(tipoVista)) { %><th>Historial</th><% } %>
                </tr>
            </thead>
            <tbody>
                <% if(usuarios != null && !usuarios.isEmpty()) { 
                    for(Usuario u : usuarios) { %>
                <tr>
                    <td><%= u.getNombre() %></td>
                    <td><%= u.getCorreo() %></td>
                    <td><%= u.getTelefono() != null ? u.getTelefono() : "No registrado" %></td>
                    <td>
                        <span class="badge <%= u.getEstado().equals("ACTIVO") ? "badge-success" : "badge-warning" %>">
                            <%= u.getEstado() %>
                        </span>
                    </td>
                    <td><%= u.getFechaRegistro() %></td>
                    <% if("CLIENTE".equals(tipoVista)) { %>
                    <td>
                        <a href="${pageContext.request.contextPath}/admin/historial-usuario?idCliente=<%= u.getIdUsuario() %>"
                           style="color: var(--naranja); font-size: 0.85rem; font-weight: 600;">📋 Ver</a>
                    </td>
                    <% } %>
                </tr>
                <%  }
                   } else { %>
                <tr>
                    <td colspan="5" style="text-align: center;">No hay <%= tipoVista.equals("VENDEDOR") ? "vendedores" : "clientes" %> registrados.</td>
                </tr>
                <% } %>
            </tbody>
        </table>
    </main>
</body>
</html>
