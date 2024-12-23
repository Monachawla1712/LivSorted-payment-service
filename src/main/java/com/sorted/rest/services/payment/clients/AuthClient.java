package com.sorted.rest.services.payment.clients;

import com.sorted.rest.common.openfeign.FeignCustomConfiguration;
import com.sorted.rest.services.payment.beans.UserDetail;
import com.sorted.rest.services.payment.beans.UserDetailsResponse;
import com.sorted.rest.services.payment.beans.UserIdsRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@FeignClient(value = "authConsumer", url = "${client.auth.url}", configuration = {FeignCustomConfiguration.class})
public interface AuthClient {

    @GetMapping(value = "/auth/internal/user/{id}")
    UserDetailsResponse getUserDetails(@PathVariable String id);

    @PostMapping(value = "auth/internal/users")
    List<UserDetailsResponse> getUserDetailsByIds(@RequestBody UserIdsRequest request);

    @PostMapping(value = "auth/internal/user/{id}")
    public void updateUserPreference(@RequestParam("paymentMethod") String paymentMethod, @PathVariable String id);

}