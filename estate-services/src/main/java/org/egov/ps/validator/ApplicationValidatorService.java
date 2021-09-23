package org.egov.ps.validator;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.egov.common.contract.request.RequestInfo;
import org.egov.ps.annotation.ApplicationValidator;
import org.egov.ps.model.Application;
import org.egov.ps.model.ApplicationCriteria;
import org.egov.ps.model.Owner;
import org.egov.ps.model.OwnerDetails;
import org.egov.ps.model.Property;
import org.egov.ps.model.PropertyCriteria;
import org.egov.ps.model.PropertyDetails;
import org.egov.ps.producer.Producer;
import org.egov.ps.repository.ApplicationRepository;
import org.egov.ps.repository.PropertyRepository;
import org.egov.ps.service.MDMSService;
import org.egov.ps.service.PropertyEnrichmentService;
import org.egov.ps.service.calculation.IEstateRentCollectionService;
import org.egov.ps.util.PSConstants;
import org.egov.ps.validator.application.OwnerValidator;
import org.egov.ps.web.contracts.ApplicationRequest;
import org.egov.ps.web.contracts.EstateAccount;
import org.egov.ps.web.contracts.EstateDemand;
import org.egov.ps.web.contracts.EstatePayment;
import org.egov.ps.web.contracts.EstateRentSummary;
import org.egov.ps.web.contracts.PropertyRequest;
import org.egov.tracer.model.CustomException;
import org.json.JSONException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;

import lombok.extern.slf4j.Slf4j;
import net.minidev.json.JSONArray;

@Service
@Slf4j
public class ApplicationValidatorService {

	/**
	 * A map whose keys are annotation names and whose values are the validator
	 * beans.
	 */
	Map<String, IApplicationValidator> validators;

	ApplicationContext context;

	MDMSService mdmsService;

	PropertyRepository propertyRepository;

	ObjectMapper objectMapper;

	@Autowired
	OwnerValidator ownerValidator;

	@Autowired
	ApplicationRepository applicationRepository;

	@Autowired
	private IEstateRentCollectionService estateRentCollectionService;

	@Autowired
	PropertyValidator propertyValidator;

	@Autowired
	private PropertyEnrichmentService enrichmentService;

	@Autowired
	private Producer producer;

	@Autowired
	PropertyRepository repository;

	@Autowired
	private org.egov.ps.config.Configuration config;

	@Autowired
	ApplicationValidatorService(ApplicationContext context, MDMSService mdmsService,
			PropertyRepository propertyRepository, ObjectMapper objectMapper, OwnerValidator ownerValidator) {
		this.context = context;
		this.mdmsService = mdmsService;
		this.propertyRepository = propertyRepository;
		this.objectMapper = objectMapper;
		this.ownerValidator = ownerValidator;
		Map<String, Object> beans = this.context.getBeansWithAnnotation(ApplicationValidator.class);

		/**
		 * Construct validators object by reading annotations from beans and discarding
		 * all beans that does not implement IApplicationValidator.
		 */
		this.validators = beans.entrySet().stream().filter(entry -> entry.getValue() instanceof IApplicationValidator)
				.collect(Collectors.toMap(e -> {
					ApplicationValidator annotation = this.context.findAnnotationOnBean(e.getKey(),
							ApplicationValidator.class);
					return annotation.value();
				}, e -> (IApplicationValidator) e.getValue()));
	}

