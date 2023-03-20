package org.mule.extension.otel.mule4.observablity.agent.internal.config.exporter.metric;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.mule.extension.otel.mule4.observablity.agent.internal.config.exporter.Header;
import org.mule.extension.otel.mule4.observablity.agent.internal.config.exporter.OtlpExporterCompressionType;
import org.mule.extension.otel.mule4.observablity.agent.internal.config.exporter.OtlpExporterConfig;
import org.mule.extension.otel.mule4.observablity.agent.internal.config.exporter.OtlpExporterTransportProtocolType;
import org.mule.extension.otel.mule4.observablity.agent.internal.util.KeyValuePair;
import org.mule.runtime.api.meta.model.display.PathModel.Type;
import org.mule.runtime.extension.api.annotation.param.NullSafe;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Example;
import org.mule.runtime.extension.api.annotation.param.display.Path;
import org.mule.runtime.extension.api.annotation.param.display.Placement;
import org.mule.runtime.extension.api.annotation.param.display.Summary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//----------------------------------------------------------------------------------
//  This class stores all of the configuration data for an OpenTelemetry Protocol 
//  compliant Metric Exporter.  
//----------------------------------------------------------------------------------
public class OtlpMetricExporterConfig implements OtlpExporterConfig
{

    private Logger logger = LoggerFactory.getLogger(OtlpMetricExporterConfig.class);

    @Parameter
    @DisplayName(value = "DISABLE Metric Signals")
    @Placement(order = 1, tab = "OTLP Metric Exporter")
    @Summary("Enable/Disable metric signal gathering in this application.  If disabled, all other configuration " +
            "details will be ignored.")
    @Optional (defaultValue = "true")
    private boolean disableMetrics;
    
    @Parameter
    @DisplayName(value = "Metric Collector Endpoint")
    @Placement(order = 10, tab = "OTLP Metric Exporter")
    @Summary(value = "Target URL to which the OTLP Exporter sends metrics. Must be a URL with " +
                     "a scheme of either http or https based on the use of TLS.")
    @Optional(defaultValue = "")
    @Example(value = "http://mycollector.com:4317/v1/metrics")
    private String metricCollectorEndpoint;

    @Parameter
    @DisplayName("Endpoint Certificate Path")
    @Placement(order = 20, tab = "OTLP Metric Exporter")
    @Path(type = Type.FILE, acceptedFileExtensions = "txt", acceptsUrls = true)
    @Optional(defaultValue = "")
    @Example(value = "mycert.pem")
    @Summary("The path to the file containing trusted certificates to use when verifying an OTLP " + 
             "trace server's TLS credentials. The file should contain one or more X.509 certificates " +
             "in PEM format. By default the host platform's trusted root certificates are used.")
    private String metricCertificatePath;
    
    @Parameter
    @DisplayName(value = "OTLP Transport Protocol")
    @Placement(order = 30, tab = "OTLP Metric Exporter")
    @Optional(defaultValue = "HTTP_PROTOBUF")
    private OtlpExporterTransportProtocolType metricTransportProtocol;

    @Parameter
    @DisplayName(value = "Compression Type")
    @Placement(order = 40, tab = "OTLP Metric Exporter")
    @Optional(defaultValue = "NONE")
    private OtlpExporterCompressionType metricCompression;
       
    @Parameter
    @DisplayName("Metric Headers")
    @Placement(order = 50, tab = "OTLP Metric Exporter")
    @Optional
    @NullSafe
    @Summary("Key-value pairs separated by commas to pass as request headers on an OTLP trace export.")
    private List<Header> metricHeaders;    
    
    @Parameter
    @DisplayName("Metric Export Interval (ms)")
    @Placement(order = 60, tab = "OTLP Metric Exporter")
    @Summary(value = "The interval, in milliseconds, between the start of two export attempts.")
    @Optional(defaultValue = "60000")
    private String metricExportInterval;
    
    //------------------------------------------------------------------------------
    //  Helper Methods
    //------------------------------------------------------------------------------
    public boolean getDisableMetrics()
    {
        return disableMetrics;
    }
    
    public List<Header> getHeaders()
    {
        return metricHeaders;
    }
    
    public OtlpExporterTransportProtocolType getTransportProtocol()
    {
        return metricTransportProtocol;
    }
    
    public OtlpExporterCompressionType getCompression()
    {
        return metricCompression;
    }
    
    public String getCollectorEndpoint()
    {
        return metricCollectorEndpoint;
    }

    public String getCertificatePath()
    {
        return metricCertificatePath;
    }
    
    public String getMetricExportInterval()
    {
        return metricExportInterval;
    }
    
    public Map<String, String> getProperties()
    {
        Map<String, String> config = new HashMap<>();
        
        if (!getDisableMetrics())
        {
            config.put("otel.metrics.exporter", "otlp");
            config.put("otel.exporter.otlp.metrics.protocol", getTransportProtocol().getProtocolType());
            config.put("otel.exporter.otlp.metrics.endpoint", getCollectorEndpoint());
            config.put("otel.exporter.otlp.metrics.compression", getCompression().getCompressionType());
            config.put("otel.exporter.otlp.metrics.headers", KeyValuePair.commaSeparatedList(getHeaders()));
     
            config.put("otel.metric.export.interval", getMetricExportInterval());
    
            logger.debug("get certificate :" + getCertificatePath() + " is empty:" + getCertificatePath().isEmpty());
            if(!(getCertificatePath().isEmpty())) 
            {            
                config.put("otel.exporter.otlp.metrics.certificate", getCertificatePath());
            }
            logger.info("Metric processing is ENABLED");
        }
        else
        {
            config.put("otel.metrics.exporter", "none");
            logger.info("Metric processing is DISABLED");
        }
        return Collections.unmodifiableMap(config);
    }

    //------------------------------------------------------------------------------
    //  Override default Object behavior
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
        
        OtlpMetricExporterConfig that = (OtlpMetricExporterConfig) o;
        
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