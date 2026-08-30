package com.depotiq.controllers;

import com.depotiq.config.SecurityConfig;
import com.depotiq.services.InventoryService;
import com.depotiq.services.ProductService;
import com.depotiq.services.StoreService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {ProductController.class, StoreController.class, InventoryController.class})
@Import(SecurityConfig.class)
@WithMockUser(roles = "ADMIN")
class CatalogValidationControllerTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @MockBean ProductService products;
    @MockBean StoreService stores;
    @MockBean InventoryService inventory;
    @MockBean JpaMetamodelMappingContext jpaMappingContext;

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void productCreateAndEditRejectEveryMissingOrBlankDetail(boolean edit) throws Exception {
        var complete = product();
        for (String field : List.of("name", "category", "brand", "supplierCode", "unitCost", "price", "weightKg", "shelfLifeDays", "perishable")) {
            var missing = complete.deepCopy(); missing.remove(field);
            rejected("products", edit, missing, field);
            var empty = complete.deepCopy(); empty.putNull(field);
            rejected("products", edit, empty, field);
            if (complete.get(field).isTextual()) {
                empty.put(field, "   "); rejected("products", edit, empty, field);
            }
        }
        verifyNoInteractions(products);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void storeCreateAndEditRequireEveryDetail(boolean edit) throws Exception {
        var complete = store();
        for (String field : List.of("name", "region", "storeType", "hasWarehouse", "storageCapacity", "deliveryLeadTimeDays", "preferredHorizonDays")) {
            var missing = complete.deepCopy(); missing.remove(field);
            rejected("stores", edit, missing, field);
            var empty = complete.deepCopy(); empty.putNull(field);
            rejected("stores", edit, empty, field);
        }
        for (String field : List.of("name", "region")) rejected("stores", edit, complete.deepCopy().put(field, "  "), field);
        verifyNoInteractions(stores);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void productRejectsInvalidNumbersAndTextLengths(boolean edit) throws Exception {
        rejected("products", edit, product().put("weightKg", 0), "weightKg");
        rejected("products", edit, product().put("weightKg", 0.0001), "weightKg");
        rejected("products", edit, product().put("shelfLifeDays", -1), "shelfLifeDays");
        rejected("products", edit, product().put("perishable", true).put("shelfLifeDays", 0), "shelfLifeValid");
        for (String field : List.of("unitCost", "price")) {
            rejected("products", edit, product().put(field, -1), field);
            rejected("products", edit, product().put(field, 1.005), field);
            rejected("products", edit, product().put(field, 10000000000L), field);
        }
        for (String field : List.of("name", "category", "brand", "supplierCode", "externalSku")) {
            rejected("products", edit, product().put(field, "x".repeat(field.equals("name") ? 151 : 101)), field);
        }
        verifyNoInteractions(products);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void acceptsCompleteCatalogDetailsIncludingExplicitFalseAndZeroCosts(boolean edit) throws Exception {
        mvc.perform(write("products", edit).content(product().put("perishable", false).put("shelfLifeDays", 0).put("unitCost", 0).put("price", 0).toString()))
                .andExpect(edit ? status().isOk() : status().isCreated());
        mvc.perform(write("stores", edit).content(store().toString()))
                .andExpect(edit ? status().isOk() : status().isCreated());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void externalIdentifiersAreOptionalForManualEntryButStillLengthLimited(boolean edit) throws Exception {
        for (String catalog : List.of("stores", "products")) {
            String field = catalog.equals("stores") ? "externalStoreId" : "externalSku";
            ObjectNode body = catalog.equals("stores") ? store() : product();
            body.remove(field);
            mvc.perform(write(catalog, edit).content(body.toString())).andExpect(edit ? status().isOk() : status().isCreated());
            body.putNull(field);
            mvc.perform(write(catalog, edit).content(body.toString())).andExpect(edit ? status().isOk() : status().isCreated());
            body.put(field, "  ");
            mvc.perform(write(catalog, edit).content(body.toString())).andExpect(edit ? status().isOk() : status().isCreated());
            rejected(catalog, edit, body.put(field, "x".repeat(101)), field);
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void storeRejectsInvalidOperatingValues(boolean edit) throws Exception {
        rejected("stores", edit, store().put("storageCapacity", 0), "storageCapacity");
        rejected("stores", edit, store().put("deliveryLeadTimeDays", 0), "deliveryLeadTimeDays");
        rejected("stores", edit, store().put("preferredHorizonDays", 2), "preferredHorizonSupported");
        verifyNoInteractions(stores);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void jsonDoesNotSilentlyTruncateFractionalDayCountsOrCapacities(boolean edit) throws Exception {
        mvc.perform(write("products", edit).content(product().put("shelfLifeDays", 1.5).toString())).andExpect(status().isBadRequest());
        mvc.perform(write("stores", edit).content(store().put("storageCapacity", 1.5).toString())).andExpect(status().isBadRequest());
        verifyNoInteractions(products, stores);
    }

    @Test
    void inventoryCannotReferToEmptyIdsOrReserveMoreThanAvailable() throws Exception {
        mvc.perform(post("/api/inventory/depot").contentType(MediaType.APPLICATION_JSON)
                .content("{\"productId\":1,\"availableUnits\":5,\"reservedUnits\":6}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.validationErrors.reservationValid").exists());
        mvc.perform(post("/api/inventory/stores").contentType(MediaType.APPLICATION_JSON)
                .content("{\"storeId\":0,\"productId\":0,\"inventoryLevel\":0,\"incomingUnits\":0}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.validationErrors.storeId").exists());
        mvc.perform(post("/api/inventory/depot").contentType(MediaType.APPLICATION_JSON)
                .content("{\"productId\":1,\"availableUnits\":5.5,\"reservedUnits\":0}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(inventory);
    }

    private void rejected(String catalog, boolean edit, ObjectNode payload, String field) throws Exception {
        mvc.perform(write(catalog, edit).content(payload.toString()))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.validationErrors." + field).exists());
    }

    private MockHttpServletRequestBuilder write(String catalog, boolean edit) {
        return (edit ? put("/api/" + catalog + "/1") : post("/api/" + catalog)).contentType(MediaType.APPLICATION_JSON);
    }

    private ObjectNode product() throws Exception {
        return (ObjectNode) json.readTree("""
                {"name":"Rice","category":"Food","brand":"Grain Co","supplierCode":"SUP-1","externalSku":"RICE-5KG",
                 "unitCost":3.25,"price":5.95,"weightKg":5.001,"shelfLifeDays":90,"perishable":true}
                """);
    }

    private ObjectNode store() throws Exception {
        return (ObjectNode) json.readTree("""
                {"name":"North Market","externalStoreId":"POS-NORTH","storeType":"SMALL","region":"North",
                 "hasWarehouse":false,"storageCapacity":1200,"deliveryLeadTimeDays":2,"preferredHorizonDays":7}
                """);
    }
}
