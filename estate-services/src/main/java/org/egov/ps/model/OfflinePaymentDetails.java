package org.egov.ps.model;

import java.math.BigDecimal;

import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@EqualsAndHashCode
public class OfflinePaymentDetails {

	@Size(max = 256)
	@JsonProperty("id")
	private String id;

	@Size(max = 256)
	@JsonProperty("propertyDetailsId")
	private String propertyDetailsId;

	@Size(max = 256)
	@JsonProperty("demandId")
	private String demandId;

	@JsonProperty("amount")
	private BigDecimal amount;

	@Size(max = 100)
	@JsonProperty("bankName")
	private String bankName;

	@Size(max = 100)
	@JsonProperty("transactionNumber")
	private String transactionNumber;
	
	@JsonProperty("dateOfPayment")
	private Long dateOfPayment;
	
}
