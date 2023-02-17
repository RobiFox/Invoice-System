package me.robi.invoicesystem.controllers.invoice.types;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.pdf.draw.LineSeparator;
import jakarta.servlet.http.HttpServletRequest;
import me.robi.invoicesystem.constants.PathConstants;
import me.robi.invoicesystem.entities.ProductEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

import static me.robi.invoicesystem.constants.ResponseConstants.REDIRECT_URL;
import static me.robi.invoicesystem.constants.ResponseConstants.RESPONSE_STATUS;

/**
 * An Invoice Type that generates a PDF file
 * and saves it at {@link PathConstants#PDF_FILE_STORAGE},
 * returning a URL path to it.
 */
public class PdfInvoiceType extends InvoiceType {
    /**
     * Returns link to the PDF file. If an exact file
     * like that doesn't exist yet, it creates another one.
     * @param request HttpServletRequest provided by Spring
     * @param entities List of all entities
     * @param totalSum Total sum of the entities amount
     * @return Link to access the PDF file
     */
    @Override
    public ResponseEntity getResponse(HttpServletRequest request, List<ProductEntity> entities, int totalSum) {
        int hashCode = entities.hashCode();
        String fileName = String.format("%s.pdf", hashCode);
        Path storagePath = Paths.get(PathConstants.PDF_FILE_STORAGE, fileName);

        if(!Files.exists(storagePath))
            try(FileOutputStream fileOutputStream = new FileOutputStream(new File(storagePath.toUri()))) {
                generatePdf(entities, totalSum, fileOutputStream);
            } catch (DocumentException | IOException e) {
                return ResponseEntity.internalServerError().body(Collections.singletonMap(RESPONSE_STATUS, String.format("Runtime Exception (%s): %s", e.getClass().getName(), e.getMessage())));
            }

        return ResponseEntity.ok().body(Collections.singletonMap(
                REDIRECT_URL,
                UriComponentsBuilder.fromUriString(request.getRequestURL().toString())
                        .replacePath("/api/access-pdf/" + fileName)
                        .build().toString()
        ));
    }

    /**
     * Generates the PDF file, and writes it into an OutputStream
     * @param entities List of all entities
     * @param totalSum Their total sum
     * @param outputStream The OutputStrea to write into
     * @return The finished, closed Document
     * @throws DocumentException An exception regarding Document should it happen
     */
    private Document generatePdf(List<ProductEntity> entities, int totalSum, OutputStream outputStream) throws DocumentException {
        Document document = new Document();
        PdfWriter.getInstance(document, outputStream);

        document.open();
        {
            PdfPTable table = new PdfPTable(2);
            entities.forEach(product -> addCellsToTable(table, product));
            document.add(table);
        }

        Chunk separator = new Chunk(new LineSeparator());
        separator.setLineHeight(8f);
        document.add(separator);

        {
            PdfPTable table = new PdfPTable(2);
            addCellsToTable(table, "Total Sum", totalSum);
            document.add(table);
        }

        document.close();

        return document;
    }


    private void addCellsToTable(PdfPTable table, ProductEntity product) {
        addCellsToTable(table, product.getName(), String.valueOf(product.getAmount()));
    }

    private void addCellsToTable(PdfPTable table, String key, Object value) {
        if(table.getNumberOfColumns() != 2) throw new IllegalArgumentException("Table Column Count is not 2.");
        {
            PdfPCell cell = new PdfPCell();
            cell.setPhrase(new Phrase(key));
            cell.setBorder(0);
            table.addCell(cell);
        }
        {
            PdfPCell cell = new PdfPCell();
            cell.setPhrase(new Phrase(String.valueOf(value)));
            cell.setBorder(0);
            cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            table.addCell(cell);
        }
    }
}
