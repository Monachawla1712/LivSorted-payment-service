package com.sorted.rest.services.payment.entity;

import com.sorted.rest.common.utils.ParamsUtils;
import com.sorted.rest.common.websupport.base.BaseEntity;
import com.sorted.rest.services.payment.beans.UserWalletMetadata;
import com.sorted.rest.services.payment.constants.PaymentConstants;
import com.sorted.rest.services.payment.constants.PaymentConstants.EntityType;
import com.sorted.rest.services.payment.constants.PaymentConstants.WalletStatus;
import lombok.Data;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.util.UUID;

@Entity
@Table(name = PaymentConstants.USER_WALLET_TABLE_NAME)
@DynamicUpdate
@Data
public class UserWalletEntity extends BaseEntity {

	@Id
	@Column(updatable = false, nullable = false)
	@GeneratedValue(generator = "UUID")
	@GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
	private UUID id;

	@Column(nullable = false)
	private String entityId;

	@Column(nullable = false)
	private Double amount;

	@Column(nullable = false)
	@Enumerated(EnumType.STRING)
	private EntityType entityType;

	@Column(nullable = false)
	private Double loyaltyCoins;

	@Column(nullable = false)
	private Double walletHold;

	@Column(nullable = false)
	private Double creditLimit;

	@Column(nullable = false)
	private WalletStatus status = WalletStatus.ACTIVE;

	@Type(type = "jsonb")
	@Column(columnDefinition = "jsonb")
	private UserWalletMetadata metadata = UserWalletMetadata.newInstance();

	public static UserWalletEntity newInstance() {
		return new UserWalletEntity();
	}

	private static double setCreditLimitStore() {
		final String STORE_DEFAULT_CREDIT_LIMIT = "STORE_DEFAULT_CREDIT_LIMIT";
		return Double.parseDouble(ParamsUtils.getParam(STORE_DEFAULT_CREDIT_LIMIT, "3000"));
	}

	private static double setCreditLimitUser() {
		final String USER_DEFAULT_CREDIT_LIMIT = "USER_DEFAULT_CREDIT_LIMIT";
		return Double.parseDouble(ParamsUtils.getParam(USER_DEFAULT_CREDIT_LIMIT, "0"));
	}

	public static UserWalletEntity buildUserWalletEntity(String entityId, EntityType entityType) {
		UserWalletEntity userWallet = newInstance();
		userWallet.setEntityId(entityId);
		userWallet.setEntityType(entityType);
		userWallet.setAmount(0.0);
		userWallet.setLoyaltyCoins(0.0);
		userWallet.setWalletHold(0.0);
		if (entityType.equals(EntityType.STORE)) {
			userWallet.setCreditLimit(setCreditLimitStore());
		} else {
			userWallet.setCreditLimit(setCreditLimitUser());
		}
		return userWallet;
	}
}
