package org.mule.extension.otel.mule4.observablity.agent.internal.notification;

import java.time.Instant;
import java.util.Map;

import javax.xml.namespace.QName;

import org.mule.extension.http.api.HttpRequestAttributes;
import org.mule.extension.http.api.HttpResponseAttributes;
import org.mule.extension.otel.mule4.observablity.agent.internal.store.config.MuleConnectorConfigStore;
import org.mule.extension.otel.mule4.observablity.agent.internal.store.trace.MuleSoftTraceStore;
import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.event.Event;
import org.mule.runtime.api.notification.EnrichedServerNotification;
import org.mule.runtime.api.notification.MessageProcessorNotification;
import org.mule.runtime.api.notification.PipelineMessageNotification;
import org.mule.runtime.api.util.MultiMap;
//import org.mule.runtime.ast.api.ComponentAst;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;

@SuppressWarnings("unchecked")
public class MuleNotificationParser
{
	private static Logger logger = LoggerFactory.getLogger(MuleNotificationParser.class);

	// ============================================================================================
	// Notification Helper Methods - get individual notification details
	// ============================================================================================
	
	public static Instant getInstantFrom(EnrichedServerNotification notification)
	{
		return Instant.ofEpochMilli(notification.getTimestamp());
	}

	public static String getDocName(EnrichedServerNotification notification)
	{
		String docName = "";

		if (notification instanceof PipelineMessageNotification)
			docName = "name";
		else if (notification instanceof MessageProcessorNotification)
			docName = "doc:name";

		String value = ((Map<String, String>) notification.getInfo()
				                                          .getComponent()
				                                          .getAnnotation(QName.valueOf("{config}componentParameters")))
				                                          .get(docName);
		return value;
	}

	/**
	 * 
	 * @param notification
	 * @return the first part of the unique location of this component.  Should be the flow name.
	 * @see
	 * <a href=https://www.mulesoft.org/docs/site/4.3.0/apidocs/org/mule/runtime/api/component/location/ComponentLocation.html>
	 * 	Component Location
	 * </a>	
	 */
	public static String getFlowId(EnrichedServerNotification notification)
	{
		return "_" + notification.getComponent().getLocation().getRootContainerName();
	}
	
	public static String getServerId(EnrichedServerNotification notification)
	{
		return notification.getServerId();
	}

	/*
	 * 	This should be unique for each component in a Mule application
	 */
	public static String getSpanName(EnrichedServerNotification notification)
	{
		return notification.getComponent().getIdentifier().getName() + "::" + getDocName(notification);
	}

	/**
	 * @param notification
	 * @return The unique absolute location of this component in the Mule application
	 */
	public static String getSpanId(EnrichedServerNotification notification)
	{
		return notification.getInfo().getComponent().getLocation().getLocation();
	}

	/**
	 * @param notification
	 * 	@return The Correlation ID in Mule Event as the OTel Trace ID - unique value
	 */
	public static String getMuleSoftTraceId(EnrichedServerNotification notification)
	{
		return notification.getEvent().getCorrelationId();
	}

	/**
	 * 
	 * @param notification
	 * @return The qualifed name {@code <namespace:name>} of this component.  For example, {@code http:listener, http:requester,}...
	 */
	public static String getComponentId(EnrichedServerNotification notification)
	{
		ComponentIdentifier componentIdentifier = (ComponentIdentifier) notification.getComponent()
				                                                                    .getAnnotation(QName.valueOf("{config}componentIdentifier"));

		return (componentIdentifier.getNamespace() + ":" + componentIdentifier.getName());
	}

	public static ComponentIdentifier getSourceIdentifier(EnrichedServerNotification notification)
	{
		ComponentIdentifier sourceIdentifier = null;
		if (notification.getEvent() != null && notification.getEvent().getContext().getOriginatingLocation() != null)
		{
			sourceIdentifier = notification.getEvent()
					                       .getContext()
					                       .getOriginatingLocation()
					                       .getComponentIdentifier()
					                       .getIdentifier();
		}
		return sourceIdentifier;
	}

	public static <T> T getComponentAnnotation(String annotationName, EnrichedServerNotification notification)
	{
		return (T) notification.getInfo().getComponent().getAnnotation(QName.valueOf(annotationName));
	}

	// ============================================================================================
	// Notification Helper Methods - add attributes
	// ============================================================================================

	// --------------------------------------------------------------------------------------------
	// Annotate the span with various HTTP Listener attributes
	// --------------------------------------------------------------------------------------------
	public static SpanBuilder addHttpListenerAttributesToSpan(EnrichedServerNotification notification,
			                                                  SpanBuilder spanBuilder)
	{
		HttpRequestAttributes httpRequestAttributes = getMessageAttributes(notification);

		try
		{
			spanBuilder.setAttribute("scheme", httpRequestAttributes.getScheme());
			spanBuilder.setAttribute("method", httpRequestAttributes.getMethod());
			spanBuilder.setAttribute("remote.address", httpRequestAttributes.getRemoteAddress());
			spanBuilder.setAttribute("request.path", httpRequestAttributes.getRequestPath());
			spanBuilder.setAttribute("user.agent", httpRequestAttributes.getHeaders().get("user-agent"));
			spanBuilder.setAttribute("host", httpRequestAttributes.getHeaders().get("host"));
		}
		catch (Exception e)
		{
			logger.debug(e.getMessage());
		}

		return spanBuilder;
	}

