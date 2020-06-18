package org.egov.prscp.repository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.egov.prscp.config.PrScpConfiguration;
import org.egov.prscp.producer.Producer;
import org.egov.prscp.repository.builder.PrQueryBuilder;
import org.egov.prscp.repository.rowmapper.EventDetailRowMapper;
import org.egov.prscp.util.ErrorConstants;
import org.egov.prscp.web.models.EventDetail;
import org.egov.prscp.web.models.NotificationReceiver;
import org.egov.prscp.web.models.RequestInfoWrapper;
import org.egov.prscp.web.models.TenderNotice;
import org.egov.tracer.model.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Repository
public class EventManegementRepository {

	private JdbcTemplate jdbcTemplate;

	private EventDetailRowMapper eventRowMapper;

	private Producer producer;

	private PrScpConfiguration config;

	@Autowired
	public EventManegementRepository(JdbcTemplate jdbcTemplate, EventDetailRowMapper eventRowMapper, Producer producer,
			PrScpConfiguration config) {
		this.jdbcTemplate = jdbcTemplate;
		this.eventRowMapper = eventRowMapper;
		this.producer = producer;
		this.config = config;
	}

	public List<EventDetail> getEvent(EventDetail eventDetail) {

		List<Object> preparedStmtList = new ArrayList<>();
		preparedStmtList.add((eventDetail.getTenantId() == null ? "" : eventDetail.getTenantId()));
		preparedStmtList.add((eventDetail.getModuleCode() == null ? "" : eventDetail.getModuleCode()));
		preparedStmtList.add((eventDetail.getStatus() == null ? "" : eventDetail.getStatus()));
		preparedStmtList.add((eventDetail.getStatus() == null ? "" : eventDetail.getStatus()));
		preparedStmtList.add((eventDetail.getEventStatus() == null ? "" : eventDetail.getEventStatus()));
		preparedStmtList.add((eventDetail.getEventStatus() == null ? "" : eventDetail.getEventStatus()));
		preparedStmtList.add((eventDetail.getEventTitle() == null ? "" : eventDetail.getEventTitle()));
		preparedStmtList.add((eventDetail.getEventTitle() == null ? "" : eventDetail.getEventTitle()));
		preparedStmtList.add((eventDetail.getEventId() == null ? "" : eventDetail.getEventId()));
		preparedStmtList.add((eventDetail.getEventId() == null ? "" : eventDetail.getEventId()));
		preparedStmtList.add((eventDetail.getEventDetailUuid() == null ? "" : eventDetail.getEventDetailUuid()));
		preparedStmtList.add((eventDetail.getEventDetailUuid() == null ? "" : eventDetail.getEventDetailUuid()));
		preparedStmtList.add((eventDetail.getStartDate() == null ? "" : eventDetail.getStartDate()));
		preparedStmtList.add((eventDetail.getStartDate() == null ? "" : eventDetail.getStartDate()));
		preparedStmtList.add((eventDetail.getEndDate() == null ? "" : eventDetail.getEndDate()));
		preparedStmtList.add((eventDetail.getEndDate() == null ? "" : eventDetail.getEndDate()));

		LocalDate today = LocalDate.now();
		LocalDate periodDate = today
				.minusDays(Integer.parseInt(config.getPeriodEvents() == null ? "0" : config.getPeriodEvents()));
		preparedStmtList.add((!eventDetail.isDefaultGrid() ? "" : periodDate.toString()));
		preparedStmtList.add((!eventDetail.isDefaultGrid() ? "" : periodDate.toString()));

		return jdbcTemplate.query(PrQueryBuilder.Event_QUERY, preparedStmtList.toArray(), eventRowMapper);
	}

	public List<EventDetail> getEventReminder(String status) {
		List<Object> preparedStmtList = new ArrayList<>();
		preparedStmtList.add(status);
		return jdbcTemplate.query(PrQueryBuilder.Event_Details_Reminder_QUERY, preparedStmtList.toArray(),
				eventRowMapper);
	}

	public void createEvent(EventDetail eventDetail) {
		RequestInfoWrapper infoWrapper = RequestInfoWrapper.builder().requestBody(eventDetail).build();
		producer.push(config.getSaveEventTopic(), infoWrapper);
	}

	public void updateEvent(EventDetail eventDetail) {
		RequestInfoWrapper infoWrapper = RequestInfoWrapper.builder().requestBody(eventDetail).build();
		producer.push(config.getUpdateEventDetailsTopic(), infoWrapper);
	}

	public void sendEventUpdateNotification(NotificationReceiver notificationReceiver) {
		producer.push(config.getInvitationSendTopic(), notificationReceiver);
	}

	public void updateEventStatus(RequestInfoWrapper infoWrapper) {
		producer.push(config.getUpdateEventStatusTopic(), infoWrapper);
	}
	
	public void checkEventExist(EventDetail eventDetail){
		Map<String, String> errorMap = new HashMap<>();
		int i= jdbcTemplate.queryForObject(PrQueryBuilder.GET_EVENT_EXIST_QUERY,
				new Object[] { eventDetail.getEventTitle(),eventDetail.getStartDate(),eventDetail.getStartTime(),eventDetail.getTenantId(), eventDetail.getModuleCode() },
				Integer.class);

		if(i>0)
		{ 
			
			errorMap.put(ErrorConstants.INVALID_EVENT,ErrorConstants.INVALID_EVENT_MESSAGE);
			throw new CustomException(errorMap);
		}
	}
}