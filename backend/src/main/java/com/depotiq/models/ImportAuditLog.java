package com.depotiq.models;

import com.depotiq.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "import_audit_logs")
public class ImportAuditLog extends BaseEntity {

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "import_type", nullable = false)
    private String importType;

    @Column(name = "processed_rows", nullable = false)
    private Integer processedRows;

    @Column(name = "created_records", nullable = false)
    private Integer createdRecords;

    @Column(name = "updated_records", nullable = false)
    private Integer updatedRecords;

    @Column(name = "skipped_rows", nullable = false)
    private Integer skippedRows;

    @Column(name = "created_stores", nullable = false)
    private Integer createdStores;

    @Column(name = "created_products", nullable = false)
    private Integer createdProducts;

    @Column(name = "error_summary", columnDefinition = "TEXT")
    private String errorSummary;

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public String getImportType() { return importType; }
    public void setImportType(String importType) { this.importType = importType; }
    public Integer getProcessedRows() { return processedRows; }
    public void setProcessedRows(Integer processedRows) { this.processedRows = processedRows; }
    public Integer getCreatedRecords() { return createdRecords; }
    public void setCreatedRecords(Integer createdRecords) { this.createdRecords = createdRecords; }
    public Integer getUpdatedRecords() { return updatedRecords; }
    public void setUpdatedRecords(Integer updatedRecords) { this.updatedRecords = updatedRecords; }
    public Integer getSkippedRows() { return skippedRows; }
    public void setSkippedRows(Integer skippedRows) { this.skippedRows = skippedRows; }
    public Integer getCreatedStores() { return createdStores; }
    public void setCreatedStores(Integer createdStores) { this.createdStores = createdStores; }
    public Integer getCreatedProducts() { return createdProducts; }
    public void setCreatedProducts(Integer createdProducts) { this.createdProducts = createdProducts; }
    public String getErrorSummary() { return errorSummary; }
    public void setErrorSummary(String errorSummary) { this.errorSummary = errorSummary; }
}
