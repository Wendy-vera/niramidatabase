<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.nirami.model.Categoria" %>
<%@ page import="java.util.List" %>
<%
    List<Categoria> categorias = (List<Categoria>) request.getAttribute("categorias");
    String error = (String) request.getAttribute("error");
%>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Gestión de Categorías - Nirami</title>
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
        .btn-small { padding: 5px 10px; font-size: 0.9rem; }
        .modal {
            display: none; position: fixed; top: 0; left: 0; width: 100%; height: 100%;
            background: rgba(0,0,0,0.5); justify-content: center; align-items: center; z-index: 1000;
        }
        .modal-content { background: #fff; padding: 2rem; border-radius: 8px; width: 400px; max-width: 90%; }
    </style>
    <script>
        function openModal(id, nombre, descripcion) {
            document.getElementById('modalCategoria').style.display = 'flex';
            if (id) {
                document.getElementById('modalTitle').innerText = "Editar Categoría";
                document.getElementById('formAccion').value = "editar";
                document.getElementById('idCategoria').value = id;
                document.getElementById('nombreCat').value = nombre;
                document.getElementById('descCat').value = descripcion;
            } else {
                document.getElementById('modalTitle').innerText = "Nueva Categoría";
                document.getElementById('formAccion').value = "crear";
                document.getElementById('idCategoria').value = "";
                document.getElementById('nombreCat').value = "";
                document.getElementById('descCat').value = "";
            }
        }
        function closeModal() {
            document.getElementById('modalCategoria').style.display = 'none';
        }
    </script>
</head>
<body>
    <header>
        <a href="${pageContext.request.contextPath}/" class="logo">Nirami Admin</a>
        <nav>
            <ul>
                <li><a href="${pageContext.request.contextPath}/admin/vendedores">Vendedores</a></li>
                <li><a href="${pageContext.request.contextPath}/admin/clientes">Clientes</a></li>
                <li><a href="${pageContext.request.contextPath}/admin/dashboard">Revisiones</a></li>
                <li><a href="${pageContext.request.contextPath}/admin/categorias" style="color: var(--naranja)">Categorías</a></li>
                <li><a href="${pageContext.request.contextPath}/admin/productos">Productos</a></li>
                <li><a href="${pageContext.request.contextPath}/admin/ventas">Ventas</a></li>
                <li><a href="${pageContext.request.contextPath}/logout" class="btn btn-secondary">Salir</a></li>
            </ul>
        </nav>
    </header>

    <main class="container">
        <div style="display: flex; justify-content: space-between; align-items: center;">
            <h2>Gestión de Categorías</h2>
            <button class="btn btn-primary" onclick="openModal()">+ Nueva Categoría</button>
        </div>
        
        <% if(error != null) { %>
            <div class="alert alert-error" style="margin-top: 1rem;"><%= error %></div>
        <% } %>

        <table class="table">
            <thead>
                <tr>
                    <th>ID</th>
                    <th>Nombre</th>
                    <th>Descripción</th>
                    <th>Estado</th>
                    <th>Acciones</th>
                </tr>
            </thead>
            <tbody>
                <% if(categorias != null && !categorias.isEmpty()) { 
                    for(Categoria c : categorias) { %>
                <tr>
                    <td><%= c.getIdCategoria() %></td>
                    <td><%= c.getNombre() %></td>
                    <td><%= c.getDescripcion() != null ? c.getDescripcion() : "" %></td>
                    <td>
                        <span class="badge <%= c.getEstado().equals("ACTIVA") ? "badge-success" : "badge-warning" %>">
                            <%= c.getEstado() %>
                        </span>
                    </td>
                    <td>
                        <button class="btn btn-secondary btn-small" onclick="openModal(<%= c.getIdCategoria() %>, '<%= c.getNombre() %>', '<%= c.getDescripcion() != null ? c.getDescripcion().replace("'","\\'") : "" %>')">Editar</button>
                        
                        <form action="${pageContext.request.contextPath}/admin/categorias" method="post" style="display:inline;">
                            <input type="hidden" name="accion" value="estado">
                            <input type="hidden" name="idCategoria" value="<%= c.getIdCategoria() %>">
                            <% if(c.getEstado().equals("ACTIVA")) { %>
                                <input type="hidden" name="nuevoEstado" value="INACTIVA">
                                <button type="submit" class="btn btn-warning btn-small">Suspender</button>
                            <% } else { %>
                                <input type="hidden" name="nuevoEstado" value="ACTIVA">
                                <button type="submit" class="btn btn-success btn-small">Activar</button>
                            <% } %>
                        </form>
                        
                        <form action="${pageContext.request.contextPath}/admin/categorias" method="post" style="display:inline;" onsubmit="return confirm('¿Seguro que deseas eliminar esta categoría? Solo se podrá si no tiene productos.');">
                            <input type="hidden" name="accion" value="estado">
                            <input type="hidden" name="idCategoria" value="<%= c.getIdCategoria() %>">
                            <input type="hidden" name="nuevoEstado" value="ELIMINADA">
                            <button type="submit" class="btn btn-error btn-small">Eliminar</button>
                        </form>
                    </td>
                </tr>
                <%  }
                   } else { %>
                <tr>
                    <td colspan="5" style="text-align: center;">No hay categorías registradas.</td>
                </tr>
                <% } %>
            </tbody>
        </table>
    </main>

    <div id="modalCategoria" class="modal">
        <div class="modal-content">
            <h3 id="modalTitle">Nueva Categoría</h3>
            <form action="${pageContext.request.contextPath}/admin/categorias" method="post">
                <input type="hidden" name="accion" id="formAccion" value="crear">
                <input type="hidden" name="idCategoria" id="idCategoria">
                
                <div class="form-group" style="margin-top: 1rem;">
                    <label>Nombre:</label>
                    <input type="text" name="nombre" id="nombreCat" class="form-control" required>
                </div>
                <div class="form-group">
                    <label>Descripción:</label>
                    <textarea name="descripcion" id="descCat" class="form-control" rows="3"></textarea>
                </div>
                
                <div style="display: flex; gap: 1rem; margin-top: 1rem;">
                    <button type="button" class="btn btn-secondary" style="flex: 1;" onclick="closeModal()">Cancelar</button>
                    <button type="submit" class="btn btn-primary" style="flex: 1;">Guardar</button>
                </div>
            </form>
        </div>
    </div>
</body>
</html>
