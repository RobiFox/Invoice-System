package me.robi.invoicesystem.controllers.invoice.types;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.pdf.draw.LineSeparator;
import jakarta.servlet.http.HttpServletRequest;
import me.robi.invoicesystem.constants.PathConstants;
import me.robi.invoicesystem.entities.ProductEntity;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
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
import java.util.Map;

import static me.robi.invoicesystem.constants.PathConstants.PDF_FILE_STORAGE;
import static me.robi.invoicesystem.constants.ResponseConstants.REDIRECT_URL;
import static me.robi.invoicesystem.constants.ResponseConstants.RESPONSE_STATUS;

/**
 * An Invoice Type that generates a PDF file
 * and saves it at {@link PathConstants#PDF_FILE_STORAGE},
 * returning a URL path to it.
 *
 * Contains Mappings for access-pdf mapping since that mapping is specific
 * to the PDF invoice type.
 */
@RestController
@RequestMapping("/api")
public class PdfInvoiceType implements InvoiceType {
    /**
     * Returns link to the PDF file. If an exact file
     * like that doesn't exist yet, it creates another one.
     * @param request HttpServletRequest provided by Spring
     * @param entities List of all entities
     * @param totalSum Total sum of the entities amount
     * @param file The file reference
     * @return Link to access the PDF file
     */
    public ResponseEntity getResponse(HttpServletRequest request, List<ProductEntity> entities, int totalSum, File file) {
        if(!Files.exists(file.toPath()))
            try(FileOutputStream fileOutputStream = new FileOutputStream(file)) {
                generatePdf(entities, totalSum, fileOutputStream);
            } catch (DocumentException | IOException e) {
                return ResponseEntity.internalServerError().body(Collections.singletonMap(RESPONSE_STATUS, String.format("Runtime Exception (%s): %s", e.getClass().getName(), e.getMessage())));
            }

        return ResponseEntity.ok().body(Collections.singletonMap(
                REDIRECT_URL,
                UriComponentsBuilder.fromUriString(request.getRequestURL().toString())
                        .replacePath("/api/access-pdf/" + file.getName())
                        .build().toString()
        ));
    }

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
        return getResponse(request, entities, totalSum, Paths.get(PDF_FILE_STORAGE, fileName).toFile());
    }

    /**
     * Generates the PDF file, and writes it into an OutputStream
     * @param entities List of all entities
     * @param totalSum Their total sum
     * @param outputStream The OutputStrea to write into
     * @return The finished, closed Document
     * @throws DocumentException An exception regarding Document should it happen
     */
    public Document generatePdf(List<ProductEntity> entities, int totalSum, OutputStream outputStream) throws DocumentException {
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

    /**
     * Accesses the given file found in {@link PathConstants#PDF_FILE_STORAGE}, making
     * sure it's a valid file with a .pdf extension.
     * @param fileName Name of the file, with an optional .pdf extension at the end
     * @return An error message if the file is missing, or on invalid file format (illegal characters), or the contents of the pdf file found in {@link PathConstants#PDF_FILE_STORAGE}/{@param fileName}
     */
    @GetMapping("/access-pdf/{file}")
    public ResponseEntity accessPdf(@PathVariable(value = "file") String fileName) {
        return accessPdf(fileName, PDF_FILE_STORAGE);
    }

    /**
     * Accesses the given file found in {@link PathConstants#PDF_FILE_STORAGE}, making
     * sure it's a valid file with a .pdf extension.
     * @param fileName Name of the file, with an optional .pdf extension at the end
     * @param directory The directory to look for.
     * @return An error message if the file is missing, or on invalid file format (illegal characters), or the contents of the pdf file found in {@link PathConstants#PDF_FILE_STORAGE}/{@param fileName}
     */
    public ResponseEntity accessPdf(String fileName, String directory) {
        if(!fileName.endsWith(".pdf"))
            fileName = fileName + ".pdf";
        if(!verifyFileName(fileName))
            return ResponseEntity.badRequest().body(Collections.singletonMap(RESPONSE_STATUS, "Illegal file access"));

        Path path = Paths.get(directory, fileName);

        if(!Files.exists(path))
            return ResponseEntity.badRequest().body(Collections.singletonMap(RESPONSE_STATUS, String.format("File %s does not exist.", fileName)));

        try {
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF).body(Files.readAllBytes(path));
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(Collections.singletonMap(RESPONSE_STATUS, String.format("Runtime Exception (%s): %s", e.getClass().getName(), e.getMessage())));
        }
    }

    /**
     * Verifies the validity of a file name from an user input.
     * Calls {@link #verifyFileName(String, String)} with an automatically parsed file extension as the second parameter, if it exists.
     * @param fileName The file name from user input
     * @return {@code true} if the file doesn't contain illegal characters, {@code false} otherwise.
     */
    public boolean verifyFileName(String fileName) {
        String extension = "";
        if(fileName.contains(".")) {
            String[] split = fileName.split("\\.");
            extension = split[split.length - 1];
        }
        return verifyFileName(fileName, extension);
    }

    /**
     * Verifies the validity of a file name from an user input.
     * File name may only contain letters, hyphen or underscore, while the extension may only contain letters.
     * @param fileName The file name from user input
     * @param fileExtensionRegex The extension of the file
     * @return {@code true} if the file doesn't contain illegal characters, {@code false} otherwise.
     */
    public boolean verifyFileName(String fileName, String fileExtensionRegex) {
        return fileName.matches("[\\w\\-_]+" + (fileExtensionRegex.length() > 0 ? "\\.\\w+" : ""));
    }
}
