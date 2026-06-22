<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.nirami.model.Producto" %>
<%@ page import="com.nirami.model.Categoria" %>
<%@ page import="java.util.List" %>
<%@ page import="com.nirami.model.Usuario" %>
<%
    List<Producto> productos = (List<Producto>) request.getAttribute("productos");
    List<Categoria> categorias = (List<Categoria>) request.getAttribute("categorias");
    Usuario usuarioLogueado = (Usuario) session.getAttribute("usuarioLogueado");
    String rol = (String) session.getAttribute("rol");
%>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Nirami - Artesanías Únicas</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/styles.css">
    <style>
        .grid-catalogo {
            display: grid;
            grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
            gap: 2rem;
            margin-top: 2rem;
        }
        .card-producto {
            background: var(--blanco-puro);
            border-radius: 12px;
            overflow: hidden;
            box-shadow: 0 4px 10px rgba(0,0,0,0.05);
            transition: transform 0.3s ease, box-shadow 0.3s ease;
            text-align: center;
        }
        .card-producto:hover {
            transform: translateY(-5px);
            box-shadow: 0 8px 20px rgba(0,0,0,0.1);
        }
        .card-img {
            width: 100%;
            height: 200px;
            object-fit: cover;
            background-color: var(--gris);
        }
        .product-info {
            padding: 1.5rem;
        }
        .filtros {
            background: var(--blanco);
            padding: 1rem;
            border-radius: 8px;
            display: flex;
            gap: 1rem;
            overflow-x: auto;
        }
    </style>
