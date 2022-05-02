package org.mule.extension.otel.mule4.observablity.agent.internal.notification.parser.service.provider;

import org.mule.extension.otel.mule4.observablity.agent.internal.store.trace.MuleSoftTraceStore;
import org.mule.extension.otel.mule4.observablity.agent.internal.util.Constants;
import org.mule.extension.otel.mule4.observablity.agent.internal.util.NotificationParserUtils;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.api.notification.EnrichedServerNotification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;

public class LoggerParser extends BaseNotificationParser
{
	@SuppressWarnings("unused")
	private static Logger logger = LoggerFactory.getLogger(LoggerParser.class);

	// --------------------------------------------------------------------------------------------
	// Verify if this Parser can handle this notification
	// --------------------------------------------------------------------------------------------	
	@Override
	public boolean canParse(EnrichedServerNotification notification)
	{
		if (NotificationParserUtils.getComponentId(notification).equalsIgnoreCase(Constants.LOGGER))
			return true;
		else
			return false;
	}

	// --------------------------------------------------------------------------------------------
	// Message Processor End Notification Parsing Handler
	// --------------------------------------------------------------------------------------------	
	@Override
	public void endProcessorNotification(EnrichedServerNotification notification, MuleSoftTraceStore traceStore)
	{
		super.endProcessorNotification(notification, traceStore);
		addLoggerEventsToSpan(notification, traceStore);
	}

	// --------------------------------------------------------------------------------------------
	// Annotate the span with Logger event
	// --------------------------------------------------------------------------------------------
	private void addLoggerEventsToSpan(EnrichedServerNotification notification, MuleSoftTraceStore traceStore)
	{
		//String loggerOutput  = NotificationParserUtils.getMessage(notification);
		String loggerOutput  = TypedValue.unwrap(notification.getEvent().getVariables().get(Constants.LOGGER_OUTPUT_KEY));
		
		Span span = traceStore.getMessageProcessorSpan(NotificationParserUtils.getMuleSoftTraceId(notification), 
				                                       NotificationParserUtils.getFlowId(notification), 
				                                       NotificationParserUtils.getSpanId(notification));
		
		// ----------------------------------------------------------------------------------------
		// Copy the output of the logger to an event in the span
		// ----------------------------------------------------------------------------------------
		if (loggerOutput != null && notification.getException() == null)
		{
			Attributes eventAttributes = Attributes.of(AttributeKey.stringKey("logger.output"), loggerOutput);
			span.addEvent("logger.output.event", eventAttributes);
		}
	}
	
}
