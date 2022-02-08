package org.mule.extension.otel.mule4.observablity.agent.internal.operations;

import static org.mule.runtime.extension.api.annotation.param.MediaType.ANY;

import java.util.List;
import java.util.Map;

import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.apache.commons.lang3.StringUtils;
import org.mule.extension.otel.mule4.observablity.agent.internal.config.OTelMule4ObservablityAgentConfiguration;
import org.mule.extension.otel.mule4.observablity.agent.internal.config.exporter.OtlpExporterConfig;
import org.mule.extension.otel.mule4.observablity.agent.internal.config.resource.Attribute;
import org.mule.extension.otel.mule4.observablity.agent.internal.config.resource.OTelResourceConfig;
import org.mule.extension.otel.mule4.observablity.agent.internal.connection.OtelSdkConnection;
import org.mule.runtime.extension.api.annotation.param.Config;
import org.mule.runtime.extension.api.annotation.param.Connection;

/**
 * This class is a container for operations, every public method in this class
 * will be taken as an extension operation.
 */
public class OTelMule4ObservablityAgentOperations
{

	/**
	 * Example of an operation that uses the configuration and a connection instance
	 * to perform some action.
	 */
	@MediaType(value = ANY, strict = false)
	public String retrieveConfigInfo(@Config OTelMule4ObservablityAgentConfiguration configuration,
			                         @Connection OtelSdkConnection connection)
	{
		OtlpExporterConfig	exporter = configuration.getTraceExporter();
		OTelResourceConfig 	resource = configuration.getResource();
		
		Map<String, String> exporterConfig 	= exporter.getProperties();
		Map<String, String> resourceConfig 	= resource.getProperties();
				
		String configInfo = StringUtils.join(exporterConfig.toString(), "\n", resourceConfig.toString());
		
		return "Retrieved configuration info from the extension: \n" + configInfo;
	}
}