	public void validateCreateRequest(ApplicationRequest request) {
		List<Application> applications = request.getApplications();
		for (Application application : applications) {
			if(application.getBranchType().equalsIgnoreCase(PSConstants.APPLICATION_BUILDING_BRANCH) && application.getApplicationType().equalsIgnoreCase(PSConstants.NOC) && application.getProperty()== null) {
				saveProperty(application,request.getRequestInfo());
			}
			else {
				String propertyId = application.getProperty().getId();
				validatePropertyExists(request.getRequestInfo(), propertyId, application);
				JsonNode applicationDetails = application.getApplicationDetails();
				if (application.getModuleType().equalsIgnoreCase(PSConstants.OWNERSHIP_TRANSFER)) {
					validateSharePercentage(application, request.getRequestInfo());
				}
				try {
					String applicationDetailsString = this.objectMapper.writeValueAsString(applicationDetails);
					Configuration conf = Configuration.defaultConfiguration().addOptions(Option.SUPPRESS_EXCEPTIONS);
					DocumentContext applicationObjectContext = JsonPath.using(conf).parse(applicationDetailsString);
					Map<String, List<String>> errorMap;
					errorMap = this.performValidationsFromMDMS(application.getMDMSModuleName(), applicationObjectContext,
							request.getRequestInfo(), application.getTenantId(), propertyId);

					if (!errorMap.isEmpty()) {
						throw new CustomException("INVALID_FIELDS", "Please enter the valid fields " + errorMap.toString());
					}
				} catch (JsonProcessingException e) {
					log.error("Can not parse Json fie", e);
				} catch (Exception e) {
					log.error("Exception", e);
				}
			}
		}
	}

	private void saveProperty(Application application, RequestInfo requestInfo) {
		PropertyDetails  newPropertyDetails = PropertyDetails.builder()
				.branchType(PSConstants.BUILDING_BRANCH)
				.houseNumber(application.getApplicationDetails().get("property").get("propertyDetails").get("houseNumber").asText())
				.village(application.getApplicationDetails().get("property").get("propertyDetails").get("village").asText())
				.mohalla(application.getApplicationDetails().get("property").get("propertyDetails").get("mohalla").asText())
				.owners(new ArrayList<Owner>())
				.areaSqft(application.getApplicationDetails().get("property").get("propertyDetails").get("areaSqft")!=null?BigDecimal.valueOf(application.getApplicationDetails().get("property").get("propertyDetails").get("areaSqft").asDouble()) :BigDecimal.ZERO)
				.build();
		Property newProperty = Property.builder().propertyMasterOrAllotmentOfSite("PROPERTY_MASTER").fileNumber(application.getApplicationDetails().get("property").get("fileNumber").asText())
				.sectorNumber(application.getApplicationDetails().get("property").get("sectorNumber").asText())
				.category(application.getApplicationDetails().get("property").get("category").asText())
				.subCategory(application.getApplicationDetails().get("property").get("subCategory")==null?"":application.getApplicationDetails().get("property").get("subCategory").asText())
				.action("")
				.siteNumber(application.getApplicationDetails().get("property").get("siteNumber")==null?null:application.getApplicationDetails().get("property").get("siteNumber").asText())
				.propertyDetails(newPropertyDetails)
				.tenantId(application.getTenantId())
				.action(PSConstants.ES_APPROVE)
				.build();
		PropertyRequest propertyRequest = PropertyRequest.builder().requestInfo(requestInfo).properties(Collections.singletonList(newProperty)).build();

		propertyValidator.validateCreateRequest(propertyRequest);
		enrichmentService.enrichPropertyRequest(propertyRequest);
		propertyRequest.getProperties().get(0).setState(PSConstants.ES_APPROVED);
		producer.push(config.getSavePropertyTopic(), propertyRequest);

		PropertyCriteria criteria = PropertyCriteria.builder().fileNumber(application.getApplicationDetails().get("property").get("fileNumber").asText().toUpperCase()).limit(1l).build();
		List<Property> properties = repository.getProperties(criteria);
		if(!properties.isEmpty() && properties!=null)
			application.setProperty(properties.get(0));
		else
			application.setProperty(propertyRequest.getProperties().get(0));

	}

	private void validateSharePercentage(Application application, RequestInfo requestInfo) {
		PropertyCriteria propertySearchCriteria = PropertyCriteria.builder()
				.propertyId(application.getProperty().getId()).build();
		List<Property> properties = propertyRepository.getProperties(propertySearchCriteria);

		properties.forEach(property -> {
			property.getPropertyDetails().getOwners().forEach(ownerFromDb -> {
				JsonNode transferor = (application.getApplicationDetails().get("transferor") != null)
						? application.getApplicationDetails().get("transferor")
								: application.getApplicationDetails().get("owner");
						if (ownerFromDb.getId().equals(transferor.get("id").asText())) {
							JsonNode transferee = application.getApplicationDetails().get("transferee");
							double percentageTransfered = transferee.get("percentageOfShareTransferred").asDouble();
							if (ownerFromDb.getShare() < percentageTransfered) {
								throw new CustomException("INVALID_SHARE", String.format("Your current share is %.0f"
										+ "%% and you can't transfer more than which you have", ownerFromDb.getShare()));
							}
						}
			});

		});
	}

