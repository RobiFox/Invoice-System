package me.robi.invoicesystem.controllers.invoice;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.pdf.draw.LineSeparator;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import me.robi.invoicesystem.constants.PathConstants;
import me.robi.invoicesystem.controllers.invoice.types.InvoiceType;
import me.robi.invoicesystem.controllers.invoice.types.PdfInvoiceType;
import me.robi.invoicesystem.controllers.invoice.types.RawInvoiceType;
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

    public static final HashMap<String, InvoiceType> INVOICE_TYPES = new HashMap<>();
    static {
        INVOICE_TYPES.put(InvoiceType.RAW_INVOICE, new RawInvoiceType());
        INVOICE_TYPES.put(InvoiceType.PDF_INVOICE, new PdfInvoiceType());
    }

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
     * @param request The HttpServletRequest provided by Spring
     * @param type Type of Response Type, from the list of {@link #INVOICE_TYPES}
     * @param id An array of longs, containing the specified IDs of products.
     * @return List of filtered products based on {@param id} and a response type of {@param type}.
     */
    @GetMapping({"/invoice", "/invoice/{type}"})
    public ResponseEntity<Map<String, Object>> createInvoiceBase(HttpServletRequest request, @PathVariable(required = false, value = "type") String type, @RequestParam long[] id) {
        if(type == null)
            type = InvoiceType.RAW_INVOICE;

        InvoiceType invoiceType = INVOICE_TYPES.get(type);

        if(invoiceType == null)
            return ResponseEntity.badRequest().body(Collections.singletonMap(RESPONSE_STATUS, String.format("Type %s does not exist.", type)));

        List<ProductEntity> entities = new ArrayList<>();
        int amountSum = 0;

        for(long l : id) {
            ProductEntity product = productRepository.findById(l).orElse(null);
            if(product == null)
                return ResponseEntity.badRequest().body(Collections.singletonMap(RESPONSE_STATUS, String.format("Product of ID %s not found.", l)));
            entities.add(product);
            amountSum += product.getAmount();
        }

        return invoiceType.getResponse(request, entities, amountSum);
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
        return fileName.matches("[\\w\\-_]+" + (fileExtensionRegex.length() > 0 ? "\\.\\w+" : ""));
    }
}
