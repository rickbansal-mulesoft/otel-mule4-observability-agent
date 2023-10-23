package org.mule.extension.otel.mule4.observablity.agent.internal.notification;

import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.Tracer;

import org.mule.runtime.api.notification.EnrichedServerNotification;
import org.mule.runtime.api.notification.MessageProcessorNotification;
import org.mule.runtime.api.notification.PipelineMessageNotification;
import org.mule.runtime.core.api.config.MuleConfiguration;
import org.mule.extension.otel.mule4.observablity.agent.internal.config.advanced.SpanGenerationConfig;
import org.mule.extension.otel.mule4.observablity.agent.internal.connection.OtelSdkConnection;
import org.mule.extension.otel.mule4.observablity.agent.internal.metric.MuleMetricMemoryUsage;
import org.mule.extension.otel.mule4.observablity.agent.internal.metric.MuleMetricSystemWorkload;
import org.mule.extension.otel.mule4.observablity.agent.internal.util.NotificationParserUtils;
import org.mule.extension.otel.mule4.observablity.agent.internal.notification.parser.service.NotificationParserService;
import org.mule.extension.otel.mule4.observablity.agent.internal.notification.parser.service.provider.BaseNotificationParser;
import org.mule.extension.otel.mule4.observablity.agent.internal.notification.parser.service.provider.NotificationParser;
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
	
	// --------------------------------------------------------------------------------------------
	// Helper methods to retrieve various singletons.
	// --------------------------------------------------------------------------------------------	
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
	
	private SpanGenerationConfig getSpanGenerationConfig()
	{
		if (otelSdkConnection == null)
		{
			otelSdkConnection = sdkConnectionSupplier.get();
		}
		return otelSdkConnection.getSpanGenerationConfig().get();
	}
	
	private MuleConnectorConfigStore getMuleConnectorConfigStore()
	{
	    if (muleConnectorConfigStore == null)
	    {
	        if (otelSdkConnection == null)
	        {
	            otelSdkConnection = sdkConnectionSupplier.get();
	        }

	        muleConnectorConfigStore = MuleConnectorConfigStore.getInstance(getMuleConfiguration(), 
	                                                                        otelSdkConnection.getExpressionManager().get());
	    }

	    return muleConnectorConfigStore;
	}
	
	private void setCustomAttributes(SpanBuilder sb, EnrichedServerNotification n)
	{
	    if (otelSdkConnection == null)
	    {
	        otelSdkConnection = sdkConnectionSupplier.get();
	    }
	    
	    otelSdkConnection.getCustomAttributesConfig().get().setAttributes(sb, otelSdkConnection.getExpressionManager().get(), n);
	}
	
	// ============================================================================================
	//                      PIPELINE/FLOW RELATED NOTIFICATION EVENTS
	// ============================================================================================

	// --------------------------------------------------------------------------------------------
	// Flow START Notification Handler
	// --------------------------------------------------------------------------------------------	
	public void handleFlowStartEvent(PipelineMessageNotification notification)
	{
		logger.debug("Handling flow start event");

		Instant startInstant = NotificationParserUtils.getInstantFrom(notification);
		
		SpanBuilder spanBuilder = getTracer().spanBuilder(NotificationParserUtils.getSpanName(notification))
				                                                                 .setStartTimestamp(startInstant);
		
		spanBuilder.setAttribute(Constants.START_DATETIME_ATTRIBUTE, startInstant.toString());
	    
		String workload = (MuleMetricSystemWorkload.getWorkloadPercent() >= 0) 
		                  ? String.format("%.2f %%", MuleMetricSystemWorkload.getWorkloadPercent())
		                  : "Data not available";
	    /*    
		spanBuilder.setAttribute(Constants.START_WORKLOAD_ATTRIBUTE, 
		                         String.format("%.2f %%", MuleMetricSystemWorkload.getWorkloadPercent()));
		*/
		
	    spanBuilder.setAttribute(Constants.START_WORKLOAD_ATTRIBUTE, workload);
		
	    spanBuilder.setAttribute(Constants.START_HEAP_USAGE_ATTRIBUTE, 
                                 String.format("%.2f MB", MuleMetricMemoryUsage.getHeapMemoryUsage()/1000000.0));
	    
	    //
	    // add custom attributes to the trace
	    //
	    setCustomAttributes(spanBuilder, notification);
	    
	      
		NotificationParser notificationParser = NotificationParserService.getInstance().getParserFor(notification)
				                                                                       .orElse(new BaseNotificationParser());
		
		if (!traceStore.isTracePresent(NotificationParserUtils.getMuleSoftTraceId(notification)))
		{
			try
			{
				spanBuilder.setAttribute(Constants.FLOW_NAME_ATTRIBUTE, NotificationParserUtils.getDocName(notification));
				spanBuilder.setAttribute(Constants.SERVER_ID_ATTRIBUTE, NotificationParserUtils.getServerId(notification));
				
				notificationParser.startPipelineNotification(notification, getMuleConnectorConfigStore(), spanBuilder);
			} 
			catch (Exception e)
			{
				logger.debug(e.getMessage());
			}
			
			traceStore.startTrace(NotificationParserUtils.getMuleSoftTraceId(notification), 
					              NotificationParserUtils.getFlowId(notification), 
					              spanBuilder.startSpan());
		} 
		else
		{
			try
			{
				spanBuilder.setAttribute(Constants.DOC_NAME_ATTRIBUTE, NotificationParserUtils.getDocName(notification));
			} 
			catch (Exception e)
			{
				logger.debug(e.getMessage());
			}
			
			traceStore.addPipelineSpan(NotificationParserUtils.getMuleSoftTraceId(notification), 
					                   NotificationParserUtils.getFlowId(notification), 
					                   spanBuilder);
		}
	}

	// --------------------------------------------------------------------------------------------
	// Flow END Notification Handler
	// --------------------------------------------------------------------------------------------
	public void handleFlowEndEvent(PipelineMessageNotification notification)
	{
		logger.debug("Handling flow end event");
		
		String mulesoftTraceId = NotificationParserUtils.getMuleSoftTraceId(notification);
	
		traceStore.endPipelineSpan(mulesoftTraceId, 
				                   NotificationParserUtils.getFlowId(notification),
				                   NotificationParserUtils.getInstantFrom(notification),
				                   notification.getException());
		
		if (traceStore.isPipelineSpansEmpty(mulesoftTraceId))
		{
			traceStore.endTrace(mulesoftTraceId);
		}
	}

	// ============================================================================================
	//                     MESSAGE PROCESSOR RELATED NOTIFICATION EVENTS
	// ============================================================================================

	// --------------------------------------------------------------------------------------------
	// Processor START Notification Handler
	// --------------------------------------------------------------------------------------------
	public void handleProcessorStartEvent(MessageProcessorNotification notification)
	{
		logger.debug("Handling processor start event");
		
		if (NotificationParserUtils.skipParsing(notification, getSpanGenerationConfig()))
			return;
		
		NotificationParser notificationParser = NotificationParserService.getInstance()
				                                                         .getParserFor(notification)
                                                                         .orElse(new BaseNotificationParser());

		SpanBuilder spanBuilder = getTracer().spanBuilder(NotificationParserUtils.getSpanName(notification));
		
	    //
        // add custom attributes to the span
        //
        setCustomAttributes(spanBuilder, notification);
		
		notificationParser.startProcessorNotification(notification, getMuleConnectorConfigStore(), spanBuilder);
		
		traceStore.addMessageProcessorSpan(NotificationParserUtils.getMuleSoftTraceId(notification), 
		                                   NotificationParserUtils.getFlowId(notification), 
		                                   NotificationParserUtils.getSpanId(notification), 
				                           spanBuilder);
	}

	// --------------------------------------------------------------------------------------------
	// Processor END Notification Handler
	// --------------------------------------------------------------------------------------------
	public void handleProcessorEndEvent(MessageProcessorNotification notification)
	{
		logger.debug("Handling end event");
		
		if (NotificationParserUtils.skipParsing(notification, getSpanGenerationConfig()))
			return;
		
		NotificationParser notificationParser = NotificationParserService.getInstance()
				                                                         .getParserFor(notification)
                                                                         .orElse(new BaseNotificationParser());

		notificationParser.endProcessorNotification(notification, getMuleSoftTraceStore());
		
		traceStore.endMessageProcessorSpan(NotificationParserUtils.getMuleSoftTraceId(notification), 
                                           NotificationParserUtils.getFlowId(notification), 
                                           NotificationParserUtils.getSpanId(notification),
                                           NotificationParserUtils.getInstantFrom(notification));
	}
}