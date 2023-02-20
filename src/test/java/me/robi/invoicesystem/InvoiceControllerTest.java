package me.robi.invoicesystem;

import static me.robi.invoicesystem.constants.ResponseConstants.InvoiceResponseConstants.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import me.robi.invoicesystem.constants.ResponseConstants;
import me.robi.invoicesystem.controllers.invoice.InvoiceController;
import me.robi.invoicesystem.entities.ProductEntity;
import me.robi.invoicesystem.repositories.ProductRepository;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.TestInstance;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

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
                        .param("id", "1, 3"))
                .andDo(result -> print())
                .andExpect(status().isOk())
                .andReturn();
        JSONObject object = new JSONObject(response.getResponse().getContentAsString());
        List<ProductEntity> productEntities = Arrays.asList(new ObjectMapper().readValue(object.getJSONArray(PRODUCTS_LIST).toString(), ProductEntity[].class));
        assertEquals(16, object.getInt(PRODUCTS_SUM));
        assertEquals(productEntities.size(), 2);
    }
}
