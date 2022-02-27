package org.mule.extension.otel.mule4.observablity.agent.internal.config;

import org.mule.extension.otel.mule4.observablity.agent.internal.config.advanced.SpanGenerationConfig;
import org.mule.extension.otel.mule4.observablity.agent.internal.config.exporter.trace.OtlpTraceExporterConfig;
import org.mule.extension.otel.mule4.observablity.agent.internal.config.resource.OTelResourceConfig;
import org.mule.extension.otel.mule4.observablity.agent.internal.connection.OTelMule4ObservablityAgentConnectionProvider;
import org.mule.extension.otel.mule4.observablity.agent.internal.connection.OtelSdkConnection;
import org.mule.extension.otel.mule4.observablity.agent.internal.notification.OTelMuleNotificationHandler;
import org.mule.extension.otel.mule4.observablity.agent.internal.notification.listener.MuleMessageProcessorNotificationListener;
import org.mule.extension.otel.mule4.observablity.agent.internal.notification.listener.MulePipelineNotificationListener;
import org.mule.extension.otel.mule4.observablity.agent.internal.operations.OTelMule4ObservablityAgentOperations;
import org.mule.runtime.api.meta.ExpressionSupport;
import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.connectivity.ConnectionProviders;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.runtime.extension.api.annotation.param.display.Placement;
import org.mule.runtime.extension.api.annotation.param.display.Summary;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.notification.NotificationListenerRegistry;
import org.mule.runtime.core.api.config.MuleConfiguration;

import java.util.function.Supplier;
import javax.inject.Inject;

//----------------------------------------------------------------------------------
//	This class stores all of the OpenTelemetry configuration details that are common
// 	across all of the extension's operations, connection providers and connections.
//
//	Configuration details can be categorized into two primary groupings:
//		1. Resource configuration details
//		2. Exporter configuration details
//
// 	Note:  System or Environment Variables will be overridden by this configuration. 
//----------------------------------------------------------------------------------
/**
 * This class represents an extension configuration. Values set in this class are 
 * commonly used across multiple operations since they represent something core 
 * from the extension.
 */
@Operations(OTelMule4ObservablityAgentOperations.class)
@ConnectionProviders(OTelMule4ObservablityAgentConnectionProvider.class)
public class OTelMule4ObservablityAgentConfiguration implements Startable
{

	//------------------------------------------------------------------------------
	//	This Parameter Group stores the configuration details that are common to
	//	an OpenTelemetry Resource.  An OpenTelemetry Resource is an immutable 
	//	representation of the entity producing telemetry as Attributes. 
	//
	//	For example, a process producing telemetry that is running in a container on 
	//	Kubernetes has a Pod name, it is in a namespace and possibly is part of a 
	//	Deployment which also has a name. All three of these attributes can be 
	//	included in the Resource
	//------------------------------------------------------------------------------
	/**
	 * @see OTelResourceConfig
	 */
	@ParameterGroup(name = "Resource")
	//@DisplayName(value = "Resources")
	@Placement(order = 1)
	@Summary("Open Telemetry Resource Configuration. An OpenTelemetry Resource is an " +
			 "immutable representation of the entity producing telemetry specified as " +
			 " a set of attributes.")
	
	private OTelResourceConfig resource;

	public OTelResourceConfig getResource()          
	{
		return resource;
	}

	//------------------------------------------------------------------------------
	//	An Exporter is an OpenTelemetry component responsible for batching and 
	//	transporting telemetry data to a backend system using the OpenTelemetry 
	//	Protocol (OTLP).  
	//
	//	OTLP is a general-purpose, request/response telemetry data delivery protocol
	//	designed to work over various transports and encodings.
	// 	
	//	Each OpenTelemetry signal type (trace, metric, log) can have its own
	//	Exporter.  
	//
	//  Note:  This @Parameter stores configuration details for a Trace Exporter.
	//------------------------------------------------------------------------------
	/**
	 * @see 
	 * 	<a href="https://github.com/open-telemetry/opentelemetry-specification/blob/main/specification/protocol/exporter.md#specify-protocol">
	 * 		OTLP Exporter
	 * 	</a>
	 */
	@ParameterGroup(name = "OTLP Trace Exporter")
	//@Parameter()
	//@DisplayName(value = "OTLP Trace Exporter Configuration")
	@Summary("OpenTelemetry Protocol Trace Exporter Configuration.  <b>Note:  System or Environment Variables will BE overriden by this configuration.</b>")
	@Placement(order = 2)
	@Expression(ExpressionSupport.NOT_SUPPORTED)
	// private OtlpExporterConfig traceExporter;
	private OtlpTraceExporterConfig traceExporter;

	//public OtlpExporterConfig getTraceExporter()
	public  OtlpTraceExporterConfig getTraceExporter() 
	{
		return traceExporter;
	}
	
	@ParameterGroup(name = "Span Generation")
	@Summary("Select if Message Processors spans should be added to the trace in general.  You can bypass certain Message Processors by adding them to list below.")
	@Placement(order = 3)
	@Expression(ExpressionSupport.NOT_SUPPORTED)
	private SpanGenerationConfig spanGenerationConfig;

	public  SpanGenerationConfig getSpanGenerationConfig() 
	{
		return spanGenerationConfig;
	}
	
	
	@Inject
	NotificationListenerRegistry notificationListenerRegistry;
	
	@Inject 
	MuleConfiguration muleConfiguration;

	@SuppressWarnings("unused")
	@Override
	public void start() throws MuleException
	{
		//------------------------------------------------------------------------------
		// 	Based on observations from our partner, this phase is too early to initiate
		//	the configuration and initialization of the OpenTelemetry SDK. It fails with 
		// 	unresolved Otel dependencies.
		//
		// 	The SDK initialization needs to be deferred to a later stage when all 
		//	dependencies have been resolved.  For now, SDK init will be done when the
		//	OpenTelemetry Mule Notification Handler receives its first event/notification.
		//------------------------------------------------------------------------------
		Supplier<OtelSdkConnection> otelSdkConnection = () -> OtelSdkConnection.getInstance(new OTelSdkConfig(getResource(), 
				 						                                                                      getTraceExporter(),
				 						                                                                      muleConfiguration,
				                                                                                              getSpanGenerationConfig()));
		
		OTelMuleNotificationHandler otelMuleNotificationHandler = new OTelMuleNotificationHandler(otelSdkConnection);
		
		//FirstProcessorInterceptorFactory firstProcessorInterceptorFactory = new FirstProcessorInterceptorFactory();
		
		/*
		 * 	Register notification listeners for both Mule Pipeline and Mule Message Processor 
		 * 	events/notifications.
		 * 
		 * 	Note:  Pipeline notifications are for the flow; while, Message Processor notifications 
		 * 	are for the individual steps/processors in the flow.
		 */
		notificationListenerRegistry.registerListener(new MuleMessageProcessorNotificationListener(otelMuleNotificationHandler));
		notificationListenerRegistry.registerListener(new MulePipelineNotificationListener(otelMuleNotificationHandler));
	}
}
