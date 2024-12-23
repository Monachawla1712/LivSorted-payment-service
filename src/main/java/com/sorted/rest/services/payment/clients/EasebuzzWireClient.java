package com.sorted.rest.services.payment.clients;

import com.sorted.rest.services.payment.beans.CreateEasebuzzVARequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.Map;

@FeignClient(value = "easebuzzWireClient", url = "${client.easebuzz.wire-url}")
public interface EasebuzzWireClient {

	@PostMapping(value = "/api/v1/insta-collect/virtual_accounts/")
	Object createEasebuzzVA(@RequestHeader Map<String, Object> headers, @RequestBody CreateEasebuzzVARequest request);
}