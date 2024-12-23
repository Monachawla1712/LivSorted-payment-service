package com.sorted.rest.services.payment.beans;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class TargetCBWalletEligibleRequest implements Serializable {

    private static final long serialVersionUID = -7538803140039235801L;

    private String walletEligibilityDate;

    private List<String> storeIds;

}