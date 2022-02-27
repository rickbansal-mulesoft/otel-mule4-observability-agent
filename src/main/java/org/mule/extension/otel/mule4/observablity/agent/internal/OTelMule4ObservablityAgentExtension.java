package org.mule.extension.otel.mule4.observablity.agent.internal;

import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.SubTypeMapping;
import org.mule.extension.otel.mule4.observablity.agent.internal.config.OTelMule4ObservablityAgentConfiguration;
import org.mule.extension.otel.mule4.observablity.agent.internal.config.exporter.OtlpExporterConfig;
import org.mule.extension.otel.mule4.observablity.agent.internal.config.exporter.trace.OtlpTraceExporterConfig;
import org.mule.runtime.extension.api.annotation.Configurations;
import org.mule.runtime.extension.api.annotation.dsl.xml.Xml;

/**
 * This is the main class of an extension, is the entry point from which
 * configurations, connection providers, operations and sources are going to be
 * declared.
 * <p>
 * @see
 * <a href="https://github.com/open-telemetry/opentelemetry-java">OpenTelemetry Java Repository</a>
 */
@Xml(prefix = "otel-mule4-observablity-agent")
@Extension(name = "OpenTelemetry Mule 4 Observablity Agent", vendor ="MuleSoft")
@Configurations(OTelMule4ObservablityAgentConfiguration.class)
//@SubTypeMapping(baseType = OtlpExporterConfig.class, 
//		        subTypes = { OtlpTraceExporterConfig.class})
public class OTelMule4ObservablityAgentExtension
{

}