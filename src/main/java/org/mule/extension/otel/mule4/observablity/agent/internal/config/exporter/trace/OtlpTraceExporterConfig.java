package org.mule.extension.otel.mule4.observablity.agent.internal.config.exporter.trace;

import java.util.*;

import org.apache.commons.lang3.StringUtils;
import org.mule.extension.otel.mule4.observablity.agent.internal.config.exporter.Header;
import org.mule.extension.otel.mule4.observablity.agent.internal.config.exporter.OtlpExporterConfig;
import org.mule.extension.otel.mule4.observablity.agent.internal.config.exporter.OtlpExporterCompressionType;
import org.mule.extension.otel.mule4.observablity.agent.internal.config.exporter.OtlpExporterTransportProtocolType;
import org.mule.extension.otel.mule4.observablity.agent.internal.util.KeyValuePair;
import org.mule.runtime.extension.api.annotation.param.NullSafe;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Example;
import org.mule.runtime.extension.api.annotation.param.display.Summary;

//----------------------------------------------------------------------------------
//	This class stores all of the configuration data for an OpenTelemetry Protocol 
//	compliant Trace Exporter.  
//----------------------------------------------------------------------------------
public class OtlpTraceExporterConfig implements OtlpExporterConfig
{

	@Parameter
	@DisplayName(value = "Trace Collector Endpoint")
	@Summary(value = "Traget URL to which the OTLP Exporter sends traces. Must be a URL with " +
	                 "a scheme of either http or https based on the use of TLS.")
	@Example(value = "http://mycollector.com:4317/v1/traces")
	private String traceCollectorEndpoint;

	@Parameter
	@Optional(defaultValue = "HTTP_PROTOBUF")
	@DisplayName(value = "OTLP Transport Protocol")
	private OtlpExporterTransportProtocolType transportProtocol;

	@Parameter
	@Optional(defaultValue = "NONE")
	@DisplayName(value = "Compression Type")
	private OtlpExporterCompressionType compression;

	@Parameter
	@DisplayName("Max Queue Size")
	@Optional(defaultValue = "2048")
	@Summary("The maximum number of spans in the waiting queue. Any new spans are dropped once the queue is full.")
	private String maxQueueSize;
	
	@Parameter
	@DisplayName("Max Batch Export Size")
	@Optional(defaultValue = "512")
	@Summary("The maximum number of spans to export in a single batch.")
	private String maxBatchExportSize;
	
	@Parameter
	@DisplayName("Batch Export Delay Interval (millisec)")
	@Optional(defaultValue = "5000")
	@Summary("The delay interval in milliseconds between two consecutive batch exports.")
	private String batchExportDelayInterval;
	
	@Parameter
	@DisplayName("Batch Export Timeout (millsec)")
	@Optional(defaultValue = "30000")
	@Summary("Maximum time (milliseconds) the OTLP exporter will wait for a batch to export before cancelling the export.")
	private String exportTimeout;
	
	@Parameter
	@DisplayName("Trace Headers")
	@Optional
	@NullSafe
	@Summary("Key-value pairs separated by commas to pass as request headers on an OTLP trace.")
	private List<Header> headers;

	//------------------------------------------------------------------------------
	//	Helper Methods
	//------------------------------------------------------------------------------
	public List<Header> getHeaders()
	{
		return headers;
	}
	
	public OtlpExporterTransportProtocolType getTransportProtocol()
	{
		return transportProtocol;
	}
	
	public OtlpExporterCompressionType getCompression()
	{
		return compression;
	}
	
	public String getExportTimeout()
	{
		return exportTimeout;
	}

	public String getTraceCollectorEndpoint()
	{
		return traceCollectorEndpoint;
	}

	public String getMaxQueueSize()
	{
		return maxQueueSize;
	}
	
	public String getMaxBatchExportSize()
	{
		return maxBatchExportSize;
	}
	
	public String getBatchExportDelayInterval()
	{
		return batchExportDelayInterval;
	}
	
	public Map<String, String> getProperties()
	{
		Map<String, String> config = new HashMap<>();
		
		config.put("otel.traces.exporter", "otlp");
		config.put("otel.exporter.otlp.protocol", transportProtocol.getProtocolType());
		config.put("otel.exporter.otlp.traces.endpoint", getTraceCollectorEndpoint());
		config.put("otel.exporter.otlp.traces.compression", compression.getCompressionType());
		config.put("otel.bsp.schedule.delay", getBatchExportDelayInterval());
		config.put("otel.bsp.max.queue.size", getMaxQueueSize());
		config.put("tel.bsp.max.export.batch.size", getMaxBatchExportSize());
		config.put("otel.bsp.export.timeout", getExportTimeout());
		config.put("otel.exporter.otlp.traces.headers", KeyValuePair.commaSeparatedList(getHeaders()));
		
		return Collections.unmodifiableMap(config);
	}

	//------------------------------------------------------------------------------
	//	Override default Object behavior
	//------------------------------------------------------------------------------
	@Override
	public String toString()
	{
		return StringUtils.join(getProperties());
	}
	
	@Override
	public boolean equals(Object o)
	{
		if (this == o)
			return true;
		
		if (o == null || getClass() != o.getClass())
			return false;
		
		OtlpTraceExporterConfig that = (OtlpTraceExporterConfig) o;
		
		return Objects.equals(traceCollectorEndpoint, that.traceCollectorEndpoint) && 
			   (transportProtocol == that.transportProtocol) && 
			   Objects.equals(headers, that.headers);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(traceCollectorEndpoint, transportProtocol, headers);
	}
}
