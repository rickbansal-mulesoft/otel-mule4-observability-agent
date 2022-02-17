package org.mule.extension.otel.mule4.observablity.agent.internal.notification.parser.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;

import org.mule.extension.otel.mule4.observablity.agent.internal.notification.parser.service.provider.NotificationParser;
import org.mule.runtime.api.notification.EnrichedServerNotification;

// ------------------------------------------------------------------------------------------------
// Singleton service for NotificationParser providers
// ------------------------------------------------------------------------------------------------
/**
 * 
 * Singleton to load all NotificationParser services available and provide a reference to the right
 * one based on if the parser is capable of parsing the specified notification.
 *
 *@see #getInstance()
 */
public class NotificationParserService
{
	private static NotificationParserService service;
	private final List<NotificationParser> notificationParsers;
	
	// --------------------------------------------------------------------------------------------
	// Constructor - Singleton:  Load in all NotificationParser providers and store them in a cache
	// --------------------------------------------------------------------------------------------
	private NotificationParserService()
	{
		ServiceLoader<NotificationParser> loader = ServiceLoader.load(NotificationParser.class,
				                                                      NotificationParser.class.getClassLoader());
		
		// ----------------------------------------------------------------------------------------
		// Cache the list of all of the notification parser providers
		// ----------------------------------------------------------------------------------------
		List<NotificationParser> list = new ArrayList<>();
		loader.iterator().forEachRemaining(list::add);
		
		notificationParsers = Collections.unmodifiableList(list);
	}
	
	// --------------------------------------------------------------------------------------------
	// Get a reference to the Singleton
	// --------------------------------------------------------------------------------------------
	public static synchronized NotificationParserService getInstance()
	{
		if (service == null)
		{
			service = new NotificationParserService();
		}
		return service;
	}

	// --------------------------------------------------------------------------------------------
	// Find a parser that can support this notification event
	// --------------------------------------------------------------------------------------------
	public Optional<NotificationParser> getParserFor(EnrichedServerNotification notification)
	{
		//NotificationParser parserParser = null;
		
		return notificationParsers.stream().filter(np -> np.canParse(notification)).findFirst();
	}
}