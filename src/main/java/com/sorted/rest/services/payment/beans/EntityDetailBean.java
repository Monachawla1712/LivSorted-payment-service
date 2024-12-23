package com.sorted.rest.services.payment.beans;

import com.sorted.rest.services.payment.constants.PaymentConstants.EntityType;
import lombok.Data;

import java.io.Serializable;

@Data
public class EntityDetailBean implements Serializable {
    private String entityId;
    private EntityType entityType;
}
