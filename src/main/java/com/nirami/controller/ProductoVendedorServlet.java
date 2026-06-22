package com.nirami.controller;

import com.nirami.dao.CategoriaDAO;
import com.nirami.dao.ProductoDAO;
import com.nirami.model.Categoria;
import com.nirami.model.Producto;
import com.nirami.model.Usuario;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.Part;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@WebServlet("/vendedor/producto")
@MultipartConfig(
    fileSizeThreshold = 1024 * 1024 * 2,  // 2MB
    maxFileSize = 1024 * 1024 * 10,       // 10MB
    maxRequestSize = 1024 * 1024 * 50     // 50MB
)
public class ProductoVendedorServlet extends HttpServlet {
    
    private ProductoDAO productoDAO = new ProductoDAO();
    private CategoriaDAO categoriaDAO = new CategoriaDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession();
        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");

        if (usuario == null || !"VENDEDOR".equals(usuario.getTipoUsuario())) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        String action = request.getParameter("action");
        if ("crear".equals(action)) {
            List<Categoria> categorias = categoriaDAO.obtenerTodasActivas();
            request.setAttribute("categorias", categorias);
            request.getRequestDispatcher("/producto_form.jsp").forward(request, response);
        } else if ("editar".equals(action)) {
            int id = Integer.parseInt(request.getParameter("id"));
            Producto p = productoDAO.obtenerPorId(id);
            if (p != null && p.getIdVendedor() == usuario.getIdUsuario()) {
                List<Categoria> categorias = categoriaDAO.obtenerTodasActivas();
                request.setAttribute("categorias", categorias);
                request.setAttribute("producto", p);
                request.getRequestDispatcher("/producto_form.jsp").forward(request, response);
            } else {
                response.sendRedirect(request.getContextPath() + "/vendedor/dashboard");
            }
        } else {
            response.sendRedirect(request.getContextPath() + "/vendedor/dashboard");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession();
        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");

        if (usuario == null || !"VENDEDOR".equals(usuario.getTipoUsuario())) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        String action = request.getParameter("action");
        if(action == null) action = "crear";

        // Obtener datos del formulario
        String nombre = request.getParameter("nombre");
        String descripcion = request.getParameter("descripcion");
        BigDecimal precio = new BigDecimal(request.getParameter("precio"));
        int stock = Integer.parseInt(request.getParameter("stock"));
        int idCategoria = Integer.parseInt(request.getParameter("idCategoria"));

        // Manejar subida de imagen
        Part part = request.getPart("imagen");
        String rutaBaseDatos = null;

        if (part != null && part.getSize() > 0) {
            String fileName = UUID.randomUUID().toString() + "_" + extractFileName(part);
            String uploadPath = "C:\\NiramiUploads";
            
            // Crear carpeta si no existe
            File uploadDir = new File(uploadPath);
            if (!uploadDir.exists()) {
                uploadDir.mkdirs();
            }

            part.write(uploadPath + File.separator + fileName);
            rutaBaseDatos = "uploads/" + fileName; // Guardamos ruta relativa en BD
        }

        if ("crear".equals(action)) {
            Producto p = new Producto();
            p.setNombre(nombre);
            p.setDescripcion(descripcion);
            p.setPrecio(precio);
            p.setStock(stock);
            p.setIdCategoria(idCategoria);
            
            boolean exito = productoDAO.crearProducto(p, usuario.getIdUsuario(), rutaBaseDatos);
            if (exito) {
                session.setAttribute("mensaje", "Producto enviado a revisión exitosamente.");
                response.sendRedirect(request.getContextPath() + "/vendedor/dashboard");
            } else {
                request.setAttribute("error", "Hubo un error al crear el producto.");
                List<Categoria> categorias = categoriaDAO.obtenerTodasActivas();
                request.setAttribute("categorias", categorias);
                request.getRequestDispatcher("/producto_form.jsp").forward(request, response);
            }
        } else if ("actualizar".equals(action)) {
            int idProducto = Integer.parseInt(request.getParameter("idProducto"));
            Producto p = productoDAO.obtenerPorId(idProducto);
            
            if (p != null && p.getIdVendedor() == usuario.getIdUsuario()) {
                p.setNombre(nombre);
                p.setDescripcion(descripcion);
                p.setPrecio(precio);
                p.setStock(stock);
                p.setIdCategoria(idCategoria);
                p.setEstado("PENDIENTE"); // Se regresa a pendiente por cambios
                
                boolean exito = productoDAO.actualizarProducto(p, rutaBaseDatos);
                if (exito) {
                    session.setAttribute("mensaje", "Producto actualizado y enviado a revisión.");
                } else {
                    session.setAttribute("mensaje", "Error al actualizar el producto.");
                }
            }
            response.sendRedirect(request.getContextPath() + "/vendedor/dashboard");
        }
    }

    private String extractFileName(Part part) {
        String contentDisp = part.getHeader("content-disposition");
        String[] items = contentDisp.split(";");
        for (String s : items) {
            if (s.trim().startsWith("filename")) {
                return s.substring(s.indexOf("=") + 2, s.length() - 1);
            }
        }
        return "";
    }
}
