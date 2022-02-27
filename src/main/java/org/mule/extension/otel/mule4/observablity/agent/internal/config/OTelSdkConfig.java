package org.mule.extension.otel.mule4.observablity.agent.internal.config;

import org.mule.extension.otel.mule4.observablity.agent.internal.config.advanced.SpanGenerationConfig;
import org.mule.extension.otel.mule4.observablity.agent.internal.config.exporter.OtlpExporterConfig;
import org.mule.extension.otel.mule4.observablity.agent.internal.config.resource.OTelResourceConfig;
import org.mule.runtime.core.api.config.DefaultMuleConfiguration;
import org.mule.runtime.core.api.config.MuleConfiguration;

//----------------------------------------------------------------------------------
//	This class is a helper/wrapper class which stores all of the high level config
//	objects needed to configure the OpenTelemetry SDK.
//----------------------------------------------------------------------------------
/**
 * 
 * Configuration details for the OpenTelemetry SDK
 *
 */
public class OTelSdkConfig
{
	private final OTelResourceConfig 	resourceConfig;
	private final OtlpExporterConfig 	traceExporterConfig;
	private final MuleConfiguration		muleConfiguration;
	private final SpanGenerationConfig	spanGenerationConfig;

	public OTelSdkConfig(OTelResourceConfig r, OtlpExporterConfig t, MuleConfiguration m, SpanGenerationConfig s)
	{
		this.resourceConfig = r;
		this.traceExporterConfig = t;
		this.muleConfiguration = m;
		this.spanGenerationConfig = s;
	}

	public OTelResourceConfig getResourceConfig()
	{
		return resourceConfig;
	}

	public OtlpExporterConfig getTraceExporterConfig()
	{
		return traceExporterConfig;
	}
	
	public MuleConfiguration getMuleConfiguration()
	{
		return muleConfiguration;
	}
	
	public SpanGenerationConfig getSpanGenerationConfig()
	{
		return spanGenerationConfig;
	}
}