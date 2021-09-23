package org.egov.ps.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.egov.common.contract.request.RequestInfo;
import org.egov.ps.config.Configuration;
import org.egov.ps.model.AccountStatementCriteria;
import org.egov.ps.model.BillV2;
import org.egov.ps.model.OfflinePaymentDetails;
import org.egov.ps.model.Owner;
import org.egov.ps.model.OwnerDetails;
import org.egov.ps.model.Property;
import org.egov.ps.model.PropertyCriteria;
import org.egov.ps.model.PropertyDueAmount;
import org.egov.ps.producer.Producer;
import org.egov.ps.repository.PropertyQueryBuilder;
import org.egov.ps.repository.PropertyRepository;
import org.egov.ps.service.calculation.DemandRepository;
import org.egov.ps.service.calculation.DemandService;
import org.egov.ps.service.calculation.IEstateRentCollectionService;
import org.egov.ps.service.calculation.IManiMajraRentCollectionService;
import org.egov.ps.util.PSConstants;
import org.egov.ps.util.Util;
import org.egov.ps.validator.PropertyValidator;
import org.egov.ps.web.contracts.AccountStatementResponse;
import org.egov.ps.web.contracts.AuditDetails;
import org.egov.ps.web.contracts.BusinessService;
import org.egov.ps.web.contracts.EstateAccount;
import org.egov.ps.web.contracts.EstateDemand;
import org.egov.ps.web.contracts.EstatePayment;
import org.egov.ps.web.contracts.EstateRentSummary;
import org.egov.ps.web.contracts.ManiMajraDemand;
import org.egov.ps.web.contracts.ManiMajraPayment;
import org.egov.ps.web.contracts.PropertyDueRequest;
import org.egov.ps.web.contracts.PropertyRequest;
import org.egov.ps.web.contracts.State;
import org.egov.ps.workflow.WorkflowIntegrator;
import org.egov.ps.workflow.WorkflowService;
import org.egov.tracer.model.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

@Service
public class PropertyService {

	@Autowired
	private PropertyEnrichmentService enrichmentService;

	@Autowired
	private Configuration config;

	@Autowired
	private Producer producer;

	@Autowired
	PropertyValidator propertyValidator;

	@Autowired
	PropertyRepository repository;

	@Autowired
	WorkflowIntegrator wfIntegrator;

	@Autowired
	private WorkflowService workflowService;

	@Autowired
	private IEstateRentCollectionService estateRentCollectionService;

	@Autowired
	private IManiMajraRentCollectionService maniMajraRentCollectionService;

	@Autowired
	private UserService userService;

	@Autowired
	private Util utils;

	@Autowired
	private DemandService demandService;

	@Autowired
	private DemandRepository demandRepository;

	@Autowired
	private MDMSService mdmsservice;

	@Autowired
	private EstateDemandGenerationService estateDemandGenerationService;

	@Autowired
	private ManiMajraDemandGenerationService maniMajraDemandGenerationService;

	@Autowired
	Util util;

	public List<Property> createProperty(PropertyRequest request) {

		propertyValidator.validateCreateRequest(request);
		// bifurcate demand
		enrichmentService.enrichPropertyRequest(request);
		if (request.getProperties().get(0).getPropertyDetails().getBranchType().contentEquals(PSConstants.MANI_MAJRA)) {
			if(!request.getProperties().get(0).getPropertyDetails().getPropertyType().equalsIgnoreCase(PSConstants.MM_PROPERTY_TYPE_OTHER))
				maniMajraSettlePayment(request);
			producer.push(config.getSavePropertyTopic(), request);
		} else {
			processRentHistory(request);
			producer.push(config.getSavePropertyTopic(), request);
			processRentSummary(request);
		}
		return request.getProperties();
	}

	private void maniMajraSettlePayment(PropertyRequest request) {
		request.getProperties().forEach(property -> {
			if (property.getPropertyDetails().getManiMajraDemands() != null
					&& property.getPropertyDetails().getManiMajraPayments() != null) {
				maniMajraRentCollectionService.settle(property.getPropertyDetails().getManiMajraDemands(),
						property.getPropertyDetails().getManiMajraPayments(),
						property.getPropertyDetails().getEstateAccount());
			}
		});
	}

