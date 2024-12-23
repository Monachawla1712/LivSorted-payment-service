package com.sorted.rest.services.payment.utils;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGenerator;

import java.io.Serializable;
import java.util.Base64;
import java.util.UUID;

public class ShortenedUUIDGenerator implements IdentifierGenerator {

	@Override
	public Serializable generate(SharedSessionContractImplementor session, Object object) {
		String shortenedUUID = generateShortUUID();
		return shortenedUUID;
	}

	private String generateShortUUID() {
		UUID uuid = UUID.randomUUID();
		byte[] uuidBytes = new byte[16];
		long mostSigBits = uuid.getMostSignificantBits();
		long leastSigBits = uuid.getLeastSignificantBits();
		for (int i = 0; i < 8; i++) {
			uuidBytes[i] = (byte) (mostSigBits >>> 8 * (7 - i));
			uuidBytes[8 + i] = (byte) (leastSigBits >>> 8 * (7 - i));
		}
		return Base64.getUrlEncoder().withoutPadding().encodeToString(uuidBytes);
	}
}
