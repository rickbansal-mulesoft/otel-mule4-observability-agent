package org.mule.extension.otel.mule4.observablity.agent.internal.notification.listener;

import org.mule.extension.otel.mule4.observablity.agent.internal.notification.OTelMuleNotificationHandler;
import org.mule.runtime.api.notification.MessageProcessorNotification;
import org.mule.runtime.api.notification.MessageProcessorNotificationListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MuleMessageProcessorNotificationListener implements MessageProcessorNotificationListener<MessageProcessorNotification>
{

	private Logger logger = LoggerFactory.getLogger(MuleMessageProcessorNotificationListener.class);

	private OTelMuleNotificationHandler	otelMuleNotificationHandler;

	public MuleMessageProcessorNotificationListener(OTelMuleNotificationHandler otelMuleNotificationHandler)
	{
		this.otelMuleNotificationHandler = otelMuleNotificationHandler;
	}
	
	@Override
	public void onNotification(MessageProcessorNotification notification)
	{
		logger.debug("===> Received " + notification.getClass().getName() + ":" + notification.getActionName());

		// Event listener
		switch (Integer.parseInt(notification.getAction().getIdentifier()))
		{
			case MessageProcessorNotification.MESSAGE_PROCESSOR_PRE_INVOKE:
				otelMuleNotificationHandler.handleProcessorStartEvent(notification);
				break;

			case MessageProcessorNotification.MESSAGE_PROCESSOR_POST_INVOKE:
				otelMuleNotificationHandler.handleProcessorEndEvent(notification);
				break;
		}
	}
}