	private void processRentSummary(PropertyRequest request) {
		request.getProperties().stream().filter(property -> property.getPropertyDetails().getEstateDemands() != null
				&& property.getPropertyDetails().getEstatePayments() != null
				&& property.getPropertyDetails().getEstateAccount() != null
				&& property.getPropertyDetails().getPaymentConfig() != null
				&& property.getPropertyDetails().getPropertyType().equalsIgnoreCase(PSConstants.ES_PM_LEASEHOLD))
		.forEach(property -> {
			estateRentCollectionService.settle(property.getPropertyDetails().getEstateDemands(),
					property.getPropertyDetails().getEstatePayments(),
					property.getPropertyDetails().getEstateAccount(), PSConstants.GST_INTEREST_RATE,
					property.getPropertyDetails().getPaymentConfig().getIsIntrestApplicable(),
					property.getPropertyDetails().getPaymentConfig().getRateOfInterest().doubleValue());
			property.setEstateRentSummary(

					estateRentCollectionService.calculateRentSummary(
							property.getPropertyDetails().getEstateDemands(),
							property.getPropertyDetails().getEstateAccount(), PSConstants.GST_INTEREST_RATE,
							property.getPropertyDetails().getPaymentConfig().getIsIntrestApplicable(),
							property.getPropertyDetails().getPaymentConfig().getRateOfInterest()
							.doubleValue()));
		});
	}

	private void processRentHistory(PropertyRequest request) {
		if (!CollectionUtils.isEmpty(request.getProperties())) {
			request.getProperties().stream()
			.filter(property -> property.getPropertyDetails().getEstateDemands() != null
			&& property.getPropertyDetails().getEstatePayments() != null
			&& property.getPropertyDetails().getEstateAccount() != null
			&& property.getPropertyDetails().getPaymentConfig() != null && property.getPropertyDetails()
			.getPropertyType().equalsIgnoreCase(PSConstants.ES_PM_LEASEHOLD))
			.forEach(property -> {
				property.getPropertyDetails().setEstateRentCollections(estateRentCollectionService.settle(
						property.getPropertyDetails().getEstateDemands(),
						property.getPropertyDetails().getEstatePayments(),
						property.getPropertyDetails().getEstateAccount(), PSConstants.GST_INTEREST_RATE,
						property.getPropertyDetails().getPaymentConfig().getIsIntrestApplicable(),
						property.getPropertyDetails().getPaymentConfig().getRateOfInterest().doubleValue()));
			});
		}
		enrichmentService.enrichCollection(request);

	}