	private void validatePropertyExists(RequestInfo requestInfo, String propertyId, Application application) {
		Property property = propertyRepository.findPropertyById(propertyId);
		if (property == null) {
			throw new CustomException("INVALID_PROPERTY", "Could not find property with the given id:" + propertyId);
		}

		List<String> propertyDetailsIds = new ArrayList<>();
		propertyDetailsIds.add(property.getPropertyDetails().getId());
		property.getPropertyDetails()
		.setEstateAccount(propertyRepository.getPropertyEstateAccountDetails(propertyDetailsIds));

		if (!property.getState().contentEquals(PSConstants.PM_APPROVED)
				&& !property.getState().contentEquals(PSConstants.ES_PM_EB_APPROVED)
				&& !property.getState().contentEquals(PSConstants.ES_APPROVED)
				&& !property.getState().contentEquals(PSConstants.ES_PM_MM_APPROVED)) {
			throw new CustomException("INVALID_PROPERTY", "Property with the given " + propertyId + " is not approved");
		}
		Double rentDue = 0.0;
		if (requestInfo.getUserInfo().getType().equalsIgnoreCase(PSConstants.ROLE_CITIZEN) && property.getPropertyDetails().getEstateAccount() != null
				&& property.getPropertyDetails().getPaymentConfig() != null
				&& property.getPropertyDetails().getEstateDemands() != null
				&& property.getPropertyDetails().getPropertyType().equalsIgnoreCase(PSConstants.ES_PM_LEASEHOLD)) {
			rentDue = getRentDue(property);
		}
		if (rentDue > 0) {
			throw new CustomException("PROPERTY_PENDING_DUE", String.format(
					"Property has pending due of Rs:%.2f, so you can not apply for %s, please clear the due before applying.",
					rentDue, application.getApplicationType()));
		}
	}

	private Double getRentDue(Property property) {
		List<String> propertyDetailsIds = new ArrayList<String>();
		propertyDetailsIds.add(property.getPropertyDetails().getId());
		List<EstateDemand> demands = propertyRepository.getDemandDetailsForPropertyDetailsIds(propertyDetailsIds);
		EstateAccount estateAccount = propertyRepository.getPropertyEstateAccountDetails(propertyDetailsIds);
		List<EstatePayment> payments = propertyRepository.getEstatePaymentsForPropertyDetailsIds(propertyDetailsIds);
		estateRentCollectionService.settle(demands, payments, estateAccount, PSConstants.GST_INTEREST_RATE,
				property.getPropertyDetails().getPaymentConfig().getIsIntrestApplicable(),
				property.getPropertyDetails().getPaymentConfig().getRateOfInterest().doubleValue());
		EstateRentSummary estateRentSummary = estateRentCollectionService.calculateRentSummary(demands, estateAccount,
				PSConstants.GST_INTEREST_RATE,
				property.getPropertyDetails().getPaymentConfig().getIsIntrestApplicable(),
				property.getPropertyDetails().getPaymentConfig().getRateOfInterest().doubleValue());
		Double rentDue = estateRentSummary.getBalanceRent() + estateRentSummary.getBalanceGST()
		+ estateRentSummary.getBalanceGSTPenalty() + estateRentSummary.getBalanceRentPenalty();
		return rentDue;
	}

