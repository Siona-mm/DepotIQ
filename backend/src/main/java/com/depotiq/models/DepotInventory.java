package com.depotiq.models;

import com.depotiq.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "depot_inventory",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_depot_inventory_product",
        columnNames = "product_id"
    )
)
public class DepotInventory extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "available_units", nullable = false)
    private Integer availableUnits = 0;

    @Column(name = "reserved_units", nullable = false)
    private Integer reservedUnits = 0;

    @Column(name = "last_updated", nullable = false)
    private LocalDateTime lastUpdated = LocalDateTime.now();

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public Integer getAvailableUnits() {
        return availableUnits;
    }

    public void setAvailableUnits(Integer availableUnits) {
        this.availableUnits = availableUnits;
    }

    public Integer getReservedUnits() {
        return reservedUnits;
    }

    public void setReservedUnits(Integer reservedUnits) {
        this.reservedUnits = reservedUnits;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
}
