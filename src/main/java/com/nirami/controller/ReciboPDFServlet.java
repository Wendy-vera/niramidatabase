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

@WebServlet("/recibo")
public class ReciboPDFServlet extends HttpServlet {
    private OrdenDAO ordenDAO = new OrdenDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession();
        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");

        if (usuario == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        String idParam = request.getParameter("idOrden");
        if (idParam == null) {
            response.sendRedirect(request.getContextPath() + "/perfil");
            return;
        }

        int idOrden = Integer.parseInt(idParam);
        Orden orden = ordenDAO.obtenerOrdenPorId(idOrden);

        if (orden == null || orden.getIdCliente() != usuario.getIdUsuario()) {
            response.sendRedirect(request.getContextPath() + "/perfil");
            return;
        }

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=\"recibo_nirami_" + idOrden + ".pdf\"");

        try {
            PdfWriter writer = new PdfWriter(response.getOutputStream());
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

            document.add(new Paragraph("NIRAMI - Artesanías Únicas")
                    .setTextAlignment(TextAlignment.CENTER)
                    .setBold()
                    .setFontSize(20));
            document.add(new Paragraph("Recibo de Compra\n").setTextAlignment(TextAlignment.CENTER));

            document.add(new Paragraph("Orden #" + orden.getIdOrden()));
            document.add(new Paragraph("Fecha: " + orden.getFechaOrden()));
            document.add(new Paragraph("Cliente: " + usuario.getNombre()));
            document.add(new Paragraph("Dirección de Envío: " + orden.getDireccion() + "\n\n"));

            float[] columnWidths = {200F, 100F, 100F, 100F};
            Table table = new Table(columnWidths);
            
            table.addHeaderCell("Producto");
            table.addHeaderCell("Cantidad");
            table.addHeaderCell("P. Unitario");
            table.addHeaderCell("Subtotal");

            for (OrdenDetalle od : orden.getDetalles()) {
                table.addCell(od.getProducto().getNombre());
                table.addCell(String.valueOf(od.getCantidad()));
                table.addCell("$" + od.getPrecioUnitario().toString());
                table.addCell("$" + od.getSubtotalLinea().toString());
            }

            document.add(table);

            document.add(new Paragraph("\nSubtotal: $" + orden.getSubtotal()).setTextAlignment(TextAlignment.RIGHT));
            document.add(new Paragraph("IVA (19%): $" + orden.getIva()).setTextAlignment(TextAlignment.RIGHT));
            document.add(new Paragraph("Total: $" + orden.getTotal()).setBold().setTextAlignment(TextAlignment.RIGHT));

            document.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
