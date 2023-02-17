package me.robi.invoicesystem.controllers.invoice.types;

import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.Map;

import static me.robi.invoicesystem.constants.ResponseConstants.InvoiceResponseConstants.PRODUCTS_LIST;
import static me.robi.invoicesystem.constants.ResponseConstants.InvoiceResponseConstants.PRODUCTS_SUM;

public class RawInvoiceType extends InvoiceType {
    public RawInvoiceType(long[] id) {
        super(id);
    }

    @Override
    public ResponseEntity getResponse() {
        Map<String, Object> responseBody = new HashMap<>();

        responseBody.put(PRODUCTS_SUM, getTotalSum());
        responseBody.put(PRODUCTS_LIST, getEntities());

        return ResponseEntity.ok(responseBody);
    }
}
