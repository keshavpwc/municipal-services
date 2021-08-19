package org.egov.ps.service;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jayway.jsonpath.JsonPath;

import org.egov.common.contract.request.RequestInfo;
import org.egov.common.contract.request.User;
import org.egov.ps.model.Application;
import org.egov.ps.model.Notifications;
import org.egov.ps.model.NotificationsEmail;
import org.egov.ps.model.NotificationsEvent;
import org.egov.ps.model.NotificationsSms;
import org.egov.ps.model.notification.uservevents.Event;
import org.egov.ps.model.notification.uservevents.EventRequest;
import org.egov.ps.repository.PropertyRepository;
import org.egov.ps.util.PSConstants;
import org.egov.ps.util.Util;
import org.egov.ps.web.contracts.ApplicationRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ApplicationsNotificationService {

	@Autowired
	private MDMSService mdmsservice;

	@Autowired
	private NotificationService notificationService;

	@Autowired
	private UserService userService;

	@Autowired
	private LocalisationService localisationService;

	@Autowired
	PropertyRepository repository;

	@Autowired
	Util util;

	@Autowired
	private ApplicationEnrichmentService applicationEnrichmentService;

	/**
	 * Invoke process notification on each application in the request
	 */
	public void processNotifications(ApplicationRequest request) {
		request.getApplications().forEach(application -> {
			try {
				/**
				 * Get the notification config from mdms.
				 */
				List<Map<String, Object>> notificationConfigs = mdmsservice.getNotificationConfig(
						application.getMDMSModuleName(), request.getRequestInfo(), application.getTenantId());
				/**
				 * Process the notification config
				 */
				this.processNotification(notificationConfigs, application, request.getRequestInfo());
			} catch (Exception e) {
				log.error("Exception while fetching notification config for application no '{}' '{}'",
						application.getApplicationNumber(), e);
			}

		});
	}

	public void processNotification(List<Map<String, Object>> rawNotificationsList, Application application,
			RequestInfo requestInfo) {
		if (CollectionUtils.isEmpty(rawNotificationsList)) {
			log.debug("No notifications configured in MDMS for application no {} for state {}",
					application.getApplicationNumber(), application.getState());
			return;
		}

		/**
		 * Deserialize config.
		 */
		ObjectMapper mapper = new ObjectMapper();
		List<Notifications> notificationList = mapper.convertValue(rawNotificationsList,
				new TypeReference<List<Notifications>>() {
		});

		/**
		 * Filter notification object relevant to current state of application.
		 */
		Optional<Notifications> notificationOptional = notificationList.stream()
				.filter(x -> x.getState().equalsIgnoreCase(application.getState())).findAny();
		if (!notificationOptional.isPresent()) {
			log.debug("No notification configured for application no {} for state {}",
					application.getApplicationNumber(), application.getState());
			return;
		}

		/**
		 * Start Processing.
		 */
		Notifications notification = notificationOptional.get();

		try {
			/**
			 * Enrich created by for application.
			 */
			String creatorUUID = application.getAuditDetails().getCreatedBy();
			User createdBy = userService.getUserByUUID(creatorUUID, requestInfo);
			application.setCreatedBy(createdBy);

			List<Map<String, Object>> feesConfigurations = mdmsservice
					.getApplicationFees(application.getMDMSModuleName(), requestInfo, application.getTenantId());
			/**
			 * Enrich total due amount details
			 */
			if(application.getBranchType().contentEquals(PSConstants.APPLICATION_BUILDING_BRANCH)
					&& application.getApplicationType().contentEquals(PSConstants.NOC)) {
				JsonNode applicationDetails = application.getApplicationDetails();
				BigDecimal developmentCharges = BigDecimal.ZERO;
				if (null != applicationDetails.get("developmentCharges")) {
					 developmentCharges = new BigDecimal(applicationDetails.get("developmentCharges").asText());
				}
				BigDecimal conversionCharges = BigDecimal.ZERO;
				if (null != applicationDetails.get("conversionCharges")) {
					conversionCharges = new BigDecimal(applicationDetails.get("conversionCharges").asText());
				}

				BigDecimal scrutinyCharges = BigDecimal.ZERO;

				if (null != applicationDetails.get("scrutinyCharges")) {
					scrutinyCharges = new BigDecimal(applicationDetails.get("scrutinyCharges").asText());
				}
				BigDecimal transferFee = BigDecimal.ZERO;
				if (null != applicationDetails.get("transferFee")) {
					transferFee = new BigDecimal(applicationDetails.get("transferFee").asText());
				}

				BigDecimal applicationNumberCharges = BigDecimal.ZERO;
				if (null != applicationDetails.get("applicationNumberCharges")) {
					applicationNumberCharges = new BigDecimal(
							applicationDetails.get("applicationNumberCharges").asText());
				}
				application.setTotalDue(developmentCharges.add(conversionCharges).add(scrutinyCharges).add(transferFee).add(applicationNumberCharges));
			}
			else if (application.getBranchType().equalsIgnoreCase(PSConstants.APPLICATION_ESTATE_BRANCH)) {
				JsonNode applicationDetails = application.getApplicationDetails();
				BigDecimal transferCharges= BigDecimal.ZERO,gst= BigDecimal.ZERO,applciationFee= BigDecimal.ZERO,inspectionFee= BigDecimal.ZERO,
						securityFee= BigDecimal.ZERO,extensionFee= BigDecimal.ZERO,allotmentFee= BigDecimal.ZERO,documentCopyingFee= BigDecimal.ZERO,conversionFee= BigDecimal.ZERO,propertyTransferCharge = BigDecimal.ZERO;

				//Transfer Charges
				if(!applicationDetails.get("transferCharges").isNull() && applicationDetails.get("transferCharges").asInt()>0) {
					transferCharges = new BigDecimal(applicationDetails.get("transferCharges").toString());
				}

				//GST
				if(!applicationDetails.get("GST").isNull() && applicationDetails.get("GST").asInt()>0) {
					gst = new BigDecimal(applicationDetails.get("GST").toString());
				}

				//Processing fees/Application Fee
				if(!applicationDetails.get("applicationFee").isNull() && applicationDetails.get("applicationFee").asInt()>0) {
					applciationFee = new BigDecimal(applicationDetails.get("applicationFee").toString());
				}

				//Inspection Fees
				if(!applicationDetails.get("inspectionFee").isNull() && applicationDetails.get("inspectionFee").asInt()>0) {
					inspectionFee = new BigDecimal(applicationDetails.get("inspectionFee").toString());
				}

				//EMD/Security
				if(!applicationDetails.get("securityFee").isNull() && applicationDetails.get("securityFee").asInt()>0) {
					securityFee = new BigDecimal(applicationDetails.get("securityFee").toString());
				}

				//Extension fees
				if(!applicationDetails.get("extensionFee").isNull() && applicationDetails.get("extensionFee").asInt()>0) {
					extensionFee = new BigDecimal(applicationDetails.get("extensionFee").toString());
				}

				//Certificate/Document Copying Fees
				if(!applicationDetails.get("DocumentCopyingFee").isNull() && applicationDetails.get("DocumentCopyingFee").asInt()>0) {
					documentCopyingFee = new BigDecimal(applicationDetails.get("DocumentCopyingFee").toString());
				}

				//Allotment Fees
				if(!applicationDetails.get("allotmentFee").isNull() && applicationDetails.get("allotmentFee").asInt()>0) {
					allotmentFee = new BigDecimal(applicationDetails.get("allotmentFee").toString());
				}

				//Conversion fees
				if(!applicationDetails.get("conversionFee").isNull() && applicationDetails.get("conversionFee").asInt()>0) {
					conversionFee = new BigDecimal(applicationDetails.get("conversionFee").toString());
				}

				//Property Transfer charges
				if(!applicationDetails.get("propertyTransferCharge").isNull() && applicationDetails.get("propertyTransferCharge").asInt()>0) {
					propertyTransferCharge = new BigDecimal(applicationDetails.get("propertyTransferCharge").toString());
				}

				application.setTotalDue(transferCharges.add(gst).add(applciationFee).add(inspectionFee).add(securityFee).add(extensionFee).add(allotmentFee).add(documentCopyingFee).add(conversionFee).add(propertyTransferCharge));
			}

			else {
				BigDecimal estimateAmount = applicationEnrichmentService.fetchEstimateAmountFromMDMSJson(feesConfigurations, application);
				BigDecimal gstEstimatePercentage = applicationEnrichmentService.feesGSTOfApplication(application, requestInfo);
				BigDecimal gstEstimateAmount = (estimateAmount.multiply(gstEstimatePercentage)).divide(new BigDecimal(100));
				application.setTotalDue(estimateAmount.add(gstEstimateAmount));
			}
			/**
			 * Enrich hearing date
			 */
			ObjectNode applicationDetails = (ObjectNode) application.getApplicationDetails();
			if(applicationDetails.get("dateOfHearing")!=null) {
				Date date = new Date(applicationDetails.get("dateOfHearing").asLong());
				DateFormat f = new SimpleDateFormat("dd MMM yyyy");
				String hearingDate = f.format(date);
				applicationDetails.put("dateOfHearing",hearingDate);
			}

			/**
			 * Enrich content by replacing paths like {createdBy.name}
			 */
			String applicationJsonString = mapper.writeValueAsString(application);
			String contentWithPathsEnriched = enrichPathPatternsWithApplication(notification.getContent(),
					applicationJsonString);
			String enrichedContent = enrichLocalizationPatternsInString(application, requestInfo,
					contentWithPathsEnriched);


			if(application.getBranchType().contentEquals(PSConstants.APPLICATION_BUILDING_BRANCH)
					&& application.getApplicationType().contentEquals(PSConstants.NOC) 
					&& application.getProperty().getFileNumber().equalsIgnoreCase(PSConstants.BB_NOC_DUMMY_FILENO) ) {
				if(enrichedContent.contains(PSConstants.BB_NOC_DUMMY_FILENO)) {
					enrichedContent = enrichedContent.replace(PSConstants.BB_NOC_DUMMY_FILENO, application.getApplicationDetails().get("property").get("fileNumber").asText());
				}
			}
			log.debug("final enriched content : "+enrichedContent);
			/**
			 * Send email
			 */
			NotificationsEmail emailConfig = notification.getModes().getEmail();
			if (emailConfig.isEnabled()) {
				if (emailConfig.isValid()) {
					String email = enrichPathPatternsWithApplication(emailConfig.getTo(), applicationJsonString);
					String subject = enrichPathPatternsWithApplication(emailConfig.getSubject(), applicationJsonString);
					enrichedContent=enrichedContent.concat(PSConstants.MAIL_SIGNATURE).replace("\n", "<br/>");
					log.debug("email enriched content : "+enrichedContent);
					this.notificationService.sendEmail(email, subject, enrichedContent);
				} else {
					log.warn("Notifications Invalid email config found {}", emailConfig);
				}
			}

			/**
			 * Send SMS
			 */
			NotificationsSms smsConfig = notification.getModes().getSms();
			if (smsConfig.isEnabled()) {
				if (smsConfig.isValid()) {
					String mobileNumber = enrichPathPatternsWithApplication(smsConfig.getTo(), applicationJsonString);
					this.notificationService.sendSMS(mobileNumber, enrichedContent);
				} else {
					log.warn("Notifications Invalid sms config found {}", smsConfig);
				}
			}
			/**
			 * Web notification
			 */
			NotificationsEvent eventConfig = notification.getModes().getEvent();
			if(eventConfig.isEnabled() && eventConfig.isValid()) {
				EventRequest eventRequest = getEventsForApplication(enrichedContent,requestInfo,application,eventConfig,applicationJsonString);
				if(null != eventRequest)
					util.sendEventNotification(eventRequest);
			}

		} catch (Exception e) {
			log.error("Could not convert enrichedApplication to JSON", e);
		}
	}

	private String enrichPathPatternsWithApplication(String sourceString, String applicationJsonString) {
		Pattern p = Pattern.compile("\\{(.*?)\\}");
		Matcher m = p.matcher(sourceString);
		Set<String> allMatches = new HashSet<String>();
		while (m.find()) {
			allMatches.add(m.group());
		}

		String replacedString = allMatches.stream().reduce(sourceString, (result, match) -> {
			String path = match.substring(1, match.length() - 1);
			Object value = (JsonPath.read(applicationJsonString, path));
			return result.replaceAll(String.format("\\{%s\\}", path), "" + value);
		});
		log.debug("Enriched '{}' to '{}' ", sourceString, replacedString);
		return replacedString;
	}

	private String enrichLocalizationPatternsInString(Application application, RequestInfo requestInfo,
			String sourceString) {
		String tenantId = this.util.getStateLevelTenantId(application.getTenantId());
		String locale = PSConstants.LOCALIZATION_LOCALE;
		List<String> listOfStringForLocalisation = localisationStringList(sourceString);

		Map<String, String> messageMap = localisationService.getAllLocalisedMessages(requestInfo, tenantId, locale,
				PSConstants.LOCALIZATION_MODULE);

		String replacedString = listOfStringForLocalisation.stream().reduce(sourceString, (result, match) -> {
			String replacement = messageMap.get(match.toUpperCase());
			if (replacement == null) {
				replacement = match;
			}
			return result.replaceAll(String.format("\\[%s\\]", match), "" + replacement);
		});
		log.debug("Original String:" + sourceString + "\n" + "Post replaced final localised string is: "
				+ replacedString);
		return replacedString;
	}

	private List<String> localisationStringList(String str) {
		Pattern pattern = Pattern.compile("\\[(.*?)\\]");
		Matcher m = pattern.matcher(str);
		List<String> list = new ArrayList<String>();

		while (m.find()) {
			list.add(m.group(1));
		}
		return list;
	}

	public EventRequest getEventsForApplication(String message,RequestInfo requestInfo, Application application, NotificationsEvent eventConfig, String applicationJsonString) {
		List<Event> events = new ArrayList<>();
		if(message == null) return null;
		String mobileNumber = enrichPathPatternsWithApplication(eventConfig.getTo(), applicationJsonString);
		String uuid = enrichPathPatternsWithApplication(eventConfig.getTo(), applicationJsonString);
		events = util.createEvent(message,mobileNumber,uuid,requestInfo,application.getTenantId(),application.getState(),application.getApplicationNumber(),eventConfig.isPayLink());
		if(!CollectionUtils.isEmpty(events)) {
			return EventRequest.builder().requestInfo(requestInfo).events(events).build();
		}else {
			return null;
		}
	}

}