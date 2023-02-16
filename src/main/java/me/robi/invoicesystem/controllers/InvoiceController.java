package me.robi.invoicesystem.controllers;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.pdf.draw.LineSeparator;
import me.robi.invoicesystem.entities.ProductEntity;
import me.robi.invoicesystem.repositories.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.*;
import java.util.*;
import java.util.List;

import static me.robi.invoicesystem.ResponseConstants.InvoiceResponseConstants.*;
import static me.robi.invoicesystem.ResponseConstants.*;

@RestController
@RequestMapping("/api")
public class InvoiceController {
    @Autowired
    public ProductRepository productRepository;

    @GetMapping("/products")
    public Iterable<ProductEntity> getProductRepository() {
        return productRepository.findAll();
    }

    @GetMapping({"/invoice", "/invoice/json"})
    public ResponseEntity<Map<String, Object>> createInvoiceJson(@RequestParam long[] id) {
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
    public ResponseEntity createInvoicePdf(@RequestParam long[] id) {
        ResponseEntity<Map<String, Object>> jsonResponse = createInvoiceJson(id);

        if(jsonResponse.getStatusCode() != HttpStatus.OK)
            return jsonResponse;

        List<ProductEntity> entities = (List<ProductEntity>) jsonResponse.getBody().get(PRODUCTS_LIST);
        int amountSum = (int) jsonResponse.getBody().get(PRODUCTS_SUM);

        try(ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
            generatePdf(entities, amountSum, byteArrayOutputStream);
            byte[] documentBytes = byteArrayOutputStream.toByteArray();
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF).body(documentBytes);
        } catch (DocumentException | IOException e) {
            return ResponseEntity.internalServerError().body(Collections.singletonMap(RESPONSE_STATUS, String.format("Runtime Exception (%s): %s", e.getClass().getName(), e.getMessage())));
        }
    }

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