	@SuppressWarnings("unchecked")
	public Map<String, List<String>> performValidationsFromMDMS(final String applicationType,
			DocumentContext applicationObject, RequestInfo RequestInfo, final String tenantId, final String propertyId)
					throws JSONException {
		List<Map<String, Object>> fieldConfigurations = this.mdmsService.getApplicationConfig(applicationType,
				RequestInfo, tenantId);
		Map<String, List<String>> errorsMap = new HashMap<String, List<String>>();
		for (int i = 0; i < fieldConfigurations.size(); i++) {
			Map<String, Object> fieldConfigMap = fieldConfigurations.get(i);

			/**
			 * Get field path from fieldConfig.
			 */
			String path = (String) fieldConfigMap.get("path");

			/**
			 * Get Value from applicationObject.
			 */
			Object value = applicationObject.read(path);
			List<Map<String, Object>> validationObjects = (List<Map<String, Object>>) fieldConfigMap.get("validations");

			/**
			 * Build IValidation object by reading from field configuration.
			 */
			List<IValidation> validations = validationObjects.stream()
					.map(validationObject -> ApplicationValidation.builder().type((String) validationObject.get("type"))
							.errorMessageFormat((String) validationObject.get("errorMessageFormat"))
							.params((Map<String, Object>) validationObject.get("params")).build())
					.collect(Collectors.toList());

			if (path.contains("..") && value instanceof JSONArray) {
				JSONArray valueArray = (JSONArray) value;
				valueArray.forEach(val -> {
					/**
					 * Construct the ApplicationField object.
					 */
					IApplicationField field = ApplicationField.builder().path(path)
							.required((boolean) fieldConfigMap.get("required")).rootObject(applicationObject).value(val)
							.validations(validations).build();

					/**
					 * Perform validations.
					 */
					List<String> errorMessages = this.performFieldValidations(applicationObject, field, propertyId);
					if (!CollectionUtils.isEmpty(errorMessages)) {
						errorsMap.put(path, errorMessages);
					}
				});

			} else {
				/**
				 * Construct the ApplicationField object.
				 */
				IApplicationField field = ApplicationField.builder().path(path)
						.required((boolean) fieldConfigMap.get("required")).rootObject(applicationObject).value(value)
						.validations(validations).build();

				/**
				 * Perform validations.
				 */
				List<String> errorMessages = this.performFieldValidations(applicationObject, field, propertyId);
				if (!CollectionUtils.isEmpty(errorMessages)) {
					errorsMap.put(path, errorMessages);
				}
			}
		}
		return errorsMap;
	}

	private static final String TYPE_REQUIRED = "required";

	private List<String> performFieldValidations(DocumentContext applicationObject, IApplicationField field,
			String propertyId) {

		Object value = field.getValue();

		/**
		 * Perform required validator validation first.
		 */
		IApplicationValidator requiredValidator = validators.get(TYPE_REQUIRED);
		List<String> requiredValidationErrors = requiredValidator
				.validate(ApplicationValidation.builder().type(TYPE_REQUIRED).build(), field, value, applicationObject);
		boolean isFieldEmpty = requiredValidationErrors != null && !requiredValidationErrors.isEmpty();

		if (field.isRequired() && isFieldEmpty) {
			log.debug("{} validator failed validating {} for path {}", TYPE_REQUIRED, value, field.getPath());
			return requiredValidationErrors;
		} else if (isFieldEmpty) {
			// field is not required and is empty. No validations to perform.
			return null;
		}

		/**
		 * Perform other validations and combine all the indivial error messages into a
		 * single list.
		 */
		return field.getValidations().stream().map(validation -> {
			if (validation.getType().equalsIgnoreCase("owner")) {
				return ownerValidator.validateOwner(propertyId, (String) value);
			}
			IApplicationValidator validator = validators.get(validation.getType());
			if (validator == null) {
				log.error("No validator found for {} for path {}", validation.getType(), field.getPath());
				return null;
			}
			log.debug("{} validator validating {} for path {}", validation.getType(), value, field.getPath());
			return validator.validate(validation, field, value, applicationObject);
		}).filter(validationErrors -> validationErrors != null && !validationErrors.isEmpty())
				.reduce(new ArrayList<String>(), (a, b) -> {
					a.addAll(b);
					return a;
				});
	}

