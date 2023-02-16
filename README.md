# Invoice System
Simple Invoice System backend.

Users can select specific invoices from a table and generate a PDF file from it to sum the amounts.

## API
- `/api/products` Lists all products in the database.
  - No parameters.
- `/api/invoice`, `/api/invoice/json` Returns the invoice for the selected items.
  - `id` List of Product IDs that are selected.
- `/api/invoice/pdf` Returns the invoice for the selected items as a PDF file.
  - `id` List of Product IDs that are selected.