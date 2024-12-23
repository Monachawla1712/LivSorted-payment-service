package com.sorted.rest.services.payment.clients;

import com.sorted.rest.common.openfeign.FeignCustomConfiguration;
import com.sorted.rest.services.payment.beans.TargetCBWalletEligibleRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(value = "offer", url = "${client.offer.url}", configuration = { FeignCustomConfiguration.class })
public interface OfferClient {

	@PostMapping(value = "/offers/targets/cashback/wallet-eligible")
	void markStoreEligibleForTargetCashback(@RequestBody TargetCBWalletEligibleRequest request);

}