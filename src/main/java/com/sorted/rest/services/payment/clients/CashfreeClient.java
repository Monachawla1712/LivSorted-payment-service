package com.sorted.rest.services.payment.clients;

import java.util.Map;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import com.sorted.rest.services.payment.beans.CashfreeTokenRequest;
import com.sorted.rest.services.payment.beans.CashfreeTokenResponse;

@FeignClient(value = "cashfreeClient", url = "${client.cashfree.url}")
public interface CashfreeClient {

	@GetMapping(value = "/api/v2/cftoken/order")
	CashfreeTokenResponse getToken(@RequestHeader Map<String, Object> headers, @RequestBody CashfreeTokenRequest request);

}