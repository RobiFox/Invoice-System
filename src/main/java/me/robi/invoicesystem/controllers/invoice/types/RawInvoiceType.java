package me.robi.invoicesystem.controllers.invoice.types;

import jakarta.servlet.http.HttpServletRequest;
import me.robi.invoicesystem.entities.ProductEntity;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static me.robi.invoicesystem.constants.ResponseConstants.InvoiceResponseConstants.PRODUCTS_LIST;
import static me.robi.invoicesystem.constants.ResponseConstants.InvoiceResponseConstants.PRODUCTS_SUM;

public class RawInvoiceType extends InvoiceType {
    @Override
    public ResponseEntity getResponse(HttpServletRequest request, List<ProductEntity> entities, int totalSum) {
        Map<String, Object> responseBody = new HashMap<>();

        responseBody.put(PRODUCTS_LIST, entities);
        responseBody.put(PRODUCTS_SUM, totalSum);

        return ResponseEntity.ok(responseBody);
    }
}
