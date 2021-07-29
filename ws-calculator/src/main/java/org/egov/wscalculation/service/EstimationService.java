package org.egov.wscalculation.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.egov.common.contract.request.RequestInfo;
import org.egov.tracer.model.CustomException;
import org.egov.wscalculation.constants.WSCalculationConstant;
import org.egov.wscalculation.model.BillingSlab;
import org.egov.wscalculation.model.CalculationCriteria;
import org.egov.wscalculation.model.Property;
import org.egov.wscalculation.model.RequestInfoWrapper;
import org.egov.wscalculation.model.SearchCriteria;
import org.egov.wscalculation.model.Slab;
import org.egov.wscalculation.model.TaxHeadEstimate;
import org.egov.wscalculation.model.WaterConnection;
import org.egov.wscalculation.model.WaterConnectionRequest;
import org.egov.wscalculation.util.CalculatorUtil;
import org.egov.wscalculation.util.WSCalculationUtil;
import org.egov.wscalculation.util.WaterCessUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;

import lombok.extern.slf4j.Slf4j;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;

@Service
@Slf4j
public class EstimationService {

	@Autowired
	private WaterCessUtil waterCessUtil;

	@Autowired
	private CalculatorUtil calculatorUtil;

	@Autowired
	private ObjectMapper mapper;

	@Autowired
	private WSCalculationUtil wSCalculationUtil;

	/**
	 * Generates a List of Tax head estimates with tax head code, tax head category
	 * and the amount to be collected for the key.
	 *
	 * @param criteria    criteria based on which calculation will be done.
	 * @param requestInfo request info from incoming request.
	 * @return Map<String, Double>
	 */
	@SuppressWarnings("rawtypes")
	public Map<String, List> getEstimationMap(CalculationCriteria criteria, RequestInfo requestInfo,
			Map<String, Object> masterData) {
		BigDecimal taxAmt = BigDecimal.ZERO;
		String tenantId = requestInfo.getUserInfo().getTenantId();
		if (criteria.getWaterConnection() == null && !StringUtils.isEmpty(criteria.getConnectionNo())) {
			criteria.setWaterConnection(
					calculatorUtil.getWaterConnection(requestInfo, criteria.getConnectionNo(), tenantId));
		}
		if (criteria.getWaterConnection() == null || StringUtils.isEmpty(criteria.getConnectionNo())) {
			StringBuilder builder = new StringBuilder();
			builder.append("Water Connection are not present for ")
					.append(StringUtils.isEmpty(criteria.getConnectionNo()) ? "" : criteria.getConnectionNo())
					.append(" connection no");
			throw new CustomException("Water Connection not found for given criteria ", builder.toString());
		}
		Map<String, JSONArray> billingSlabMaster = new HashMap<>();
		Map<String, JSONArray> timeBasedExemptionMasterMap = new HashMap<>();
		ArrayList<String> billingSlabIds = new ArrayList<>();
		billingSlabMaster.put(WSCalculationConstant.WC_BILLING_SLAB_MASTER,
				(JSONArray) masterData.get(WSCalculationConstant.WC_BILLING_SLAB_MASTER));
		billingSlabMaster.put(WSCalculationConstant.CALCULATION_ATTRIBUTE_CONST,
				(JSONArray) masterData.get(WSCalculationConstant.CALCULATION_ATTRIBUTE_CONST));
		timeBasedExemptionMasterMap.put(WSCalculationConstant.WC_WATER_CESS_MASTER,
				(JSONArray) (masterData.getOrDefault(WSCalculationConstant.WC_WATER_CESS_MASTER, null)));
		// mDataService.setWaterConnectionMasterValues(requestInfo, tenantId,
		// billingSlabMaster,
		// timeBasedExemptionMasterMap);

		taxAmt = getWaterEstimationCharge(criteria.getWaterConnection(), criteria, billingSlabMaster, billingSlabIds,
				requestInfo);
		List<TaxHeadEstimate> taxHeadEstimates = getEstimatesForTax(taxAmt, criteria.getWaterConnection(),
				timeBasedExemptionMasterMap, RequestInfoWrapper.builder().requestInfo(requestInfo).build());

		Map<String, List> estimatesAndBillingSlabs = new HashMap<>();
		estimatesAndBillingSlabs.put("estimates", taxHeadEstimates);
		// Billing slab id
		estimatesAndBillingSlabs.put("billingSlabIds", billingSlabIds);
		return estimatesAndBillingSlabs;
	}

	/**
	 * 
	 * @param waterCharge
	 * @param connection
	 * @param timeBasedExemeptionMasterMap
	 * @param requestInfoWrapper
	 * @return
	 */
	private List<TaxHeadEstimate> getEstimatesForTax(BigDecimal waterCharge, WaterConnection connection,
			Map<String, JSONArray> timeBasedExemeptionMasterMap, RequestInfoWrapper requestInfoWrapper) {
		List<TaxHeadEstimate> estimates = new ArrayList<>();
		// water_charge
		estimates.add(TaxHeadEstimate.builder().taxHeadCode(WSCalculationConstant.WS_CHARGE)
				.estimateAmount(waterCharge.setScale(2, 2)).build());

		// Water_cess
		if (timeBasedExemeptionMasterMap.get(WSCalculationConstant.WC_WATER_CESS_MASTER) != null) {
			List<Object> waterCessMasterList = timeBasedExemeptionMasterMap
					.get(WSCalculationConstant.WC_WATER_CESS_MASTER);
			BigDecimal waterCess;
			waterCess = waterCessUtil.getWaterCess(waterCharge, WSCalculationConstant.Assesment_Year,
					waterCessMasterList);
			estimates.add(TaxHeadEstimate.builder().taxHeadCode(WSCalculationConstant.WS_WATER_CESS)
					.estimateAmount(waterCess.setScale(2, 2)).build());
		}
//		 get applicable rebate and penalty
//		Map<String, BigDecimal> rebatePenaltyMap = payService.applyPenaltyRebateAndInterest(payableTax, BigDecimal.ZERO,
//				assessmentYear, timeBasedExemeptionMasterMap);
//		if (null != rebatePenaltyMap) {
//			BigDecimal rebate = rebatePenaltyMap.get(WSCalculationConstant.WS_TIME_REBATE);
//			BigDecimal penalty = rebatePenaltyMap.get(WSCalculationConstant.WS_TIME_PENALTY);
//			BigDecimal interest = rebatePenaltyMap.get(WSCalculationConstant.WS_TIME_INTEREST);
//			estimates.add(TaxHeadEstimate.builder().taxHeadCode(WSCalculationConstant.WS_TIME_REBATE)
//					.estimateAmount(rebate).build());
//			estimates.add(TaxHeadEstimate.builder().taxHeadCode(WSCalculationConstant.WS_TIME_PENALTY)
//					.estimateAmount(penalty).build());
//			estimates.add(TaxHeadEstimate.builder().taxHeadCode(WSCalculationConstant.WS_TIME_INTEREST)
//					.estimateAmount(interest).build());
//			payableTax = payableTax.add(rebate).add(penalty).add(interest);
//		}
		return estimates;
	}

	/**
	 * method to do a first level filtering on the slabs based on the values present
	 * in the Water Details
	 */

