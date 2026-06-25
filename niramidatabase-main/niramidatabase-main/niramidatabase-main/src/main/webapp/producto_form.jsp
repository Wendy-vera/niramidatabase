<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.nirami.model.Categoria" %>
<%@ page import="com.nirami.model.Producto" %>
<%@ page import="java.util.List" %>
<%
    List<Categoria> categorias = (List<Categoria>) request.getAttribute("categorias");
    String error = (String) request.getAttribute("error");
    Producto p = (Producto) request.getAttribute("producto");
    boolean isEdit = (p != null);
%>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><%= isEdit ? "Editar Producto" : "Crear Producto" %> - Nirami</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/styles.css">
    <style>
        .form-container {
            background: var(--blanco-puro);
            padding: 2rem;
            border-radius: 12px;
            box-shadow: 0 4px 15px rgba(0,0,0,0.05);
            max-width: 600px;
            margin: 2rem auto;
        }
        .form-container h2 {
            margin-bottom: 1.5rem;
            color: var(--oscuro);
        }
        textarea.form-control {
            resize: vertical;
            min-height: 100px;
        }
    </style>
</head>
<body>
    <header>
        <a href="${pageContext.request.contextPath}/" class="logo">Nirami Vendedor</a>
        <nav>
            <ul>
                <li><a href="${pageContext.request.contextPath}/vendedor/dashboard">Mi Inventario</a></li>
                <li><a href="${pageContext.request.contextPath}/vendedor/ventas">Ventas</a></li>
                <li><a href="${pageContext.request.contextPath}/vendedor/pagos">Pagos</a></li>
                <li><a href="${pageContext.request.contextPath}/perfil">Perfil</a></li>
                <li><a href="${pageContext.request.contextPath}/logout" class="btn btn-secondary">Salir</a></li>
            </ul>
        </nav>
    </header>

    <main class="container">
        <div class="form-container">
            <h2><%= isEdit ? "Editar Producto" : "Sugerir Nuevo Producto" %></h2>
            <p style="margin-bottom: 1.5rem; color:#555;">Este producto será revisado por un administrador antes de aparecer en el catálogo global. Una vez aprobado, otros usuarios podrán verlo.</p>

            <% if (error != null) { %>
                <div class="alert alert-error"><%= error %></div>
            <% } %>

            <form action="${pageContext.request.contextPath}/vendedor/producto?action=<%= isEdit ? "actualizar" : "crear" %>" method="post" enctype="multipart/form-data">
                <% if(isEdit) { %>
                    <input type="hidden" name="idProducto" value="<%= p.getIdProducto() %>">
                <% } %>
                
                <div class="form-group">
                    <label for="nombre">Nombre de la Artesanía</label>
                    <input type="text" id="nombre" name="nombre" class="form-control" value="<%= isEdit ? p.getNombre() : "" %>" required>
                </div>
                
                <div class="form-group">
                    <label for="idCategoria">Categoría</label>
                    <select id="idCategoria" name="idCategoria" class="form-control" required>
                        <option value="">Seleccione una categoría...</option>
                        <% if(categorias != null) { 
                            for(Categoria c : categorias) { %>
                                <option value="<%= c.getIdCategoria() %>" <%= (isEdit && p.getIdCategoria() == c.getIdCategoria()) ? "selected" : "" %>><%= c.getNombre() %></option>
                        <%  } 
                           } %>
                    </select>
                </div>

                <div class="form-group" style="display: flex; gap: 1rem;">
                    <div style="flex:1;">
                        <label for="precio">Precio Sugerido ($)</label>
                        <input type="number" id="precio" name="precio" step="0.01" min="0.01" class="form-control" value="<%= isEdit ? p.getPrecio() : "" %>" required>
                    </div>
                    <div style="flex:1;">
                        <label for="stock">Tu Stock</label>
                        <input type="number" id="stock" name="stock" min="1" class="form-control" value="<%= isEdit ? p.getStock() : "" %>" required>
                    </div>
                </div>

                <div class="form-group">
                    <label for="descripcion">Descripción</label>
                    <textarea id="descripcion" name="descripcion" class="form-control" required placeholder="Materiales, dimensiones..."><%= isEdit ? p.getDescripcion() : "" %></textarea>
                </div>

                <div class="form-group">
                    <label for="imagen">Imagen Principal <%= isEdit ? "(Opcional si no desea cambiarla)" : "" %></label>
                    <input type="file" id="imagen" name="imagen" class="form-control" accept="image/*" <%= isEdit ? "" : "required" %>>
                    <small style="color:#888;">Formatos soportados: JPG, PNG, WEBP (Max 10MB).</small>
                </div>

                <div style="display:flex; gap: 1rem; margin-top: 2rem;">
                    <button type="submit" class="btn btn-primary" style="flex:1;"><%= isEdit ? "Actualizar y Revisar" : "Enviar a Revisión" %></button>
                    <a href="${pageContext.request.contextPath}/vendedor/dashboard" class="btn btn-secondary" style="text-align:center;">Cancelar</a>
                </div>
            </form>
        </div>
    </main>
</body>
</html>
