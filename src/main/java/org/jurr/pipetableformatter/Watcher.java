package org.jurr.pipetableformatter;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class Watcher implements Runnable
{
	private boolean running = true;
	private final List<Path> directories;
	private final String fileMask;

	Watcher(final List<Path> directories, final String fileMask)
	{
		this.directories = directories;
		this.fileMask = fileMask;
	}

	public void stop()
	{
		running = false;
	}

	@Override
	public void run()
	{
		final String javaNioFileMask = "glob:**/" + fileMask;
		final PathMatcher pathMatcher = FileSystems.getDefault().getPathMatcher(javaNioFileMask);

		try (WatchService watcher = FileSystems.getDefault().newWatchService())
		{
			final Map<WatchKey, Path> keys = new HashMap<>();

			setupWatchService(watcher, keys);

			while (running && !keys.isEmpty())
			{
				final WatchKey key = watcher.poll(5, TimeUnit.SECONDS);
				if (key == null)
				{
					continue;
				}

				handleWatchServiceEvent(pathMatcher, keys, key);
			}
		}
		catch (InterruptedException e)
		{
			running = false;
			Thread.currentThread().interrupt();
		}
		catch (IOException e)
		{
			throw new WatcherException("Error while creating watch service", e);
		}
	}

	private void handleWatchServiceEvent(final PathMatcher pathMatcher, final Map<WatchKey, Path> keys, final WatchKey key)
	{
		final Path dir = keys.get(key);
		if (dir == null)
		{
			return;
		}

		for (WatchEvent<?> event : key.pollEvents())
		{
			final WatchEvent.Kind<?> kind = event.kind();

			if (kind == StandardWatchEventKinds.OVERFLOW)
			{
				continue;
			}

			@SuppressWarnings("unchecked")
			final WatchEvent<Path> ev = (WatchEvent<Path>) event;
			final Path filename = ev.context();
			final Path file = dir.resolve(filename);

			if (pathMatcher.matches(file))
			{
				new PipeTableFormatter().pipeTablesInFile(file);
			}
		}

		if (!key.reset())
		{
			keys.remove(key);
		}
	}

	private void setupWatchService(final WatchService watcher, final Map<WatchKey, Path> keys)
	{
		Consumer<Path> register = p -> {
			if (!p.toFile().exists() || !p.toFile().isDirectory())
			{
				throw new IllegalArgumentException(p + " does not exist or is not a directory");
			}
			try
			{
				Files.walkFileTree(p, new SimpleFileVisitor<Path>()
				{
					@Override
					public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException
					{
						final WatchEvent.Kind<?>[] events = new WatchEvent.Kind[] { StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY };

						@SuppressWarnings({ "restriction", "squid:S1191" }) // Suppress Sonar warning "Use classes from the Java API instead of Sun classes."
						final WatchKey watchKey = dir.register(watcher, events, com.sun.nio.file.SensitivityWatchEventModifier.HIGH);

						keys.put(watchKey, dir);

						return FileVisitResult.CONTINUE;
					}
				});
			}
			catch (IOException e)
			{
				throw new WatcherException("Error registering directories " + p, e);
			}
		};

		directories.forEach(register::accept);
	}
}
