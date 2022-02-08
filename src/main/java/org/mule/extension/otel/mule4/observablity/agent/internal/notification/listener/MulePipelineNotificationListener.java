package org.mule.extension.otel.mule4.observablity.agent.internal.notification.listener;

import org.mule.extension.otel.mule4.observablity.agent.internal.notification.OTelMuleNotificationHandler;
import org.mule.runtime.api.notification.PipelineMessageNotification;
import org.mule.runtime.api.notification.PipelineMessageNotificationListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * Listener for Mule notifications on flow start, end and completion.
 */
public class MulePipelineNotificationListener implements PipelineMessageNotificationListener<PipelineMessageNotification>
{

	private Logger logger = LoggerFactory.getLogger(MulePipelineNotificationListener.class);
	private OTelMuleNotificationHandler	otelMuleNotificationHandler;

	public MulePipelineNotificationListener(OTelMuleNotificationHandler otelMuleNotificationHandler)
	{
		this.otelMuleNotificationHandler = otelMuleNotificationHandler;
	}
	
	@Override
	public void onNotification(PipelineMessageNotification notification)
	{
		logger.debug("===> Received " + notification.getClass().getName() + ":" + notification.getActionName());

		// Event listener
		switch (Integer.parseInt(notification.getAction().getIdentifier()))
		{
			case PipelineMessageNotification.PROCESS_START:
				otelMuleNotificationHandler.handleFlowStartEvent(notification);
				break;

			// On exception this event doesn't fire, only on successful flow completion.
			case PipelineMessageNotification.PROCESS_END:
				break;

			case PipelineMessageNotification.PROCESS_COMPLETE:
				otelMuleNotificationHandler.handleFlowEndEvent(notification);
				break;
		}
	}

}
