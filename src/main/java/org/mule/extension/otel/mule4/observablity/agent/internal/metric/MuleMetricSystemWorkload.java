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
        /*
         *  The system load average is the sum of the number of runnable entities queued to the available 
         *  processors and the number of runnable entities running on the available processors averaged 
         *  over the last minute of time. The way in which the load average is calculated is operating system 
         *  specific but is typically a damped time-dependent average. If the load average is not available, 
         *  a negative value is returned.
         *  
         *  The system load average indicates not only CPU demand but also file I/O demand, network I/O demand, 
         *  disk I/0 demand, and cycles waiting for locks.
         *  
         *  For example, assume the load average is 6 over the last minute, then for:
         *  CPU = 1
         *   - percentWorkload = 600% which means the system was 500% overloaded the past minute (i.e., there 
         *     was 5x more work queued up then the system could handle
         *  CPU = 12
         *   - percentWorkload = 50% which means the system could've handled an additional 2x more work over  
         *     the past minute
         */
        
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
