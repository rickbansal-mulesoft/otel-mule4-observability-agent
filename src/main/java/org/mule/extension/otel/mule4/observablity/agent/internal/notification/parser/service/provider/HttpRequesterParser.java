package org.mule.extension.otel.mule4.observablity.agent.internal.notification.parser.service.provider;

import java.util.Map;

import org.mule.extension.http.api.HttpResponseAttributes;
import org.mule.extension.otel.mule4.observablity.agent.internal.store.config.MuleConnectorConfigStore;
import org.mule.extension.otel.mule4.observablity.agent.internal.store.trace.MuleSoftTraceStore;
import org.mule.extension.otel.mule4.observablity.agent.internal.util.Constants;
import org.mule.extension.otel.mule4.observablity.agent.internal.util.NotificationParserUtils;
import org.mule.runtime.api.notification.EnrichedServerNotification;
import org.mule.runtime.api.util.MultiMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.SpanKind;

public class HttpRequesterParser extends BaseNotificationParser
{
	private static Logger logger = LoggerFactory.getLogger(HttpRequesterParser.class);

	// --------------------------------------------------------------------------------------------
	// Verifiy if this Parser can handle this notification
	// --------------------------------------------------------------------------------------------	
	@Override
	public boolean canParse(EnrichedServerNotification notification)
	{
		if (NotificationParserUtils.getComponentId(notification).equalsIgnoreCase(Constants.HTTP_REQUESTER))
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
		
		spanBuilder.setSpanKind(SpanKind.CLIENT);
		
		return addHttpRequesterAttributesToSpan(notification, muleConnectorConfigStore, spanBuilder);
	}

	// --------------------------------------------------------------------------------------------
	// Message Processor End Notification Parsing Handler
	// --------------------------------------------------------------------------------------------	
	@Override
	public void endProcessorNotification(EnrichedServerNotification notification, MuleSoftTraceStore traceStore)
	{
		super.endProcessorNotification(notification, traceStore);
		addHttpResponseAttributesToSpan(notification, traceStore);
	}

	// --------------------------------------------------------------------------------------------
	// Annotate the span with various HTTP Requester attributes
	// --------------------------------------------------------------------------------------------
	private SpanBuilder addHttpRequesterAttributesToSpan(EnrichedServerNotification notification,
                                                          MuleConnectorConfigStore muleConnectorConfigStore,
			                                              SpanBuilder spanBuilder)
	{
		Map<String, String> requesterAttributes = NotificationParserUtils.getComponentAnnotation("{config}componentParameters", notification);
		String configRef = requesterAttributes.get("config-ref");
	
		MuleConnectorConfigStore.HttpRequesterConfig httpRequesterConfig = muleConnectorConfigStore.getConfig(configRef);

		try 
		{
			spanBuilder.setAttribute("scheme", httpRequesterConfig.getProtocol());
			spanBuilder.setAttribute("method", requesterAttributes.get("method"));
			spanBuilder.setAttribute("request.path", requesterAttributes.get("path"));
			spanBuilder.setAttribute("requester.host", httpRequesterConfig.getHost());
			spanBuilder.setAttribute("requester.port", httpRequesterConfig.getPort());			
		}
		catch (Exception e)
		{
			logger.debug(e.getMessage());
		}

		return spanBuilder;
	}
	
	// --------------------------------------------------------------------------------------------
	// Annotate the span with various HTTP Response attributes
	// --------------------------------------------------------------------------------------------

	private void addHttpResponseAttributesToSpan(EnrichedServerNotification notification, MuleSoftTraceStore traceStore)
	{
		HttpResponseAttributes responseAttributes = NotificationParserUtils.getMessageAttributes(notification);
		
		Span span = traceStore.getMessageProcessorSpan(NotificationParserUtils.getMuleSoftTraceId(notification), 
				                                       NotificationParserUtils.getFlowId(notification), 
				                                       NotificationParserUtils.getSpanId(notification));
		try
		{
			MultiMap<String, String> responseHeaders = responseAttributes.getHeaders();
	
			span.setAttribute("response.status.code", responseAttributes.getStatusCode());
			span.setAttribute("response.reason.phrase", responseAttributes.getReasonPhrase());
			span.setAttribute("response.content.length", responseHeaders.get("content-length"));
			span.setAttribute("response.date", responseHeaders.get("date"));

			//additional annotation data from the mule event of HTTP Response

			responseAttributes.getHeaders().forEach((key, collection) -> {span.setAttribute("response.headers."+key,collection);});
		}
		catch (Exception e)
		{
			logger.debug(e.getMessage());
		}
	}
}
