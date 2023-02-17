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

    @GetMapping("/products")
    public Iterable<ProductEntity> getProductRepository() {
        return productRepository.findAll();
    }

    @GetMapping({"/invoice", "/invoice/{type}"})
    public ResponseEntity<Map<String, Object>> createInvoiceBase(HttpServletRequest request, @PathVariable(required = false, value = "type") String type, @RequestParam long[] id) {
        if(type == null)
            type = InvoiceType.RAW_INVOICE;

        InvoiceType invoiceType = INVOICE_TYPES.get(type);
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

    public boolean verifyFileName(String fileName) {
        String extension = "";
        if(fileName.contains(".")) {
            String[] split = fileName.split("\\.");
            extension = split[split.length - 1];
        }
        return verifyFileName(fileName, extension);
    }

    public boolean verifyFileName(String fileName, String fileExtensionRegex) {
        return fileName.matches("[\\w\\-_]+" + (fileExtensionRegex.length() > 0 ? "\\.\\w+" : ""));
    }
}
