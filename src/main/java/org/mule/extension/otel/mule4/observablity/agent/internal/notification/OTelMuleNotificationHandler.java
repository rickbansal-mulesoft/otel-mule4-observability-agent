package org.mule.extension.otel.mule4.observablity.agent.internal.notification;

import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;

import org.mule.runtime.api.notification.MessageProcessorNotification;
import org.mule.runtime.api.notification.PipelineMessageNotification;
import org.mule.runtime.core.api.config.MuleConfiguration;
import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.extension.http.api.HttpRequestAttributes;
import org.mule.extension.otel.mule4.observablity.agent.internal.connection.OtelSdkConnection;
import org.mule.extension.otel.mule4.observablity.agent.internal.context.propagation.HttpRequestAttributesGetter;
import org.mule.extension.otel.mule4.observablity.agent.internal.context.propagation.OTelContextPropagator;
import org.mule.extension.otel.mule4.observablity.agent.internal.store.config.MuleConnectorConfigStore;
import org.mule.extension.otel.mule4.observablity.agent.internal.store.trace.MuleSoftTraceStore;
import org.mule.extension.otel.mule4.observablity.agent.internal.util.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.function.Supplier;

public class OTelMuleNotificationHandler
{
	private static Logger logger = LoggerFactory.getLogger(OTelMuleNotificationHandler.class);

	private static MuleSoftTraceStore traceStore = new MuleSoftTraceStore();
	
	private OtelSdkConnection otelSdkConnection;
	private MuleConnectorConfigStore muleConnectorConfigStore;
	private final Supplier<OtelSdkConnection> sdkConnectionSupplier;

	// --------------------------------------------------------------------------------------------
	// Constructor
	// --------------------------------------------------------------------------------------------
	public OTelMuleNotificationHandler(Supplier<OtelSdkConnection> s)
	{
		sdkConnectionSupplier = s;
	}

	/**
	 * 
	 * @return traceStore - a reference to the MuleSoftTraceStore created and used by this class
	 */
	public static MuleSoftTraceStore getMuleSoftTraceStore()
	{
		return traceStore;
	}
	
	private Tracer getTracer()
	{
		if (otelSdkConnection == null)
		{
			otelSdkConnection = sdkConnectionSupplier.get();
		}
		return otelSdkConnection.getTracer().get();
	}

	private MuleConfiguration getMuleConfiguration()
	{
		if (otelSdkConnection == null)
		{
			otelSdkConnection = sdkConnectionSupplier.get();
		}
		return otelSdkConnection.getMuleConfiguration().get();
	}
	
	private MuleConnectorConfigStore getMuleConnectorConfigStore()
	{
		if (muleConnectorConfigStore == null)
		{
			muleConnectorConfigStore = MuleConnectorConfigStore.getInstance(getMuleConfiguration());
		}
		
		return muleConnectorConfigStore;
	}
	// ============================================================================================
	// FLOW RELATED NOTIFICATION EVENTS
	// ============================================================================================

	// --------------------------------------------------------------------------------------------
	// Flow START Notification Handler
	// --------------------------------------------------------------------------------------------	
	public void handleFlowStartEvent(PipelineMessageNotification notification)
	{
		String sourceComponent;

		logger.debug("Handling flow start event");

		Instant startInstant = MuleNotificationParser.getInstantFrom(notification);
		
		SpanBuilder spanBuilder = getTracer().spanBuilder(MuleNotificationParser.getSpanName(notification)).setStartTimestamp(startInstant);
		
		spanBuilder.setAttribute(Constants.START_DATETIME_ATTRIBUTE, startInstant.toString());
		
		ComponentIdentifier sourceIdentifier = MuleNotificationParser.getSourceIdentifier(notification);
		
		if (!traceStore.isTracePresent(MuleNotificationParser.getMuleSoftTraceId(notification)))
		{
			try
			{
				spanBuilder.setAttribute(Constants.FLOW_NAME_ATTRIBUTE, MuleNotificationParser.getDocName(notification));
				spanBuilder.setAttribute(Constants.SERVER_ID_ATTRIBUTE, MuleNotificationParser.getServerId(notification));

				sourceComponent = sourceIdentifier.getNamespace() + ":" + sourceIdentifier.getName();

				if (sourceComponent.equalsIgnoreCase(Constants.HTTP_LISTENER))
				{
					spanBuilder.setSpanKind(SpanKind.SERVER);
					MuleNotificationParser.addHttpListenerAttributesToSpan(notification, spanBuilder);
					
					HttpRequestAttributes httpRequestAttributes = MuleNotificationParser.getMessageAttributes(notification);

					Context context = OTelContextPropagator.extract(httpRequestAttributes, new HttpRequestAttributesGetter());
					spanBuilder.setParent(context);
				}
			} catch (Exception e)
			{
				// Suppress
			}
			traceStore.startTrace(MuleNotificationParser.getMuleSoftTraceId(notification), 
					              MuleNotificationParser.getFlowId(notification), 
					              spanBuilder.startSpan());
		} 
		else
		{
			String docName = null;
			try
			{
				docName = MuleNotificationParser.getDocName(notification);
			} 
			catch (Exception e)
			{
				// Suppress
			}

			if (docName != null)
				spanBuilder.setAttribute(Constants.DOC_NAME_ATTRIBUTE, docName);
			
			traceStore.addPipelineSpan(MuleNotificationParser.getMuleSoftTraceId(notification), 
					                   MuleNotificationParser.getFlowId(notification), 
					                   spanBuilder);
		}
	}

