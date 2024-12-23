package com.sorted.rest.services.payment.entity;

import com.sorted.rest.common.websupport.base.BaseEntity;
import com.sorted.rest.services.payment.constants.PaymentConstants;
import lombok.Data;
import org.hibernate.annotations.DynamicUpdate;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = PaymentConstants.CC_OTP_TABLE_NAME)
@DynamicUpdate
@Data
public class CashCollectionOtpEntity extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(nullable = false)
	private Long id;

	@Column()
	private String phoneNumber;

	@Column()
	private String storeId;

	@Column()
	private String otp;

	@Column()
	private Date expiry;

	@Column()
	private Integer attempts=0;

	@Column()
	private Integer verified=0;

	@Column()
	private Double amount;

	public static CashCollectionOtpEntity newInstance() {
		return new CashCollectionOtpEntity();
	}

	public static CashCollectionOtpEntity buildCashCollectionOtpEntity(String phoneNumber, String storeId, String otp, Date otpExpiry, Double amount) {
		CashCollectionOtpEntity ccOtp = newInstance();
		ccOtp.setOtp(otp);
		ccOtp.setStoreId(storeId);
		ccOtp.setPhoneNumber(phoneNumber);
		ccOtp.setExpiry(otpExpiry);
		return ccOtp;
	}
}
