package com.sorted.rest.services.payment.utils;

import com.sorted.rest.services.payment.beans.UserDetail;
import com.sorted.rest.services.payment.beans.UserDetailsResponse;
import com.sorted.rest.services.payment.clients.ClientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class UserUtils {

	@Autowired
	ClientService clientService;

	public UserDetail getUserDetail(UUID userId) {
		UserDetailsResponse entity = clientService.getUserDetails(userId);
		if (entity == null) {
			return null;
		}
		UserDetail userDetail = UserDetail.newInstance();
		userDetail.setName(entity.getName());
		userDetail.setPhone(entity.getPhoneNumber());
		userDetail.setEmail(entity.getEmail());
		userDetail.setId(entity.getId());
		return userDetail;
	}
}
