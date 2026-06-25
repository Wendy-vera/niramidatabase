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

/**
 * Servlet que gestiona la creación y edición de productos por el Vendedor.
 * URL: /vendedor/producto
 * GET  → muestra el formulario de crear o editar un producto
 * POST → guarda el producto nuevo o actualiza uno existente, incluyendo subida de imagen
 *
 * @MultipartConfig: Permite recibir archivos (imágenes) en el request multipart/form-data.
 *   - fileSizeThreshold: 2MB en memoria antes de escribir a disco
 *   - maxFileSize: máximo 10MB por archivo individual
 *   - maxRequestSize: máximo 50MB por todo el formulario (para formularios con múltiples imágenes)
 *
 * Conexiones:
 *   → session.getAttribute("usuarioLogueado") (identificar al vendedor actual)
 *   → ProductoDAO.crearProducto()             (INSERT producto + INSERT vendedor_producto)
 *   → ProductoDAO.actualizarProducto()        (UPDATE producto)
 *   → CategoriaDAO.obtenerTodasActivas()      (categorías para el <select> del formulario)
 *   → producto_form.jsp                       (formulario de producto)
 *   → /vendedor/dashboard                     (redirección tras guardar)
 *
 * Imágenes: Se guardan físicamente en C:\NiramiUploads\
 *           y la ruta relativa "uploads/nombrearchivo" se guarda en la BD (tabla producto_imagenes)
 */
@WebServlet("/vendedor/producto") // Responde a /vendedor/producto
@MultipartConfig(
    fileSizeThreshold = 1024 * 1024 * 2,  // Hasta 2MB se mantienen en memoria antes de ir a disco
    maxFileSize       = 1024 * 1024 * 10, // Tamaño máximo de la imagen: 10MB
    maxRequestSize    = 1024 * 1024 * 50  // Tamaño máximo del formulario completo: 50MB
)
public class ProductoVendedorServlet extends HttpServlet {

    // DAO para crear y actualizar productos en la BD
    // Conecta con tablas: productos, vendedor_producto, producto_imagenes
    private ProductoDAO productoDAO = new ProductoDAO();

    // DAO para traer categorías activas que el vendedor puede seleccionar
    // Conecta con: SELECT * FROM categorias WHERE estado = 'ACTIVA'
    private CategoriaDAO categoriaDAO = new CategoriaDAO();

    /**
     * GET /vendedor/producto?action=crear → Muestra el formulario de nuevo producto
     * GET /vendedor/producto?action=editar&id=X → Muestra el formulario pre-cargado para editar
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // Obtener la sesión y verificar que sea un VENDEDOR
        HttpSession session = request.getSession();
        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");

        // Protección de rol: solo vendedores pueden acceder a este servlet
        if (usuario == null || !"VENDEDOR".equals(usuario.getTipoUsuario())) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        String action = request.getParameter("action"); // "crear" o "editar"

        if ("crear".equals(action)) {
            /* ── Modo crear: formulario vacío ──
             * Solo se necesita traer las categorías para el <select>
             */
            // Traer categorías activas para el <select> del formulario
            List<Categoria> categorias = categoriaDAO.obtenerTodasActivas();
            request.setAttribute("categorias", categorias); // La JSP lo usa en el <select>