	public void validateUpdateRequest(ApplicationRequest applicationRequest) {
		for (Application application : applicationRequest.getApplications()) {
			if(application.getBranchType().equalsIgnoreCase(PSConstants.APPLICATION_BUILDING_BRANCH)
					&& application.getApplicationType().equalsIgnoreCase(PSConstants.NOC)) {
				Property property = propertyRepository.findPropertyById(application.getProperty().getId());
				if(property==null) {
					saveProperty(application,applicationRequest.getRequestInfo());
				}
			}

			validateApplicationIdExistsInDB(application.getId());

			if (application.getApplicationType().contains(PSConstants.APPLICATION_TYPE_NDC)
					&& application.getState().contains(PSConstants.PENDING_SO_APPROVAL)) {

				Property property = propertyRepository.findPropertyById(application.getProperty().getId());
				Double rentDue = 0.0;
				if (!CollectionUtils.isEmpty(property.getPropertyDetails().getEstatePayments())
						&& property.getPropertyDetails().getEstateAccount() != null
						&& property.getPropertyDetails().getPaymentConfig() != null && property.getPropertyDetails()
						.getPropertyType().equalsIgnoreCase(PSConstants.ES_PM_LEASEHOLD)) {
					rentDue = getRentDue(property);
				}

				if (rentDue > 0) {
					throw new CustomException("PROPERTY_RENT_DUE",
							String.format("Property has rent due: %s, so can not approve for NDC", rentDue));
				}
			}
		}
	}

	private void validateApplicationIdExistsInDB(String applicationId) {
		ApplicationCriteria criteria = ApplicationCriteria.builder().applicationId(applicationId).build();
		List<Application> applications = applicationRepository.getApplications(criteria);
		if (CollectionUtils.isEmpty(applications)) {
			log.warn("The application id to be updated does not exist {}", applicationId);
			throw new CustomException("APPLICATION NOT FOUND",
					"The application id to be updated does not exist " + applicationId);
		}
	}

	public void updateNOCPropertyDetails(Application application, RequestInfo requestInfo) {
		Property property = propertyRepository.findPropertyById(application.getProperty().getId());
		if(property==null) {
			saveProperty(application,requestInfo);
			property = application.getProperty();
		}
		Set<org.egov.ps.model.Document> ownerDocs= new HashSet<>();
		application.getApplicationDocuments().forEach(doc->{
			ownerDocs.add(doc);
		});

		if(ownerDocs.size()>=8 && (property.getPropertyDetails().getOwners()==null || property.getPropertyDetails().getOwners().isEmpty())) {
			OwnerDetails  newPropertyOwnerDetails = OwnerDetails.builder()
					.ownerName(application.getApplicationDetails().get("owner").get("name").asText())
					.guardianName(application.getApplicationDetails().get("owner").get("ownerDetails").get("guardianName").asText())
					.guardianRelation(application.getApplicationDetails().get("owner").get("ownerDetails").get("relation").asText())
					.address(application.getApplicationDetails().get("owner").get("ownerDetails").get("address").asText())
					.mobileNumber(application.getApplicationDetails().get("owner").get("ownerDetails").get("mobileNumber").asText())
					.isCurrentOwner(application.getApplicationDetails().get("owner").get("isCurrentOwner").asBoolean())
					.possesionDate(application.getApplicationDetails().get("owner").get("possessionDate")==null?null:application.getApplicationDetails().get("owner").get("possessionDate").asLong())
					.build();

			ownerDocs.forEach(ownDoc->{
				ownDoc.setId(null);
				ownDoc.setReferenceId(null);
			});

			List<org.egov.ps.model.Document> ownerDocList = new ArrayList<org.egov.ps.model.Document>();
			ownerDocList.addAll(ownerDocs);
			newPropertyOwnerDetails.setOwnerDocuments(ownerDocList);
			Owner newPropertyOwner = Owner.builder().share(application.getApplicationDetails().get("owner").get("share").asDouble())
					.ownerDetails(newPropertyOwnerDetails)
					.build();
			property.getPropertyDetails().setOwners(Collections.singletonList(newPropertyOwner));

			PropertyRequest propertyRequest = PropertyRequest.builder().requestInfo(requestInfo).properties(Collections.singletonList(property)).build();

			propertyValidator.validateUpdateRequest(propertyRequest);
			enrichmentService.enrichPropertyRequest(propertyRequest);
			producer.push(config.getUpdatePropertyTopic(), propertyRequest);
			Property updatedProperty = propertyRepository.findPropertyById(application.getProperty().getId());
			application.setProperty(updatedProperty);

		}
	}
}