	/**
	 * Updates the property
	 *
	 * @param request PropertyRequest containing list of properties to be update
	 * @return List of updated properties
	 */
	public List<Property> updateProperty(PropertyRequest request) {
		propertyValidator.validateUpdateRequest(request);
		Property property = request.getProperties().get(0);

		String action = property.getAction();
		String state = property.getState();

		// bifurcate demand
		if (!CollectionUtils.isEmpty(property.getPropertyDetails().getEstateDemands())
				&& property.getPropertyDetails().getBranchType().equalsIgnoreCase(PSConstants.ESTATE_BRANCH)
				&& property.getPropertyDetails().getPaymentConfig().getGroundRentGenerationType()
				.equalsIgnoreCase(PSConstants.MONTHLY)) {
			estateDemandGenerationService.bifurcateDemand(property);
		}
		/* Approved Property Add Estate Branch Missing Demands */
		if (null != request.getProperties().get(0).getState()
				&& (PSConstants.PENDING_SO_APPROVAL.equalsIgnoreCase(property.getState())
						|| PSConstants.ES_EB_PM_PENDING_APPROVAL.equalsIgnoreCase(property.getState()))
				&& property.getPropertyDetails().getBranchType().equalsIgnoreCase(PSConstants.ESTATE_BRANCH)
				&& !action.contentEquals("")
				&& property.getPropertyDetails().getPropertyType().equalsIgnoreCase(PSConstants.ES_PM_LEASEHOLD)) {
			if (null != request.getProperties().get(0).getPropertyDetails().getBidders()
					&& request.getProperties().get(0).getPropertyDetails().getPropertyType()
					.equalsIgnoreCase(PSConstants.EB_ALLOCATION_TYPE_AUCTION)) {
				/**
				 * Adding emd amount to account remaining amount of winning bidder
				 * This only works if the EMD amounts collect by all the users are same
				 */
				request.getProperties().get(0).getPropertyDetails().getEstateAccount()
				.setRemainingAmount(request.getProperties().get(0).getPropertyDetails().getBidders().get(0)
						.getDepositedEMDAmount().doubleValue());
			}
			estateDemandGenerationService.createMissingDemands(property);
			estateDemandGenerationService.addCredit(property);
		}

		/* ManiMajra Demands */
		if (null != request.getProperties().get(0).getState()
				&& PSConstants.PENDING_PM_MM_APPROVAL.equalsIgnoreCase(property.getState())
				&& property.getPropertyDetails().getBranchType().equalsIgnoreCase(PSConstants.MANI_MAJRA) 
				&& !property.getPropertyDetails().getPropertyType().equalsIgnoreCase(PSConstants.MM_PROPERTY_TYPE_OTHER)
				&& !action.contentEquals("")) {
			maniMajraDemandGenerationService.createMissingDemandsForMM(property, request.getRequestInfo());
		}

		enrichmentService.enrichPropertyRequest(request);
		if (property.getPropertyDetails().getBranchType().contentEquals(PSConstants.MANI_MAJRA)
				&& !property.getPropertyDetails().getPropertyType().equalsIgnoreCase(PSConstants.MM_PROPERTY_TYPE_OTHER)) {
			maniMajraSettlePayment(request);
		} else {
			processRentHistory(request);
		}

		if (config.getIsWorkflowEnabled() && !action.contentEquals("") && !action.contentEquals(PSConstants.ES_DRAFT)
				//				&& !(state.contentEquals(PSConstants.PM_APPROVED)
				//						|| state.contentEquals(PSConstants.ES_PM_EB_APPROVED))
				) {
			wfIntegrator.callWorkFlow(request);
		}
		else if (!CollectionUtils.isEmpty(request.getProperties().get(0).getPropertyDetails().getBidders())) {
			String roeAction = request.getProperties().get(0).getPropertyDetails().getBidders().get(0).getAction();
			String addCourtCases = request.getProperties().get(0).getPropertyDetails().getAddCourtCases()!=null?request.getProperties().get(0).getPropertyDetails().getAddCourtCases():"";
			if (config.getIsWorkflowEnabled() && !roeAction.contentEquals("")
					//					&& (state.contentEquals(PSConstants.PM_APPROVED)
					//							|| state.contentEquals(PSConstants.ES_PM_EB_APPROVED))
					&& !addCourtCases.contentEquals(PSConstants.EB_ADD_COURT_CASES)
					&& !request.getProperties().get(0).getPropertyDetails().isAdhocDemand()
					&& !request.getProperties().get(0).getPropertyDetails().isAdhocPayment()) {
				wfIntegrator.callWorkFlow(request);
			}
		}
		producer.push(config.getUpdatePropertyTopic(), request);
		if ((!property.getPropertyDetails().getBranchType().contentEquals(PSConstants.MANI_MAJRA) && !property.getPropertyDetails().getBranchType().contentEquals(PSConstants.BUILDING_BRANCH))
				&& !property.getPropertyDetails().getPropertyType().equalsIgnoreCase(PSConstants.MM_PROPERTY_TYPE_OTHER)) {
			processRentSummary(request);
		}
		return request.getProperties();
	}

