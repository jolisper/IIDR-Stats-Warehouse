package com.ibm.replication.iidr.warehouse.logging;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Properties;

import com.ibm.replication.iidr.utils.Settings;

public class LogDatabase extends LogInterface {

	// Database connection parameters
	private Connection con = null;
	private PreparedStatement insertSubStatus;
	private PreparedStatement insertSubMetrics;
	private PreparedStatement insertEvents;

	String dbUserName;
	String dbPassword;
	String dbUrl;
	String dbDriverName;
	String dbSchema;

	public LogDatabase(Settings settings)
			throws IllegalAccessException, InstantiationException, ClassNotFoundException, IOException {
		super(settings);
		dbUserName = settings.getString(this.getClass().getName() + ".dbUserName");
		dbPassword = settings.getEncryptedString(this.getClass().getName() + ".dbPassword");
		dbUrl = settings.getString(this.getClass().getName() + ".dbUrl");
		dbDriverName = settings.getString(this.getClass().getName() + ".dbDriverName");
		dbSchema = settings.getString(this.getClass().getName() + ".dbSchema");

		connect();

	}

	/**
	 * Establishes a connection to the database.
	 * 
	 * @throws SQLException
	 * @throws IOException
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 * @throws ClassNotFoundException
	 */
	public void connect() throws IOException, IllegalAccessException, InstantiationException, ClassNotFoundException {
		try {
			Properties props = new Properties();
			props.put("user", dbUserName);
			props.put("password", dbPassword);
			logger.debug("Database URL: " + dbUrl + ", driver: " + dbDriverName);
			Class.forName(dbDriverName).newInstance();
			con = DriverManager.getConnection(dbUrl, props);

			logger.debug("Connecting to url : " + dbUrl + " using user name " + dbUserName);
			DatabaseMetaData dbmd = con.getMetaData();
			logger.debug("DatabaseProductName: " + dbmd.getDatabaseProductName());
			logger.debug("DatabaseProductVersion: " + dbmd.getDatabaseProductVersion());
			// Disable auto-commit, this should be done in the harden() method
			logger.debug("Disabling auto-commit");
			con.setAutoCommit(false);
		} catch (SQLException esql) {
			logger.error(esql.toString());
			disconnect();
		} catch (ClassNotFoundException esql) {
			logger.error(esql.toString());
			disconnect();
		} catch (Exception e) {
			logger.error("Connecting to database failed, error: " + e.toString());
		}
	}

	public void disconnect() {
		if (con != null) {
			try {
				con.rollback();
			} catch (SQLException ignore) {
				logger.debug("Error during rollback: " + ignore.getMessage());
			}
			try {
				con.close();
			} catch (SQLException ignore) {
				logger.debug("Error while closing connection to database: " + ignore.getMessage());
			}
		}
	}

	/**
	 * Logs the status of the subscription into a database table
	 * 
	 * @throws SQLException
	 */
	@Override
	public void logSubscriptionStatus(String dataStore, String subscriptionName, Timestamp collectTimestamp,
			String subscriptionState) throws SQLException {
		// Only try to insert the status if the connection has been established
		if (con != null) {
			// Prepare statement if this is the first time
			if (insertSubStatus == null)
				insertSubStatus = con.prepareStatement("insert into " + dbSchema + ".CDC_SUB_STATUS "
						+ "(SRC_DATASTORE,SUBSCRIPTION,COLLECT_TS,SUBSCRIPTION_STATUS) " + "VALUES (?,?,?,?)");

			// Write the subscription status into the table
			insertSubStatus.setString(1, dataStore);
			insertSubStatus.setString(2, subscriptionName);
			insertSubStatus.setTimestamp(3, collectTimestamp);
			insertSubStatus.setString(4, subscriptionState);
			insertSubStatus.execute();
		}
	}

	/**
	 * Logs the metrics into the database
	 */
	@Override
	public void logMetrics(String dataStore, String subscriptionName, Timestamp collectTimestamp,
			String metricSourceTarget, int metricID, long metricValue) throws SQLException {
		// Only try to insert the status if the connection has been established
		if (con != null) {
			if (insertSubMetrics == null)
				insertSubMetrics = con.prepareStatement("insert into " + dbSchema + ".CDC_STATS_ALL "
						+ "(SRC_DATASTORE,SUBSCRIPTION,COLLECT_TS,SRC_TGT,METRIC_ID,METRIC_VALUE) "
						+ "VALUES (?,?,?,?,?,?)");

			// Write the metrics into the table
			insertSubMetrics.setString(1, dataStore);
			insertSubMetrics.setString(2, subscriptionName);
			insertSubMetrics.setTimestamp(3, collectTimestamp);
			insertSubMetrics.setString(4, metricSourceTarget);
			insertSubMetrics.setInt(5, metricID);
			insertSubMetrics.setLong(6, metricValue);
			insertSubMetrics.execute();
		}
	}

	/**
	 * Logs the events into the database
	 * 
	 * @throws SQLException
	 */
	@Override
	public void logEvent(String dataStore, String subscriptionName, String sourceTarget, String eventIDString,
			String eventType, String eventTimestampString, String eventMessage) throws SQLException {
		// Only try to insert the status if the connection has been established
		if (con != null) {
			if (insertEvents == null)
				insertEvents = con.prepareStatement("insert into " + dbSchema + ".CDC_EVENTS "
						+ "(SRC_DATASTORE,SUBSCRIPTION,SRC_TGT,EVENT_ID,EVENT_TYPE,EVENT_TIMESTAMP,EVENT_MESSAGE) "
						+ "VALUES (?,?,?,?,?,?,?)");
		}

		// Try to convert event ID to integer
		int eventID = 0;
		try {
			eventID = Integer.parseInt(eventIDString);
		} catch (NumberFormatException e) {
			logger.error("Error converting event ID " + eventIDString + " to numeric, will be set to 0");
		}

		// Try to convert event timestamp to Timestamp
		Timestamp eventTimestamp = Timestamp.valueOf(eventTimestampString);

		// Write the event into the table
		insertEvents.setString(1, dataStore);
		insertEvents.setString(2, subscriptionName);
		insertEvents.setString(3, sourceTarget);
		insertEvents.setInt(4, eventID);
		insertEvents.setString(5, eventType);
		insertEvents.setTimestamp(6, eventTimestamp);
		insertEvents.setString(7, eventMessage);
		insertEvents.execute();
	}

	/**
	 * Hardens (commits) the transaction into the database
	 */
	@Override
	public void harden() throws SQLException {
		if (con != null)
			con.commit();
	}

	/**
	 * Final processing (disconnect from the database)
	 */
	@Override
	public void finish() {
		logger.debug("Finalizing processing for logging to database");
		disconnect();
	}

}