	public BigDecimal getWaterEstimationCharge(WaterConnection waterConnection, CalculationCriteria criteria,
			Map<String, JSONArray> billingSlabMaster, ArrayList<String> billingSlabIds, RequestInfo requestInfo) {
		BigDecimal waterCharge = BigDecimal.ZERO;
		if (billingSlabMaster.get(WSCalculationConstant.WC_BILLING_SLAB_MASTER) == null)
			throw new CustomException("No Billing Slab are found on criteria ", "Billing Slab are Empty");
		ObjectMapper mapper = new ObjectMapper();
		List<BillingSlab> mappingBillingSlab;
		try {
			mappingBillingSlab = mapper.readValue(
					billingSlabMaster.get(WSCalculationConstant.WC_BILLING_SLAB_MASTER).toJSONString(),
					mapper.getTypeFactory().constructCollectionType(List.class, BillingSlab.class));
		} catch (IOException e) {
			throw new CustomException("Parsing Exception", " Billing Slab can not be parsed!");
		}
		JSONObject calculationAttributeMaster = new JSONObject();
		calculationAttributeMaster.put(WSCalculationConstant.CALCULATION_ATTRIBUTE_CONST,
				billingSlabMaster.get(WSCalculationConstant.CALCULATION_ATTRIBUTE_CONST));
		String calculationAttribute = getCalculationAttribute(calculationAttributeMaster,
				waterConnection.getConnectionType());
		List<BillingSlab> billingSlabs = getSlabsFiltered(waterConnection, mappingBillingSlab, calculationAttribute,
				requestInfo);
		if (billingSlabs == null || billingSlabs.isEmpty())
			throw new CustomException("No Billing Slab are found on criteria ", "Billing Slab are Empty");
		if (billingSlabs.size() > 1)
			throw new CustomException("More than one Billing Slab are found on criteria ",
					"More than one billing slab found");
		billingSlabIds.add(billingSlabs.get(0).getId());
		log.info(" Billing Slab Id For Water Charge Calculation --->  " + billingSlabIds.toString());

		// WaterCharge Calculation
		Double totalUOM = 0.0;
		totalUOM = getUnitOfMeasurement(waterConnection, calculationAttribute, criteria);
		if (totalUOM == 0.0)
			return waterCharge;
		BillingSlab billSlab = billingSlabs.get(0);
		// IF calculation type is flat then take flat rate else take slab and calculate
		// the charge
		// For metered connection calculation on graded fee slab
		// For Non metered connection calculation on normal connection
		if (isRangeCalculation(calculationAttribute)) {
			if (waterConnection.getConnectionType().equalsIgnoreCase(WSCalculationConstant.meteredConnectionType)) {
				for (Slab slab : billSlab.getSlabs()) {
					if (totalUOM > slab.getTo()) {
						waterCharge = waterCharge
								.add(BigDecimal.valueOf(((slab.getTo()) - (slab.getFrom())) * slab.getCharge()));
						totalUOM = totalUOM - ((slab.getTo()) - (slab.getFrom()));
					} else if (totalUOM < slab.getTo()) {
						waterCharge = waterCharge.add(BigDecimal.valueOf(totalUOM * slab.getCharge()));
						totalUOM = ((slab.getTo()) - (slab.getFrom())) - totalUOM;
						break;
					}
				}
				if (billSlab.getMinimumCharge() > waterCharge.doubleValue()) {
					waterCharge = BigDecimal.valueOf(billSlab.getMinimumCharge());
				}
			} else if (waterConnection.getConnectionType()
					.equalsIgnoreCase(WSCalculationConstant.nonMeterdConnection)) {
				for (Slab slab : billSlab.getSlabs()) {
					if (totalUOM >= slab.getFrom() && totalUOM < slab.getTo()) {
						waterCharge = BigDecimal.valueOf((totalUOM * slab.getCharge()));
						if (billSlab.getMinimumCharge() > waterCharge.doubleValue()) {
							waterCharge = BigDecimal.valueOf(billSlab.getMinimumCharge());
						}
						break;
					}
				}
			}
		} else {
			waterCharge = BigDecimal.valueOf(billSlab.getMinimumCharge());
		}
		return waterCharge;
	}

	private List<BillingSlab> getSlabsFiltered(WaterConnection waterConnection, List<BillingSlab> billingSlabs,
			String calculationAttribue, RequestInfo requestInfo) {

		Property property = wSCalculationUtil.getProperty(
				WaterConnectionRequest.builder().waterConnection(waterConnection).requestInfo(requestInfo).build());
		// get billing Slab
		log.debug(" the slabs count : " + billingSlabs.size());
		final String buildingType = (property.getUsageCategory() != null) ? property.getUsageCategory().split("\\.")[0]
				: "";
		// final String buildingType = "Domestic";
		final String connectionType = waterConnection.getConnectionType();
		final String calculationAttribute = calculationAttribue;

		return billingSlabs.stream().filter(slab -> {
			boolean isBuildingTypeMatching = slab.getBuildingType().equalsIgnoreCase(buildingType);
			boolean isConnectionTypeMatching = slab.getConnectionType().equalsIgnoreCase(connectionType);
			boolean isCalculationAttributeMatching = slab.getCalculationAttribute()
					.equalsIgnoreCase(calculationAttribute);
			return isBuildingTypeMatching && isConnectionTypeMatching && isCalculationAttributeMatching;
		}).collect(Collectors.toList());
	}

	private String getCalculationAttribute(Map<String, Object> calculationAttributeMap, String connectionType) {
		if (calculationAttributeMap == null)
			throw new CustomException("CALCULATION_ATTRIBUTE_MASTER_NOT_FOUND",
					"Calculation attribute master not found!!");
		JSONArray filteredMasters = JsonPath.read(calculationAttributeMap,
				"$.CalculationAttribute[?(@.name=='" + connectionType + "')]");
		JSONObject master = mapper.convertValue(filteredMasters.get(0), JSONObject.class);
		return master.getAsString(WSCalculationConstant.ATTRIBUTE);
	}

	/**
	 * 
	 * @param type will be calculation Attribute
	 * @return true if calculation Attribute is not Flat else false
	 */
	private boolean isRangeCalculation(String type) {
		if (type.equalsIgnoreCase(WSCalculationConstant.flatRateCalculationAttribute))
			return false;
		return true;
	}

	public String getAssessmentYear() {
		LocalDateTime localDateTime = LocalDateTime.now();
		int currentMonth = localDateTime.getMonthValue();
		String assesmentYear = "";
		if (currentMonth >= Month.APRIL.getValue()) {
			assesmentYear = Integer.toString(YearMonth.now().getYear()) + "-";
			assesmentYear = assesmentYear
					+ (Integer.toString(YearMonth.now().getYear() + 1).substring(2, assesmentYear.length() - 1));
		} else {
			assesmentYear = Integer.toString(YearMonth.now().getYear() - 1) + "-";
			assesmentYear = assesmentYear
					+ (Integer.toString(YearMonth.now().getYear()).substring(2, assesmentYear.length() - 1));

		}
		return assesmentYear;
	}