            // Mostrar el formulario vacío
            request.getRequestDispatcher("/producto_form.jsp").forward(request, response);

        } else if ("editar".equals(action)) {
            /* ── Modo editar: formulario pre-cargado con datos del producto ──
             * Se verifica que el producto pertenezca al vendedor logueado (seguridad)
             */
            int id = Integer.parseInt(request.getParameter("id")); // ID del producto a editar

            // Buscar el producto en la BD
            Producto p = productoDAO.obtenerPorId(id);

            // SEGURIDAD: Verificar que el producto existe Y pertenece al vendedor actual
            // Un vendedor no puede editar productos de otro vendedor
            if (p != null && p.getIdVendedor() == usuario.getIdUsuario()) {
                List<Categoria> categorias = categoriaDAO.obtenerTodasActivas();
                request.setAttribute("categorias", categorias); // Categorías para el <select>
                request.setAttribute("producto", p);            // Producto actual para pre-llenar el form
                request.getRequestDispatcher("/producto_form.jsp").forward(request, response);
            } else {
                // El producto no existe o no pertenece a este vendedor → ir al dashboard
                response.sendRedirect(request.getContextPath() + "/vendedor/dashboard");
            }

        } else {
            // URL sin acción válida → ir al dashboard
            response.sendRedirect(request.getContextPath() + "/vendedor/dashboard");
        }
    }

    /**
     * POST /vendedor/producto → Procesa el formulario de crear o actualizar un producto.
     * Maneja la subida de imágenes con @MultipartConfig.
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // Verificar que sea un VENDEDOR autenticado
        HttpSession session = request.getSession();
        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");

        if (usuario == null || !"VENDEDOR".equals(usuario.getTipoUsuario())) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        // Determinar la acción: "crear" o "actualizar"
        String action = request.getParameter("action");
        if (action == null) action = "crear"; // Por defecto es crear

        // ── Leer todos los campos del formulario del producto ──
        String nombre      = request.getParameter("nombre");      // Nombre del producto
        String descripcion = request.getParameter("descripcion"); // Descripción detallada
        // Convertir el precio de String a BigDecimal para precisión decimal exacta
        BigDecimal precio  = new BigDecimal(request.getParameter("precio")); // Precio de venta
        int stock          = Integer.parseInt(request.getParameter("stock")); // Stock inicial
        int idCategoria    = Integer.parseInt(request.getParameter("idCategoria")); // FK categoría

        // ── Manejo de la imagen subida ──
        // request.getPart("imagen") extrae el archivo del formulario multipart
        Part part = request.getPart("imagen"); // Campo <input type="file" name="imagen">
        String rutaBaseDatos = null; // Ruta relativa que se guardará en la BD

        if (part != null && part.getSize() > 0) {
            /* Si se subió una imagen:
             * 1. Generar nombre único con UUID para evitar colisiones
             * 2. Guardar físicamente en C:\NiramiUploads\
             * 3. Guardar la ruta relativa en la BD (para que ImageServlet la sirva)
             */
            // UUID garantiza nombre único: "a3f4c2-..._nombreoriginal.jpg"
            String fileName  = UUID.randomUUID().toString() + "_" + extractFileName(part);

            // Directorio de subidas en el servidor (fuera del WAR para persistencia)
            String uploadPath = "C:\\NiramiUploads";

            // Crear el directorio si no existe (primera vez)
            File uploadDir = new File(uploadPath);
            if (!uploadDir.exists()) {
                uploadDir.mkdirs(); // Crea la carpeta y todas las padres necesarias
            }

            // Escribir el archivo al disco con el nombre generado
            part.write(uploadPath + File.separator + fileName);

            // Guardar ruta RELATIVA en BD (no la ruta completa del servidor)
            // ImageServlet lee esta ruta: GET /uploads/a3f4c2-..._img.jpg
            rutaBaseDatos = "uploads/" + fileName;
        }

        if ("crear".equals(action)) {
            /* ── Crear nuevo producto ──
             * Construir el objeto Producto y llamar al DAO
             * ProductoDAO.crearProducto() hace en transacción:
             *   1. INSERT INTO productos (nombre, descripcion, precio, estado='PENDIENTE', ...)
             *   2. INSERT INTO vendedor_producto (id_vendedor, id_producto, stock_local)
             *   3. INSERT INTO producto_imagenes (id_producto, ruta, es_principal=1) si hay imagen
             */
            Producto p = new Producto();
            p.setNombre(nombre);
            p.setDescripcion(descripcion);
            p.setPrecio(precio);
            p.setStock(stock);
            p.setIdCategoria(idCategoria);
            // El estado se fija en 'PENDIENTE' dentro del DAO (debe ser aprobado por admin)

            // usuario.getIdUsuario() → FK a vendedores.id_usuario, establece quién es el dueño
            boolean exito = productoDAO.crearProducto(p, usuario.getIdUsuario(), rutaBaseDatos);

            if (exito) {
                // Guardar mensaje en sesión para mostrarlo en el dashboard tras la redirección
                session.setAttribute("mensaje", "Producto enviado a revisión exitosamente.");
                response.sendRedirect(request.getContextPath() + "/vendedor/dashboard");
            } else {
                // Si falló (error en BD), volver al formulario con el mensaje de error
                request.setAttribute("error", "Hubo un error al crear el producto.");
                List<Categoria> categorias = categoriaDAO.obtenerTodasActivas();
                request.setAttribute("categorias", categorias);
                request.getRequestDispatcher("/producto_form.jsp").forward(request, response);
            }

        } else if ("actualizar".equals(action)) {
            /* ── Actualizar producto existente ──
             * Al editar un producto, vuelve a estado PENDIENTE para nueva revisión del admin
             */
            int idProducto = Integer.parseInt(request.getParameter("idProducto")); // ID del producto a editar

            // Buscar el producto actual para verificar pertenencia
            Producto p = productoDAO.obtenerPorId(idProducto);

            // SEGURIDAD: Solo el vendedor dueño puede actualizar su propio producto
            if (p != null && p.getIdVendedor() == usuario.getIdUsuario()) {
                // Aplicar los nuevos valores al objeto producto
                p.setNombre(nombre);
                p.setDescripcion(descripcion);
                p.setPrecio(precio);
                p.setStock(stock);
                p.setIdCategoria(idCategoria);
                // Cualquier cambio devuelve el producto a PENDIENTE para nueva revisión del admin
                p.setEstado("PENDIENTE");

                // rutaBaseDatos es null si no se subió nueva imagen → el DAO mantiene la anterior
                boolean exito = productoDAO.actualizarProducto(p, rutaBaseDatos);

                if (exito) {
                    session.setAttribute("mensaje", "Producto actualizado y enviado a revisión.");
                } else {
                    session.setAttribute("mensaje", "Error al actualizar el producto.");
                }
            }
            // Redirigir al dashboard en cualquier caso (éxito o acceso denegado)
            response.sendRedirect(request.getContextPath() + "/vendedor/dashboard");
        }
    }

    /**
     * Extrae el nombre del archivo del header "content-disposition" del Part HTTP.
     * El header tiene formato: form-data; name="imagen"; filename="foto.jpg"
     *
     * @param part El Part del archivo subido
     * @return Nombre del archivo original (ej: "foto.jpg"); cadena vacía si no se encuentra
     */
    private String extractFileName(Part part) {
        // Leer el header que describe el contenido del campo de formulario
        String contentDisp = part.getHeader("content-disposition");

        // Dividir por ";" para obtener cada atributo: ["form-data", " name=\"imagen\"", " filename=\"foto.jpg\""]
        String[] items = contentDisp.split(";");

        for (String s : items) {
            if (s.trim().startsWith("filename")) {
                // Extraer el valor entre comillas después de "filename="
                // Substring: desde después de '=' y la comilla de apertura, hasta antes de la de cierre
                return s.substring(s.indexOf("=") + 2, s.length() - 1);
            }
        }
        return ""; // Si no se encontró el atributo filename
    }
}
