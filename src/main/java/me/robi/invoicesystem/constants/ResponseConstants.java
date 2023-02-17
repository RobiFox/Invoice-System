package me.robi.invoicesystem.constants;

/**
 * A list of global response keys used when returning a ResponseEntity.
 * Also contains a subclass that has keys specific to controllers.
 */
public class ResponseConstants {
    public static final String RESPONSE_STATUS = "status";
    public static final String REDIRECT_URL = "redirectUrl";

    public static class InvoiceResponseConstants {
        public static final String PRODUCTS_LIST = "productsList";
        public static final String PRODUCTS_SUM = "amountSum";
    }
}
