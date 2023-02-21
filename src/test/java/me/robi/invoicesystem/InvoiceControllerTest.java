package me.robi.invoicesystem;

import static me.robi.invoicesystem.constants.ResponseConstants.InvoiceResponseConstants.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itextpdf.text.Document;
import me.robi.invoicesystem.constants.PathConstants;
import me.robi.invoicesystem.controllers.invoice.InvoiceController;
import me.robi.invoicesystem.controllers.invoice.types.PdfInvoiceType;
import me.robi.invoicesystem.entities.ProductEntity;
import me.robi.invoicesystem.repositories.ProductRepository;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.TestInstance;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@WebMvcTest(InvoiceController.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class InvoiceControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductRepository repository;

    @Test
    public void testProductsList() throws Exception {
        when(repository.findAll()).thenReturn(Arrays.asList(
                        new ProductEntity("TestItem 1", 7),
                        new ProductEntity("TestItem 2", 14)
                )
        );

        MvcResult response = mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andReturn();

        List<ProductEntity> productEntities = Arrays.asList(new ObjectMapper().readValue(response.getResponse().getContentAsString(), ProductEntity[].class));
        assertEquals(productEntities.size(), 2);
        assertEquals(productEntities.get(0).getName(), "TestItem 1");
    }

    @Test
    public void testRawResponse() throws Exception {
        List<ProductEntity> list = Arrays.asList(
                new ProductEntity("TestItem 1", 7),
                new ProductEntity("TestItem 2", 14),
                new ProductEntity("TestItem 3", 9),
                new ProductEntity("TestItem 4", 11)
        );
        when(repository.findAll()).thenReturn(list);
        when(repository.findById(anyLong())).thenAnswer(invocationOnMock -> Optional.of(list.get((int) (invocationOnMock.getArgument(0, Long.class) - 1))));

        MvcResult response = mockMvc.perform(get("/api/invoice")
                        .param("id", "1,3"))
                .andDo(result -> print())
                .andExpect(status().isOk())
                .andReturn();
        JSONObject object = new JSONObject(response.getResponse().getContentAsString());
        List<ProductEntity> productEntities = Arrays.asList(new ObjectMapper().readValue(object.getJSONArray(PRODUCTS_LIST).toString(), ProductEntity[].class));
        assertEquals(16, object.getInt(PRODUCTS_SUM));
        assertEquals(productEntities.size(), 2);
    }

    @Test
    public void testPdfResponse() throws Exception {
        List<ProductEntity> list = Arrays.asList(
                new ProductEntity("TestItem 1", 7),
                new ProductEntity("TestItem 2", 14),
                new ProductEntity("TestItem 3", 9),
                new ProductEntity("TestItem 4", 11)
        );
        int sum = 16;
        when(repository.findAll()).thenReturn(list);
        when(repository.findById(anyLong())).thenAnswer(invocationOnMock -> Optional.of(list.get((int) (invocationOnMock.getArgument(0, Long.class) - 1))));
        PdfInvoiceType pdfInvoiceType = new PdfInvoiceType();
        File f = Paths.get(PathConstants.TEST_PDF_FILE_STORAGE, "test.pdf").toFile();
        ResponseEntity response = pdfInvoiceType.getResponse(new MockHttpServletRequest(), Arrays.asList(list.get(0), list.get(2)), sum, "test.pdf");
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }
}
