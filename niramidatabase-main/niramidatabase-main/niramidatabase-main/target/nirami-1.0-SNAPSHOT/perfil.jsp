<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.nirami.model.Producto" %>
<%@ page import="com.nirami.model.Orden" %>
<%@ page import="com.nirami.model.OrdenDetalle" %>
<%@ page import="java.util.List" %>
<%@ page import="com.nirami.model.Usuario" %>
<%
    Usuario usuarioLogueado = (Usuario) session.getAttribute("usuarioLogueado");
    List<Producto> favoritos = (List<Producto>) request.getAttribute("favoritos");
    List<Orden> historial = (List<Orden>) request.getAttribute("historial");
    String extraInfo = (String) request.getAttribute("extraInfo");
    String error = (String) request.getAttribute("error");
    String mensaje = (String) request.getAttribute("mensaje");
%>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Mi Perfil - Nirami</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/styles.css">
    <style>
        .tabs {
            display: flex;
            gap: 1rem;
            margin-bottom: 2rem;
            border-bottom: 2px solid var(--gris);
            padding-bottom: 1rem;
        }
        .tab-btn {
            background: none;
            border: none;
            font-size: 1.1rem;
            color: #555;
            cursor: pointer;
            padding: 0.5rem 1rem;
            font-family: var(--font-title);
        }
        .tab-btn.active {
            color: var(--naranja);
            font-weight: bold;
            border-bottom: 3px solid var(--naranja);
            margin-bottom: -1.1rem;
        }
        .tab-content {
            display: none;
        }
        .tab-content.active {
            display: block;
        }
        
        /* Grid de favoritos simplificado */
        .fav-grid {
            display: grid;
            grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
            gap: 1.5rem;
        }
        
        /* Historial de órdenes */
        .orden-card {
            background: var(--blanco-puro);
            border-radius: 8px;
            padding: 1.5rem;
            margin-bottom: 1.5rem;
            box-shadow: 0 2px 8px rgba(0,0,0,0.05);
            border-left: 4px solid var(--turquesa);
        }
        .orden-header {
            display: flex;
            justify-content: space-between;
            border-bottom: 1px solid var(--gris);
            padding-bottom: 1rem;
            margin-bottom: 1rem;
        }
        .orden-item {
            display: flex;
            align-items: center;
            gap: 1rem;
            margin-bottom: 1rem;
        }
        .orden-item img {
            width: 50px;
            height: 50px;
            object-fit: cover;
            border-radius: 4px;
        }
    </style>
