package me.robi.invoicesystem.controllers.invoice;

import me.robi.invoicesystem.entities.ProductEntity;
import me.robi.invoicesystem.repositories.ProductRepository;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static me.robi.invoicesystem.constants.ResponseConstants.RESPONSE_STATUS;

public abstract class InvoiceType {
    private List<ProductEntity> entities;
    private int totalSum;

    public InvoiceType(ProductRepository productRepository, long[] id) {
        entities = new ArrayList<>();
        totalSum = 0;

        for(long l : id) {
            ProductEntity product = productRepository.findById(l).orElse(null);
            if(product == null)
                throw new NullPointerException(String.format("Product of ID %s not found.", l));
            entities.add(product);
            totalSum += product.getAmount();
        }
    }

    public int getTotalSum() {
        return totalSum;
    }

    public List<ProductEntity> getEntities() {
        return entities;
    }
}
