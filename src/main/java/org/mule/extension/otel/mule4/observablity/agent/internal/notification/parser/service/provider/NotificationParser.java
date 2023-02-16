package org.mule.extension.otel.mule4.observablity.agent.internal.notification.parser.service.provider;

import org.mule.extension.otel.mule4.observablity.agent.internal.store.config.MuleConnectorConfigStore;
import org.mule.extension.otel.mule4.observablity.agent.internal.store.trace.MuleSoftTraceStore;
import org.mule.runtime.api.notification.EnrichedServerNotification;

import io.opentelemetry.api.trace.SpanBuilder;

/**
 * 
 * 	Set of APIs to support parsing of notification events either at the <b>pipeline</b> level or the 
 * 	<b>message processor</b> level.  Each processor component (e.g., HTTP Requester, DB Connector, 
 * 	Logger, ...) will have their own implementation of these APIs to support the embellishment of the 
 * 	trace/span.
 * 
 * 	<ul> Following APIs are supported:
 * 		<li> {@link #canParse(EnrichedServerNotification)}) </li>
 * 		<li> {@link #startPipelineNotification(EnrichedServerNotification, MuleConnectorConfigStore, SpanBuilder)} </li>
 * 		<li> {@link #endPipelineNotification(EnrichedServerNotification, MuleSoftTraceStore)} </li>
 * 		<li> {@link #startProcessorNotification(EnrichedServerNotification, MuleConnectorConfigStore, SpanBuilder)} </li>
 * 		<li> {@link #endProcessorNotification(EnrichedServerNotification, MuleSoftTraceStore)} </li>	
 * </u>
 * 
 */
public interface NotificationParser
{
	// --------------------------------------------------------------------------------------------
	// API to Verify if this Parser can handle this notification event
	// --------------------------------------------------------------------------------------------
	/**
	 * API to Verify if this Parser can handle this notification event
	 * @param notification
	 * @return true if it can, else false
	 */
	public boolean canParse(EnrichedServerNotification notification);

	// --------------------------------------------------------------------------------------------
	// API for Pipeline Start Notification Event
	// --------------------------------------------------------------------------------------------
	/**
	 * API to support a pipeline start notification event
	 * 
	 * @param notification - Pipeline start notification
	 * @param muleConnectorConfigStore - Metadata store of config details for various connectors
	 * @param spanBuilder - reference to the current SpanBuilder
	 * @return - SpanBuilder reference
	 */
	public SpanBuilder startPipelineNotification(EnrichedServerNotification notification,
                                                 MuleConnectorConfigStore muleConnectorConfigStore,
                                                 SpanBuilder spanBuilder);

	// --------------------------------------------------------------------------------------------
	// API for Pipeline End Notification Event
	// --------------------------------------------------------------------------------------------
	/**
	 * API to support a pipeline start notification event
	 * 
	 * @param notification - Pipeline end notification
	 * @param traceStore -  Store where trace/span data is for this notification
	 */
	public void endPipelineNotification(EnrichedServerNotification notification, MuleSoftTraceStore traceStore);

	
	// --------------------------------------------------------------------------------------------
	// API for Message Processor Start Notification Event
	// --------------------------------------------------------------------------------------------	
	/**
	 * 
	 * @param notification -  Message Processor start notification
	 * @param muleConnectorConfigStore - Metadata store of config details for various connectors
	 * @param spanBuilder - Reference to the current SpanBuilder
	 * @return - SpanBuilder reference
	 */
	public SpanBuilder startProcessorNotification(EnrichedServerNotification notification,
                                                  MuleConnectorConfigStore muleConnectorConfigStore,
                                                  SpanBuilder spanBuilder);
	
	// --------------------------------------------------------------------------------------------
	// API for Message Processor End Notification Event
	// --------------------------------------------------------------------------------------------	
	/**
	 * 
	 * @param notification - Message Processor end notification
	 * @param traceStore -  Store where trace/span data is for this notification
	 */
	public void endProcessorNotification(EnrichedServerNotification notification, MuleSoftTraceStore traceStore);
	
}