	public List<Property> searchProperty(PropertyCriteria criteria, RequestInfo requestInfo) {
		/**
		 * Convert file number to upper case if provided.
		 */
		if (criteria.getFileNumber() != null) {
			criteria.setFileNumber(criteria.getFileNumber().trim().toUpperCase());
		}

		if (criteria.isEmpty()) {
			/**
			 * Set the list of states to exclude draft states. Allow criteria to have
			 * creator as current user.
			 */
			List<String> allStates = new ArrayList<>();

			BusinessService businessService = workflowService.getBusinessService(PSConstants.TENANT_ID, requestInfo,
					config.getAosBusinessServiceValue());
			List<String> states = businessService.getStates().stream().map(State::getApplicationStatus)
					.filter(s -> s != null && s.length() != 0).collect(Collectors.toList());

			allStates.addAll(states);

			BusinessService pmBusinessService = workflowService.getBusinessService(PSConstants.TENANT_ID, requestInfo,
					config.getEbPmBusinessServiceValue());

			List<String> pmStates = pmBusinessService.getStates().stream().map(State::getApplicationStatus)
					.filter(s -> s != null && s.length() != 0).collect(Collectors.toList());

			allStates.addAll(pmStates);

			criteria.setState(allStates);
			criteria.setUserId(requestInfo.getUserInfo().getUuid());
		} else if (criteria.getState() != null && criteria.getState().contains(PSConstants.PM_DRAFTED)) {
			/**
			 * If only drafted state is asked for, fetch currently logged in user's
			 * properties.
			 */
			criteria.setUserId(requestInfo.getUserInfo().getUuid());
		}
		/**
		 * when trying to access property through auctionId
		 */
		if (criteria.getAuctionId() != null && criteria.getRelations() != null
				&& !criteria.getRelations().contains(PSConstants.RELATION_BIDDER))
			criteria.getRelations().add(PSConstants.RELATION_BIDDER);

		if (requestInfo.getUserInfo().getType().equalsIgnoreCase(PSConstants.ROLE_EMPLOYEE)) {
			Set<String> employeeBranches = new HashSet<>();
			String businessServiceName = null;
			String ebPmBusinessServiceName = null;
			requestInfo.getUserInfo().getRoles().stream().filter(role -> role.getCode() != PSConstants.ROLE_EMPLOYEE)
			.map(role -> role.getCode()).forEach(rolecode -> {
				if (rolecode.startsWith("ES_EB")) {
					employeeBranches.add(PSConstants.ESTATE_BRANCH);
				}
				if (rolecode.startsWith("ES_BB")) {
					employeeBranches.add(PSConstants.BUILDING_BRANCH);
				}
				if (rolecode.startsWith("ES_MM")) {
					employeeBranches.add(PSConstants.MANI_MAJRA);
				}
				if (rolecode.equalsIgnoreCase("ES_ADDITIONAL_COMMISSIONER")) {
					employeeBranches.add(PSConstants.ESTATE_BRANCH);
					employeeBranches.add(PSConstants.BUILDING_BRANCH);
					employeeBranches.add(PSConstants.MANI_MAJRA);
				}
			});
			if ((criteria.getBranchType() != null && !criteria.getBranchType().isEmpty())) {
				if (!criteria.getBranchType().stream().filter(branch -> employeeBranches.contains(branch)).findAny()
						.isPresent())
					throw new CustomException("INVALID ACCESS", "You are not able to access this resource.");

				if (criteria.branchTypeOnly()) {
					for (String branch : criteria.getBranchType()) {

						if (branch.equalsIgnoreCase(PSConstants.ESTATE_BRANCH)) {

							businessServiceName = config.getAosBusinessServiceValue();
							ebPmBusinessServiceName = config.getEbPmBusinessServiceValue();
						} else if (branch.equalsIgnoreCase(PSConstants.BUILDING_BRANCH))
							businessServiceName = config.getBbPmBusinessServiceValue();
						else
							businessServiceName = config.getMmPmBusinessServiceValue();

						BusinessService businessService = workflowService.getBusinessService(PSConstants.TENANT_ID,
								requestInfo, businessServiceName);

						List<String> states = businessService.getStates().stream().map(State::getApplicationStatus)
								.filter(s -> s != null && s.length() != 0).collect(Collectors.toList());

						if (null != ebPmBusinessServiceName) {
							BusinessService ebPmBusinessService = workflowService
									.getBusinessService(PSConstants.TENANT_ID, requestInfo, ebPmBusinessServiceName);
							states.addAll(ebPmBusinessService.getStates().stream().map(State::getApplicationStatus)
									.filter(s -> s != null && s.length() != 0).collect(Collectors.toList()));
						}
						criteria.setState(states);
						criteria.setUserId(requestInfo.getUserInfo().getUuid());
					}
				} else if (!CollectionUtils.isEmpty(criteria.getState())
						&& criteria.getState().contains(PSConstants.PM_DRAFTED))
					criteria.setUserId(requestInfo.getUserInfo().getUuid());
			} else {
				criteria.setBranchType(new ArrayList<>(employeeBranches));
			}
		}

		List<Property> properties = repository.getProperties(criteria);

		if (CollectionUtils.isEmpty(properties)) {
			return Collections.emptyList();
		}
		// Note : criteria.getRelations().contains(PSConstants.RELATION_FINANCE) filter
		// is in rented-properties do we need to put here?
		if (properties.size() <= 1 || !CollectionUtils.isEmpty(criteria.getRelations())) {
			properties.stream().forEach(property -> {
				List<String> propertyDetailsIds = new ArrayList<>();
				propertyDetailsIds.add(property.getPropertyDetails().getId());

				List<EstateDemand> demands = repository.getDemandDetailsForPropertyDetailsIds(propertyDetailsIds);
				List<EstatePayment> payments = repository.getEstatePaymentsForPropertyDetailsIds(propertyDetailsIds);
				EstateAccount estateAccount = repository.getPropertyEstateAccountDetails(propertyDetailsIds);

				if (!CollectionUtils.isEmpty(demands) && property.getPropertyDetails().getPaymentConfig() != null
						&& property.getPropertyDetails().getPropertyType()
						.equalsIgnoreCase(PSConstants.ES_PM_LEASEHOLD)) {
					property.setEstateRentSummary(estateRentCollectionService.calculateRentSummary(demands,
							estateAccount, PSConstants.GST_INTEREST_RATE,
							property.getPropertyDetails().getPaymentConfig().getIsIntrestApplicable(),
							property.getPropertyDetails().getPaymentConfig().getRateOfInterest().doubleValue()));
					property.getPropertyDetails().setEstateDemands(demands);
					property.getPropertyDetails().setEstatePayments(payments);

				}
				if (estateAccount != null) {
					property.getPropertyDetails().setEstateAccount(estateAccount);
				}
			});
		}

		if (properties.size() <= 1 && !CollectionUtils.isEmpty(criteria.getRelations())
				&& criteria.getRelations().contains(PSConstants.RELATION_OPD)) {
			List<String> propertyDetailsIds = new ArrayList<String>();
			properties.stream().forEach(property -> {
				propertyDetailsIds.add(property.getPropertyDetails().getId());
				List<OfflinePaymentDetails> offlinePaymentDetails = repository
						.getOfflinePaymentsForPropertyDetailsIds(propertyDetailsIds, criteria);
				if (!CollectionUtils.isEmpty(offlinePaymentDetails)) {
					property.getPropertyDetails().setOfflinePaymentDetails(offlinePaymentDetails);
				}
			});
		}

		return properties;
	}

