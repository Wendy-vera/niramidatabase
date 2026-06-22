<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.nirami.model.Producto" %>
<%@ page import="java.util.List" %>
<%@ page import="com.nirami.model.Usuario" %>
<%
    List<Producto> favoritos = (List<Producto>) request.getAttribute("favoritos");
    Usuario usuarioLogueado = (Usuario) session.getAttribute("usuarioLogueado");
    String rol = (String) session.getAttribute("rol");
%>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Mis Favoritos - Nirami</title>
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
    </style>
</head>
<body>
    <header>
        <a href="${pageContext.request.contextPath}/" class="logo">Nirami</a>
        <nav>
            <ul>
                <li><a href="${pageContext.request.contextPath}/">Inicio / Catálogo</a></li>
                <li><a href="${pageContext.request.contextPath}/favoritos" style="color: var(--naranja)">Favoritos</a></li>
                <li><a href="${pageContext.request.contextPath}/carrito">Carrito</a></li>
                <li><a href="${pageContext.request.contextPath}/perfil">Perfil</a></li>
                <li><a href="${pageContext.request.contextPath}/logout" class="btn btn-secondary">Salir</a></li>
            </ul>
        </nav>
    </header>

    <main class="container">
        <div style="margin-bottom: 2rem;">
            <h2 style="color: var(--naranja);">Tus Favoritos ❤️</h2>
            <p style="color: var(--oscuro);">Los productos artesanales que más te han gustado.</p>
        </div>
        
        <div class="grid-catalogo">
            <% if (favoritos != null && !favoritos.isEmpty()) {
                for (Producto p : favoritos) { %>
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
                                
                                <form action="${pageContext.request.contextPath}/favoritos/toggle" method="post">
                                    <input type="hidden" name="idProducto" value="<%= p.getIdProducto() %>">
                                    <input type="hidden" name="action" value="quitar">
                                    <button type="submit" class="btn btn-secondary" style="padding: 10px 15px;" title="Quitar de favoritos">
                                        ❤️
                                    </button>
                                </form>
                            </div>
                    </div>
                </div>
            <%  }
               } else { %>
               <p style="grid-column: 1 / -1; text-align: center; font-size: 1.2rem;">Aún no tienes productos favoritos. <a href="${pageContext.request.contextPath}/" style="color:var(--azul);">Explorar el catálogo</a>.</p>
            <% } %>
        </div>
    </main>
</body>
</html>
