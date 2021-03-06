package com.ibm.replication.iidr.warehouse.logging;

import java.sql.Timestamp;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;

import com.ibm.replication.iidr.utils.Settings;

public class LogCsv extends LogInterface {

	Logger csvLogger;
	String statusHeader = null;
	String metricsHeader = null;
	String eventsHeader = null;

	String csvSeparator = "|";

	public LogCsv(Settings settings) {
		super(settings);
		csvLogger = LogManager.getLogger(this.getClass());
		csvSeparator = settings.getString(this.getClass().getName() + ".csvSeparator", csvSeparator);
		statusHeader = "dataStore" + csvSeparator + "subscriptionName" + csvSeparator + "collectTimestamp"
				+ csvSeparator + "subscriptionState";
		metricsHeader = "dataStore" + csvSeparator + "subscriptionName" + csvSeparator + "collectTimestamp"
				+ csvSeparator + "metricSourceTarget" + csvSeparator + "metricID" + csvSeparator + "metricValue";
		eventsHeader = "dataStore" + csvSeparator + "subscriptionName" + csvSeparator + csvSeparator + "sourceTarget"
				+ csvSeparator + "eventID" + csvSeparator + "eventType" + csvSeparator + "eventMessage";
	}

	/**
	 * Logs the status of the subscription into the flat file
	 */
	@Override
	public void logSubscriptionStatus(String dataStore, String subscriptionName, Timestamp collectTimestamp,
			String subscriptionState) {
		ThreadContext.put("separator", csvSeparator);
		ThreadContext.put("dataStore", dataStore);
		ThreadContext.put("subscriptionName", subscriptionName);
		ThreadContext.put("type", "SubStatus");
		ThreadContext.put("header", statusHeader);
		csvLogger.info("ignore", dataStore, subscriptionName, collectTimestamp, subscriptionState);
	}

	/**
	 * Logs the metrics into the flat file
	 */
	@Override
	public void logMetrics(String dataStore, String subscriptionName, Timestamp collectTimestamp,
			String metricSourceTarget, int metricID, long metricValue) {
		ThreadContext.put("separator", csvSeparator);
		ThreadContext.put("dataStore", dataStore);
		ThreadContext.put("subscriptionName", subscriptionName);
		ThreadContext.put("type", "Statistics");
		ThreadContext.put("header", metricsHeader);
		csvLogger.info("ignore", dataStore, subscriptionName, collectTimestamp, metricSourceTarget, metricID,
				metricValue);
	}

	@Override
	public void logEvent(String dataStore, String subscriptionName, String sourceTarget, String eventID,
			String eventType, String eventTimestamp, String eventMessage) {
		ThreadContext.put("separator", csvSeparator);
		ThreadContext.put("dataStore", dataStore);
		ThreadContext.put("subscriptionName", subscriptionName);
		ThreadContext.put("type", "Events");
		ThreadContext.put("header", eventsHeader);
		csvLogger.info("ignore", dataStore, subscriptionName, sourceTarget, eventID, eventType, eventTimestamp,
				eventMessage);
	}

	/**
	 * Hardens (commits) the written records
	 */
	@Override
	public void harden() {
	}

	/**
	 * Final processing
	 */
	@Override
	public void finish() {
		logger.debug("Finalizing processing for logging to CSV");
	}

	@Override
	public void connect() throws Exception {
	}

	@Override
	public void disconnect() throws Exception {
	}

}
