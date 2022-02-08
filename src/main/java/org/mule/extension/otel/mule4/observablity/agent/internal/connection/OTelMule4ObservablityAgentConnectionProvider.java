package org.mule.extension.otel.mule4.observablity.agent.internal.connection;

import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.api.connection.PoolingConnectionProvider;
import org.mule.runtime.api.connection.ConnectionProvider;

import java.util.function.Supplier;

import org.mule.runtime.api.connection.CachedConnectionProvider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class (as it's name implies) provides connection instances and the
 * funcionality to disconnect and validate those connections.
 * <p>
 * All connection related parameters (values required in order to create a
 * connection) must be declared in the connection providers.
 * <p>
 * This particular example is a {@link PoolingConnectionProvider} which declares
 * that connections resolved by this provider will be pooled and reused. There
 * are other implementations like {@link CachedConnectionProvider} which lazily
 * creates and caches connections or simply {@link ConnectionProvider} if you
 * want a new connection each time something requires one.
 */
public class OTelMule4ObservablityAgentConnectionProvider implements CachedConnectionProvider<OtelSdkConnection>
{	
	private final Logger LOGGER = LoggerFactory.getLogger(OTelMule4ObservablityAgentConnectionProvider.class);

	@Override
	public OtelSdkConnection connect() throws ConnectionException
	{
		Supplier<ConnectionException> connectionExecption = OTelMule4ObservablityAgentConnectionProvider::connectionException;
				
		return OtelSdkConnection.get().orElseThrow(connectionExecption);
	}

	@Override
	public void disconnect(OtelSdkConnection connection)
	{
		try
		{
			connection.invalidate();
		} 
		catch (Exception e)
		{
			LOGGER.error("Error while disconnecting from the Otel SDK: " + e.getMessage(), e);
		}
	}

	@Override
	public ConnectionValidationResult validate(OtelSdkConnection connection)
	{
		return ConnectionValidationResult.success();
	}
	
	public static ConnectionException connectionException()
	{
		return new ConnectionException("Cannot get an OpenTelemetry SDK connection before the SDK is initialized"); 
	}
}
