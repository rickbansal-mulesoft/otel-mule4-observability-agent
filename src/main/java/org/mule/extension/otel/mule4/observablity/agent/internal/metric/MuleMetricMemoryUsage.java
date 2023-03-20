package org.mule.extension.otel.mule4.observablity.agent.internal.metric;

import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.metrics.ObservableDoubleMeasurement;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;

public class MuleMetricMemoryUsage
{
    private static final Logger logger = LoggerFactory.getLogger(MuleMetricMemoryUsage.class);
    
    private static MuleMetricMemoryUsage muleMetricMemoryUsage;
    private static MemoryMXBean memoryMxBean;

    //------------------------------------------------------------------------------------------------
    //  Singleton 
    //------------------------------------------------------------------------------------------------
    /**
     * 
     * @param openTelemetry 
     */
    private MuleMetricMemoryUsage(OpenTelemetry openTelemetry)
    {
        logger.info("Initializing the Memory Usage Metric"); 
        
        Meter meter = openTelemetry.getMeter("org.mulesoft.extension.otel.mule4.observability.agent.metrics");
        
        Consumer<ObservableDoubleMeasurement> recordMeasure = (result) -> MuleMetricMemoryUsage.record(result);

        memoryMxBean = ManagementFactory.getMemoryMXBean();
        
        meter.gaugeBuilder("jvm.memory.heap.usage")
             .setDescription("Reports JVM Heap memory usage.")
             .setUnit("byte")
             .buildWithCallback(recordMeasure);
    }
    
    public static void record(ObservableDoubleMeasurement mesaure)
    {
        Attributes attribute = Attributes.of(AttributeKey.stringKey("mule.memory.usage"), "heap");;
        
        mesaure.record(getHeapMemoryUsage(), attribute);
    }
    
    public static long getHeapMemoryUsage()
    {
        return (memoryMxBean != null) ? memoryMxBean.getHeapMemoryUsage().getUsed() : 0;
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
        if (muleMetricMemoryUsage == null)
        {
            muleMetricMemoryUsage = new MuleMetricMemoryUsage(ot);
        }
    }      
}
