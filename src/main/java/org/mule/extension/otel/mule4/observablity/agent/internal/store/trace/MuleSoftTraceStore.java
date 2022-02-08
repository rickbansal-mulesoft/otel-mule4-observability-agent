package org.mule.extension.otel.mule4.observablity.agent.internal.store.trace;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.context.Context;
import io.opentelemetry.api.trace.Span;

import org.mule.extension.otel.mule4.observablity.agent.internal.util.ObservabilitySemantics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//-------------------------------------------------------------------------------
//	Class for storing all MuleSoft traces/flows associated with a MuleSoft app.
//	The class is a collection of MuleSoftTraces where each MuleSoftTrace has an
//	associated rootSpan (i.e., the flow) and some number of child spans (i.e.,
// 	a message processor - Logger, Transformer, ...).  A new MuleSoftTrace is
//	started whenever a flow is initiated by a Source (e.g., an HTTP Listener).
//-------------------------------------------------------------------------------
public class MuleSoftTraceStore
{
	private static Logger logger = LoggerFactory.getLogger(MuleSoftTraceStore.class);

	//------------------------------------------------------------------------
	//	Collection of MuleSoftTraces persisted as simple hash map
	//------------------------------------------------------------------------
	private Map<String, MuleSoftTrace> muleSoftTraces = new ConcurrentHashMap<>();
	
	//------------------------------------------------------------------------
	//	Nested class for storing all MuleSoft spans (pipeline and message 
	//	processor) associated with a MuleSoft trace.
	//------------------------------------------------------------------------
	private class MuleSoftTrace
	{
		private Span traceRootSpan;
		
		private Map<String, PipelineSpan> pipelineSpans = new ConcurrentHashMap<>();
		
		private MuleSoftTrace(String rootSpanId, Span rootSpan)
		{
			this.traceRootSpan = rootSpan;
			pipelineSpans.put(rootSpanId, new PipelineSpan(rootSpan));
		}
		
		
		private Span getRootSpan()
		{
			return traceRootSpan;
		}
		
		
		private boolean isPipelineSpansEmpty()
		{
			return pipelineSpans.isEmpty();
		}
		
		private void putPipelineSpan(String pipelineSpanId, Span pipelineSpan)
		{
			pipelineSpans.put(pipelineSpanId, new PipelineSpan(pipelineSpan));
		}
		
		private PipelineSpan removePipelineSpan(String pipelineSpanId)
		{
			return pipelineSpans.remove(pipelineSpanId);
		}
		
		private PipelineSpan getPipelineSpan(String pipelineSpanId)
		{
			return pipelineSpans.get(pipelineSpanId);
		}

		private void end()
		{
			pipelineSpans.forEach((id, pipelineSpan) -> pipelineSpan.end());
			//traceRootSpan.end();
		}

		//--------------------------------------------------------------------
		//	Nested class for storing all MuleSoft processor spans associated 
		//	with a MuleSoft pipeline span.
		//--------------------------------------------------------------------
		private class PipelineSpan
		{
			private Span pipelineRootSpan;
			private Map<String, Span> messageProcessorSpans = new ConcurrentHashMap<>();
			
			private PipelineSpan(Span rootSpan)
			{
				this.pipelineRootSpan = rootSpan;
			}
	
			private Span getRootSpan()
			{
				return pipelineRootSpan;
			}
			
			private void putSpan(String spanId, Span span)
			{
				this.messageProcessorSpans.put(spanId, span);
			}
			
			private Span getSpan(String spanID)
			{
				return this.messageProcessorSpans.get(spanID);			
			}
			
			private void endSpan(String spanId, Instant endInstant, Exception e)
			{
				Span messageProcessorSpan = messageProcessorSpans.get(spanId);
				
				if (e != null)
				{
					messageProcessorSpan.setStatus(StatusCode.ERROR, e.getMessage());
					messageProcessorSpan.recordException(e);
				}
				messageProcessorSpan.setAttribute(ObservabilitySemantics.END_DATETIME_ATTRIBUTE, endInstant.toString());
				messageProcessorSpan.end(endInstant);
			}
			
			private void end()
			{
				end(Instant.now(), null);
			}
			
			private void end(Instant endInstant, Exception e)
			{
				messageProcessorSpans.forEach((id, span) -> span.end());
				
				if (e != null)
				{
					pipelineRootSpan.setStatus(StatusCode.ERROR, e.getMessage());
					pipelineRootSpan.recordException(e);
				}
				pipelineRootSpan.setAttribute(ObservabilitySemantics.END_DATETIME_ATTRIBUTE, endInstant.toString());
				pipelineRootSpan.end(endInstant);
			}
		}
	}

