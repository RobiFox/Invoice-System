package me.robi.invoicesystem.controllers.invoice.types;

import jakarta.servlet.http.HttpServletRequest;
import me.robi.invoicesystem.entities.ProductEntity;
import org.springframework.http.ResponseEntity;

import java.util.List;

/**
 * An interface for handling different types of responses
 */
public interface InvoiceType {
    String RAW_INVOICE = "raw";
    String PDF_INVOICE = "pdf";

    /**
     * Handles response based on given type
     * @param request HttpServletRequest provided by Spring
     * @param entities List of all entities
     * @param totalSum Total sum of the entities amount
     * @return Response in the given type
     */
    ResponseEntity getResponse(HttpServletRequest request, List<ProductEntity> entities, int totalSum);
}
