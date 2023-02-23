# Invoice System
Simple Invoice System backend.

Users can select specific invoices from a table and generate a PDF file from it to sum the amounts.

## Frontends
- [React](https://github.com/RobiFox/Invoice-System-Frontend)
- [Flutter](https://github.com/RobiFox/Invoice-System-Flutter)

## API
- `/api/products` Lists all products in the database.
  - No parameters.
- `/api/invoice`, `/api/invoice/{type}` Returns the invoice for the selected items.
  - `id` List of Product IDs that are selected.
  - `{type}` Type of Response Type. Optional, defaults to `raw`. sAvailable responses:
    - `raw` JSON type, as Spring returns it by default
    - `pdf` Generates a PDF file on disk, and returns a URL to it.
- `/api/access-pdf/{file}` Returns the PDF as stored on the server.
  - `{file}` Name of the file. `.pdf` extension ending is optional 

## Notes
- The software is designed in a way to allow easy refactor and extension of the application.
- As per the required task, the PDF file is saved to disk.
  -  In a [previous commit](https://github.com/RobiFox/Invoice-System/blob/a640ebeb4e09f6e705abecceef882ffa72cd6ed0/src/main/java/me/robi/invoicesystem/controllers/InvoiceController.java#L68), it was directly sent to the user, no saving on disk
- [InvoiceType](https://github.com/RobiFox/Invoice-System/tree/master/src/main/java/me/robi/invoicesystem/controllers/invoice/types) used to be an Abstract Class, however an Interface seems more fit, because it contains only one method, no constructor necessarily, and interfaces with singular method can be inlined.
- PDF File Saving [used to name the file after its hash code](https://github.com/RobiFox/Invoice-System/blob/0f0de5f5e10462dd2f1abe982c22aa62b7818a37/src/main/java/me/robi/invoicesystem/controllers/invoice/types/PdfInvoiceType.java#L80), but has been changed to a random UUID that gets cached locally.
  - This prevents an edge case where two different list of `ProductEntities` would result in the same hashCode, thus not creating the file.
  - This also lets the developers update the PDF format, should it be needed. Querying an invoice with the same `id`s across different sessions would regenerate the file.