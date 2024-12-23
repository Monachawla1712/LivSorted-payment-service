package com.sorted.rest.services.payment.constants;

public class PaymentConstants {

	public enum PaymentGateway {

		RAZORPAY, CASHFREE, LITHOS, EASEBUZZ, JUSPAY;
	}

	public enum PaymentStatus {

		PENDING(0), IN_PROGRESS(1), SUCCESS(2), FAILED(3);

		private int value;

		private PaymentStatus(int value) {
			this.value = value;
		}

		public int getValue() {
			return value;
		}
	}

	public enum PaymentNotifyStatus {

		PENDING, INCOMPLETE, FAILED, FLAGGED, USER_DROPPED, SUCCESS, CANCELLED, VOID;
	}

	public enum OrderStatus {

		IN_CART(0), NEW_ORDER(1), ORDER_ACCEPTED_BY_STORE(2), ORDER_CANCELLED_BY_STORE(3), ORDER_BILLED(4), ORDER_CANCELLED_BY_CUSTOMER(5), READY_FOR_PICKUP(
				6), ORDER_OUT_FOR_DELIVERY(7), ORDER_DELIVERED(8), ORDER_REFUNDED(9);

		private int value;

		private OrderStatus(int value) {
			this.value = value;
		}

		public int getValue() {
			return value;
		}
	}

	public enum EntityType {
		USER, STORE, AM;
	}

	public enum WalletTxnMode {
		DEBIT, CREDIT;
	}

	public enum WalletType {
		WALLET, COINS, HOLD;

		public static WalletType fromString(String status) {
			for (WalletType value : WalletType.values()) {
				if (value.toString().equals(status)) {
					return value;
				}
			}
			return null;
		}
	}

	public enum CashCollectionStatus {
		REQUESTED, COLLECTED, CANCELLED, RECEIVED, APPROVED, REJECTED, UNCOLLECTED, REVERSED ;

		public static CashCollectionStatus fromString(String status) {
			for (CashCollectionStatus value : CashCollectionStatus.values()) {
				if (value.toString().equals(status)) {
					return value;
				}
			}
			return null;
		}
	}

	public enum PaymentRequestStatus {
		REQUESTED, APPROVED, REJECTED;

		public static PaymentRequestStatus fromString(String status) {
			for (PaymentRequestStatus value : PaymentRequestStatus.values()) {
				if (value.toString().equals(status)) {
					return value;
				}
			}
			return null;
		}
	}

	public enum StoreType {
		FRANCHISE, DARK, OFFLINE, SUPPLY;
	}

	public enum EasebuzzPaymentStatus {
		preinitiated(PaymentStatus.PENDING), initiated(PaymentStatus.PENDING), pending(PaymentStatus.IN_PROGRESS), success(PaymentStatus.SUCCESS), failure(
				PaymentStatus.FAILED), usercancelled(PaymentStatus.FAILED), dropped(PaymentStatus.FAILED), bounced(PaymentStatus.FAILED);

		private PaymentStatus value;

		private EasebuzzPaymentStatus(PaymentStatus value) {
			this.value = value;
		}

		public PaymentStatus getValue() {
			return value;
		}

		public static EasebuzzPaymentStatus fromString(String status) {
			for (EasebuzzPaymentStatus value : EasebuzzPaymentStatus.values()) {
				if (value.toString().equals(status)) {
					return value;
				}
			}
			return EasebuzzPaymentStatus.failure;
		}
	}

	public enum PaymentMode {
		PG, QR, JUSPAY;
	}

	public enum JuspayOrderStatus {
		NEW(PaymentStatus.FAILED), PENDING_VBV(PaymentStatus.IN_PROGRESS), AUTHORIZING(PaymentStatus.IN_PROGRESS), CHARGED(
				PaymentStatus.SUCCESS), AUTHENTICATION_FAILED(PaymentStatus.FAILED), AUTHORIZATION_FAILED(PaymentStatus.FAILED), JUSPAY_DECLINED(
				PaymentStatus.FAILED), STARTED(PaymentStatus.FAILED), FAILED(PaymentStatus.FAILED);