	public AccountStatementResponse searchPayments(AccountStatementCriteria accountStatementCriteria,
			RequestInfo requestInfo) {
		LocalDate fromLocalDate = null;
		/**
		 * converting timestamp to date
		 */
		if (accountStatementCriteria.getFromDate() != null)
			fromLocalDate = Instant.ofEpochMilli(accountStatementCriteria.getFromDate()).atZone(ZoneId.systemDefault())
			.toLocalDate();

		LocalDate toLocalDate = Instant.ofEpochMilli(accountStatementCriteria.getToDate())
				.atZone(ZoneId.systemDefault()).toLocalDate();

		AccountStatementResponse accountStatementResponse = new AccountStatementResponse();

		if (accountStatementCriteria.getFromDate() != null && toLocalDate.isBefore(fromLocalDate)) {
			throw new CustomException("DATE_VALIDATION", "From date cannot be greater than to date");
		}

		List<Property> properties = repository
				.getProperties(PropertyCriteria.builder().propertyId(accountStatementCriteria.getPropertyid())
						.relations(Collections.singletonList("finance")).build());
		if (CollectionUtils.isEmpty(properties)) {
			return AccountStatementResponse.builder().estateAccountStatements(Collections.emptyList()).build();
		}

		Property property = properties.get(0);

		List<String> propertyDetailsIds = new ArrayList<String>();
		propertyDetailsIds.add(property.getPropertyDetails().getId());

		EstateAccount estateAccount = repository.getPropertyEstateAccountDetails(propertyDetailsIds);

		if (property.getPropertyDetails().getBranchType().equalsIgnoreCase(PSConstants.MANI_MAJRA)) {
			List<ManiMajraDemand> mmDemands = repository
					.getManiMajraDemandDetails(Collections.singletonList(property.getPropertyDetails().getId()));

			List<ManiMajraPayment> mmPayments = repository
					.getManiMajraPaymentsDetails(Collections.singletonList(property.getPropertyDetails().getId()));

			if (!CollectionUtils.isEmpty(mmDemands) && null != estateAccount) {

				accountStatementResponse = AccountStatementResponse.builder()
						.mmAccountStatements(maniMajraRentCollectionService.getAccountStatement(mmDemands, mmPayments,
								accountStatementCriteria.getFromDate(), accountStatementCriteria.getToDate()))
						.build();
			}
		} else {
			List<EstateDemand> demands = repository.getDemandDetailsForPropertyDetailsIds(
					Collections.singletonList(property.getPropertyDetails().getId()));

			List<EstatePayment> payments = repository.getEstatePaymentsForPropertyDetailsIds(
					Collections.singletonList(property.getPropertyDetails().getId()));

			if (!CollectionUtils.isEmpty(property.getPropertyDetails().getEstateDemands()) && null != estateAccount
					&& property.getPropertyDetails().getPaymentConfig() != null
					&& property.getPropertyDetails().getPropertyType().equalsIgnoreCase(PSConstants.ES_PM_LEASEHOLD)) {

				accountStatementResponse = AccountStatementResponse.builder()
						.estateAccountStatements(estateRentCollectionService.getAccountStatement(demands, payments,
								PSConstants.GST_INTEREST_RATE, accountStatementCriteria.getFromDate(),
								accountStatementCriteria.getToDate(),
								property.getPropertyDetails().getPaymentConfig().getIsIntrestApplicable(),
								property.getPropertyDetails().getPaymentConfig().getRateOfInterest().doubleValue()))
						.build();
			}
		}
		return accountStatementResponse;

	}

