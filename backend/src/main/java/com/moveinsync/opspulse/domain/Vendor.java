package com.moveinsync.opspulse.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "vendors")
public class Vendor {

    @Id
    private String id;

    private String name;

    @Column(name = "sla_ota_pct")
    private BigDecimal slaOtaPct;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getSlaOtaPct() {
        return slaOtaPct;
    }

    public void setSlaOtaPct(BigDecimal slaOtaPct) {
        this.slaOtaPct = slaOtaPct;
    }
}