		private PaymentStatus value;

		private JuspayOrderStatus(PaymentStatus value) {
			this.value = value;
		}

		public PaymentStatus getValue() {
			return value;
		}

		public static JuspayOrderStatus fromString(String status) {
			for (JuspayOrderStatus value : JuspayOrderStatus.values()) {
				if (value.toString().equals(status)) {
					return value;
				}
			}
			return JuspayOrderStatus.FAILED;
		}
	}

	public enum JuspayOrderEvent {
		ORDER_SUCCEEDED(PaymentStatus.SUCCESS), ORDER_FAILED(PaymentStatus.FAILED);

		private PaymentStatus value;

		private JuspayOrderEvent(PaymentStatus value) {
			this.value = value;
		}

		public PaymentStatus getValue() {
			return value;
		}

		public static JuspayOrderEvent fromString(String status) {
			for (JuspayOrderEvent value : JuspayOrderEvent.values()) {
				if (value.toString().equals(status)) {
					return value;
				}
			}
			return null;
		}
	}

	public enum WalletStatus {

		INACTIVE(0), ACTIVE(1);

		private int value;

		private WalletStatus(int value) {
			this.value = value;
		}

		public int getValue() {
			return value;
		}
	}

	public enum AppType {
		CONSUMER("com.sorted.consumerflutterapp"), PARTNER("com.sorted.partnerflutterapp"), FOS("com.sorted.fos");

		private String value;

		private AppType(String value) {
			this.value = value;
		}

		public String getValue() {
			return value;
		}

		public static AppType fromString(String appType) {
			for (AppType value : AppType.values()) {
				if (value.toString().equals(appType)) {
					return value;
				}
			}
			return null;
		}
	}

	public enum CashCollectionType {
		DUE_COLLECTION,
		ADVANCE_REQUEST
	}

	public static final String TRANSACTIONS_TABLE_NAME = "transactions";

	public static final String USER_WALLET_TABLE_NAME = "user_wallet";

	public static final String CREDIT_LIMIT_CHANGE_TABLE_NAME = "credit_limit_change";

	public static final String CC_OTP_TABLE_NAME = "cc_otp";

	public static final String WALLET_STATEMENT_TABLE_NAME = "wallet_statement";

	public static final String WALLET_HOLD_TABLE_NAME = "wallet_hold";

	public static final String CASH_COLLECTIONS_TABLE_NAME = "cash_collections";

	public static final String PAYMENT_REQUESTS_TABLE_NAME = "payment_requests";

	public static final String EASEBUZZ_VIRTUAL_ACCOUNTS_TABLE_NAME = "easebuzz_virtual_accounts";

	public static final String FO_TXN_TYPE = "Franchise-Order";

	public static final String PAYMENT_CASH_TXN_TYPE = "Payment-CASH";

	public static final String AM_PAYMENT_REMARKS = "Approved by payment via FOS app";

	public static final String FOS_USER_ROLE = "FOSUSER";

	public static final String PARTNER_APP_USER_ROLE = "FRANCHISEOWNER";

	public static final String PAYMENT_PG_KEY = "PAYMENT_PG|";

	public static final String PAYMENT_QR_KEY = "PAYMENT_QR|";

	public static final String PAYMENT_CC_KEY = "PAYMENT_CC|";

	public static final String PAYMENT_PR_KEY = "PAYMENT_PR|";

	public static final String WS_KEY_CONSTRAINT = "wallet_statement_key_index";

	public static final String PSQL_CONSTRAINT_VIOLATION_SQL_STATE = "23505";

	public static final String PAYMENT_REVERSAL ="Payment has been reversed by Executive";

	public static final String CASH = "Cash";

	public static final String UPI = "Digital/UPI";

	public static final String CO_TXN_TYPE = "Consumer-Order";
}