	public List<Property> generateFinanceDemand(PropertyRequest propertyRequest) {
		/**
		 * Validate not empty
		 */
		if (CollectionUtils.isEmpty(propertyRequest.getProperties())) {
			return Collections.emptyList();
		}
		Property propertyFromRequest = propertyRequest.getProperties().get(0);
		/**
		 * Validate that this is a valid property id.
		 */
		if (propertyFromRequest.getId() == null) {
			throw new CustomException(
					Collections.singletonMap("NO_PROPERTY_ID_FOUND", "No Property found to process rent"));
		}
		if (propertyFromRequest.getPropertyDetails().getOfflinePaymentDetails().get(0).getAmount() == null) {
			throw new CustomException(
					Collections.singletonMap("NO_PAYMENT_AMOUNT_FOUND", "Payment amount should not be empty"));
		}
		PropertyCriteria propertyCriteria = PropertyCriteria.builder().relations(Arrays.asList("owner"))
				.propertyId(propertyFromRequest.getId()).build();

		/**
		 * Retrieve properties from db with the given ids.
		 */
		List<Property> propertiesFromDB = repository.getProperties(propertyCriteria);
		if (CollectionUtils.isEmpty(propertiesFromDB)) {
			throw new CustomException(Collections.singletonMap("PROPERTIES_NOT_FOUND",
					"Could not find any valid properties with id " + propertyFromRequest.getId()));
		}

		Property property = propertiesFromDB.get(0);
		property.getPropertyDetails().setOfflinePaymentDetails(propertyFromRequest.getPropertyDetails().getOfflinePaymentDetails());
		Owner owner = null;
		if (property.getPropertyDetails().getOfflinePaymentDetails().get(0).getPayerName()!=null) {
			owner = utils.getOwnerFromPayerName(property);
		}
		if (null != propertyFromRequest.getPayer() && null != propertyFromRequest.getPayer().getUuid()
				&& !propertyFromRequest.getPayer().getUuid().isEmpty()) {
			owner = property.getPropertyDetails().getOwners().stream()
					.filter(o -> o.getId().equals(propertyFromRequest.getPayer().getUuid())).findFirst().get();
		} else {
			owner = utils.getCurrentOwnerFromProperty(property);
		}

		/**
		 * Create egov user if not already present.
		 */
		userService.createUser(propertyRequest.getRequestInfo(), owner.getOwnerDetails().getMobileNumber(),
				owner.getOwnerDetails().getOwnerName(), property.getTenantId());

		/**
		 * Extract property detail ids.
		 */
		List<String> propertyDetailsIds = propertiesFromDB.stream()
				.map(propertyFromDb -> propertyFromDb.getPropertyDetails().getId()).collect(Collectors.toList());

		if (property.getPropertyDetails().getBranchType().contentEquals(PSConstants.MANI_MAJRA)) {

			List<ManiMajraDemand> demands = repository.getManiMajraDemandDetails(propertyDetailsIds);
			EstateAccount account = repository.getAccountDetailsForPropertyDetailsIds(propertyDetailsIds);

			if (!CollectionUtils.isEmpty(demands) && null != account) {
				List<ManiMajraPayment> payments = repository.getManiMajraPaymentsDetails(propertyDetailsIds);
				maniMajraRentCollectionService.settle(demands, payments, account);
				property.getPropertyDetails()
				.setOfflinePaymentDetails(propertyFromRequest.getPropertyDetails().getOfflinePaymentDetails());
				enrichmentService.enrichMmRentDemand(property);
			}

		} else {

			/**
			 * Generate Calculations for the property.
			 */
			List<EstateDemand> demands = repository.getDemandDetailsForPropertyDetailsIds(propertyDetailsIds);
			EstateAccount account = repository.getAccountDetailsForPropertyDetailsIds(propertyDetailsIds);

			if (!CollectionUtils.isEmpty(demands) && null != account
					&& property.getPropertyDetails().getPaymentConfig() != null
					&& property.getPropertyDetails().getPropertyType().equalsIgnoreCase(PSConstants.ES_PM_LEASEHOLD)) {
				List<EstatePayment> payments = repository.getEstatePaymentsForPropertyDetailsIds(propertyDetailsIds);
				estateRentCollectionService.settle(demands, payments, account, PSConstants.GST_INTEREST_RATE,
						property.getPropertyDetails().getPaymentConfig().getIsIntrestApplicable(),
						property.getPropertyDetails().getPaymentConfig().getRateOfInterest().doubleValue());
				EstateRentSummary rentSummary = estateRentCollectionService.calculateRentSummary(demands, account,
						PSConstants.GST_INTEREST_RATE,
						property.getPropertyDetails().getPaymentConfig().getIsIntrestApplicable(),
						property.getPropertyDetails().getPaymentConfig().getRateOfInterest().doubleValue());
				property.getPropertyDetails()
				.setOfflinePaymentDetails(propertyFromRequest.getPropertyDetails().getOfflinePaymentDetails());
				enrichmentService.enrichRentDemand(property, rentSummary);
			}
		}

		/**
		 * Generate an actual finance demand
		 */
		demandService.generateFinanceRentDemand(propertyRequest.getRequestInfo(), property);

		/**
		 * Get the bill generated.
		 */
		List<BillV2> bills = demandRepository.fetchBill(propertyRequest.getRequestInfo(), property.getTenantId(),
				property.getRentPaymentConsumerCode(), property.getPropertyDetails().getBillingBusinessService());
		if (CollectionUtils.isEmpty(bills)) {
			throw new CustomException("BILL_NOT_GENERATED",
					"No bills were found for the consumer code " + property.getRentPaymentConsumerCode());
		}

		AuditDetails auditDetails = util.getAuditDetails(propertyRequest.getRequestInfo().getUserInfo().getUuid(),
				true);

		if (propertyRequest.getRequestInfo().getUserInfo().getType().equalsIgnoreCase(PSConstants.ROLE_EMPLOYEE)) {
			/**
			 * if offline, create a payment.
			 */
			if (property.getPropertyMasterOrAllotmentOfSite()
					.equalsIgnoreCase(PSConstants.PROPERTY_MASTER)) {
				demandService.createCashPaymentProperty(propertyRequest.getRequestInfo(),
						property.getPropertyDetails().getOfflinePaymentDetails().get(0).getAmount(),
						bills.get(0).getId(), owner, config.getEbPmBusinessServiceValue());
			} else {
				demandService.createCashPaymentProperty(propertyRequest.getRequestInfo(),
						property.getPropertyDetails().getOfflinePaymentDetails().get(0).getAmount(),
						bills.get(0).getId(), owner, config.getAosBusinessServiceValue());
			}

			if (!property.getPropertyDetails().getBranchType().contentEquals(PSConstants.MANI_MAJRA)) {
				OfflinePaymentDetails offlinePaymentDetails = OfflinePaymentDetails.builder()
						.id(UUID.randomUUID().toString()).propertyDetailsId(property.getPropertyDetails().getId())
						.demandId(bills.get(0).getBillDetails().get(0).getDemandId())
						.amount(property.getPropertyDetails().getOfflinePaymentDetails().get(0).getAmount())
						.bankName(property.getPropertyDetails().getOfflinePaymentDetails().get(0).getBankName())
						.transactionNumber(
								property.getPropertyDetails().getOfflinePaymentDetails().get(0).getTransactionNumber())
						.dateOfPayment(
								property.getPropertyDetails().getOfflinePaymentDetails().get(0).getDateOfPayment())
						.auditDetails(auditDetails).build();
				property.getPropertyDetails()
				.setOfflinePaymentDetails(Collections.singletonList(offlinePaymentDetails));
			}

		} 
		if (property.getPropertyDetails().getBranchType().contentEquals(PSConstants.MANI_MAJRA)) {
			OfflinePaymentDetails offlinePaymentDetails = OfflinePaymentDetails.builder()
					.id(UUID.randomUUID().toString()).propertyDetailsId(property.getPropertyDetails().getId())
					.demandId(bills.get(0).getBillDetails().get(0).getDemandId())
					.amount(property.getPropertyDetails().getOfflinePaymentDetails().get(0).getAmount())
					.bankName(property.getPropertyDetails().getOfflinePaymentDetails().get(0).getBankName())
					.transactionNumber(
							property.getPropertyDetails().getOfflinePaymentDetails().get(0).getTransactionNumber())
					.dateOfPayment(
							property.getPropertyDetails().getOfflinePaymentDetails().get(0).getDateOfPayment())
					.fromDate(propertyFromRequest.getPropertyDetails().getOfflinePaymentDetails().get(0)
							.getFromDate())
					.toDate(propertyFromRequest.getPropertyDetails().getOfflinePaymentDetails().get(0).getToDate())
					.auditDetails(auditDetails).build();
			property.getPropertyDetails()
			.setOfflinePaymentDetails(Collections.singletonList(offlinePaymentDetails));
		}

		/**
		 * We return the property along with the consumerCode that we set earlier. Also
		 * save it so the consumer code gets persisted.
		 */
		propertyRequest.setProperties(Collections.singletonList(property));
		producer.push(config.getUpdatePropertyTopic(), propertyRequest);


		return Collections.singletonList(property);
	}

