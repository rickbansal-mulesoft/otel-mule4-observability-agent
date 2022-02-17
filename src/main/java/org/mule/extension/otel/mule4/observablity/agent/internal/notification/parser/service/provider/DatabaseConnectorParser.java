package org.mule.extension.otel.mule4.observablity.agent.internal.notification.parser.service.provider;

import java.util.Map;

import org.mule.extension.otel.mule4.observablity.agent.internal.store.config.MuleConnectorConfigStore;
import org.mule.extension.otel.mule4.observablity.agent.internal.util.Constants;
import org.mule.extension.otel.mule4.observablity.agent.internal.util.NotificationParserUtils;
import org.mule.runtime.api.notification.EnrichedServerNotification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.opentelemetry.api.trace.SpanBuilder;

public class DatabaseConnectorParser extends BaseNotificationParser
{
	private static Logger logger = LoggerFactory.getLogger(DatabaseConnectorParser.class);

	// --------------------------------------------------------------------------------------------
	// Verifiy if this Parser can handle this notification
	// --------------------------------------------------------------------------------------------	
	@Override
	public boolean canParse(EnrichedServerNotification notification)
	{
		if (NotificationParserUtils.getComponentId(notification).matches(Constants.DB_MATCHER))
			return true;
		else
			return false;
	}
	
	// --------------------------------------------------------------------------------------------
	// Message Processor Start Notification Parsing Handler
	// --------------------------------------------------------------------------------------------	
	@Override
	public SpanBuilder startProcessorNotification(EnrichedServerNotification notification,
			                                      MuleConnectorConfigStore muleConnectorConfigStore, 
			                                      SpanBuilder spanBuilder)
	{
		super.startProcessorNotification(notification, muleConnectorConfigStore, spanBuilder);
		
		return addDatabaseAttributesToSpan(notification, muleConnectorConfigStore, spanBuilder);
	}

	// --------------------------------------------------------------------------------------------
	// Annotate the span with various Database attributes
	// --------------------------------------------------------------------------------------------
	private SpanBuilder addDatabaseAttributesToSpan(EnrichedServerNotification notification,
			                                        MuleConnectorConfigStore muleConnectorConfigStore,
			                                        SpanBuilder spanBuilder)
	{		
		Map<String, String> componentParameters = NotificationParserUtils.getComponentAnnotation("{config}componentParameters", notification);
		
		String sql = componentParameters.get("sql");
		String configRef = componentParameters.get("config-ref");

		MuleConnectorConfigStore.DbConfig dbConfig = muleConnectorConfigStore.getConfig(configRef);
		
		try
		{
			spanBuilder.setAttribute("sql.statement", sql);
			spanBuilder.setAttribute("db.host", dbConfig.getHost());
			spanBuilder.setAttribute("db.port", dbConfig.getPort());
			spanBuilder.setAttribute("db.user", dbConfig.getUser());
			spanBuilder.setAttribute("db.name", dbConfig.getDbName());
			spanBuilder.setAttribute("db.connection.type", dbConfig.getConnectionType());
		}
		catch (Exception e)
		{
			logger.debug(e.getMessage());
		}
		return spanBuilder;
	}
}