	private Double getUnitOfMeasurement(WaterConnection waterConnection, String calculationAttribute,
			CalculationCriteria criteria) {
		Double totalUnit = 0.0;
		if (waterConnection.getConnectionType().equals(WSCalculationConstant.meteredConnectionType)) {
			totalUnit = (criteria.getCurrentReading() - criteria.getLastReading());
			return totalUnit;
		} else if (waterConnection.getConnectionType().equals(WSCalculationConstant.nonMeterdConnection)
				&& calculationAttribute.equalsIgnoreCase(WSCalculationConstant.noOfTapsConst)) {
			if (waterConnection.getNoOfTaps() == null)
				return totalUnit;
			return totalUnit = new Double(waterConnection.getNoOfTaps());
		} else if (waterConnection.getConnectionType().equals(WSCalculationConstant.nonMeterdConnection)
				&& calculationAttribute.equalsIgnoreCase(WSCalculationConstant.pipeSizeConst)) {
			if (waterConnection.getPipeSize() == null)
				return totalUnit;
			// return totalUnit = Double.parseDouble(waterConnection.getPipeSize());
		}
		return 0.0;
	}

	public Map<String, Object> getQuaterStartAndEndDate(Map<String, Object> billingPeriod) {
		Date date = new Date();
		Calendar fromDateCalendar = Calendar.getInstance();
		fromDateCalendar.setTime(date);
		fromDateCalendar.set(Calendar.MONTH, fromDateCalendar.get(Calendar.MONTH) / 3 * 3);
		fromDateCalendar.set(Calendar.DAY_OF_MONTH, 1);
		setTimeToBeginningOfDay(fromDateCalendar);
		Calendar toDateCalendar = Calendar.getInstance();
		toDateCalendar.setTime(date);
		toDateCalendar.set(Calendar.MONTH, toDateCalendar.get(Calendar.MONTH) / 3 * 3 + 2);
		toDateCalendar.set(Calendar.DAY_OF_MONTH, toDateCalendar.getActualMaximum(Calendar.DAY_OF_MONTH));
		setTimeToEndofDay(toDateCalendar);
		billingPeriod.put(WSCalculationConstant.STARTING_DATE_APPLICABLES, fromDateCalendar.getTimeInMillis());
		billingPeriod.put(WSCalculationConstant.ENDING_DATE_APPLICABLES, toDateCalendar.getTimeInMillis());
		return billingPeriod;
	}

	public Map<String, Object> getMonthStartAndEndDate(Map<String, Object> billingPeriod) {
		Date date = new Date();
		Calendar monthStartDate = Calendar.getInstance();
		monthStartDate.setTime(date);
		monthStartDate.set(Calendar.DAY_OF_MONTH, monthStartDate.getActualMinimum(Calendar.DAY_OF_MONTH));
		setTimeToBeginningOfDay(monthStartDate);

		Calendar monthEndDate = Calendar.getInstance();
		monthEndDate.setTime(date);
		monthEndDate.set(Calendar.DAY_OF_MONTH, monthEndDate.getActualMaximum(Calendar.DAY_OF_MONTH));
		setTimeToEndofDay(monthEndDate);
		billingPeriod.put(WSCalculationConstant.STARTING_DATE_APPLICABLES, monthStartDate.getTimeInMillis());
		billingPeriod.put(WSCalculationConstant.ENDING_DATE_APPLICABLES, monthEndDate.getTimeInMillis());
		return billingPeriod;
	}