	// --------------------------------------------------------------------------------------------
	// Flow END Notification Handler
	// --------------------------------------------------------------------------------------------
	public void handleFlowEndEvent(PipelineMessageNotification notification)
	{
		logger.debug("Handling flow end event");
		
		String mulesoftTraceId = MuleNotificationParser.getMuleSoftTraceId(notification);
	
		traceStore.endPipelineSpan(mulesoftTraceId, 
				                   MuleNotificationParser.getFlowId(notification),
				                   MuleNotificationParser.getInstantFrom(notification),
				                   notification.getException());
		
		if (traceStore.isPipelineSpansEmpty(mulesoftTraceId))
		{
			traceStore.endTrace(mulesoftTraceId);
		}
	}

	// ============================================================================================
	// PROCESSOR RELATED NOTIFICATION EVENTS
	// ============================================================================================

	// --------------------------------------------------------------------------------------------
	// Processor START Notification Handler
	// --------------------------------------------------------------------------------------------
	public void handleProcessorStartEvent(MessageProcessorNotification notification)
	{
		logger.debug("Handling processor start event");

		String docName = null;

		Instant startInstant = MuleNotificationParser.getInstantFrom(notification);
		
		SpanBuilder spanBuilder = getTracer().spanBuilder(MuleNotificationParser.getSpanName(notification)).setStartTimestamp(startInstant);
		
		spanBuilder.setAttribute(Constants.START_DATETIME_ATTRIBUTE, startInstant.toString());

		try
		{
			docName = MuleNotificationParser.getDocName(notification);
		} 
		catch (Exception e)
		{
			// Suppress
		}

		if (docName != null)
		{
			spanBuilder.setAttribute(Constants.DOC_NAME_ATTRIBUTE, docName);
		}

		if (MuleNotificationParser.getComponentId(notification).equalsIgnoreCase(Constants.HTTP_REQUESTER))
		{
			spanBuilder.setSpanKind(SpanKind.CLIENT);
			MuleNotificationParser.addHttpRequesterAttributesToSpan(notification, getMuleConnectorConfigStore(), spanBuilder);
		}

		if (MuleNotificationParser.getComponentId(notification).equalsIgnoreCase(Constants.HTTP_LISTENER))
		{
			MuleNotificationParser.addHttpListenerAttributesToSpan(notification, spanBuilder);
		}
		
		if (MuleNotificationParser.getComponentId(notification).matches(Constants.DB_MATCHER))
		{			
			MuleNotificationParser.addDatabaseAttributesToSpan(notification, getMuleConnectorConfigStore(), spanBuilder);
		}
		
		traceStore.addMessageProcessorSpan(MuleNotificationParser.getMuleSoftTraceId(notification), 
		                                   MuleNotificationParser.getFlowId(notification), 
		                                   MuleNotificationParser.getSpanId(notification), 
				                           spanBuilder);
	}

	// --------------------------------------------------------------------------------------------
	// Processor END Notification Handler
	// --------------------------------------------------------------------------------------------
	public void handleProcessorEndEvent(MessageProcessorNotification notification)
	{
		logger.debug("Handling end event");
		
		if (MuleNotificationParser.getComponentId(notification).equalsIgnoreCase(Constants.HTTP_REQUESTER))
		{			
			MuleNotificationParser.addHttpResponseAttributesToSpan(notification, traceStore);
		}
		else if(MuleNotificationParser.getComponentId(notification).equalsIgnoreCase(Constants.LOGGER))
		{
			MuleNotificationParser.addLoggerEventsToSpan(notification, traceStore);
		}
		
		traceStore.endMessageProcessorSpan(MuleNotificationParser.getMuleSoftTraceId(notification), 
                                           MuleNotificationParser.getFlowId(notification), 
                                           MuleNotificationParser.getSpanId(notification),
                                           MuleNotificationParser.getInstantFrom(notification),
                                           notification.getException());		
	}
}