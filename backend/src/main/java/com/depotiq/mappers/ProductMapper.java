package com.depotiq.mappers;

import com.depotiq.dtos.product.CreateProductRequest;
import com.depotiq.dtos.product.ProductResponse;
import com.depotiq.dtos.product.UpdateProductRequest;
import com.depotiq.models.Product;
import org.springframework.stereotype.Component;

@Component
public class ProductMapper {
    public ProductResponse toResponse(Product product) {
        ProductResponse response = new ProductResponse();

        response.setId(product.getId());
        response.setProductCode(product.getProductCode());
        response.setName(product.getName());
        response.setCategory(product.getCategory());
        response.setBrand(product.getBrand());
        response.setSupplierCode(product.getSupplierCode());
        response.setExternalSku(product.getExternalSku());
        response.setUnitCost(product.getUnitCost());
        response.setPrice(product.getPrice());
        response.setWeightKg(product.getWeightKg());
        response.setShelfLifeDays(product.getShelfLifeDays());
        response.setPerishable(product.getPerishable());
        response.setCreatedAt(product.getCreatedAt());
        response.setUpdatedAt(product.getUpdatedAt());

        return response;
    }

    public Product toEntity(CreateProductRequest request, String productCode) {
        Product product = new Product();

        product.setProductCode(productCode);
        product.setName(request.getName());
        product.setCategory(request.getCategory());
        product.setBrand(request.getBrand());
        product.setSupplierCode(request.getSupplierCode());
        product.setExternalSku(normalizeOptional(request.getExternalSku()));
        product.setUnitCost(request.getUnitCost());
        product.setPrice(request.getPrice());
        product.setWeightKg(request.getWeightKg());
        product.setShelfLifeDays(request.getShelfLifeDays());
        product.setPerishable(request.getPerishable());

        return product;
    }

    public void updateEntity(Product product, UpdateProductRequest request) {
        product.setName(request.getName());
        product.setCategory(request.getCategory());
        product.setBrand(request.getBrand());
        product.setSupplierCode(request.getSupplierCode());
        product.setExternalSku(normalizeOptional(request.getExternalSku()));
        product.setUnitCost(request.getUnitCost());
        product.setPrice(request.getPrice());
        product.setWeightKg(request.getWeightKg());
        product.setShelfLifeDays(request.getShelfLifeDays());
        product.setPerishable(request.getPerishable());
    }

    private String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
