# Invoice System
Simple Invoice System backend.

Users can select specific invoices from a table and generate a PDF file from it to sum the amounts.

## API
- `/api/products` Lists all products in the database.
  - No parameters.
- `/api/invoice`, `/api/invoice/{type}` Returns the invoice for the selected items.
  - `id` List of Product IDs that are selected.
  - `{type}` Type of Response Type. Available responses:
    - `raw` JSON type, as Spring returns it by default
    - `pdf` Generates a PDF file on disk, and returns an URL to it.
- `/api/access-pdf/{file}` Returns the PDF as stored on the server.
  - `{file}` Name of the file. `.pdf` extension ending is optional 