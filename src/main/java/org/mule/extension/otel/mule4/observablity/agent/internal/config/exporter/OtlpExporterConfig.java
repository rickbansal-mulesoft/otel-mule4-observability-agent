package org.mule.extension.otel.mule4.observablity.agent.internal.config.exporter;

import java.util.Map;

//----------------------------------------------------------------------------------
//	General interface for an OpenTelemetry Protocol (OTLP) Exporter: 
//		- trace/span, metric, log
//----------------------------------------------------------------------------------
public interface OtlpExporterConfig
{
	Map<String, String> getProperties();
}
