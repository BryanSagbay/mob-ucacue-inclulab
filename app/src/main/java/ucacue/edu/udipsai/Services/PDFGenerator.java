package ucacue.edu.udipsai.Services;

import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.events.Event;
import com.itextpdf.kernel.events.IEventHandler;
import com.itextpdf.kernel.events.PdfDocumentEvent;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.*;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.kernel.pdf.xobject.PdfFormXObject;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.*;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

public class PDFGenerator {

    private static class BackgroundEventHandler implements IEventHandler {
        private final PdfFormXObject background;

        public BackgroundEventHandler(PdfFormXObject background) {
            this.background = background;
        }

        @Override
        public void handleEvent(Event event) {
            PdfDocumentEvent docEvent = (PdfDocumentEvent) event;
            PdfPage page = docEvent.getPage();
            new PdfCanvas(page.newContentStreamBefore(), page.getResources(), docEvent.getDocument())
                    .addXObject(background);
        }
    }

    public static void generatePDF(
            OutputStream outputStream,
            String email,
            String date,
            List<Map<String, Object>> dataList,
            String pacienteNombre
    ) throws Exception {
        if (outputStream == null) {
            throw new Exception("OutputStream es nulo, no se puede generar el PDF.");
        }

        // Cargar la plantilla para extraer el fondo
        InputStream templateStream = PDFGenerator.class.getClassLoader().getResourceAsStream("assets/Plantilla.pdf");
        if (templateStream == null) {
            throw new Exception("No se pudo encontrar la plantilla PDF en assets.");
        }

        PdfReader templateReader = new PdfReader(templateStream);
        PdfDocument templatePdf = new PdfDocument(templateReader);
        PdfPage templatePage = templatePdf.getFirstPage();
        PageSize pageSize = new PageSize(templatePage.getPageSize());

        // Crear nuevo documento PDF
        PdfWriter writer = new PdfWriter(outputStream);
        PdfDocument pdfDoc = new PdfDocument(writer);
        Document document = new Document(pdfDoc, pageSize);

        // Extraer el fondo como XObject
        PdfFormXObject background = templatePage.copyAsFormXObject(pdfDoc);
        templatePdf.close();

        // Añadir evento para fondo en cada página
        pdfDoc.addEventHandler(PdfDocumentEvent.START_PAGE, new BackgroundEventHandler(background));

        // Ajustar márgenes para evitar superposición
        float margenSuperior = 80;
        float margenInferior = 80;
        document.setMargins(margenSuperior, 50, margenInferior, 50);

        PdfFont font = PdfFontFactory.createFont("assets/fonts/segoe-ui-emoji.ttf", PdfEncodings.IDENTITY_H);
        DeviceRgb pinColor = new DeviceRgb(0, 102, 204);
        DeviceRgb checkColor = new DeviceRgb(0, 153, 0);

        // Contenido del PDF
        document.add(new Paragraph("\n\n\n"));
        document.add(new Paragraph("📌 Reporte de Resultados").setFont(font).setFontColor(pinColor).setBold().setFontSize(16));
        document.add(new Paragraph("Usuario: " + email).setFont(font).setFontSize(12));

        if (date.equals("Todas")) {
            document.add(new Paragraph("Fecha: Todas las fechas").setFont(font).setFontSize(12));
        } else {
            document.add(new Paragraph("Fecha: " + date).setFont(font).setFontSize(12));
        }

        if (pacienteNombre != null && !pacienteNombre.isEmpty()) {
            document.add(new Paragraph("Paciente: " + pacienteNombre).setFont(font).setFontSize(12));
        } else {
            document.add(new Paragraph("Paciente: Todos los pacientes").setFont(font).setFontSize(12));
        }

        document.add(new Paragraph(" "));

        if (dataList.isEmpty()) {
            document.add(new Paragraph("No hay datos disponibles.").setFont(font));
        } else {
            for (Map<String, Object> data : dataList) {
                if (data.containsKey("titulo")) {
                    document.add(new Paragraph("📌 " + data.get("titulo").toString()).setFont(font).setFontColor(pinColor).setBold().setFontSize(14));
                }
                document.add(new Paragraph(" "));

                Table table = new Table(2);
                for (Map.Entry<String, Object> entry : data.entrySet()) {
                    String key = entry.getKey();
                    Object value = entry.getValue();

                    if (!key.equals("timestamp") && !key.equals("correoUsuario") && !key.equals("titulo") && !key.equals("nombrePaciente")) {
                        table.addCell(new Cell().add(new Paragraph(key).setFont(font).setBold()));

                        Paragraph valueParagraph = new Paragraph();
                        String valueStr = value.toString();

                        for (int i = 0; i < valueStr.length(); i++) {
                            char c = valueStr.charAt(i);
                            String charStr = String.valueOf(c);

                            if (charStr.equals("✅")) {
                                valueParagraph.add(new Text(charStr).setFont(font).setFontColor(checkColor));
                            } else if (charStr.equals("❌")) {
                                valueParagraph.add(new Text(charStr).setFont(font).setFontColor(ColorConstants.RED));
                            } else if (charStr.equals("📌")) {
                                valueParagraph.add(new Text(charStr).setFont(font).setFontColor(pinColor));
                            } else {
                                valueParagraph.add(new Text(charStr).setFont(font));
                            }
                        }

                        table.addCell(new Cell().add(valueParagraph));
                    }
                }

                document.add(table);
                document.add(new Paragraph(" "));

                if (document.getRenderer().getCurrentArea().getBBox().getHeight() < 100) {
                    document.add(new AreaBreak());
                    document.add(new Paragraph("\n\n\n"));
                }
            }
        }

        document.close();
    }
}
