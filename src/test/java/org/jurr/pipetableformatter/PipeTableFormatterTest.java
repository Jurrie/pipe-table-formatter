package org.jurr.pipetableformatter;

import static org.junit.jupiter.api.Assertions.assertIterableEquals;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PipeTableFormatterTest
{
	@Test
	void testPipeTablesInFile(@TempDir final Path tempDir) throws IOException
	{
		// Given
		final Path sourceFile = Paths.get("src/test/resources/org/jurr/pipetableformatter/TableFormatterTest/pipeTablesInFile/input.txt");
		final Path expectedFile = sourceFile.resolveSibling("expected.txt");
		final Path testFile = tempDir.resolve("output.txt");
		Files.copy(sourceFile, testFile);
		final List<String> expected = Files.readAllLines(expectedFile, StandardCharsets.UTF_8);

		// When
		new PipeTableFormatter().pipeTablesInFile(testFile);

		// Then
		final List<String> actual = Files.readAllLines(testFile, StandardCharsets.UTF_8);

		assertIterableEquals(expected, actual);
	}
}