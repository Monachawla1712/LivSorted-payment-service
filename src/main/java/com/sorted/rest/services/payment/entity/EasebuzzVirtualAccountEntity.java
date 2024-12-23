package com.sorted.rest.services.payment.entity;

import com.sorted.rest.common.websupport.base.BaseEntity;
import com.sorted.rest.services.payment.beans.EasebuzzVAMetadata;
import com.sorted.rest.services.payment.constants.PaymentConstants;
import lombok.Data;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.Type;

import javax.persistence.*;

@Entity
@Table(name = PaymentConstants.EASEBUZZ_VIRTUAL_ACCOUNTS_TABLE_NAME)
@DynamicUpdate
@Data
public class EasebuzzVirtualAccountEntity extends BaseEntity {

	private static final long serialVersionUID = -7538803140039235801L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(updatable = false, nullable = false)
	private Integer id;

	@Column(nullable = false)
	private String virtualAccountId;

	@Column(nullable = false)
	private String entityId;

	@Column(nullable = false)
	private String label;

	@Column(nullable = false)
	private String virtualAccountNumber;

	@Column(nullable = false)
	private String virtualIfscCode;

	@Column(nullable = false)
	private String virtualUpiHandle;

	@Column(nullable = false)
	private String qrCodePng;

	@Column(nullable = false)
	private String qrCodePdf;

	@Column(nullable = false)
	@Enumerated(EnumType.STRING)
	private PaymentConstants.EntityType entityType;

	@Type(type = "jsonb")
	@Column(columnDefinition = "jsonb")
	private EasebuzzVAMetadata metadata = new EasebuzzVAMetadata();

	public static EasebuzzVirtualAccountEntity newInstance() {
		EasebuzzVirtualAccountEntity entity = new EasebuzzVirtualAccountEntity();
		return entity;
	}
}