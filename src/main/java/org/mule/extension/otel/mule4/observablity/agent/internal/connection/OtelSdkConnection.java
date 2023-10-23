package org.mule.extension.otel.mule4.observablity.agent.internal.connection;

import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdkBuilder;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.propagation.TextMapPropagator;

import java.io.FileReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import org.mule.extension.otel.mule4.observablity.agent.internal.config.OTelSdkConfig;
import org.mule.extension.otel.mule4.observablity.agent.internal.config.advanced.CustomAttributesConfig;
import org.mule.extension.otel.mule4.observablity.agent.internal.config.advanced.SpanGenerationConfig;
import org.mule.extension.otel.mule4.observablity.agent.internal.metric.MuleMetricMemoryUsage;
import org.mule.extension.otel.mule4.observablity.agent.internal.metric.MuleMetricSystemWorkload;
import org.mule.extension.otel.mule4.observablity.agent.internal.util.Constants;
import org.mule.runtime.core.api.config.DefaultMuleConfiguration;
import org.mule.runtime.core.api.config.MuleConfiguration;
import org.mule.runtime.core.api.el.ExpressionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonParser;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * This is a Singleton class represents a connection to the local OpenTelemetry SDK.  
 * Any component requiring access to the SDK will need to retrieve the <b>shared connection</b> 
 * instance from this class using the getInstance() method.
 * <p>
 * When the singleton is instantiated, the SDK will be configured by properties and values set
 * in the <i><b>OpenTelemetry Mule 4 Observability Agent Config</b></i> editor.
 */
public final class OtelSdkConnection
{
	private static final Logger logger = LoggerFactory.getLogger(OtelSdkConnection.class);
	private final OpenTelemetry openTelemetry;
	
	private static OtelSdkConnection   otelSdkConnection;
	private final Tracer               tracer;
	private final TextMapPropagator    textMapPropagator;
	private final MuleConfiguration    muleConfiguration;
	private final SpanGenerationConfig spanGenerationConfig;
	
	private final ExpressionManager        expressionManager;
	private final CustomAttributesConfig   customAttributesConfig;
	
	//------------------------------------------------------------------------------------------------
	//	Singleton 
	//------------------------------------------------------------------------------------------------
	/**
	 * 
	 * @param name 
	 * @param version
	 * @param otelSdkConfig - container holding the configuration for the OpenTelemetry SDK
	 * @see	
	 * <a href=https://javadoc.io/doc/io.opentelemetry/opentelemetry-api/latest/io/opentelemetry/api/OpenTelemetry.html>
	 * 	OpenTelemetry Entry Point
	 * </a>  	
	 * @see
	 * <a href=https://github.com/open-telemetry/opentelemetry-java/tree/main/sdk-extensions/autoconfigure>
	 * 	OpenTelemetry SDK Auto-configure
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
			//	this will/could be expanded to include metric and log property editors.
			//----------------------------------------------------------------------------------------
			if (otelSdkConfig.getResourceConfig() != null)
			{
				configMap.putAll(otelSdkConfig.getResourceConfig().getProperties());
			}
			if (otelSdkConfig.getTraceExporterConfig() != null)
			{
				configMap.putAll(otelSdkConfig.getTraceExporterConfig().getProperties());
			}
			
			if (otelSdkConfig.getMetricExporterConfig() != null)
			{
			    configMap.putAll(otelSdkConfig.getMetricExporterConfig().getProperties());
			}

			Supplier<Map<String, String>> propertiesSupplier = () -> Collections.unmodifiableMap(configMap);

			builder.addPropertiesSupplier(propertiesSupplier);
		}
		
		openTelemetry     = builder.build().getOpenTelemetrySdk();
		tracer            = openTelemetry.getTracer(name, version);
		textMapPropagator = openTelemetry.getPropagators().getTextMapPropagator();
		
		muleConfiguration = otelSdkConfig.getMuleConfiguration();
        expressionManager = otelSdkConfig.getExpressionManager();
		
		spanGenerationConfig   = otelSdkConfig.getSpanGenerationConfig();
		customAttributesConfig = otelSdkConfig.getCustomAttributesConfig();
		
		MuleMetricMemoryUsage.setInstance(openTelemetry);
		MuleMetricSystemWorkload.setInstance(openTelemetry);
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
			otelSdkConnection = new OtelSdkConnection(Constants.INSTRUMENTATION_NAME,
						                              getAgentVersion(otelSdkConfig),
					                                  otelSdkConfig);
		}
		return otelSdkConnection;
	}
	
	//------------------------------------------------------------------------------------------------
	//	Helper method to retrieve the current version of the extension by parsing through the 
	// 	classloader-model.json file. There might be an easier way to do this but I could not find it...
	//------------------------------------------------------------------------------------------------
	private static String getAgentVersion(OTelSdkConfig otelSdkConfig)
	{
		String version = Constants.INSTRUMENTATION_VERSION_DEFAULT;

		try
		{
		    DefaultMuleConfiguration defaultMuleConfiguration = (DefaultMuleConfiguration) otelSdkConfig.getMuleConfiguration();
		    
			String filePath = defaultMuleConfiguration.getMuleHomeDirectory() + "/apps/" + 
                              defaultMuleConfiguration.getDataFolderName() + 
                              "/META-INF/mule-artifact/classloader-model.json";
			
			JsonElement jsonElement = JsonParser.parseReader(new FileReader(filePath));
			
			if (!jsonElement.isJsonObject())
				return version;
			
			JsonObject jsonObject = jsonElement.getAsJsonObject();
			
			if (!jsonObject.has("dependencies"))
				return version;
						
			Iterator<JsonElement> dependenciesIt = jsonObject.get("dependencies").getAsJsonArray().iterator();
			
			while (dependenciesIt.hasNext())	
			{				
				Set<Entry<String, JsonElement>> dependencySet = dependenciesIt.next().getAsJsonObject().entrySet();
				
				Iterator<Entry<String, JsonElement>> setIt = dependencySet.iterator();
				
				while (setIt.hasNext())
				{
					Entry<String, JsonElement> entry = setIt.next();
					
					if (entry.getValue().isJsonObject())
					{
						JsonObject jo = entry.getValue().getAsJsonObject();
						
						if (jo.has("artifactId"))
						{
							String artificatId = jo.get("artifactId").getAsString();
							
							if (artificatId.equalsIgnoreCase(Constants.AGENT_ARTIFACT_ID))
								version = jo.get("version").getAsString();
							
							break;  	// from inner loop
						}
					}

					logger.debug("Entry:  " + entry.toString());				
				}
				
				if (!version.equalsIgnoreCase(Constants.INSTRUMENTATION_VERSION_DEFAULT))
					break;				// from outer loop
			}
		}
		catch (Exception e)
		{
			logger.debug(e.getMessage());
		}
		
		return version;
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
	
	/**
	 * 
	 * @return the MuleConfiguration for this application
	 */
	public Optional<MuleConfiguration> getMuleConfiguration()
	{
		return Optional.ofNullable(muleConfiguration);
	}
	
	public Optional<SpanGenerationConfig> getSpanGenerationConfig()
	{
		return Optional.ofNullable(spanGenerationConfig);
	}
	
	public Optional<ExpressionManager> getExpressionManager()
	{
	    return Optional.ofNullable(expressionManager);
	}

	public Optional<CustomAttributesConfig> getCustomAttributesConfig()
	{
	    return Optional.ofNullable(customAttributesConfig);
	}
}
