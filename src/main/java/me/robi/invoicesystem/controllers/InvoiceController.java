package me.robi.invoicesystem.controllers;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.pdf.draw.LineSeparator;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import me.robi.invoicesystem.constants.PathConstants;
import me.robi.invoicesystem.entities.ProductEntity;
import me.robi.invoicesystem.repositories.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;

import static me.robi.invoicesystem.constants.ResponseConstants.InvoiceResponseConstants.*;
import static me.robi.invoicesystem.constants.ResponseConstants.*;

/**
 * This class handles everything related to invoice
 * and their generation.
 */
@RestController
@RequestMapping("/api")
public class InvoiceController {
    @Autowired
    public ProductRepository productRepository;

    /**
     * Lists all products that are available in the repository
     * @return List of all products with all their fields.
     */
    @GetMapping("/products")
    public Iterable<ProductEntity> getProductRepository() {
        return productRepository.findAll();
    }

    /**
     * Base method for filtering products based on an array of ID.
     * Alternatively also returns them as a JSON if accessed via a GET request.
     * @param id An array of longs, containing the specified IDs of products.
     * @return List of filtered products based on {@param id}.
     */
    @GetMapping({"/invoice", "/invoice/json"})
    public ResponseEntity<Map<String, Object>> createInvoiceBase(@RequestParam long[] id) {
        List<ProductEntity> entities = new ArrayList<>();
        int amountSum = 0;

        for(long l : id) {
            ProductEntity product = productRepository.findById(l).orElse(null);
            if(product == null)
                return ResponseEntity.badRequest().body(Collections.singletonMap(RESPONSE_STATUS, String.format("Product of ID %s not found.", l)));
            entities.add(product);
            amountSum += product.getAmount();
        }

        Map<String, Object> responseBody = new HashMap<>();

        responseBody.put(PRODUCTS_SUM, amountSum);
        responseBody.put(PRODUCTS_LIST, entities);

        return ResponseEntity.ok(responseBody);
    }

    @GetMapping("/invoice/pdf")
    public ResponseEntity createInvoicePdf(HttpServletResponse httpServletResponse, HttpServletRequest request, @RequestParam long[] id) {
        ResponseEntity<Map<String, Object>> baseResponse = createInvoiceBase(id);

        if(baseResponse.getStatusCode() != HttpStatus.OK)
            return baseResponse;

        List<ProductEntity> entities = (List<ProductEntity>) baseResponse.getBody().get(PRODUCTS_LIST);
        int amountSum = (int) baseResponse.getBody().get(PRODUCTS_SUM);
        int hashCode = entities.hashCode();
        String fileName = String.format("%s.pdf", hashCode);
        Path storagePath = Paths.get(PathConstants.PDF_FILE_STORAGE, fileName);

        if(!Files.exists(storagePath))
            try(FileOutputStream fileOutputStream = new FileOutputStream(new File(storagePath.toUri()))) {
                generatePdf(entities, amountSum, fileOutputStream);
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
     * Accesses the given file found in {@link PathConstants#PDF_FILE_STORAGE}, making
     * sure it's a valid file with a .pdf extension.
     * @param fileName Name of the file, with an optional .pdf extension at the end
     * @return An error message if the file is missing, or on invalid file format (illegal characters), or the contents of the pdf file found in {@link PathConstants#PDF_FILE_STORAGE}/{@param fileName}
     */
    @GetMapping("/access-pdf/{file}")
    public ResponseEntity accessPdf(@PathVariable(value = "file") String fileName) {
        if(!fileName.endsWith(".pdf"))
            fileName = fileName + ".pdf";
        if(!verifyFileName(fileName))
            return ResponseEntity.badRequest().body(Collections.singletonMap(RESPONSE_STATUS, "Illegal file access"));

        Path path = Paths.get(PathConstants.PDF_FILE_STORAGE, fileName);

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
        return fileName.matches("\\w+" + (fileExtensionRegex.length() > 0 ? "\\.\\w+" : ""));
    }

    /**
     * Generates a PDF File given from the arguments
     * @param entities List of entities to put into the document
     * @param totalSum The number to put at the end of the document, after the seperator. It's the sum of all entities, calculated beforehand.
     * @param outputStream The output stream to write the document to.
     * @return The generated Document
     * @throws DocumentException An exception regarding the Document.
     */
    private Document generatePdf(List<ProductEntity> entities, int totalSum, OutputStream outputStream) throws FileNotFoundException, DocumentException {
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
