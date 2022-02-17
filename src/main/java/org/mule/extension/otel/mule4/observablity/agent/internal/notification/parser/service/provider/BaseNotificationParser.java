package org.mule.extension.otel.mule4.observablity.agent.internal.notification.parser.service.provider;


import java.time.Instant;

import org.mule.extension.otel.mule4.observablity.agent.internal.store.config.MuleConnectorConfigStore;
import org.mule.extension.otel.mule4.observablity.agent.internal.store.trace.MuleSoftTraceStore;
import org.mule.extension.otel.mule4.observablity.agent.internal.util.Constants;
import org.mule.extension.otel.mule4.observablity.agent.internal.util.NotificationParserUtils;
import org.mule.runtime.api.notification.EnrichedServerNotification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.StatusCode;


/**
 * 
 * Base implementation of a {@link NotificationParser}
 *
 */
public class BaseNotificationParser implements NotificationParser
{
	private static Logger logger = LoggerFactory.getLogger(BaseNotificationParser.class);

	// --------------------------------------------------------------------------------------------
	// Verifiy if this Parser can handle this notification
	// --------------------------------------------------------------------------------------------	
	@Override
	public boolean canParse(EnrichedServerNotification notification)
	{
		return true;
	}
	
	// --------------------------------------------------------------------------------------------
	// Pipeline Start Notification Parsing Handler
	// --------------------------------------------------------------------------------------------	
	@Override
	public SpanBuilder startPipelineNotification(EnrichedServerNotification notification,
			                                     MuleConnectorConfigStore muleConnectorConfigStore, 
			                                     SpanBuilder spanBuilder)
	{
		return spanBuilder;
	}


	// --------------------------------------------------------------------------------------------
	// Pipeline End Notification Parsing Handler
	// --------------------------------------------------------------------------------------------	
	@Override
	public void endPipelineNotification(EnrichedServerNotification notification, MuleSoftTraceStore traceStore)
	{
		// do nothing for now
	}

	// --------------------------------------------------------------------------------------------
	// Message Processor Start Notification Parsing Handler
	// --------------------------------------------------------------------------------------------		
	@Override
	public SpanBuilder startProcessorNotification(EnrichedServerNotification notification,
			                                      MuleConnectorConfigStore muleConnectorConfigStore, 
			                                      SpanBuilder spanBuilder)
	{
		try
		{			
			spanBuilder.setStartTimestamp(NotificationParserUtils.getInstantFrom(notification));
			
			spanBuilder.setAttribute(Constants.START_DATETIME_ATTRIBUTE, 
					                 NotificationParserUtils.getInstantFrom(notification).toString());
			
			spanBuilder.setAttribute(Constants.DOC_NAME_ATTRIBUTE, 
					                 NotificationParserUtils.getDocName(notification));
		} 
		catch (Exception e)
		{
			logger.debug(e.getMessage());
		}
		
		return spanBuilder;
	}

	// --------------------------------------------------------------------------------------------
	// Message Processor End Notification Parsing Handler
	// --------------------------------------------------------------------------------------------	
	@Override
	public void endProcessorNotification(EnrichedServerNotification notification, MuleSoftTraceStore traceStore)
	{		
		Span span = traceStore.getMessageProcessorSpan(NotificationParserUtils.getMuleSoftTraceId(notification), 
                                                       NotificationParserUtils.getFlowId(notification), 
                                                       NotificationParserUtils.getSpanId(notification));
		
		Exception e = notification.getException();
		
		try
		{
			span.setAttribute(Constants.END_DATETIME_ATTRIBUTE, 
					          NotificationParserUtils.getInstantFrom(notification).toString());
			
			if (e != null)
			{
				span.setStatus(StatusCode.ERROR, e.getMessage());
				span.recordException(e);
			}
		} 
		catch (Exception ex)
		{
			logger.debug(ex.getMessage());
		}
	}
}
