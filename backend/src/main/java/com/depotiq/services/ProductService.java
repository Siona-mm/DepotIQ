package com.depotiq.services;

import com.depotiq.dtos.product.CreateProductRequest;
import com.depotiq.dtos.product.ProductResponse;
import com.depotiq.dtos.product.UpdateProductRequest;
import com.depotiq.mappers.ProductMapper;
import com.depotiq.models.Product;
import com.depotiq.repositories.ProductRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class ProductService {
    private final ProductRepository productRepository;
    private final ProductMapper productMapper;
    private final BusinessCodeGenerator businessCodeGenerator;

    public ProductService(
            ProductRepository productRepository,
            ProductMapper productMapper,
            BusinessCodeGenerator businessCodeGenerator
    ) {
        this.productRepository = productRepository;
        this.productMapper = productMapper;
        this.businessCodeGenerator = businessCodeGenerator;
    }

    public List<ProductResponse> getAllProducts() {
        return productRepository.findAll()
                .stream()
                .map(productMapper::toResponse)
                .toList();
    }

    public ProductResponse getProductById(Long id) {
        Product product = findProductOrThrow(id);
        return productMapper.toResponse(product);
    }

    public ProductResponse createProduct(CreateProductRequest request) {
        validateExternalSku(request.getExternalSku(), null);
        Product product = productMapper.toEntity(
                request,
                businessCodeGenerator.nextProductCode()
        );
        Product savedProduct = productRepository.save(product);

        return productMapper.toResponse(savedProduct);
    }

    public ProductResponse updateProduct(Long id, UpdateProductRequest request) {
        Product product = findProductOrThrow(id);

        validateExternalSku(request.getExternalSku(), id);
        productMapper.updateEntity(product, request);
        Product savedProduct = productRepository.save(product);

        return productMapper.toResponse(savedProduct);
    }

    public void deleteProduct(Long id) {
        Product product = findProductOrThrow(id);
        productRepository.delete(product);
    }

    private void validateExternalSku(String externalSku, Long currentId) {
        if (externalSku == null || externalSku.isBlank()) return;
        productRepository.findByExternalSkuIgnoreCase(externalSku.trim()).ifPresent(existing -> {
            if (!existing.getId().equals(currentId)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "External SKU is already assigned to " + existing.getProductCode());
            }
        });
    }

    private Product findProductOrThrow(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
    }
}
