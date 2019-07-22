package org.jurr.pipetableformatter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TableFormatterTest
{
	private StringBuilder in;
	private List<String> expected;

	@Test
	void testNoFormatting()
	{
		in("| xyz | yz | z | za | zab |");
		out("| xyz | yz | z | za | zab |");
	}

	@Test
	void testSingleTableRowSingle()
	{
		in("  |   x   |  ");
		out("| x |");
	}

	@Test
	void testSingleTableRowMultiColumn()
	{
		in("  |   x   |  y  |  z  |  ");
		out("| x | y | z |");
	}

	@Test
	void testTableComments()
	{
		in("  |--   x  |   x  |  ");
		out("|-- x | x |");
	}

	@Test
	void testContentsThatLooksLikeTableComment()
	{
		in("  | --  x  |  x  |  ");
		out("| --  x | x |");
	}

	@Test
	void testTableCommentNotInFirstCell()
	{
		// JBehave sees table comments that are not in the first cell as cell content
		in("|  abc  |--  this is   not   a comment |   ...is  it  ??  |  ");
		out("| abc | --  this is   not   a comment | ...is  it  ?? |");
	}

	@Test
	void testLineComments()
	{
		in("       |   y  |   y  |  ");
		in("  !--  |   x  |   x  |  ");
		out("| y | y |");
		out("  !--  |   x  |   x  |  ");
	}

	@Test
	void testLineCommentsInsideTable()
	{
		in("  |   !--  x  |   zz  | ");
		out("| !--  x | zz |");
	}

	@Test
	void testEmptyCell()
	{
		in("||x|");
		in("|||");
		in("||||");
		out("| x |");
		out("|   |");
		out("|   |");
	}

	@Test
	void testEmptyTable()
	{
		in("When a table is empty");
		in("||");
		in("Then it will disappear");
		out("When a table is empty");
		out("Then it will disappear");
	}

	@Test
	void testSloppyTable()
	{
		in("   This | is  |  a   table!  ");
		in("   and  |  this|line|contains  | even | more | columns...||");
		out("| This | is   | a   table! |          |      |      |            |");
		out("| and  | this | line       | contains | even | more | columns... |");
	}

	@Test
	void testNonTable()
	{
		in(" This is not   |   a table.");
		out(" This is not   |   a table.");
	}

	@Test
	void testStartWithTable()
	{
		in("  |  x  |  y  |  ");
		in("Some text");
		out("| x | y |");
		out("Some text");
	}

	@Test
	void testEndWithTable()
	{
		in("Some text");
		in("  |  x  |  y  |  ");
		out("Some text");
		out("| x | y |");
	}

	@Test
	void testTableInTheMiddle()
	{
		in("Some text");
		in("  |  x  |  y  |  ");
		in("Some other text");
		out("Some text");
		out("| x | y |");
		out("Some other text");
	}

	@BeforeEach
	private void setup()
	{
		in = new StringBuilder();
		expected = new ArrayList<>();
	}

	@AfterEach
	private void teardown() throws IOException
	{
		check();
	}

	private void in(final String line)
	{
		in.append(line);
		in.append('\n');
	}

	private void out(final String line)
	{
		expected.add(line);
	}

	private void check() throws IOException
	{
		final StringReader sr = new StringReader(in.toString());

		final ByteArrayOutputStream baos = new ByteArrayOutputStream();
		final PrintStream ps = new PrintStream(baos);

		final TableFormatter tableFormatter = new TableFormatter(ps);
		tableFormatter.format(sr);
		tableFormatter.close();

		final String[] actual = new String(baos.toByteArray(), StandardCharsets.UTF_8).split("\n");

		assertEquals(expected.size(), actual.length, "The number of lines differs");

		for (int i = 0; i < actual.length; i++)
		{
			assertEquals(expected.get(i), actual[i]);
		}
	}
}