	// --------------------------------------------------------------------------------------------
	// Annotate the span with various HTTP Requester attributes
	// --------------------------------------------------------------------------------------------
	public static SpanBuilder addHttpRequesterAttributesToSpan(EnrichedServerNotification notification,
                                                               MuleConnectorConfigStore muleConnectorConfigStore,
			                                                   SpanBuilder spanBuilder)
	{
		Map<String, String> requesterAttributes = getComponentAnnotation("{config}componentParameters", notification);
		String configRef = requesterAttributes.get("config-ref");
	
		MuleConnectorConfigStore.HttpRequesterConfig httpRequesterConfig = (MuleConnectorConfigStore.HttpRequesterConfig) muleConnectorConfigStore.getConfig(configRef);

		/*
		Map<String,TypedValue<?>> variables = notification.getEvent().getVariables();
		
		HttpRequesterRequestBuilder httpRequestBuilder = (HttpRequesterRequestBuilder) variables.get(ObservabilitySemantics.HTTP_REQUEST_BUILDER).getValue();

		if (httpRequestBuilder != null)
		{
			Map<String, String> traceContextMap = TypedValue.unwrap(notification.getEvent().getVariables()
                                                                                .get(ObservabilitySemantics.TRACE_CONTEXT_MAP_KEY));

			MultiMap<String, String> headers = new MultiMap<>();

			headers.put(ObservabilitySemantics.TRACEPARENT, traceContextMap.get(ObservabilitySemantics.TRACEPARENT));
			headers.put(ObservabilitySemantics.TRACESTATE, traceContextMap.get(ObservabilitySemantics.TRACESTATE));

			httpRequestBuilder.setHeaders(headers);
		}
		*/
		try 
		{
			spanBuilder.setAttribute("scheme", "HTTP");
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
	// Annotate the span with various Database attributes
	// --------------------------------------------------------------------------------------------
	public static SpanBuilder addDatabaseAttributesToSpan(EnrichedServerNotification notification,
			                                              MuleConnectorConfigStore muleConnectorConfigStore,
			                                              SpanBuilder spanBuilder)
	{		
		Map<String, String> componentParameters = getComponentAnnotation("{config}componentParameters", notification);
		
		String sql = componentParameters.get("sql");
		String configRef = componentParameters.get("config-ref");

		MuleConnectorConfigStore.DbConfig dbConfig =  (MuleConnectorConfigStore.DbConfig)muleConnectorConfigStore.getConfig(configRef);
		
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
	
	// --------------------------------------------------------------------------------------------
	// Annotate the span with various HTTP Response attributes
	// --------------------------------------------------------------------------------------------

	public static void addHttpResponseAttributesToSpan(EnrichedServerNotification notification, MuleSoftTraceStore traceStore)
	{
		HttpResponseAttributes responseAttributes = getMessageAttributes(notification);
		
		Span span = traceStore.getMessageProcessorSpan(MuleNotificationParser.getMuleSoftTraceId(notification), 
                                                       MuleNotificationParser.getFlowId(notification), 
                                                       MuleNotificationParser.getSpanId(notification));
		try
		{
			MultiMap<String, String> responseHeaders = responseAttributes.getHeaders();
	
			span.setAttribute("response.status.code", responseAttributes.getStatusCode());
			span.setAttribute("response.reason.phrase", responseAttributes.getReasonPhrase());
			span.setAttribute("response.content.length", responseHeaders.get("content-length"));
			span.setAttribute("response.date", responseHeaders.get("date"));
		}
		catch (Exception e)
		{
			logger.debug(e.getMessage());
		}
	}
	
	// --------------------------------------------------------------------------------------------
	// Annotate the span with Logger event
	// --------------------------------------------------------------------------------------------

	public static void addLoggerEventsToSpan(EnrichedServerNotification notification, MuleSoftTraceStore traceStore)
	{
		String loggerOutput  = getMessage(notification);
		
		Span span = traceStore.getMessageProcessorSpan(MuleNotificationParser.getMuleSoftTraceId(notification), 
                                                       MuleNotificationParser.getFlowId(notification), 
                                                       MuleNotificationParser.getSpanId(notification));
		
		if (loggerOutput != null && notification.getException() == null)
		{
			Attributes eventAttributes = Attributes.of(AttributeKey.stringKey("logger.output"), loggerOutput);
			span.addEvent("logger.output.event", eventAttributes);
		}
	}
	
	
	// --------------------------------------------------------------------------------------------
	// Generic helpers
	// --------------------------------------------------------------------------------------------
	public static <T> T getMessageAttributes(EnrichedServerNotification notification)
	{
		return (T) notification.getEvent().getMessage().getAttributes().getValue();
	}
	
	public static <T> T getMessageAttributes(Event event)
	{
		return (T) event.getMessage().getAttributes().getValue();
	}
	
	public static <T> T getMessage(EnrichedServerNotification notification)
	{
		return (T) notification.getEvent().getMessage().getPayload().getValue();
	}
}