</head>
<body>
    <header>
        <a href="${pageContext.request.contextPath}/" class="logo">
            Nirami <%= usuarioLogueado.getTipoUsuario().equals("VENDEDOR") ? "Vendedor" : (usuarioLogueado.getTipoUsuario().equals("ADMIN") ? "Admin" : "") %>
        </a>
        <nav>
            <ul>
                <% if(usuarioLogueado.getTipoUsuario().equals("VENDEDOR")) { %>
                    <li><a href="${pageContext.request.contextPath}/vendedor/dashboard">Mi Inventario</a></li>
                    <li><a href="${pageContext.request.contextPath}/vendedor/ventas">Ventas</a></li>
                    <li><a href="${pageContext.request.contextPath}/vendedor/pagos">Pagos</a></li>
                <% } else if(usuarioLogueado.getTipoUsuario().equals("ADMIN")) { %>
                    <li><a href="${pageContext.request.contextPath}/admin/vendedores">Vendedores</a></li>
                    <li><a href="${pageContext.request.contextPath}/admin/clientes">Clientes</a></li>
                    <li><a href="${pageContext.request.contextPath}/admin/dashboard">Revisiones</a></li>
                    <li><a href="${pageContext.request.contextPath}/admin/categorias">Categorías</a></li>
                    <li><a href="${pageContext.request.contextPath}/admin/productos">Productos</a></li>
                    <li><a href="${pageContext.request.contextPath}/admin/ventas">Ventas</a></li>
                <% } else { %>
                    <li><a href="${pageContext.request.contextPath}/">Inicio / Catálogo</a></li>
                    <li><a href="${pageContext.request.contextPath}/favoritos">Favoritos</a></li>
                    <li><a href="${pageContext.request.contextPath}/carrito">Carrito</a></li>
                <% } %>
                <li><a href="${pageContext.request.contextPath}/perfil" style="color: var(--naranja); display:inline-flex; align-items:center; gap:5px;">
                    <% if(usuarioLogueado.getFotoPerfil() != null && !usuarioLogueado.getFotoPerfil().isEmpty()) { %>
                        <img src="${pageContext.request.contextPath}/<%= usuarioLogueado.getFotoPerfil() %>" alt="Perfil" style="width: 25px; height: 25px; border-radius: 50%; object-fit: cover;">
                    <% } %>
                    Perfil
                </a></li>
                <li><a href="${pageContext.request.contextPath}/logout" class="btn btn-secondary">Salir</a></li>
            </ul>
        </nav>
    </header>

    <main class="container">
        <div style="display: flex; align-items: center; gap: 1rem; margin-bottom: 1rem;">
            <% if (usuarioLogueado.getFotoPerfil() != null && !usuarioLogueado.getFotoPerfil().isEmpty()) { %>
                <img src="${pageContext.request.contextPath}/<%= usuarioLogueado.getFotoPerfil() %>" alt="Foto de perfil" style="width: 80px; height: 80px; border-radius: 50%; object-fit: cover; border: 2px solid var(--naranja);">
            <% } else { %>
                <div style="width: 80px; height: 80px; border-radius: 50%; background-color: var(--gris); display: flex; align-items: center; justify-content: center; font-size: 2rem; color: white;">
                    <%= usuarioLogueado.getNombre().substring(0, 1).toUpperCase() %>
                </div>
            <% } %>
            <h2 style="margin: 0;">Hola, <%= usuarioLogueado.getNombre() %></h2>
        </div>
        
        <% if(error != null) { %><div class="alert alert-error" style="margin-bottom:1rem;"><%= error %></div><% } %>
        <% if(mensaje != null) { %><div class="alert alert-success" style="margin-bottom:1rem;"><%= mensaje %></div><% } %>

        <div class="tabs">
            <% if(!"VENDEDOR".equals(usuarioLogueado.getTipoUsuario())) { %>
                <button class="tab-btn active" onclick="showTab('favoritos')">❤️ Mis Favoritos</button>
                <button class="tab-btn" onclick="showTab('historial')">📦 Historial de Compras</button>
            <% } %>
            <button class="tab-btn <%= "VENDEDOR".equals(usuarioLogueado.getTipoUsuario()) ? "active" : "" %>" onclick="showTab('ajustes')">⚙️ Ajustes de Cuenta</button>
        </div>

        <!-- TAB FAVORITOS -->
        <div id="favoritos" class="tab-content <%= !"VENDEDOR".equals(usuarioLogueado.getTipoUsuario()) ? "active" : "" %>">
            <% if(favoritos == null || favoritos.isEmpty()) { %>
                <p>Aún no tienes productos favoritos. <a href="${pageContext.request.contextPath}/catalogo" style="color:var(--azul);">Explorar</a></p>
            <% } else { %>
                <div class="fav-grid">
                    <% for(Producto p : favoritos) { %>
                        <div class="product-card" style="padding:1rem;">
                            <% if(p.getImagenPrincipal() != null) { %>
                                <img src="${pageContext.request.contextPath}/<%= p.getImagenPrincipal() %>" style="width:100%; height:150px; object-fit:cover; border-radius:8px;">
                            <% } %>
                            <h4 style="margin: 0.5rem 0;"><%= p.getNombre() %></h4>
                            <p style="color: var(--azul); font-weight:bold;">$<%= p.getPrecio() %></p>
                            
                            <div style="display:flex; gap:0.5rem; margin-top:1rem;">
                                <a href="${pageContext.request.contextPath}/catalogo?action=detalle&id=<%= p.getIdProducto() %>" class="btn btn-primary" style="flex:1; text-align:center; padding:5px;">Ver</a>
                                
                                <form action="${pageContext.request.contextPath}/favoritos/toggle" method="post">
                                    <input type="hidden" name="idProducto" value="<%= p.getIdProducto() %>">
                                    <input type="hidden" name="action" value="quitar">
                                    <button type="submit" class="btn btn-secondary" style="padding:5px;">💔</button>
                                </form>
                            </div>
                        </div>
                    <% } %>
                </div>
            <% } %>
        </div>

        <!-- TAB HISTORIAL -->
        <div id="historial" class="tab-content">
            <% if(historial == null || historial.isEmpty()) { %>
                <p>No tienes compras previas.</p>
            <% } else { 
                for(Orden o : historial) { %>
                
                <div class="orden-card">
                    <div class="orden-header">
                        <div>
                            <strong>Orden #<%= o.getIdOrden() %></strong><br>
                            <span style="font-size:0.9rem; color:#666;"><%= o.getFechaOrden() %></span>
                        </div>
                        <div style="text-align:right;">
                            <strong style="color:var(--azul); font-size:1.2rem;">$<%= o.getTotal() %></strong><br>
                            <span style="font-size:0.9rem; color:#666;">Envío a: <%= o.getDireccion() %></span>
                        </div>
                    </div>
                    
                    <div>
                        <% for(OrdenDetalle od : o.getDetalles()) { %>
                            <div class="orden-item">
                                <% if(od.getProducto().getImagenPrincipal() != null) { %>
                                    <img src="${pageContext.request.contextPath}/<%= od.getProducto().getImagenPrincipal() %>">
                                <% } else { %>
                                    <div style="width:50px;height:50px;background:#eee;border-radius:4px;"></div>
                                <% } %>
                                
                                <div style="flex:1;">
                                    <strong style="display:block;"><%= od.getProducto().getNombre() %></strong>
                                    <span style="font-size:0.9rem; color:#555;">Cantidad: <%= od.getCantidad() %> | Unitario: $<%= od.getPrecioUnitario() %></span>
                                </div>
                                <div style="font-weight:bold;">
                                    $<%= od.getSubtotalLinea() %>
                                </div>
                            </div>
                        <% } %>
                    </div>
                    
                    <div style="margin-top: 1rem; text-align: right;">
                        <a href="${pageContext.request.contextPath}/recibo?idOrden=<%= o.getIdOrden() %>" class="btn btn-primary" style="padding: 8px 15px; font-size: 0.9rem;">⬇️ Descargar Recibo PDF</a>
                    </div>
                </div>

            <%  }
               } %>
        </div>

        <!-- TAB AJUSTES DE CUENTA -->
        <div id="ajustes" class="tab-content <%= "VENDEDOR".equals(usuarioLogueado.getTipoUsuario()) ? "active" : "" %>">
            <div style="background: var(--blanco-puro); padding: 1.5rem; border-radius: 8px; box-shadow: 0 2px 8px rgba(0,0,0,0.05); max-width: 600px; margin: 0 auto;">
                <h3 style="margin-bottom: 1rem;">Actualizar Datos Personales</h3>
                <form action="${pageContext.request.contextPath}/perfil" method="post" enctype="multipart/form-data">
                    <div class="form-group">
                        <label>Foto de Perfil:</label>
                        <input type="file" name="fotoPerfil" class="form-control" accept="image/*">
                    </div>
                    <div class="form-group">
                        <label>Nombre:</label>
                        <input type="text" name="nombre" class="form-control" value="<%= usuarioLogueado.getNombre() %>" required minlength="3">
                    </div>
                    <div class="form-group">
                        <label>Correo Electrónico:</label>
                        <input type="email" name="correo" class="form-control" value="<%= usuarioLogueado.getCorreo() %>" required>
                    </div>
                    <div class="form-group">
                        <label>Teléfono (Opcional, 10 dígitos):</label>
                        <input type="text" name="telefono" class="form-control" value="<%= usuarioLogueado.getTelefono() != null ? usuarioLogueado.getTelefono() : "" %>" pattern="\d{10}">
                    </div>
                    <div class="form-group">
                        <label>Nueva Contraseña (Opcional, min 8 chars, 1 mayús, 1 num):</label>
                        <input type="password" name="contrasena" class="form-control" placeholder="Dejar en blanco para no cambiar">
                    </div>
                    <% if("VENDEDOR".equals(usuarioLogueado.getTipoUsuario())) { %>
                        <div class="form-group">
                            <label>Cuenta Bancaria (10 dígitos):</label>
                            <input type="text" name="extraInfo" class="form-control" value="<%= extraInfo %>" required pattern="\d{10}">
                        </div>
                    <% } else if("CLIENTE".equals(usuarioLogueado.getTipoUsuario())) { %>
                        <div class="form-group">
                            <label>Dirección de Envío:</label>
                            <input type="text" name="extraInfo" class="form-control" value="<%= extraInfo %>">
                        </div>
                    <% } %>
                    <button type="submit" class="btn btn-primary" style="width: 100%;">Guardar Cambios</button>
                </form>
            </div>
        </div>

    </main>

    <script>
        function showTab(tabId) {
            document.querySelectorAll('.tab-content').forEach(el => el.classList.remove('active'));
            document.querySelectorAll('.tab-btn').forEach(el => el.classList.remove('active'));
            
            document.getElementById(tabId).classList.add('active');
            event.currentTarget.classList.add('active');
        }
    </script>
</body>
</html>
