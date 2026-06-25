package com.nirami.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * Servlet que sirve las imágenes subidas por los vendedores al navegador.
 * URL: /uploads/*
 * GET → lee el archivo del disco y lo envía como respuesta HTTP binaria
 *
 * PROBLEMA QUE RESUELVE:
 * Las imágenes se guardan en C:\NiramiUploads\ (fuera del directorio WAR).
 * Los navegadores no pueden acceder directamente a rutas del sistema de archivos del servidor.
 * Este servlet actúa como un "puente" entre la URL /uploads/imagen.jpg y el archivo en disco.
 *
 * Flujo:
 *   Navegador: GET /uploads/a3f4c2_foto.jpg
 *   → ImageServlet.doGet()
 *   → Lee C:\NiramiUploads\a3f4c2_foto.jpg
 *   → Envía los bytes del archivo como respuesta con el Content-Type correcto
 *
 * Conexiones:
 *   → C:\NiramiUploads\ (directorio de archivos subidos por ProductoVendedorServlet y PerfilServlet)
 *   → Las JSPs incluyen imágenes con <img src="/uploads/ruta_guardada_en_bd">
 */
@WebServlet("/uploads/*") // El * captura cualquier ruta después de /uploads/
public class ImageServlet extends HttpServlet {

    // Directorio físico donde se almacenan las imágenes subidas
    // Coincide con el uploadPath en ProductoVendedorServlet y PerfilServlet
    private static final String UPLOAD_DIR = "C:\\NiramiUploads";

    /**
     * GET /uploads/{filename} → Lee el archivo del disco y lo envía como respuesta HTTP.
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // getPathInfo() devuelve la parte de la URL después de /uploads
        // Ejemplo: si la URL es /uploads/foto.jpg → filename = "/foto.jpg"
        String filename = request.getPathInfo();

        // Validar que se proporcionó un nombre de archivo (no solo /uploads/)
        if (filename == null || filename.equals("/")) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND); // 404
            return;
        }

        // Construir la ruta absoluta del archivo en el servidor
        // new File(UPLOAD_DIR, "/foto.jpg") → C:\NiramiUploads\foto.jpg
        File file = new File(UPLOAD_DIR, filename);

        // Verificar que el archivo exista y no sea un directorio (seguridad)
        // Esto también protege contra path traversal (../../etc/passwd)
        if (!file.exists() || file.isDirectory()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND); // 404
            return;
        }

        // Determinar el tipo MIME del archivo según su extensión
        // getServletContext().getMimeType() usa el archivo web.xml del servidor para mapear extensiones
        // Ejemplos: .jpg → "image/jpeg", .png → "image/png", .pdf → "application/pdf"
        String mimeType = getServletContext().getMimeType(filename);

        // Si el tipo MIME no es reconocido, usar un tipo genérico de descarga
        if (mimeType == null) {
            mimeType = "application/octet-stream"; // Tipo genérico = fuerza descarga en navegador
        }

        // Configurar los headers de la respuesta HTTP para el archivo
        response.setContentType(mimeType);                                                   // Tipo del archivo
        response.setHeader("Content-Length", String.valueOf(file.length()));                  // Tamaño en bytes
        response.setHeader("Content-Disposition", "inline; filename=\"" + file.getName() + "\""); // Mostrar en navegador (no descargar)

        // Copiar los bytes del archivo directamente al OutputStream de la respuesta HTTP
        // Files.copy() es eficiente: no carga todo en memoria, usa streams internamente
        Files.copy(file.toPath(), response.getOutputStream());
    }
}
