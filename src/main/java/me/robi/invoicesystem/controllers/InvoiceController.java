package me.robi.invoicesystem.controllers;

import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfWriter;
import me.robi.invoicesystem.ResponseConstants;
import me.robi.invoicesystem.entities.ProductEntity;
import me.robi.invoicesystem.repositories.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

@RestController
@RequestMapping("/api")
public class InvoiceController {
    @Autowired
    public ProductRepository productRepository;

    @GetMapping("/products")
    public Iterable<ProductEntity> getProductRepository() {
        return productRepository.findAll();
    }

    @GetMapping("/invoice")
    public ResponseEntity createInvoice(@RequestParam long[] id, @RequestParam(required = false) boolean returnPdf) {
        List<ProductEntity> entities = new ArrayList<>();
        int amountSum = 0;

        for(long l : id) {
            ProductEntity product = productRepository.findById(l).orElse(null);
            if(product == null)
                return new ResponseEntity(Collections.singletonMap(ResponseConstants.RESPONSE_STATUS, String.format("Product of ID %s not found.", l)), HttpStatus.BAD_REQUEST);
            entities.add(product);
            amountSum += product.getAmount();
        }

        Map<String, Object> responseBody = new HashMap<>();

        // TODO
        if(returnPdf) {
            try(ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
                Document d = generatePdf(entities);
                PdfWriter.getInstance(d, byteArrayOutputStream);
                d.open();
                byte[] documentBytes = byteArrayOutputStream.toByteArray();
                d.close();
            } catch (DocumentException | IOException e) {
                return new ResponseEntity(Collections.singletonMap(ResponseConstants.RESPONSE_STATUS, String.format("Runtime Exception (%s): %s", e.getClass().getName(), e.getMessage())), HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }

        responseBody.put(ResponseConstants.InvoiceResponseConstants.PRODUCTS_SUM, amountSum);
        responseBody.put(ResponseConstants.InvoiceResponseConstants.PRODUCTS_LIST, entities);

        return new ResponseEntity(responseBody, HttpStatus.OK);
    }

    // TODO
    private Document generatePdf(List<ProductEntity> entities) throws FileNotFoundException, DocumentException {
        Document document = new Document();
        document.open();
        PdfPTable table = new PdfPTable(2);
        for(ProductEntity product : entities) {
            {
                PdfPCell cell = new PdfPCell();
                cell.setPhrase(new Phrase(product.getName()));
                table.addCell(cell);
            }
            {
                PdfPCell cell = new PdfPCell();
                cell.setPhrase(new Phrase(String.valueOf(product.getAmount())));
                table.addCell(cell);
            }
        }
        document.add(table);
        document.close();
        return document;
    }
}