	//------------------------------------------------------------------------
	//	Various Public Helper Methods for MuleSoft Traces
	//------------------------------------------------------------------------
	/**
	 * 
	 * @param mulesoftTraceId
	 * @return <b>true</b> if a trace with this id exists in the store; else false
	 */
	public boolean isTracePresent(String mulesoftTraceId)
	{
		return muleSoftTraces.containsKey(mulesoftTraceId);
	}

	/**
	 * 
	 * @param mulesoftTraceId
	 * @return <b>true</b> if there are no more pipeline spans associated with this 
	 * 		   traceid, else false
	 */
	public boolean isPipelineSpansEmpty(String mulesoftTraceId)
	{
		return muleSoftTraces.get(mulesoftTraceId).isPipelineSpansEmpty();
	}
	
	/**
	 * Create a new MuleSoftTrace with id {@code mulesoftTraceid} and set {@code rootSpan} as 
	 * the overall uber (root/parent) span
	 * 
	 * @param mulesoftTraceId - unique id for this trace
	 * @param rootSpanId - unique id for the root (parent) span for this trace
	 * @param rootSpan - the parent {@link Span}
	 */
	public void startTrace(String mulesoftTraceId, String rootSpanId, Span rootSpan)
	{
		muleSoftTraces.put(mulesoftTraceId, new MuleSoftTrace(rootSpanId, rootSpan));
	}
	
	public Context getTraceContextFor(String mulesoftTraceId)
	{
		Context context = Context.current();
		
		if (isTracePresent(mulesoftTraceId))
		{
			context = muleSoftTraces.get(mulesoftTraceId).getRootSpan().storeInContext(Context.current());
		}

		return context;
	}
	
	/**
	 * 	Remove the trace with id {@code mulesoftTraceid} from the store. Also, close off the trace 
	 *  by ending each span within the trace.
	 *  
	 *  @param mulesoftTraceId - unique id for this trace
	 */
	public void endTrace(String mulesoftTraceId)
	{
		MuleSoftTrace muleSoftTrace = muleSoftTraces.remove(mulesoftTraceId);
		muleSoftTrace.end();
	}

	//------------------------------------------------------------------------
	//	Helper Methods for Pipeline Spans
	//------------------------------------------------------------------------
	public void addPipelineSpan(String mulesoftTraceId, String pipelineId, SpanBuilder spanBuilder)
	{
		MuleSoftTrace muleSoftTrace = muleSoftTraces.get(mulesoftTraceId);
		
		Span newSpan = spanBuilder.setParent(Context.current().with(muleSoftTrace.getRootSpan()))
				                  .startSpan();
		
		muleSoftTrace.putPipelineSpan(pipelineId, newSpan);
	}
	
	public void endPipelineSpan(String mulesoftTraceId, String pipelineId)
	{
		endPipelineSpan(mulesoftTraceId, pipelineId, Instant.now(), null);
	}
	
	public void endPipelineSpan(String mulesoftTraceId, String pipelineId, Instant endInstant, Exception e)
	{
		MuleSoftTrace muleSoftTrace = muleSoftTraces.get(mulesoftTraceId);
		MuleSoftTrace.PipelineSpan pipelineSpan = muleSoftTrace.removePipelineSpan(pipelineId);
		pipelineSpan.end(endInstant, e);
	}
	
	//------------------------------------------------------------------------
	//	Helper Methods for Message Processor Spans
	//------------------------------------------------------------------------
	public void addMessageProcessorSpan(String mulesoftTraceId, String pipelineId, 
			                            String messageProcessorId, SpanBuilder spanBuilder)
	{
		MuleSoftTrace muleSoftTrace = muleSoftTraces.get(mulesoftTraceId);
		MuleSoftTrace.PipelineSpan pipelineSpan = muleSoftTrace.getPipelineSpan(pipelineId);
		
		Span newMessageProcessorSpan = spanBuilder.setParent(Context.current()
				                                                    .with(pipelineSpan
				                                                    .getRootSpan()))
				                                  .startSpan();
				
		pipelineSpan.putSpan(messageProcessorId, newMessageProcessorSpan);
	}

	public Span getMessageProcessorSpan(String mulesoftTraceId, String pipelineId, 
			                            String messageProcessorId)
	{
		return muleSoftTraces.get(mulesoftTraceId)
				             .getPipelineSpan(pipelineId)
				             .getSpan(messageProcessorId);
	}
	
	
	public void endMessageProcessorSpan(String mulesoftTraceId, String pipelineId, 
                                        String messageProcessorId, Instant endInstant, Exception e)
	{
		muleSoftTraces.get(mulesoftTraceId).getPipelineSpan(pipelineId).endSpan(messageProcessorId, endInstant, e);
	}
}
