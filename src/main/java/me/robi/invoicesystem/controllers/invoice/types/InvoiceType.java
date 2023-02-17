package me.robi.invoicesystem.controllers.invoice.types;

import jakarta.servlet.http.HttpServletRequest;
import me.robi.invoicesystem.entities.ProductEntity;
import org.springframework.http.ResponseEntity;

import java.util.List;

public abstract class InvoiceType {
    public static final String RAW_INVOICE = "raw";
    public static final String PDF_INVOICE = "pdf";

    public abstract ResponseEntity getResponse(HttpServletRequest request, List<ProductEntity> entities, int totalSum);
}
