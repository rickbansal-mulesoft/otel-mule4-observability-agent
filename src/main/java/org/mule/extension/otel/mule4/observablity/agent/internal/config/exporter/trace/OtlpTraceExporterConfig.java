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
import org.mule.runtime.extension.api.annotation.param.display.Path;
import org.mule.runtime.extension.api.annotation.param.display.Placement;
import org.mule.runtime.extension.api.annotation.param.display.Summary;
import org.mule.runtime.api.meta.model.display.PathModel.Type;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//----------------------------------------------------------------------------------
//	This class stores all of the configuration data for an OpenTelemetry Protocol 
//	compliant Trace Exporter.  
//----------------------------------------------------------------------------------
public class OtlpTraceExporterConfig implements OtlpExporterConfig
{
    private Logger logger = LoggerFactory.getLogger(OtlpTraceExporterConfig.class);

    @Parameter
    @DisplayName(value = "DISABLE Trace Signals")
    @Placement(order = 1, tab = "OTLP Trace Exporter")
    @Summary("Enable/Disable tracing signal gathering in this application.  If disabled, all other configuration " +
            "details will be ignored.")
    @Optional (defaultValue = "false")
    private boolean disableTraces;
    
    @Parameter
	@DisplayName(value = "Trace Collector Endpoint")
    @Placement(order = 10, tab = "OTLP Trace Exporter")
	@Summary(value = "Target URL to which the OTLP Exporter sends traces. Must be a URL with " +
	                 "a scheme of either http or https based on the use of TLS.")
    @Optional(defaultValue = "")
	@Example(value = "http://mycollector.com:4317/v1/traces")
	private String traceCollectorEndpoint;

	@Parameter
    @DisplayName("Endpoint Certificate Path")
    @Placement(order = 20, tab = "OTLP Trace Exporter")
	@Path(type = Type.FILE, acceptedFileExtensions = "txt", acceptsUrls = true)
	@Optional(defaultValue = "")
    @Example(value = "mycert.pem")
    @Summary("The path to the file containing trusted certificates to use when verifying an OTLP " + 
             "trace server's TLS credentials. The file should contain one or more X.509 certificates " +
             "in PEM format. By default the host platform's trusted root certificates are used.")
    private String traceCertificatePath;
	
	@Parameter
	@DisplayName(value = "OTLP Transport Protocol")
    @Placement(order = 30, tab = "OTLP Trace Exporter")
    @Optional(defaultValue = "HTTP_PROTOBUF")
	private OtlpExporterTransportProtocolType traceTransportProtocol;

	@Parameter
    @DisplayName(value = "Compression Type")
    @Placement(order = 40, tab = "OTLP Trace Exporter")
	@Optional(defaultValue = "NONE")
	private OtlpExporterCompressionType traceCompression;
	   
    @Parameter
    @DisplayName("Trace Headers")
    @Placement(order = 45, tab = "OTLP Trace Exporter")
    @Optional
    @NullSafe
    @Summary("Key-value pairs separated by commas to pass as request headers on an OTLP trace export.")
    private List<Header> traceHeaders;
    
	@Parameter
    @DisplayName("Max Queue Size")
    @Placement(order = 50, tab = "OTLP Trace Exporter")
	@Optional(defaultValue = "2048")
	@Summary("The maximum number of spans in the waiting queue. Any new spans are dropped once the queue is full.")
	private String maxQueueSize;
	
	@Parameter
    @DisplayName("Max Batch Export Size")
    @Placement(order = 60, tab = "OTLP Trace Exporter")
	@Optional(defaultValue = "512")
	@Summary("The maximum number of spans to export in a single batch.")
	private String maxBatchExportSize;
	
	@Parameter
    @DisplayName("Batch Export Delay Interval (millisec)")
    @Placement(order = 70, tab = "OTLP Trace Exporter")
	@Optional(defaultValue = "5000")
	@Summary("The delay interval in milliseconds between two consecutive batch exports.")
	private String batchExportDelayInterval;
	
	@Parameter
    @DisplayName("Batch Export Timeout (millsec)")
    @Placement(order = 80, tab = "OTLP Trace Exporter")
	@Optional(defaultValue = "30000")
	@Summary("Maximum time (milliseconds) the OTLP exporter will wait for a batch to export before cancelling the export.")
	private String exportTimeout;

	//------------------------------------------------------------------------------
	//	Helper Methods
	//------------------------------------------------------------------------------
    public boolean getDisableTraces()
    {
        return disableTraces;
    }
        
	public List<Header> getHeaders()
	{
		return traceHeaders;
	}
	
	public OtlpExporterTransportProtocolType getTransportProtocol()
	{
		return traceTransportProtocol;
	}
	
	public OtlpExporterCompressionType getCompression()
	{
		return traceCompression;
	}
	
	public String getCollectorEndpoint()
	{
		return traceCollectorEndpoint;
	}

    public String getCertificatePath()
    {
        return traceCertificatePath;
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
	
	public String getExportTimeout()
	{
	    return exportTimeout;
	}

	public Map<String, String> getProperties()
	{
		Map<String, String> config = new HashMap<>();
		
		if (!getDisableTraces())
	    {
    		config.put("otel.traces.exporter", "otlp");
    		config.put("otel.exporter.otlp.traces.protocol", getTransportProtocol().getProtocolType());
    		config.put("otel.exporter.otlp.traces.endpoint", getCollectorEndpoint());
    		config.put("otel.exporter.otlp.traces.compression", getCompression().getCompressionType());
    		config.put("otel.bsp.schedule.delay", getBatchExportDelayInterval());
    		config.put("otel.bsp.max.queue.size", getMaxQueueSize());
    		config.put("otel.bsp.max.export.batch.size", getMaxBatchExportSize());
    		config.put("otel.bsp.export.timeout", getExportTimeout());
    		config.put("otel.exporter.otlp.traces.headers", KeyValuePair.commaSeparatedList(getHeaders()));
    		
            logger.debug("get certificate :" + getCertificatePath() + " is empty:" + getCertificatePath().isEmpty());
            if(!(getCertificatePath().isEmpty())) 
            {            
                config.put("otel.exporter.otlp.traces.certificate", getCertificatePath());
            }
            
            logger.info("Trace processing is ENABLED");
    	}
		else
		{
		    config.put("otel.traces.exporter", "none");
            logger.info("Trace processing is DISABLED");
		}
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
        
        return Objects.equals(this.getCollectorEndpoint(), that.getCollectorEndpoint()) && 
               (this.getTransportProtocol() == that.getTransportProtocol()) && 
               Objects.equals(this.getHeaders(), that.getHeaders());
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(getCollectorEndpoint(), getTransportProtocol(), getHeaders());
    }
}
