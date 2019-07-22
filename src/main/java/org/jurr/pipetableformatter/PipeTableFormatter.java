package org.jurr.pipetableformatter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class PipeTableFormatter
{
	public void pipeTablesInDirectories(final List<Path> directories, final String fileMask)
	{
		directories.parallelStream().forEach(p -> pipeTablesInDirectory(p, fileMask));
	}

	private void pipeTablesInDirectory(final Path directory, final String fileMask)
	{
		final String javaNioFileMask = "glob:**/" + fileMask;
		final PathMatcher pathMatcher = FileSystems.getDefault().getPathMatcher(javaNioFileMask);

		try (Stream<Path> allFiles = Files.walk(directory))
		{
			allFiles.parallel().filter(pathMatcher::matches).forEach(this::pipeTablesInFile);
		}
		catch (IOException e)
		{
			throw new PipeTableFormatterException("Error while listing files in directory", e);
		}
	}

	public void pipeTablesInFile(final Path storyFile)
	{
		try
		{
			final byte[] oldContent = Files.readAllBytes(storyFile);
			final int assumedNewContentSize = (int) (oldContent.length * 1.1d);

			try (final ByteArrayOutputStream baos = new ByteArrayOutputStream(assumedNewContentSize); //
					final PrintStream ps = new PrintStream(baos, false, StandardCharsets.UTF_8.name()); //
					final ByteArrayInputStream bais = new ByteArrayInputStream(oldContent); //
					final InputStreamReader isr = new InputStreamReader(bais, StandardCharsets.UTF_8))
			{
				final TableFormatter table = new TableFormatter(ps);

				table.format(isr);

				table.close();
				final byte[] newContent = baos.toByteArray();

				if (!Arrays.equals(newContent, oldContent))
				{
					atomicallyWriteFile(storyFile, newContent);
				}
			}
		}
		catch (IOException e)
		{
			throw new PipeTableFormatterException("Error during reading or writing of file", e);
		}
	}

	private void atomicallyWriteFile(final Path path, final byte[] bytes)
	{
		final Path directory = path.getParent();
		if (directory == null)
		{
			throw new IllegalArgumentException("Story file " + path.toString() + " has no parent");
		}

		final Path filename = path.getFileName();
		if (filename == null)
		{
			throw new IllegalArgumentException("Story file " + path.toString() + " has no filename");
		}

		try
		{
			final Path tempFile = Files.createTempFile(directory, filename.toString(), null);
			Files.write(tempFile, bytes);
			Files.move(tempFile, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
		}
		catch (IOException e)
		{
			throw new PipeTableFormatterException("Error while atomically writing file", e);
		}
	}
}