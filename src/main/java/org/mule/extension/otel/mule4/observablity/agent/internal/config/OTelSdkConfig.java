package org.mule.extension.otel.mule4.observablity.agent.internal.config;

import org.mule.extension.otel.mule4.observablity.agent.internal.config.exporter.OtlpExporterConfig;
import org.mule.extension.otel.mule4.observablity.agent.internal.config.resource.OTelResourceConfig;

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

	public OTelSdkConfig(OTelResourceConfig r, OtlpExporterConfig t)
	{
		this.resourceConfig = r;
		this.traceExporterConfig = t;
	}

	public OTelResourceConfig getResourceConfig()
	{
		return resourceConfig;
	}

	public OtlpExporterConfig getTraceExporterConfig()
	{
		return traceExporterConfig;
	}
}