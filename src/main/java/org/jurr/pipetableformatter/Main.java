package org.jurr.pipetableformatter;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;

@SuppressWarnings("squid:S106") // Suppress Sonar warning "Replace this use of System.out or System.err by a logger."
public class Main
{
	private static final int EXIT_OK = 0;
	private static final int EXIT_CMDLINE_INVALID = 1;

	public static void main(final String[] args) throws InterruptedException, IOException
	{
		final JCommander jCommander = JCommander.newBuilder().addObject(Settings.INSTANCE).build();
		try
		{
			jCommander.setProgramName(getCurrentExecutable());
			jCommander.parse(args);
		}
		catch (ParameterException e)
		{
			System.err.println(e.getLocalizedMessage());
			e.usage();
			System.exit(EXIT_CMDLINE_INVALID);
		}

		if (Settings.INSTANCE.isHelp())
		{
			jCommander.usage();
			System.exit(EXIT_OK);
		}

		if (Settings.INSTANCE.isWatch())
		{
			watchDirectoriesForChanges();
		}
		else
		{
			new PipeTableFormatter().pipeTablesInDirectories(Settings.INSTANCE.getFilesOrDirectories(), Settings.INSTANCE.getFileMask());
		}
	}

	private static void watchDirectoriesForChanges() throws IOException, InterruptedException
	{
		final Watcher watcher = new Watcher(Settings.INSTANCE.getFilesOrDirectories(), Settings.INSTANCE.getFileMask());
		final Thread watcherThread = new Thread(watcher);
		watcherThread.start();

		System.out.println("Watching directories for changes. Press ENTER to stop...");
		System.in.read();
		System.out.println("Stopping, hang on...");

		watcher.stop();
		watcherThread.join();
	}

	private static String getCurrentExecutable()
	{
		try
		{
			final String uri = Main.class.getProtectionDomain().getCodeSource().getLocation().toURI().toString();
			// We are a JAR, or a JAR within a WAR
			final String innerURI = uri.replaceFirst("jar:", "").replaceFirst("file:", "").replaceFirst("!/WEB-INF/classes!/", "");
			final Path currentExecutablePath = Paths.get(innerURI);
			if (!currentExecutablePath.toFile().isDirectory())
			{
				final Path currentExecutableFileName = currentExecutablePath.getFileName();
				if (currentExecutableFileName != null)
				{
					return currentExecutableFileName.toString();
				}
			}
		}
		catch (final URISyntaxException | RuntimeException e)
		{
			// Do nothing, just return the default
		}

		// We can not determine the jar file from which we run.
		// We are probably running the class directly from IntelliJ or Eclipse.
		// Default to returning the canonical name of this class.
		return Main.class.getCanonicalName();
	}
}