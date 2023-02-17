package me.robi.invoicesystem.controllers.invoice.types;

import jakarta.servlet.http.HttpServletRequest;
import me.robi.invoicesystem.entities.ProductEntity;
import me.robi.invoicesystem.repositories.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;

public abstract class InvoiceType {
    @Autowired
    private ProductRepository productRepository;

    public abstract ResponseEntity getResponse(HttpServletRequest request, List<ProductEntity> entities, int totalSum);
}
