package com.nirami.controller;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.nirami.dao.OrdenDAO;
import com.nirami.model.Orden;
import com.nirami.model.OrdenDetalle;
import com.nirami.model.Usuario;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

/**
 * Servlet que genera y sirve el recibo de compra de una orden en formato PDF.
 * URL: /recibo?idOrden=X
 * GET → genera el PDF y lo devuelve como descarga al navegador del cliente
 *
 * Librería PDF: iText 7 (com.itextpdf) declarada en pom.xml.
 *
 * SEGURIDAD: Solo el cliente dueño de la orden puede descargar su recibo.
 *   Si alguien intenta descargar el recibo de otro cliente, es redirigido.
 *
 * Conexiones:
 *   → session.getAttribute("usuarioLogueado") (verificar identidad del cliente)
 *   → OrdenDAO.obtenerOrdenPorId()             (leer la orden con sus detalles)
 *   → response.getOutputStream()               (escribir el PDF directamente al HTTP response)
 *   → perfil.jsp                               (el link "Descargar Recibo" apunta aquí)
 */
@WebServlet("/recibo") // Responde a /recibo?idOrden=X
public class ReciboPDFServlet extends HttpServlet {

    // DAO para leer la orden y sus líneas de detalle (tablas: ordenes + orden_detalle + productos)
    private OrdenDAO ordenDAO = new OrdenDAO();

    /**
     * GET /recibo?idOrden=X → Genera el PDF del recibo y lo envía como descarga.
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // Obtener el usuario logueado de la sesión (puesto por LoginServlet)
        HttpSession session = request.getSession();
        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");

        // Verificar que haya sesión activa
        if (usuario == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        // Leer el ID de la orden del parámetro de URL (?idOrden=X)
        String idParam = request.getParameter("idOrden");
        if (idParam == null) {
            // No se proporcionó ID → redirigir al perfil donde están los pedidos
            response.sendRedirect(request.getContextPath() + "/perfil");
            return;
        }

        int idOrden = Integer.parseInt(idParam);

        // Consultar la orden completa desde la BD, incluyendo sus líneas de detalle
        // obtenerOrdenPorId hace: SELECT * FROM ordenes + JOIN orden_detalle + JOIN productos
        Orden orden = ordenDAO.obtenerOrdenPorId(idOrden);

        // SEGURIDAD: Verificar que la orden existe Y que pertenece al cliente logueado
        // Un cliente no puede descargar el recibo de la orden de otro cliente
        if (orden == null || orden.getIdCliente() != usuario.getIdUsuario()) {
            response.sendRedirect(request.getContextPath() + "/perfil");
            return;
        }

        // ── Configurar la respuesta HTTP para que el navegador descargue un PDF ──
        response.setContentType("application/pdf"); // Tipo MIME de PDF

        // "attachment" hace que el navegador descargue el archivo (no abrirlo en pestaña)
        // "filename=" es el nombre con que se descargará el archivo
        response.setHeader("Content-Disposition",
            "attachment; filename=\"recibo_nirami_" + idOrden + ".pdf\"");

        try {
            /* ── Construcción del PDF con iText 7 ──
             * Flujo: PdfWriter (escribe bytes) → PdfDocument (estructura PDF) → Document (contenido)
             * Se escribe directamente al OutputStream del response HTTP (no se crea archivo en disco)
             */
            PdfWriter   writer   = new PdfWriter(response.getOutputStream()); // Escribe al HTTP
            PdfDocument pdf      = new PdfDocument(writer);                    // Estructura interna del PDF
            Document    document = new Document(pdf);                           // API de alto nivel para agregar contenido

            // ── Encabezado del recibo ──
            document.add(new Paragraph("NIRAMI - Artesanías Únicas")
                    .setTextAlignment(TextAlignment.CENTER) // Centrado
                    .setBold()                              // Negrita
                    .setFontSize(20));                      // Tamaño de fuente

            document.add(new Paragraph("Recibo de Compra\n")
                    .setTextAlignment(TextAlignment.CENTER));

            // ── Información general de la orden ──
            document.add(new Paragraph("Orden #" + orden.getIdOrden()));                        // ID único de la orden
            document.add(new Paragraph("Fecha: " + orden.getFechaOrden()));                     // Fecha/hora de la compra
            document.add(new Paragraph("Cliente: " + usuario.getNombre()));                     // Nombre del comprador
            document.add(new Paragraph("Dirección de Envío: " + orden.getDireccion() + "\n\n")); // Dirección donde se enviará

            // ── Tabla de productos comprados ──
            // Los anchos de columna en puntos (pt): Producto=200, Cantidad=100, Precio=100, Subtotal=100
            float[] columnWidths = {200F, 100F, 100F, 100F};
            Table table = new Table(columnWidths); // Crear tabla con 4 columnas

            // Encabezados de la tabla
            table.addHeaderCell("Producto");
            table.addHeaderCell("Cantidad");
            table.addHeaderCell("P. Unitario");
            table.addHeaderCell("Subtotal");

            // Una fila por cada línea de detalle de la orden
            // orden.getDetalles() → List<OrdenDetalle> cargado por OrdenDAO
            for (OrdenDetalle od : orden.getDetalles()) {
                table.addCell(od.getProducto().getNombre());                    // Nombre del producto
                table.addCell(String.valueOf(od.getCantidad()));                // Cantidad comprada
                table.addCell("$" + od.getPrecioUnitario().toString());         // Precio al momento de la compra
                table.addCell("$" + od.getSubtotalLinea().toString());          // precio × cantidad
            }

            document.add(table); // Agregar la tabla al documento PDF

            // ── Totales alineados a la derecha ──
            document.add(new Paragraph("\nSubtotal: $" + orden.getSubtotal())
                    .setTextAlignment(TextAlignment.RIGHT));

            document.add(new Paragraph("IVA (19%): $" + orden.getIva())
                    .setTextAlignment(TextAlignment.RIGHT));

            document.add(new Paragraph("Total: $" + orden.getTotal())
                    .setBold()                               // Total en negrita para destacarlo
                    .setTextAlignment(TextAlignment.RIGHT));

            // Cerrar el documento finaliza el PDF y libera recursos
            // Los bytes ya fueron escritos al OutputStream del response HTTP
            document.close();

        } catch (Exception e) {
            // Si falla la generación del PDF (ej: error de iText), solo registrar en log
            e.printStackTrace();
        }
    }
}