	public void getDueAmount(RequestInfo requestInfo) {
		List<String> states = new ArrayList<>();
		states.add(PSConstants.PM_APPROVED);
		states.add(PSConstants.ES_PM_EB_APPROVED);
		PropertyCriteria criteria = PropertyCriteria.builder().state(states)
				.relations(Arrays.asList(PropertyQueryBuilder.RELATION_OWNER)).build();
		List<Property> properties = repository.getProperties(criteria);
		if (CollectionUtils.isEmpty(properties))
			throw new CustomException("NO_PROPERTY_FOUND", "No approved property found");

		List<PropertyDueAmount> PropertyDueAmounts = new ArrayList<>();
		for(Property property:properties) {
//		properties.stream().forEach(property -> {
			if(property.getFileNumber().equalsIgnoreCase(PSConstants.BB_NOC_DUMMY_FILENO)) {
				continue;
			}
			Optional<OwnerDetails> currentOwnerDetails = property.getPropertyDetails().getOwners().stream()
					.map(owner -> owner.getOwnerDetails())
					.filter(ownerDetail -> ownerDetail.getIsCurrentOwner() == true).findFirst();
			List<Map<String, Object>> propertyTypeConfigurations = mdmsservice.getBranchRoles("propertyType",
					requestInfo, property.getTenantId());

			List<Map<String, Object>> sectorConfigurations = mdmsservice.getBranchRoles("sector", requestInfo,
					property.getTenantId());

			PropertyDueAmount propertyDueAmount = PropertyDueAmount.builder().propertyId(property.getId())
					.fileNumber(property.getFileNumber()).tenantId(property.getTenantId())
					.branchType(property.getPropertyDetails().getBranchType())
					.ownerName(currentOwnerDetails.get().getOwnerName())
					.mobileNumber(currentOwnerDetails.get().getMobileNumber()).build();

			propertyTypeConfigurations.stream()
			.filter(propertyType -> property.getPropertyDetails().getPropertyType()
					.equalsIgnoreCase(propertyType.get("code").toString()))
			.forEach(propertyType -> propertyDueAmount.setPropertyType(propertyType.get("name").toString()));

			sectorConfigurations.stream()
			.filter(sector -> property.getSectorNumber().equalsIgnoreCase(sector.get("code").toString()))
			.forEach(sector -> propertyDueAmount.setSectorNumber(sector.get("name").toString()));

			List<String> propertyDetailsIds = new ArrayList<>();
			propertyDetailsIds.add(property.getPropertyDetails().getId());
			List<EstateDemand> demands = repository.getDemandDetailsForPropertyDetailsIds(propertyDetailsIds);
			EstateAccount estateAccount = repository.getPropertyEstateAccountDetails(propertyDetailsIds);

			if (!CollectionUtils.isEmpty(demands) && property.getPropertyDetails().getPaymentConfig() != null
					&& property.getPropertyDetails().getPropertyType().equalsIgnoreCase(PSConstants.ES_PM_LEASEHOLD)) {
				propertyDueAmount.setEstateRentSummary(estateRentCollectionService.calculateRentSummary(demands,
						estateAccount, PSConstants.GST_INTEREST_RATE,
						property.getPropertyDetails().getPaymentConfig().getIsIntrestApplicable(),
						property.getPropertyDetails().getPaymentConfig().getRateOfInterest().doubleValue()));
			}
			PropertyDueAmounts.add(propertyDueAmount);
		}
		PropertyDueRequest propertyDueRequest = PropertyDueRequest.builder().requestInfo(requestInfo)
				.propertyDueAmounts(PropertyDueAmounts).build();
		producer.push(config.getDueAmountTopic(), propertyDueRequest);
	}

}
