package org.mule.extension.otel.mule4.observablity.agent.internal.connection;

import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdkBuilder;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import  io.opentelemetry.context.propagation.TextMapPropagator;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import org.mule.extension.otel.mule4.observablity.agent.internal.config.OTelSdkConfig;
import org.mule.extension.otel.mule4.observablity.agent.internal.util.ObservabilitySemantics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is a Singleton class represents a connection to the local OpenTelemetry SDK.  
 * Any component requring access to the SDK will need to retrieve the <b>shared connection</b> 
 * instance from this clasd using the getInstance() method.
 * <p>
 * When the singleton is instantiated, the SDK will be configured by properties and values set
 * in the <i><b>OpenTelemetry Mule 4 Observability Agent Config</b></i> editor.
 */
public final class OtelSdkConnection
{
	private final Logger logger = LoggerFactory.getLogger(OtelSdkConnection.class);
	private final OpenTelemetry openTelemetry;
	
	private static OtelSdkConnection otelSdkConnection;
	private final Tracer tracer;
	private final TextMapPropagator textMapPropagator;

	//------------------------------------------------------------------------------------------------
	//	Singleton 
	//------------------------------------------------------------------------------------------------
	/**
	 * 
	 * @param name 
	 * @param version
	 * @param otelSdkConfig - container holding the configuraion for the OpenTelemetry SDK
	 * @see	
	 * <a href=https://javadoc.io/doc/io.opentelemetry/opentelemetry-api/latest/io/opentelemetry/api/OpenTelemetry.html>
	 * 	OpenTelemetry Entry Point
	 * </a>  	
	 * @see
	 * <a href=https://github.com/open-telemetry/opentelemetry-java/tree/main/sdk-extensions/autoconfigure>
	 * 	OpenTelemetry SDK Autoconfigure
	 * </a> 
	 * @see #getTracer()
	 * @see #getTextMapPropagator() 	 
	 */
	private OtelSdkConnection(String name, String version, OTelSdkConfig otelSdkConfig)
	{
		logger.info("Initializing the OpenTelemetry Mule 4 Observability Agent {}:{}", name, version);
		
		//--------------------------------------------------------------------------------------------
		//	Configure the OpenTelemetry SDK using the SDK's auto configuration builder.  
		//
		// 	Note:	Configuration details defined in the extension will override any system and env.
		//			variables.
		//
		// 	See here for autoconfigure options
		// 	https://github.com/open-telemetry/opentelemetry-java/tree/main/sdk-extensions/autoconfigure
		//--------------------------------------------------------------------------------------------
		AutoConfiguredOpenTelemetrySdkBuilder builder = AutoConfiguredOpenTelemetrySdk.builder();
		
		if (otelSdkConfig != null)
		{
			final Map<String, String> configMap = new HashMap<>();
			
			//----------------------------------------------------------------------------------------
			//	Configure the OpenTelemety SDK using properties from the various configuration editors.
			//	For now, only resource and exporter properties have editors; however, in the future,
			//	this will/coould be expanded to include metric and log property editors.
			//----------------------------------------------------------------------------------------
			if (otelSdkConfig.getResourceConfig() != null)
			{
				configMap.putAll(otelSdkConfig.getResourceConfig().getProperties());
			}
			if (otelSdkConfig.getTraceExporterConfig() != null)
			{
				configMap.putAll(otelSdkConfig.getTraceExporterConfig().getProperties());
			}

			Supplier<Map<String, String>> propertiesSupplier = () -> Collections.unmodifiableMap(configMap);

			builder.addPropertiesSupplier(propertiesSupplier);
		}
		openTelemetry = builder.build().getOpenTelemetrySdk();
		
		tracer = openTelemetry.getTracer(name, version);
		
		textMapPropagator = openTelemetry.getPropagators().getTextMapPropagator();
	}

	public void invalidate()
	{
		// Nothing to invalidate.
	}

	public static Optional<OtelSdkConnection> get()
	{
		return Optional.ofNullable(otelSdkConnection);
	}

	//------------------------------------------------------------------------------------------------
	//	Retrieve the singleton instance of this OtelSdkConnection object.  Create the singleton if it 
	//	doesn't already exist.
	//------------------------------------------------------------------------------------------------
	/**
	 * 
	 * @param otelSdkConfig
	 * @return OtelSdkConnection
	 * @see #getTracer()
	 * @see #getTextMapPropagator()
	 */
	public static synchronized OtelSdkConnection getInstance(OTelSdkConfig otelSdkConfig)
	{
		if (otelSdkConnection == null)
		{
			otelSdkConnection = new OtelSdkConnection(ObservabilitySemantics.INSTRUMENTATION_NAME,
					                                  ObservabilitySemantics.INSTRUMENTATION_VERSION, 
					                                  otelSdkConfig);
		}
		return otelSdkConnection;
	}
	
	//------------------------------------------------------------------------------------------------
	//	Retrieve a Tracer object:
	//
	//	A Tracer is used for Span creation and interaction with the in-process context.  Both  manual 
	//	and automatic Context propagation are supported.  Automatic context propagation is done using 
	// 	the Context interface.
	//------------------------------------------------------------------------------------------------
	/**
	 * 
	 * @return Optional&#60;Tracer&#62;
	 * @see	
	 * <a href=https://javadoc.io/doc/io.opentelemetry/opentelemetry-api/latest/io/opentelemetry/api/trace/Tracer.html>
	 * 	Tracer
	 * </a>  
	 * @see	
	 * <a href=https:https://javadoc.io/doc/io.opentelemetry/opentelemetry-api/latest/io/opentelemetry/api/trace/SpanBuilder.html>
	 * 	SpanBuilder
	 * </a>  
	 * @see	
	 * <a href=https://javadoc.io/doc/io.opentelemetry/opentelemetry-api/latest/io/opentelemetry/api/trace/Span.html>
	 * 	Span
	 * </a>  
	 * @see	
	 * <a href=https://javadoc.io/doc/io.opentelemetry/opentelemetry-context/latest/io/opentelemetry/context/Context.html>
	 * 	Context
	 * </a>  
	 */
	public synchronized Optional<Tracer> getTracer()
	{
		return Optional.ofNullable(tracer);
	}
	
	//------------------------------------------------------------------------------------------------
	//	Retrieve a TextMapPropagator object:
	//
	//	A TextMapPropagator injects & extracts a value as text into carriers that travel in-band across 
	//	process boundaries. Encoding is expected to conform to the HTTP Header Field semantics. The 
	//	carrier of propagated data on both the client (injector) and server (extractor) side is usually 
	//	an http request. 
	//------------------------------------------------------------------------------------------------
	/**
	 * 
	 * @return Optional&#60;TextMapPropagator&#62;
	 * @see	
	 * <a href=https://javadoc.io/doc/io.opentelemetry/opentelemetry-context/latest/io/opentelemetry/context/propagation/TextMapPropagator.html>
	 * 	TextMapPropagator
	 * </a>  
	 * @see	
	 * <a href=https://javadoc.io/doc/io.opentelemetry/opentelemetry-context/latest/io/opentelemetry/context/Context.html>
	 * 	Context
	 * </a>  
	 */
	public synchronized Optional<TextMapPropagator> getTextMapPropagator()
	{
		return Optional.ofNullable(textMapPropagator);
	}
}
