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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;

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
}
