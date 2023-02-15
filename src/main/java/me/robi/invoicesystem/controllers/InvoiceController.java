package me.robi.invoicesystem.controllers;

import me.robi.invoicesystem.ResponseConstants;
import me.robi.invoicesystem.entities.ProductEntity;
import me.robi.invoicesystem.repositories.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity createInvoice(@RequestParam long[] id) {
        List<ProductEntity> entities = new ArrayList<>();
        int amountSum = 0;

        for(long l : id) {
            ProductEntity product = productRepository.findById(l).orElse(null);
            if(product == null)
                return new ResponseEntity(Collections.singletonMap(ResponseConstants.RESPONSE_STATUS, String.format("Product of ID %s not found.", l)), HttpStatus.BAD_REQUEST);
            entities.add(product);
            amountSum += product.getAmount();
        }

        // TODO create pdf file

        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put(ResponseConstants.InvoiceResponseConstants.PRODUCTS_SUM, amountSum);
        responseBody.put(ResponseConstants.InvoiceResponseConstants.PRODUCTS_LIST, entities);

        return new ResponseEntity(responseBody, HttpStatus.OK);
    }
}
