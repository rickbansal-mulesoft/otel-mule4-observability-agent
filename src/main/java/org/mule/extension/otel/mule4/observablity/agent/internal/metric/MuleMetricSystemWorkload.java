package org.mule.extension.otel.mule4.observablity.agent.internal.metric;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.metrics.ObservableDoubleMeasurement;

public class MuleMetricSystemWorkload
{
    private static final Logger logger = LoggerFactory.getLogger(MuleMetricSystemWorkload.class);
    
    private static MuleMetricSystemWorkload muleMetricCpuWorkload;
    private static OperatingSystemMXBean osMxBean;

    //------------------------------------------------------------------------------------------------
    //  Singleton 
    //------------------------------------------------------------------------------------------------
    /**
     * 
     */
    private MuleMetricSystemWorkload(OpenTelemetry openTelemetry)
    {
        logger.info("Initializing the System Workload Utilization Metric"); 
        
        Meter meter = openTelemetry.getMeter("org.mulesoft.extension.otel.mule4.observability.agent.metrics");

        Consumer<ObservableDoubleMeasurement> recordMeasure = (result) -> MuleMetricSystemWorkload.record(result);
        
        osMxBean = ManagementFactory.getOperatingSystemMXBean();

        meter.gaugeBuilder("system.workload.utilization")
             .setDescription("Reports the System Workload Utilization")
             .setUnit("percent")
             .buildWithCallback(recordMeasure);    
    }
    
    public static void record(ObservableDoubleMeasurement mesaure)
    {
        Attributes attribute = Attributes.of(AttributeKey.stringKey("mule.fullDomain"), "fullDomain");
        
        if (osMxBean != null)
        {    
            mesaure.record(getWorkloadPercent(), attribute);
        }
    }
    
    public static double getWorkloadPercent()
    {
        double percentWorkload = (osMxBean.getSystemLoadAverage()/osMxBean.getAvailableProcessors()) * 100;
        return percentWorkload;
    }
    
    //------------------------------------------------------------------------------------------------
    //  Create the singleton if it doesn't already exist.
    //------------------------------------------------------------------------------------------------
    /**
     * 
     * @param openTelemetry instance
     */
    public static void setInstance(OpenTelemetry ot)
    {
        if (muleMetricCpuWorkload == null)
        {
            muleMetricCpuWorkload = new MuleMetricSystemWorkload(ot);
        }
    }  
}
