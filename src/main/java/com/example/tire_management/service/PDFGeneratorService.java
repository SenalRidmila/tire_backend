package com.example.tire_management.service;

import java.io.ByteArrayOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.example.tire_management.model.TireOrder;
import com.example.tire_management.model.TireRequest;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;

@Service
public class PDFGeneratorService {

    private static final Logger logger = LoggerFactory.getLogger(PDFGeneratorService.class);

    public byte[] generateTireRequestPDF(TireRequest request) {
        if (request == null) {
            logger.error("Cannot generate PDF: Tire request is null");
            return new byte[0];
        }

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf, PageSize.A4);

            document.add(new Paragraph("Tire Replacement Request Details")
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontSize(18));

            Table table = new Table(2).useAllAvailableWidth();

            addTableRow(table, "Request ID", request.getId());
            addTableRow(table, "Vehicle Number", request.getVehicleNo());
            addTableRow(table, "Vehicle Type", request.getVehicleType());
            addTableRow(table, "Vehicle Brand", request.getVehicleBrand());
            addTableRow(table, "Vehicle Model", request.getVehicleModel());
            addTableRow(table, "User Section", request.getUserSection());
            addTableRow(table, "Tire Size", request.getTireSize());
            addTableRow(table, "Number of Tires", request.getNoOfTires());
            addTableRow(table, "Number of Tubes", request.getNoOfTubes());
            addTableRow(table, "Cost Center", request.getCostCenter());
            addTableRow(table, "Present KM", request.getPresentKm());
            addTableRow(table, "Previous KM", request.getPreviousKm());
            addTableRow(table, "Wear Indicator", request.getWearIndicator());
            addTableRow(table, "Wear Pattern", request.getWearPattern());
            addTableRow(table, "Officer Service No", request.getOfficerServiceNo());

            if (request.getComments() != null && !request.getComments().isEmpty()) {
                addTableRow(table, "Comments", request.getComments());
            }

            document.add(table);
            document.close();

            logger.info("PDF generated successfully for request: {}", request.getId());
            return baos.toByteArray();

        } catch (Exception e) {
            logger.error("Error generating PDF for request: {}", request.getId(), e);
            return new byte[0];
        }
    }

    public byte[] generateTireOrderPDF(TireOrder order) {
        if (order == null) {
            return new byte[0];
        }

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf, PageSize.A4);

            document.add(new Paragraph("Tire Order Details")
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontSize(18));

            Table table = new Table(2).useAllAvailableWidth();

            addTableRow(table, "Order ID", order.getId());
            addTableRow(table, "Vehicle No", order.getVehicleNo());
            addTableRow(table, "Vendor Name", order.getVendorName());
            addTableRow(table, "User Email", order.getUserEmail());
            addTableRow(table, "Quantity", String.valueOf(order.getQuantity()));
            addTableRow(table, "Tire Brand", order.getTireBrand());
            addTableRow(table, "Location", order.getLocation());
            addTableRow(table, "Status", order.getStatus());
            if ("rejected".equalsIgnoreCase(order.getStatus())) {
                addTableRow(table, "Rejection Reason", order.getRejectionReason());
            }

            document.add(table);
            document.close();
            return baos.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Error generating Tire Order PDF", e);
        }
    }

    private void addTableRow(Table table, String label, String value) {
        Cell labelCell = new Cell().add(new Paragraph(label))
                .setBackgroundColor(ColorConstants.LIGHT_GRAY)
                .setFontColor(ColorConstants.BLACK);
        Cell valueCell = new Cell().add(new Paragraph(value != null ? value : "N/A"));

        table.addCell(labelCell);
        table.addCell(valueCell);
    }
}