	private static void setTimeToBeginningOfDay(Calendar calendar) {
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);
	}

	private static void setTimeToEndofDay(Calendar calendar) {
		calendar.set(Calendar.HOUR_OF_DAY, 23);
		calendar.set(Calendar.MINUTE, 59);
		calendar.set(Calendar.SECOND, 59);
		calendar.set(Calendar.MILLISECOND, 999);
	}

	/**
	 * 
	 * @param criteria
	 * @param requestInfo
	 * @param masterData
	 * @return Fee Estimation Map
	 */
	@SuppressWarnings("rawtypes")
	public Map<String, List> getFeeEstimation(CalculationCriteria criteria, RequestInfo requestInfo,
			Map<String, Object> masterData) {
		WaterConnection waterConnection = null;
		if (StringUtils.isEmpty(criteria.getWaterConnection()) && !StringUtils.isEmpty(criteria.getApplicationNo())) {
			SearchCriteria searchCriteria = new SearchCriteria();
			searchCriteria.setApplicationNumber(criteria.getApplicationNo());
			searchCriteria.setTenantId(criteria.getTenantId());
			waterConnection = calculatorUtil.getWaterConnectionOnApplicationNO(requestInfo, searchCriteria,
					requestInfo.getUserInfo().getTenantId());
			criteria.setWaterConnection(waterConnection);
		}
		if (StringUtils.isEmpty(criteria.getWaterConnection())) {
			throw new CustomException("WATER_CONNECTION_NOT_FOUND",
					"Water Connection are not present for " + criteria.getApplicationNo() + " Application no");
		}
		ArrayList<String> billingSlabIds = new ArrayList<>();
		billingSlabIds.add("");
		List<TaxHeadEstimate> taxHeadEstimates = new ArrayList<TaxHeadEstimate>();
		String activityType = criteria.getWaterConnection().getActivityType();
		if (activityType.equalsIgnoreCase(WSCalculationConstant.WS_PERMANENT_DISCONNECTION)
				|| activityType.equalsIgnoreCase(WSCalculationConstant.WS_TEMPORARY_DISCONNECTION)
				|| activityType.equalsIgnoreCase(WSCalculationConstant.WS_REACTIVATE)
				|| activityType.equalsIgnoreCase(WSCalculationConstant.WS_CHANGE_OWNER_INFO)
				|| activityType.equalsIgnoreCase(WSCalculationConstant.WS_CONVERSION)
				|| activityType.equalsIgnoreCase(WSCalculationConstant.WS_UPDATEMETER)) {
			taxHeadEstimates = getTaxHeadForwaterActivity(criteria, masterData, requestInfo);
		} else {
			if (criteria.getWaterConnection().getWaterApplicationType()
					.equalsIgnoreCase(WSCalculationConstant.WS_TEMP_CONNECTION_TYPE)|| activityType.equalsIgnoreCase(WSCalculationConstant.WS_APPLY_FOR_TEMP_REGULAR_CON)) {
				taxHeadEstimates = getTaxHeadForFeeEstimationForTempAppCon(criteria, masterData, requestInfo);
			} else {
				taxHeadEstimates = getTaxHeadForRegularConnection(criteria, masterData, requestInfo);
			}
		}
		Map<String, List> estimatesAndBillingSlabs = new HashMap<>();
		estimatesAndBillingSlabs.put("estimates", taxHeadEstimates);
		// //Billing slab id
		estimatesAndBillingSlabs.put("billingSlabIds", billingSlabIds);
		return estimatesAndBillingSlabs;
	}

	private List<TaxHeadEstimate> getTaxHeadForwaterActivity(CalculationCriteria criteria,
			Map<String, Object> masterData, RequestInfo requestInfo) {
		List<TaxHeadEstimate> estimates = new ArrayList<>();

		if (criteria.getWaterConnection().getActivityType().equalsIgnoreCase(WSCalculationConstant.WS_UPDATEMETER)) {

			JSONArray regularSlab = (JSONArray) masterData.getOrDefault(WSCalculationConstant.WS_REGULAR_CHARGES, null);
			JSONObject masterSlab = new JSONObject();

			if (regularSlab != null) {
				masterSlab.put("PipeSize", regularSlab);
				JSONArray filteredMasters = JsonPath.read(masterSlab,
						"$.PipeSize[?(@.size=='" + criteria.getWaterConnection().getProposedPipeSize() + "')]");
				JSONObject chargesSlab = new JSONObject();

				chargesSlab.put("Charges", filteredMasters.get(0));
				ObjectMapper mapper = new ObjectMapper();
				List<BillingSlab> mappingBillingSlab;
				try {
					mappingBillingSlab = mapper.readValue(filteredMasters.toJSONString(),
							mapper.getTypeFactory().constructCollectionType(List.class, BillingSlab.class));
				} catch (IOException e) {
					throw new CustomException("Parsing Exception", " Billing Slab can not be parsed!");
				}

				BigDecimal meterTestingFee = BigDecimal.ZERO;
				BigDecimal meterFittingFee = BigDecimal.ZERO;
				BigDecimal stolenMeterCharges = BigDecimal.ZERO;
				BigDecimal additionalCharges = BigDecimal.ZERO;
				BigDecimal constructionCharges = BigDecimal.ZERO;

				if (mappingBillingSlab != null) {

					meterTestingFee = new BigDecimal(
							mappingBillingSlab.get(0).getMeterUpdateCharges().get(0).getMetertesting());

					meterFittingFee = new BigDecimal(
							mappingBillingSlab.get(0).getMeterUpdateCharges().get(0).getMeterfitting());
					if(criteria.getWaterConnection().getWaterApplication().getIsMeterStolen()) {
						stolenMeterCharges =  new BigDecimal(
								mappingBillingSlab.get(0).getMeterUpdateCharges().get(0).getStolenmetercharges());
						
					}
					additionalCharges = new BigDecimal(
							criteria.getWaterConnection().getWaterApplication().getAdditionalCharges() == null ? 0.0
									: criteria.getWaterConnection().getWaterApplication().getAdditionalCharges());

					constructionCharges = new BigDecimal(
							criteria.getWaterConnection().getWaterApplication().getConstructionCharges() == null ? 0.0
									: criteria.getWaterConnection().getWaterApplication().getConstructionCharges());

				}

				if (!(meterTestingFee.compareTo(BigDecimal.ZERO) == 0))
					estimates.add(TaxHeadEstimate.builder().taxHeadCode(WSCalculationConstant.WS_METER_TESTING_CHARGE)
							.estimateAmount(meterTestingFee.setScale(2, 2)).build());
				if (!(meterFittingFee.compareTo(BigDecimal.ZERO) == 0))
					estimates.add(TaxHeadEstimate.builder().taxHeadCode(WSCalculationConstant.WS_METER_CHARGE)
							.estimateAmount(meterFittingFee.setScale(2, 2)).build());
				if (!(stolenMeterCharges.compareTo(BigDecimal.ZERO) == 0))
					estimates.add(TaxHeadEstimate.builder().taxHeadCode(WSCalculationConstant.WS_SALES_METER)
							.estimateAmount(stolenMeterCharges.setScale(2, 2)).build());
				if (!(additionalCharges.compareTo(BigDecimal.ZERO) == 0))
					estimates.add(TaxHeadEstimate.builder().taxHeadCode(WSCalculationConstant.WS_ADDITIONAL_CHARGE)
							.estimateAmount(additionalCharges.setScale(2, 2)).build());
				if (!(constructionCharges.compareTo(BigDecimal.ZERO) == 0))
					estimates.add(TaxHeadEstimate.builder().taxHeadCode(WSCalculationConstant.WS_CONSTRUCTION_CHARGE)
							.estimateAmount(constructionCharges.setScale(2, 2)).build());

			}
			addAdhocPenalityAndRebate(estimates, criteria.getWaterConnection());
			return estimates;
		} else {
			JSONArray regularSlab = (JSONArray) masterData.getOrDefault(WSCalculationConstant.WS_WATER_ACTIVITY, null);
			JSONObject masterSlab = new JSONObject();

			if (regularSlab != null) {
				masterSlab.put("waterActivity", regularSlab);
				JSONArray filteredMasters = JsonPath.read(masterSlab,
						"$.waterActivity[?(@.code=='" + criteria.getWaterConnection().getActivityType() + "')]");
				JSONObject charge = mapper.convertValue(filteredMasters.get(0), JSONObject.class);
				BigDecimal unitCost = BigDecimal.ZERO;
				unitCost = new BigDecimal(charge.getAsNumber(WSCalculationConstant.UNIT_COST_CONST).toString());

				BigDecimal securityFee = BigDecimal.ZERO;

				BigDecimal additionalCharges = BigDecimal.ZERO;
				BigDecimal constructionCharges = BigDecimal.ZERO;
				securityFee = new BigDecimal(
						criteria.getWaterConnection().getWaterApplication().getSecurityCharge() == null ? 0.0
								: criteria.getWaterConnection().getWaterApplication().getSecurityCharge());

				additionalCharges = new BigDecimal(
						criteria.getWaterConnection().getWaterApplication().getAdditionalCharges() == null ? 0.0
								: criteria.getWaterConnection().getWaterApplication().getAdditionalCharges());

				constructionCharges = new BigDecimal(
						criteria.getWaterConnection().getWaterApplication().getConstructionCharges() == null ? 0.0
								: criteria.getWaterConnection().getWaterApplication().getConstructionCharges());

				if (!(securityFee.compareTo(BigDecimal.ZERO) == 0))
					estimates.add(TaxHeadEstimate.builder().taxHeadCode(WSCalculationConstant.WS_SECURITY_CHARGE)
							.estimateAmount(securityFee.setScale(2, 2)).build());

				if (!(additionalCharges.compareTo(BigDecimal.ZERO) == 0))
					estimates.add(TaxHeadEstimate.builder().taxHeadCode(WSCalculationConstant.WS_ADDITIONAL_CHARGE)
							.estimateAmount(additionalCharges.setScale(2, 2)).build());

				if (!(constructionCharges.compareTo(BigDecimal.ZERO) == 0))
					estimates.add(TaxHeadEstimate.builder().taxHeadCode(WSCalculationConstant.WS_CONSTRUCTION_CHARGE)
							.estimateAmount(constructionCharges.setScale(2, 2)).build());

				if (criteria.getWaterConnection().getActivityType()
						.equalsIgnoreCase(WSCalculationConstant.WS_PERMANENT_DISCONNECTION)
						|| criteria.getWaterConnection().getActivityType()
								.equalsIgnoreCase(WSCalculationConstant.WS_TEMPORARY_DISCONNECTION)) {
					if (!(unitCost.compareTo(BigDecimal.ZERO) == 0)) {
						estimates.add(TaxHeadEstimate.builder().taxHeadCode(WSCalculationConstant.WS_DISCONNECTION_FEE)
								.estimateAmount(unitCost.setScale(2, 2)).build());
					}
				}
				else	if (criteria.getWaterConnection().getActivityType()
							.equalsIgnoreCase(WSCalculationConstant.WS_CHANGE_OWNER_INFO)) {
						if (!(unitCost.compareTo(BigDecimal.ZERO) == 0)) {
							estimates.add(TaxHeadEstimate.builder()
									.taxHeadCode(WSCalculationConstant.WS_UPDATE_CONNECTION_HOLDER)
									.estimateAmount(unitCost.setScale(2, 2)).build());
						}
					
				}
				else	if (criteria.getWaterConnection().getActivityType()
						.equalsIgnoreCase(WSCalculationConstant.WS_REACTIVATE)) {
					if (!(unitCost.compareTo(BigDecimal.ZERO) == 0)) {
						estimates.add(TaxHeadEstimate.builder()
								.taxHeadCode(WSCalculationConstant.WS_TEMPORARY_CONN_FEE)
								.estimateAmount(unitCost.setScale(2, 2)).build());
					}
				
			}
				// addAdhocPenalityAndRebate(estimates, criteria.getWaterConnection());
			}

			return estimates;
		}
	}

	private List<TaxHeadEstimate> getTaxHeadForFeeEstimationForTempAppCon(CalculationCriteria criteria,
			Map<String, Object> masterData, RequestInfo requestInfo) {
		Map<String, JSONArray> billingSlabMaster = new HashMap<>();
		String propertyUsage = "";
		if (criteria.getWaterConnection().getWaterApplication().getActivityType()
				.equalsIgnoreCase(WSCalculationConstant.WS_APPLY_FOR_TEMP_TEMP_CON)) {

			billingSlabMaster.put(WSCalculationConstant.WS_TEMP_BILLING_CHARGES,
					(JSONArray) masterData.get(WSCalculationConstant.WS_TEMP_BILLING_CHARGES));

			Property property = wSCalculationUtil.getProperty(WaterConnectionRequest.builder()
					.waterConnection(criteria.getWaterConnection()).requestInfo(requestInfo).build());
			List<TaxHeadEstimate> estimates = new ArrayList<>();
			ObjectMapper mapper = new ObjectMapper();
			List<BillingSlab> mappingBillingSlab;
			try {
				mappingBillingSlab = mapper.readValue(
						billingSlabMaster.get(WSCalculationConstant.WS_TEMP_BILLING_CHARGES).toJSONString(),
						mapper.getTypeFactory().constructCollectionType(List.class, BillingSlab.class));
			} catch (IOException e) {
				throw new CustomException("Parsing Exception", " Billing Slab can not be parsed!");
			}

			List<BillingSlab> billingSlabs = null;

			billingSlabs = mappingBillingSlab.stream().filter(slab -> {
				String propertyString = property.getUsageCategory().split("\\.")[0];
				if (property.getUsageCategory().split("\\.")[0]
						.equalsIgnoreCase(WSCalculationConstant.WS_RESIDENTIAL)) {
					propertyString = property.getUsageCategory();
				}
				boolean isBuildingTypeMatching = slab.getBuildingType().equalsIgnoreCase(propertyString);// property.usagecategory

				return isBuildingTypeMatching;
			}).collect(Collectors.toList());

			Double multiplier = 0.0;

			for (Slab slabs : billingSlabs.get(0).getSlabs()) {

				if (property.getSuperBuiltUpArea() > slabs.getFrom() && property.getSuperBuiltUpArea() <= slabs.getTo()
						&& (!property.getUsageCategory().split("\\.")[0]
								.equalsIgnoreCase(WSCalculationConstant.WS_RESIDENTIAL))
						&& slabs.getCode().equalsIgnoreCase(property.getUsageCategory())) {
					multiplier = slabs.getCharge();
					break;
				} else if (property.getSuperBuiltUpArea() > slabs.getFrom()
						&& property.getSuperBuiltUpArea() <= slabs.getTo()
						&& (property.getUsageCategory().split("\\.")[0]
								.equalsIgnoreCase(WSCalculationConstant.WS_RESIDENTIAL))) {
					multiplier = slabs.getCharge();
				}

			}
			BigDecimal formFee = BigDecimal.ZERO;
			BigDecimal additionalCharges = BigDecimal.ZERO;
			BigDecimal constructionCharges = BigDecimal.ZERO;
			if (multiplier != null) {

				formFee = new BigDecimal(multiplier);
			}

			additionalCharges = new BigDecimal(
					criteria.getWaterConnection().getWaterApplication().getAdditionalCharges() == null ? 0.0
							: criteria.getWaterConnection().getWaterApplication().getAdditionalCharges());

			constructionCharges = new BigDecimal(
					criteria.getWaterConnection().getWaterApplication().getConstructionCharges() == null ? 0.0
							: criteria.getWaterConnection().getWaterApplication().getConstructionCharges());

			if (!(additionalCharges.compareTo(BigDecimal.ZERO) == 0))
				estimates.add(TaxHeadEstimate.builder().taxHeadCode(WSCalculationConstant.WS_ADDITIONAL_CHARGE)
						.estimateAmount(additionalCharges.setScale(2, 2)).build());

			if (!(constructionCharges.compareTo(BigDecimal.ZERO) == 0))
				estimates.add(TaxHeadEstimate.builder().taxHeadCode(WSCalculationConstant.WS_CONSTRUCTION_CHARGE)
						.estimateAmount(constructionCharges.setScale(2, 2)).build());

			if (!(formFee.compareTo(BigDecimal.ZERO) == 0))
				estimates.add(TaxHeadEstimate.builder().taxHeadCode(WSCalculationConstant.WS_TEMPORARY_CONN_FEE)
						.estimateAmount(formFee.setScale(2, 2)).build());

			return estimates;

		} else if (criteria.getWaterConnection().getWaterApplication().getActivityType()
				.equalsIgnoreCase(WSCalculationConstant.WS_APPLY_FOR_TEMP_REGULAR_CON)) {
			List<TaxHeadEstimate> estimates = new ArrayList<>();
			BigDecimal securityFee = BigDecimal.ZERO;

			BigDecimal additionalCharges = BigDecimal.ZERO;
			BigDecimal constructionCharges = BigDecimal.ZERO;
			securityFee = new BigDecimal(
					criteria.getWaterConnection().getWaterApplication().getSecurityCharge() == null ? 0.0
							: criteria.getWaterConnection().getWaterApplication().getSecurityCharge());

			additionalCharges = new BigDecimal(
					criteria.getWaterConnection().getWaterApplication().getAdditionalCharges() == null ? 0.0
							: criteria.getWaterConnection().getWaterApplication().getAdditionalCharges());

			constructionCharges = new BigDecimal(
					criteria.getWaterConnection().getWaterApplication().getConstructionCharges() == null ? 0.0
							: criteria.getWaterConnection().getWaterApplication().getConstructionCharges());

			if (!(additionalCharges.compareTo(BigDecimal.ZERO) == 0))
				estimates.add(TaxHeadEstimate.builder().taxHeadCode(WSCalculationConstant.WS_ADDITIONAL_CHARGE)
						.estimateAmount(additionalCharges.setScale(2, 2)).build());

			if (!(constructionCharges.compareTo(BigDecimal.ZERO) == 0))
				estimates.add(TaxHeadEstimate.builder().taxHeadCode(WSCalculationConstant.WS_CONSTRUCTION_CHARGE)
						.estimateAmount(constructionCharges.setScale(2, 2)).build());

			if (!(securityFee.compareTo(BigDecimal.ZERO) == 0))
				estimates.add(TaxHeadEstimate.builder().taxHeadCode(WSCalculationConstant.WS_SECURITY_CHARGE)
						.estimateAmount(securityFee.setScale(2, 2)).build());

			return estimates;

		} else {
			billingSlabMaster.put(WSCalculationConstant.WS_TEMP_APP_CHARGES,
					(JSONArray) masterData.get(WSCalculationConstant.WS_TEMP_APP_CHARGES));

			Property property = wSCalculationUtil.getProperty(WaterConnectionRequest.builder()
					.waterConnection(criteria.getWaterConnection()).requestInfo(requestInfo).build());
			List<TaxHeadEstimate> estimates = new ArrayList<>();
			ObjectMapper mapper = new ObjectMapper();
			List<BillingSlab> mappingBillingSlab;
			try {
				mappingBillingSlab = mapper.readValue(
						billingSlabMaster.get(WSCalculationConstant.WS_TEMP_APP_CHARGES).toJSONString(),
						mapper.getTypeFactory().constructCollectionType(List.class, BillingSlab.class));
			} catch (IOException e) {
				throw new CustomException("Parsing Exception", " Billing Slab can not be parsed!");
			}

			List<BillingSlab> billingSlabs = null;
			billingSlabs = mappingBillingSlab.stream().filter(slab -> {
				String propertyString = property.getUsageCategory().split("\\.")[0];
				if (property.getUsageCategory().split("\\.")[0]
						.equalsIgnoreCase(WSCalculationConstant.WS_RESIDENTIAL)) {
					propertyString = property.getUsageCategory();

				}
				return slab.getBuildingType().equalsIgnoreCase(propertyString);// property.usagecategory

			}).collect(Collectors.toList());

			Double multiplier = 0.0;
			for (Slab slabs : billingSlabs.get(0).getSlabs()) {

				if (property.getSuperBuiltUpArea() > slabs.getFrom() && property.getSuperBuiltUpArea() <= slabs.getTo()
						&& (!property.getUsageCategory().split("\\.")[0]
								.equalsIgnoreCase(WSCalculationConstant.WS_RESIDENTIAL))
						&& slabs.getCode().equalsIgnoreCase(property.getUsageCategory())) {
					multiplier = slabs.getCharge();
					break;
				} else if (property.getSuperBuiltUpArea() > slabs.getFrom()
						&& property.getSuperBuiltUpArea() <= slabs.getTo()
						&& (property.getUsageCategory().split("\\.")[0]
								.equalsIgnoreCase(WSCalculationConstant.WS_RESIDENTIAL))) {
					multiplier = slabs.getCharge();
				}

			}
			BigDecimal formFee = BigDecimal.ZERO;
			BigDecimal additionalCharges = BigDecimal.ZERO;
			BigDecimal constructionCharges = BigDecimal.ZERO;
			if (multiplier != null) {
				if (multiplier == 0) {

					formFee = (new BigDecimal(criteria.getWaterConnection().getContractValue()))
							.multiply((BigDecimal.valueOf(1.5)).divide(WSCalculationConstant.HUNDRED));
				} else {

					formFee = new BigDecimal(multiplier * property.getSuperBuiltUpArea());
				}
			}

			additionalCharges = new BigDecimal(
					criteria.getWaterConnection().getWaterApplication().getAdditionalCharges() == null ? 0.0
							: criteria.getWaterConnection().getWaterApplication().getAdditionalCharges());

			constructionCharges = new BigDecimal(
					criteria.getWaterConnection().getWaterApplication().getConstructionCharges() == null ? 0.0
							: criteria.getWaterConnection().getWaterApplication().getConstructionCharges());

			if (!(additionalCharges.compareTo(BigDecimal.ZERO) == 0))
				estimates.add(TaxHeadEstimate.builder().taxHeadCode(WSCalculationConstant.WS_ADDITIONAL_CHARGE)
						.estimateAmount(additionalCharges.setScale(2, 2)).build());

			if (!(constructionCharges.compareTo(BigDecimal.ZERO) == 0))
				estimates.add(TaxHeadEstimate.builder().taxHeadCode(WSCalculationConstant.WS_CONSTRUCTION_CHARGE)
						.estimateAmount(constructionCharges.setScale(2, 2)).build());

			if (multiplier == 0) {
				if (!(formFee.compareTo(BigDecimal.ZERO) == 0))
					estimates.add(TaxHeadEstimate.builder().taxHeadCode(WSCalculationConstant.WS_TEMPORARY_COTRACT_FEE)
							.estimateAmount(formFee.setScale(2, 2)).build());
			} else {
				if (!(formFee.compareTo(BigDecimal.ZERO) == 0))
					estimates.add(TaxHeadEstimate.builder().taxHeadCode(WSCalculationConstant.WS_TEMPORARY_CONN_FEE)
							.estimateAmount(formFee.setScale(2, 2)).build());
			}
			return estimates;
		}
	}

	private List<TaxHeadEstimate> getTaxHeadForRegularConnection(CalculationCriteria criteria,
			Map<String, Object> masterData, RequestInfo requestInfo) {
		List<TaxHeadEstimate> estimates = new ArrayList<>();

		JSONArray regularSlab = (JSONArray) masterData.getOrDefault(WSCalculationConstant.WS_REGULAR_CHARGES, null);
		JSONObject masterSlab = new JSONObject();

		if (regularSlab != null) {
			masterSlab.put("PipeSize", regularSlab);
			JSONArray filteredMasters = JsonPath.read(masterSlab,
					"$.PipeSize[?(@.size=='" + criteria.getWaterConnection().getProposedPipeSize() + "')]");
			JSONObject chargesSlab = new JSONObject();

			chargesSlab.put("Charges", filteredMasters.get(0));
			ObjectMapper mapper = new ObjectMapper();
			List<BillingSlab> mappingBillingSlab;
			try {
				mappingBillingSlab = mapper.readValue(filteredMasters.toJSONString(),
						mapper.getTypeFactory().constructCollectionType(List.class, BillingSlab.class));
			} catch (IOException e) {
				throw new CustomException("Parsing Exception", " Billing Slab can not be parsed!");
			}

			BigDecimal securityFee = BigDecimal.ZERO;
			BigDecimal ferruleFee = BigDecimal.ZERO;
			BigDecimal meterTestingFee = BigDecimal.ZERO;
			BigDecimal meterFittingFee = BigDecimal.ZERO;
			BigDecimal additionalCharges = BigDecimal.ZERO;
			BigDecimal constructionCharges = BigDecimal.ZERO;

			if (mappingBillingSlab != null) {

				securityFee = new BigDecimal(
						criteria.getWaterConnection().getWaterApplication().getSecurityCharge() == null ? 0.0
								: criteria.getWaterConnection().getWaterApplication().getSecurityCharge());

				additionalCharges = new BigDecimal(
						criteria.getWaterConnection().getWaterApplication().getAdditionalCharges() == null ? 0.0
								: criteria.getWaterConnection().getWaterApplication().getAdditionalCharges());

				constructionCharges = new BigDecimal(
						criteria.getWaterConnection().getWaterApplication().getConstructionCharges() == null ? 0.0
								: criteria.getWaterConnection().getWaterApplication().getConstructionCharges());

				if (criteria.getWaterConnection().getWaterApplication().getIsFerruleApplicable()) {
					ferruleFee = new BigDecimal(mappingBillingSlab.get(0).getCharges().get(0).getFerrule());
				}
				meterTestingFee = new BigDecimal(mappingBillingSlab.get(0).getCharges().get(0).getMetertesting());

				meterFittingFee = new BigDecimal(mappingBillingSlab.get(0).getCharges().get(0).getMeterfitting());

			}

			if (!(securityFee.compareTo(BigDecimal.ZERO) == 0))
				estimates.add(TaxHeadEstimate.builder().taxHeadCode(WSCalculationConstant.WS_SECURITY_CHARGE)
						.estimateAmount(securityFee.setScale(2, 2)).build());
			if (!(ferruleFee.compareTo(BigDecimal.ZERO) == 0))
				estimates.add(TaxHeadEstimate.builder().taxHeadCode(WSCalculationConstant.WS_FERRULE_CHARGE)
						.estimateAmount(ferruleFee.setScale(2, 2)).build());
			if (!(meterTestingFee.compareTo(BigDecimal.ZERO) == 0))
				estimates.add(TaxHeadEstimate.builder().taxHeadCode(WSCalculationConstant.WS_METER_TESTING_CHARGE)
						.estimateAmount(meterTestingFee.setScale(2, 2)).build());
			if (!(meterFittingFee.compareTo(BigDecimal.ZERO) == 0))
				estimates.add(TaxHeadEstimate.builder().taxHeadCode(WSCalculationConstant.WS_METER_CHARGE)
						.estimateAmount(meterFittingFee.setScale(2, 2)).build());
			if (!(additionalCharges.compareTo(BigDecimal.ZERO) == 0))
				estimates.add(TaxHeadEstimate.builder().taxHeadCode(WSCalculationConstant.WS_ADDITIONAL_CHARGE)
						.estimateAmount(additionalCharges.setScale(2, 2)).build());
			if (!(constructionCharges.compareTo(BigDecimal.ZERO) == 0))
				estimates.add(TaxHeadEstimate.builder().taxHeadCode(WSCalculationConstant.WS_CONSTRUCTION_CHARGE)
						.estimateAmount(constructionCharges.setScale(2, 2)).build());

		}
		addAdhocPenalityAndRebate(estimates, criteria.getWaterConnection());
		return estimates;
	}

	/**
	 * 
	 * @param criteria
	 * @param masterData
	 * @return return all tax heads
	 */
	private List<TaxHeadEstimate> getTaxHeadForFeeEstimation(CalculationCriteria criteria,
			Map<String, Object> masterData, RequestInfo requestInfo) {
		JSONArray feeSlab = (JSONArray) masterData.getOrDefault(WSCalculationConstant.WC_FEESLAB_MASTER, null);
		if (feeSlab == null)
			throw new CustomException("FEE_SLAB_NOT_FOUND", "fee slab master data not found!!");

		Property property = wSCalculationUtil.getProperty(WaterConnectionRequest.builder()
				.waterConnection(criteria.getWaterConnection()).requestInfo(requestInfo).build());

		JSONObject feeObj = mapper.convertValue(feeSlab.get(0), JSONObject.class);
		BigDecimal formFee = BigDecimal.ZERO;
		if (feeObj.get(WSCalculationConstant.FORM_FEE_CONST) != null) {
			formFee = new BigDecimal(feeObj.getAsNumber(WSCalculationConstant.FORM_FEE_CONST).toString());
		}
		BigDecimal scrutinyFee = BigDecimal.ZERO;
		if (feeObj.get(WSCalculationConstant.SCRUTINY_FEE_CONST) != null) {
			scrutinyFee = new BigDecimal(feeObj.getAsNumber(WSCalculationConstant.SCRUTINY_FEE_CONST).toString());
		}
		BigDecimal otherCharges = BigDecimal.ZERO;
		if (feeObj.get(WSCalculationConstant.OTHER_CHARGE_CONST) != null) {
			otherCharges = new BigDecimal(feeObj.getAsNumber(WSCalculationConstant.OTHER_CHARGE_CONST).toString());
		}
		BigDecimal taxAndCessPercentage = BigDecimal.ZERO;
		if (feeObj.get(WSCalculationConstant.TAX_PERCENTAGE_CONST) != null) {
			taxAndCessPercentage = new BigDecimal(
					feeObj.getAsNumber(WSCalculationConstant.TAX_PERCENTAGE_CONST).toString());
		}
		BigDecimal meterCost = BigDecimal.ZERO;
		if (feeObj.get(WSCalculationConstant.METER_COST_CONST) != null
				&& criteria.getWaterConnection().getConnectionType() != null && criteria.getWaterConnection()
						.getConnectionType().equalsIgnoreCase(WSCalculationConstant.meteredConnectionType)) {
			meterCost = new BigDecimal(feeObj.getAsNumber(WSCalculationConstant.METER_COST_CONST).toString());
		}
		BigDecimal roadCuttingCharge = BigDecimal.ZERO;
		if (criteria.getWaterConnection().getRoadType() != null)
			roadCuttingCharge = getChargeForRoadCutting(masterData, criteria.getWaterConnection().getRoadType(),
					criteria.getWaterConnection().getRoadCuttingArea());
		BigDecimal roadPlotCharge = BigDecimal.ZERO;
		if (property.getLandArea() != null)
			roadPlotCharge = getPlotSizeFee(masterData, property.getLandArea());
		BigDecimal usageTypeCharge = BigDecimal.ZERO;
		if (criteria.getWaterConnection().getRoadCuttingArea() != null)
			usageTypeCharge = getUsageTypeFee(masterData,
					criteria.getWaterConnection().getWaterProperty().getUsageCategory(),
					criteria.getWaterConnection().getRoadCuttingArea());
		BigDecimal tax = BigDecimal.ZERO;
		BigDecimal totalCharge = BigDecimal.ZERO;
		totalCharge = formFee.add(scrutinyFee).add(otherCharges).add(meterCost).add(roadCuttingCharge)
				.add(roadPlotCharge).add(usageTypeCharge);
		tax = totalCharge.multiply(taxAndCessPercentage.divide(WSCalculationConstant.HUNDRED));
		List<TaxHeadEstimate> estimates = new ArrayList<>();
		//
		if (!(formFee.compareTo(BigDecimal.ZERO) == 0))
			estimates.add(TaxHeadEstimate.builder().taxHeadCode(WSCalculationConstant.WS_FORM_FEE)
					.estimateAmount(formFee.setScale(2, 2)).build());
		if (!(scrutinyFee.compareTo(BigDecimal.ZERO) == 0))
			estimates.add(TaxHeadEstimate.builder().taxHeadCode(WSCalculationConstant.WS_SCRUTINY_FEE)
					.estimateAmount(scrutinyFee.setScale(2, 2)).build());
		if (!(meterCost.compareTo(BigDecimal.ZERO) == 0))
			estimates.add(TaxHeadEstimate.builder().taxHeadCode(WSCalculationConstant.WS_METER_CHARGE)
					.estimateAmount(meterCost.setScale(2, 2)).build());
		if (!(otherCharges.compareTo(BigDecimal.ZERO) == 0))
			estimates.add(TaxHeadEstimate.builder().taxHeadCode(WSCalculationConstant.WS_OTHER_CHARGE)
					.estimateAmount(otherCharges.setScale(2, 2)).build());
		if (!(roadCuttingCharge.compareTo(BigDecimal.ZERO) == 0))
			estimates.add(TaxHeadEstimate.builder().taxHeadCode(WSCalculationConstant.WS_ROAD_CUTTING_CHARGE)
					.estimateAmount(roadCuttingCharge.setScale(2, 2)).build());
		if (!(usageTypeCharge.compareTo(BigDecimal.ZERO) == 0))
			estimates.add(TaxHeadEstimate.builder().taxHeadCode(WSCalculationConstant.WS_ONE_TIME_FEE)
					.estimateAmount(usageTypeCharge.setScale(2, 2)).build());
		if (!(roadPlotCharge.compareTo(BigDecimal.ZERO) == 0))
			estimates.add(TaxHeadEstimate.builder().taxHeadCode(WSCalculationConstant.WS_SECURITY_CHARGE)
					.estimateAmount(roadPlotCharge.setScale(2, 2)).build());
		if (!(tax.compareTo(BigDecimal.ZERO) == 0))
			estimates.add(TaxHeadEstimate.builder().taxHeadCode(WSCalculationConstant.WS_TAX_AND_CESS)
					.estimateAmount(tax.setScale(2, 2)).build());
		addAdhocPenalityAndRebate(estimates, criteria.getWaterConnection());
		return estimates;
	}

	/**
	 * 
	 * @param masterData
	 * @param roadType
	 * @param roadCuttingArea
	 * @return road cutting charge
	 */
	private BigDecimal getChargeForRoadCutting(Map<String, Object> masterData, String roadType, Float roadCuttingArea) {
		JSONArray roadSlab = (JSONArray) masterData.getOrDefault(WSCalculationConstant.WC_ROADTYPE_MASTER, null);
		JSONArray roadCutTax = (JSONArray) masterData.getOrDefault(WSCalculationConstant.WS_ROAD_CUT_TAX_CHARGES, null);
		JSONObject roadcutSlab = new JSONObject();
		BigDecimal gst_tax = BigDecimal.ZERO;
		BigDecimal supervision_tax = BigDecimal.ZERO;

		if (roadCutTax != null) {
			roadcutSlab.put("RoadCutTaxSlab", roadCutTax);
			JSONArray GST = JsonPath.read(roadcutSlab,
					"$.RoadCutTaxSlab[?(@.code=='" + WSCalculationConstant.TAX_GST + "')]");
			if (CollectionUtils.isEmpty(GST))
				return BigDecimal.ZERO;
			JSONArray supervision = JsonPath.read(roadcutSlab,
					"$.RoadCutTaxSlab[?(@.code=='" + WSCalculationConstant.TAX_SUPERVISION + "')]");

			JSONObject gstTax = mapper.convertValue(GST.get(0), JSONObject.class);
			JSONObject supervisionTax = mapper.convertValue(supervision.get(0), JSONObject.class);

			gst_tax = new BigDecimal(gstTax.getAsNumber(WSCalculationConstant.TAX_PERCENTAGE).toString());
			supervision_tax = new BigDecimal(
					supervisionTax.getAsNumber(WSCalculationConstant.TAX_PERCENTAGE).toString());

		}
		BigDecimal charge = BigDecimal.ZERO;
		JSONObject masterSlab = new JSONObject();
		if (roadSlab != null) {
			masterSlab.put("RoadType", roadSlab);
			JSONArray filteredMasters = JsonPath.read(masterSlab, "$.RoadType[?(@.code=='" + roadType + "')]");
			if (CollectionUtils.isEmpty(filteredMasters))
				return BigDecimal.ZERO;
			JSONObject master = mapper.convertValue(filteredMasters.get(0), JSONObject.class);
			charge = new BigDecimal(master.getAsNumber(WSCalculationConstant.UNIT_COST_CONST).toString());
			charge = charge.multiply(new BigDecimal(roadCuttingArea));
			charge = charge.add(charge.multiply(supervision_tax.divide(WSCalculationConstant.HUNDRED)));
			charge = charge.add(charge.multiply(gst_tax.divide(WSCalculationConstant.HUNDRED)));

		}
		return charge;
	}

	/**
	 * 
	 * @param masterData
	 * @param plotSize
	 * @return get fee based on plot size
	 */
	private BigDecimal getPlotSizeFee(Map<String, Object> masterData, Double plotSize) {
		BigDecimal charge = BigDecimal.ZERO;
		JSONArray plotSlab = (JSONArray) masterData.getOrDefault(WSCalculationConstant.WC_PLOTSLAB_MASTER, null);
		JSONObject masterSlab = new JSONObject();
		if (plotSlab != null) {
			masterSlab.put("PlotSizeSlab", plotSlab);
			JSONArray filteredMasters = JsonPath.read(masterSlab,
					"$.PlotSizeSlab[?(@.from <=" + plotSize + "&& @.to > " + plotSize + ")]");
			if (CollectionUtils.isEmpty(filteredMasters))
				return charge;
			JSONObject master = mapper.convertValue(filteredMasters.get(0), JSONObject.class);
			charge = new BigDecimal(master.getAsNumber(WSCalculationConstant.UNIT_COST_CONST).toString());
		}
		return charge;
	}

	/**
	 * 
	 * @param masterData
	 * @param usageType
	 * @param roadCuttingArea
	 * @return
	 */
	private BigDecimal getUsageTypeFee(Map<String, Object> masterData, String usageType, Float roadCuttingArea) {
		BigDecimal charge = BigDecimal.ZERO;
		JSONArray usageSlab = (JSONArray) masterData.getOrDefault(WSCalculationConstant.WC_PROPERTYUSAGETYPE_MASTER,
				null);
		JSONObject masterSlab = new JSONObject();
		BigDecimal cuttingArea = new BigDecimal(roadCuttingArea.toString());
		if (usageSlab != null) {
			masterSlab.put("PropertyUsageType", usageSlab);
			JSONArray filteredMasters = JsonPath.read(masterSlab,
					"$.PropertyUsageType[?(@.code=='" + usageType + "')]");
			if (CollectionUtils.isEmpty(filteredMasters))
				return charge;
			JSONObject master = mapper.convertValue(filteredMasters.get(0), JSONObject.class);
			charge = new BigDecimal(master.getAsNumber(WSCalculationConstant.UNIT_COST_CONST).toString());
			charge = charge.multiply(cuttingArea);
		}
		return charge;
	}

	/**
	 * Enrich the adhoc penality and adhoc rebate
	 * 
	 * @param estimates  tax head estimate
	 * @param connection water connection object
	 */
	@SuppressWarnings({ "unchecked" })
	private void addAdhocPenalityAndRebate(List<TaxHeadEstimate> estimates, WaterConnection connection) {
		if (connection.getAdditionalDetails() != null) {
			HashMap<String, Object> additionalDetails = mapper.convertValue(connection.getAdditionalDetails(),
					HashMap.class);
			if (additionalDetails.getOrDefault(WSCalculationConstant.ADHOC_PENALTY, null) != null) {
				estimates.add(TaxHeadEstimate.builder().taxHeadCode(WSCalculationConstant.WS_ADHOC_PENALTY)
						.estimateAmount(
								new BigDecimal(additionalDetails.get(WSCalculationConstant.ADHOC_PENALTY).toString()))
						.build());
			}
			if (additionalDetails.getOrDefault(WSCalculationConstant.ADHOC_REBATE, null) != null) {
				estimates
						.add(TaxHeadEstimate.builder().taxHeadCode(WSCalculationConstant.WS_ADHOC_REBATE)
								.estimateAmount(new BigDecimal(
										additionalDetails.get(WSCalculationConstant.ADHOC_REBATE).toString()).negate())
								.build());
			}
		}
	}
}
