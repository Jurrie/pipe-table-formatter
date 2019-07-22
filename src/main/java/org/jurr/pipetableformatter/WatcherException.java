package org.jurr.pipetableformatter;

public class WatcherException extends RuntimeException
{
	private static final long serialVersionUID = 1L;

	WatcherException(final String message, final Throwable cause)
	{
		super(message, cause);
	}
}
