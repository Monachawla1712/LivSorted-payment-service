package com.sorted.rest.services.payment.entity;

import com.sorted.rest.common.websupport.base.BaseEntity;
import com.sorted.rest.services.payment.constants.PaymentConstants;
import com.sorted.rest.services.payment.constants.PaymentConstants.EntityType;
import lombok.Data;
import org.hibernate.annotations.DynamicUpdate;

import javax.persistence.*;

@Entity
@Table(name = PaymentConstants.WALLET_STATEMENT_TABLE_NAME)
@DynamicUpdate
@Data
public class WalletStatementEntity extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(updatable = false, nullable = false)
	private Integer id;

	@Column(nullable = false)
	private String entityId;

	@Column(nullable = false)
	@Enumerated(EnumType.STRING)
	private EntityType entityType;

	@Column(nullable = false)
	private Double amount;

	@Column(nullable = false)
	private Double balance;

	@Column(nullable = false)
	private String txnMode;

	@Column(nullable = false)
	private String txnType;

	@Column(nullable = false)
	private String txnDetail;

	@Column(nullable = false)
	private String walletType;

	@Column(nullable = true)
	private String remarks;

	@Column(nullable = true)
	private String key;
}