</head>
<body>
    <header>
        <a href="${pageContext.request.contextPath}/" class="logo">Nirami</a>
        <nav>
            <ul>
                <li><a href="${pageContext.request.contextPath}/" style="color: var(--naranja)">Inicio / Catálogo</a></li>
                <% if (usuarioLogueado == null) { %>
                    <li><a href="${pageContext.request.contextPath}/login" class="btn btn-primary">Iniciar Sesión</a></li>
                <% } else { %>
                    <% if ("ADMIN".equals(rol)) { %>
                        <li><a href="${pageContext.request.contextPath}/admin/dashboard">Panel Admin</a></li>
                    <% } else if ("VENDEDOR".equals(rol)) { %>
                        <li><a href="${pageContext.request.contextPath}/vendedor/dashboard">Panel Vendedor</a></li>
                    <% } else { %>
                        <li><a href="${pageContext.request.contextPath}/favoritos">Favoritos</a></li>
                        <li><a href="${pageContext.request.contextPath}/carrito">Carrito</a></li>
                    <% } %>
                    <li><a href="${pageContext.request.contextPath}/perfil" style="display:inline-flex; align-items:center; gap:5px;">
                        <% if(usuarioLogueado.getFotoPerfil() != null && !usuarioLogueado.getFotoPerfil().isEmpty()) { %>
                            <img src="${pageContext.request.contextPath}/<%= usuarioLogueado.getFotoPerfil() %>" alt="Perfil" style="width: 25px; height: 25px; border-radius: 50%; object-fit: cover;">
                        <% } %>
                        Perfil
                    </a></li>
                    <li><a href="${pageContext.request.contextPath}/logout" class="btn btn-secondary">Salir</a></li>
                <% } %>
            </ul>
        </nav>
    </header>

    <main class="container">
        <!-- HERO SECTION: Mensaje de Bienvenida -->
        <section class="hero" style="margin-bottom: 3rem;">
            <h1>Descubre el Arte en tus Manos</h1>
            <% if(usuarioLogueado != null && "CLIENTE".equals(rol)) { %>
                <p>Bienvenido(a) de nuevo, <%= usuarioLogueado.getNombre() %>. Encuentra piezas únicas hechas a mano por artesanos locales.</p>
            <% } else { %>
                <p>Encuentra piezas únicas, hechas a mano por artesanos locales, y dale un toque especial a tu vida con Nirami.</p>
            <% } %>
        </section>
        
        <!-- CATÁLOGO -->
        <div style="display: flex; justify-content: flex-end; align-items: center; flex-wrap: wrap;">
            <form action="${pageContext.request.contextPath}/catalogo" method="get" style="display: flex; gap: 0.5rem; margin-bottom: 1rem;">
                <input type="text" name="q" placeholder="Buscar producto..." class="form-control" style="width: 250px;" value="<%= request.getAttribute("busqueda") != null ? request.getAttribute("busqueda") : "" %>">
                <button type="submit" class="btn btn-primary">Buscar</button>
                <% if(request.getAttribute("busqueda") != null) { %>
                    <a href="${pageContext.request.contextPath}/catalogo" class="btn btn-secondary">Limpiar</a>
                <% } %>
            </form>
        </div>
        
        <div class="filtros" style="display: flex; gap: 10px; flex-wrap: wrap; align-items: center; padding: 10px 0; border-bottom: 1px solid #ddd; margin-bottom: 20px;">
            <strong style="margin-right: 10px;">Categorías:</strong>
            <a href="${pageContext.request.contextPath}/catalogo" style="padding: 5px 15px; border-radius: 20px; text-decoration: none; font-weight: <%= request.getAttribute("categoriaSel") == null ? "bold" : "normal" %>; background-color: <%= request.getAttribute("categoriaSel") == null ? "var(--oscuro)" : "#eee" %>; color: <%= request.getAttribute("categoriaSel") == null ? "white" : "var(--oscuro)" %>; transition: background 0.2s;">Todas</a>
            <% if (categorias != null) {
                Integer categoriaSel = (Integer) request.getAttribute("categoriaSel");
                for (Categoria c : categorias) { 
                    boolean esSel = (categoriaSel != null && categoriaSel.equals(c.getIdCategoria()));
            %>
                <a href="${pageContext.request.contextPath}/catalogo?categoria=<%= c.getIdCategoria() %>" style="padding: 5px 15px; border-radius: 20px; text-decoration: none; font-weight: <%= esSel ? "bold" : "normal" %>; background-color: <%= esSel ? "var(--naranja)" : "#eee" %>; color: <%= esSel ? "white" : "var(--oscuro)" %>; transition: background 0.2s;"><%= c.getNombre() %></a>
            <%  }
               } %>
        </div>

        <div class="grid-catalogo">
            <% if (productos != null && !productos.isEmpty()) {
                for (Producto p : productos) { %>
                <div class="card-producto">
                    <% if(p.getImagenPrincipal() != null) { %>
                        <img src="${pageContext.request.contextPath}/<%= p.getImagenPrincipal() %>" alt="<%= p.getNombre() %>" class="card-img">
                    <% } else { %>
                        <div class="card-img" style="display:flex; align-items:center; justify-content:center; color:#888;">
                            Sin Imagen
                        </div>
                    <% } %>
                    <div class="product-info">
                            <h3 style="margin-bottom: 0.5rem;"><%= p.getNombre() %></h3>
                            <p style="color: var(--azul); font-weight: 600; font-size: 1.2rem; margin-bottom: 1rem;">$<%= p.getPrecio() %></p>
                            
                            <div style="display:flex; gap:0.5rem;">
                                <a href="${pageContext.request.contextPath}/catalogo?action=detalle&id=<%= p.getIdProducto() %>" class="btn btn-primary" style="flex:1; text-align:center;">Ver Detalles</a>
                                
                                <% 
                                    java.util.List<Integer> favIds = (java.util.List<Integer>) request.getAttribute("favIds");
                                    boolean esFav = (favIds != null && favIds.contains(p.getIdProducto()));
                                %>
                                <form action="${pageContext.request.contextPath}/favoritos/toggle" method="post">
                                    <input type="hidden" name="idProducto" value="<%= p.getIdProducto() %>">
                                    <input type="hidden" name="action" value="<%= esFav ? "quitar" : "agregar" %>">
                                    <button type="submit" class="btn btn-secondary" style="padding: 10px 15px;" title="<%= esFav ? "Quitar de favoritos" : "Añadir a favoritos" %>">
                                        <%= esFav ? "❤️" : "🤍" %>
                                    </button>
                                </form>
                            </div>
                    </div>
                </div>
            <%  }
               } else { %>
               <p style="grid-column: 1 / -1; text-align: center; font-size: 1.2rem;">No hay productos disponibles por el momento.</p>
            <% } %>
        </div>
    </main>
</body>
</html>
