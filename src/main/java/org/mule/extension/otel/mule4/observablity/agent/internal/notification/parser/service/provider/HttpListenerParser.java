package org.mule.extension.otel.mule4.observablity.agent.internal.notification.parser.service.provider;

import org.mule.extension.http.api.HttpRequestAttributes;
import org.mule.extension.otel.mule4.observablity.agent.internal.context.propagation.HttpRequestAttributesGetter;
import org.mule.extension.otel.mule4.observablity.agent.internal.context.propagation.OTelContextPropagator;
import org.mule.extension.otel.mule4.observablity.agent.internal.store.config.MuleConnectorConfigStore;
import org.mule.extension.otel.mule4.observablity.agent.internal.util.Constants;
import org.mule.extension.otel.mule4.observablity.agent.internal.util.NotificationParserUtils;
import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.notification.EnrichedServerNotification;
import org.mule.runtime.api.notification.PipelineMessageNotification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;

public class HttpListenerParser extends BaseNotificationParser
{
	private static Logger logger = LoggerFactory.getLogger(HttpListenerParser.class);

	// --------------------------------------------------------------------------------------------
	// Verify if this Parser can handle this notification
	// --------------------------------------------------------------------------------------------	
	@Override
	public boolean canParse(EnrichedServerNotification notification)
	{
		ComponentIdentifier sourceIdentifier = NotificationParserUtils.getSourceIdentifier(notification);
		String sourceComponent = sourceIdentifier.getNamespace() + ":" + sourceIdentifier.getName();
		int action = Integer.parseInt(notification.getAction().getIdentifier());
		
		// ----------------------------------------------------------------------------------------
		// Only parse HTTP Listener notifications which are a source/trigger to the start of a flow
		// ----------------------------------------------------------------------------------------
		if ( sourceComponent.equalsIgnoreCase(Constants.HTTP_LISTENER)  && 
			 action == PipelineMessageNotification.PROCESS_START )
		{
			return true;
		}
		else
		{
			return false;
		}
	}
	
	// --------------------------------------------------------------------------------------------
	// Pipeline Start Notification Parsing Handler
	// --------------------------------------------------------------------------------------------
	@Override
	public SpanBuilder startPipelineNotification(EnrichedServerNotification notification,
			                                     MuleConnectorConfigStore muleConnectorConfigStore, 
			                                     SpanBuilder spanBuilder)
	{
		super.startPipelineNotification(notification, muleConnectorConfigStore, spanBuilder);
		
		try
		{
			spanBuilder.setSpanKind(SpanKind.SERVER);
			spanBuilder = addHttpListenerAttributesToSpan(notification, spanBuilder);
			
			HttpRequestAttributes httpRequestAttributes = NotificationParserUtils.getMessageAttributes(notification);
			
			// ------------------------------------------------------------------------------------
			// 	Copy over any WC3 Trace Headers from the incoming Http request into the current
			//	trace context
			// ------------------------------------------------------------------------------------
			Context context = OTelContextPropagator.extract(httpRequestAttributes, new HttpRequestAttributesGetter());
			spanBuilder.setParent(context);	
		}
		catch (Exception e)
		{
			logger.debug(e.getMessage());
		}
		
		return spanBuilder;
	}

	// --------------------------------------------------------------------------------------------
	// Annotate the span with various HTTP Listener attributes
	// --------------------------------------------------------------------------------------------
	private SpanBuilder addHttpListenerAttributesToSpan(EnrichedServerNotification notification,
			                                            SpanBuilder spanBuilder)
	{
		HttpRequestAttributes httpRequestAttributes = NotificationParserUtils.getMessageAttributes(notification);

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
}
