package com.naturgy.workshop.service;

import com.naturgy.workshop.domain.model.Invoice;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Generates a simple PDF invoice using Apache PDFBox.
 */
@Service
public class PdfService {

    public byte[] generateInvoicePdf(Invoice invoice) throws IOException {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                float margin = 50;
                float y = page.getMediaBox().getHeight() - margin;

                PDType1Font bold    = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
                PDType1Font regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

                // Header
                cs.beginText();
                cs.setFont(bold, 20);
                cs.newLineAtOffset(margin, y);
                cs.showText("NATURGY WORKSHOP - FACTURA");
                cs.endText();
                y -= 30;

                cs.beginText();
                cs.setFont(regular, 12);
                cs.newLineAtOffset(margin, y);
                cs.showText("Nº Factura: " + invoice.getInvoiceId());
                cs.endText();
                y -= 18;

                cs.beginText();
                cs.setFont(regular, 12);
                cs.newLineAtOffset(margin, y);
                cs.showText("Periodo: " + invoice.getPeriod());
                cs.endText();
                y -= 18;

                cs.beginText();
                cs.setFont(regular, 12);
                cs.newLineAtOffset(margin, y);
                cs.showText("Fecha generacion: " + invoice.getGeneratedAt().toString().substring(0, 16).replace("T", " "));
                cs.endText();
                y -= 30;

                // Separator
                cs.moveTo(margin, y);
                cs.lineTo(page.getMediaBox().getWidth() - margin, y);
                cs.stroke();
                y -= 20;

                // Customer info
                cs.beginText();
                cs.setFont(bold, 12);
                cs.newLineAtOffset(margin, y);
                cs.showText("DATOS DEL CLIENTE");
                cs.endText();
                y -= 18;

                cs.beginText();
                cs.setFont(regular, 11);
                cs.newLineAtOffset(margin, y);
                cs.showText("Cliente: " + invoice.getCustomerFullName());
                cs.endText();
                y -= 16;

                cs.beginText();
                cs.setFont(regular, 11);
                cs.newLineAtOffset(margin, y);
                cs.showText("Contrato: " + invoice.getContractId() + "  |  Contador: " + invoice.getMeterId());
                cs.endText();
                y -= 16;

                cs.beginText();
                cs.setFont(regular, 11);
                cs.newLineAtOffset(margin, y);
                cs.showText("Tipo contrato: " + invoice.getContractType());
                cs.endText();
                y -= 30;

                // Separator
                cs.moveTo(margin, y);
                cs.lineTo(page.getMediaBox().getWidth() - margin, y);
                cs.stroke();
                y -= 20;

                // Billing detail
                cs.beginText();
                cs.setFont(bold, 12);
                cs.newLineAtOffset(margin, y);
                cs.showText("DETALLE DE FACTURACION");
                cs.endText();
                y -= 18;

                cs.beginText();
                cs.setFont(regular, 11);
                cs.newLineAtOffset(margin, y);
                cs.showText("Energia consumida: " + invoice.getTotalKwh() + " kWh");
                cs.endText();
                y -= 16;

                cs.beginText();
                cs.setFont(regular, 11);
                cs.newLineAtOffset(margin, y);
                cs.showText("Subtotal: " + invoice.getSubtotal() + " EUR");
                cs.endText();
                y -= 16;

                cs.beginText();
                cs.setFont(regular, 11);
                cs.newLineAtOffset(margin, y);
                cs.showText("Impuestos: " + invoice.getTax() + " EUR");
                cs.endText();
                y -= 20;

                // Total
                cs.beginText();
                cs.setFont(bold, 14);
                cs.newLineAtOffset(margin, y);
                cs.showText("TOTAL: " + invoice.getTotal() + " EUR");
                cs.endText();
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.save(out);
            return out.toByteArray();
        }
    }
}
