package org.jurr.pipetableformatter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

public class TableFormatter implements AutoCloseable
{
	/**
	 * The character that is the column separator for tables
	 */
	private static final char COLUMN_SEPARATOR = '|';

	/**
	 * The line comment
	 */
	private static final String LINE_COMMENT = "!--";

	/**
	 * The table comment (without the leading {@value #COLUMN_SEPARATOR})
	 */
	private static final String TABLE_COMMENT = "--";

	/**
	 * The padding left and right of the cell contents
	 */
	private static final String CELL_PADDING = " ";

	/**
	 * The padding between table comment and the cell content
	 */
	private static final String TABLE_COMMENT_PADDING = " ";

	/**
	 * A list of the maximum cell content length seen per column index.
	 */
	private List<Integer> columnWidth;

	/**
	 * The content of the table (trimmed and with {@value #CELL_PADDING} left and right).
	 */
	private List<List<String>> rows;

	/**
	 * The PrintStream we should output to.
	 */
	private PrintStream ps;

	public TableFormatter(final PrintStream ps)
	{
		rows = new ArrayList<>();
		columnWidth = new ArrayList<>();
		this.ps = ps;
	}

	public void format(final Reader input) throws IOException
	{
		try (BufferedReader br = new BufferedReader(input))
		{
			String line = null;
			while ((line = br.readLine()) != null)
			{
				readLine(line);
			}
		}
	}

	private void readLine(final String line)
	{
		if (lineIsATableLine(line))
		{
			addTableRow(line);
		}
		else
		{
			dumpTable();
			ps.println(line);
		}
	}

	private boolean lineIsATableLine(final String line)
	{
		int firstPipe = line.indexOf(COLUMN_SEPARATOR);
		if (firstPipe < 0)
		{
			// No pipes found in line
			return false;
		}

		int lastPipe = line.lastIndexOf(COLUMN_SEPARATOR);
		if (firstPipe == lastPipe)
		{
			// Only one pipe found in line
			return false;
		}

		int comment = line.indexOf(LINE_COMMENT);
		if (comment < 0)
		{
			// No line comment in line
			return true;
		}

		/*
		 * Line comments break the table in JBehave. Everything below the line comment is not considered part of the table.
		 * Line comments need not be at the start of the line. But they should be before the first pipe.
		 */
		return firstPipe <= comment;
	}

	@Override
	public void close()
	{
		dumpTable();
	}

	private void addTableRow(final String line)
	{
		final List<String> cells = splitTableLineIntoCells(line);
		final List<String> row = new ArrayList<>(cells.size());

		for (int i = 0; i < cells.size(); i++)
		{
			final String cellContent = formatCellContent(cells.get(i), i);
			row.add(cellContent);

			updateColumnWidthForCell(cellContent, i);
		}

		rows.add(row);
	}

	private List<String> splitTableLineIntoCells(final String line)
	{
		final int firstPipe = line.indexOf(COLUMN_SEPARATOR);
		final int lastPipe = line.lastIndexOf(COLUMN_SEPARATOR);
		final List<String> result = new ArrayList<>();

		final String partBeforeFirstPipe = line.substring(0, firstPipe);
		if (!partBeforeFirstPipe.trim().equals(""))
		{
			result.add(partBeforeFirstPipe);
		}

		int currPos = firstPipe;
		while (currPos < lastPipe)
		{
			int nextPos = line.indexOf(COLUMN_SEPARATOR, currPos + 1);
			final String cellContent = line.substring(currPos + 1, nextPos);
			result.add(cellContent);
			currPos = nextPos;
		}

		final String partAfterLastPipe = line.substring(lastPipe + 1);
		if (!partAfterLastPipe.trim().equals(""))
		{
			result.add(partAfterLastPipe);
		}

		return result;
	}

	private void updateColumnWidthForCell(final String cell, final int columnIndex)
	{
		if (columnWidth.size() <= columnIndex)
		{
			columnWidth.add(cell.length());
		}
		else if (columnWidth.get(columnIndex) < cell.length())
		{
			columnWidth.remove(columnIndex);
			columnWidth.add(columnIndex, cell.length());
		}
	}

	/**
	 * This will return the cell content with {@value #CELL_PADDING} before and after.
	 * Table comments are supported (and will not trigger a space before the content).
	 */
	private String formatCellContent(final String content, final int columnIndex)
	{
		if (content.startsWith(TABLE_COMMENT) && columnIndex == 0)
		{
			return TABLE_COMMENT + TABLE_COMMENT_PADDING + content.substring(TABLE_COMMENT.length()).trim() + CELL_PADDING;
		}

		final String trimmedContent = content.trim();
		if (trimmedContent.startsWith(TABLE_COMMENT) && CELL_PADDING.equals(""))
		{
			return CELL_PADDING + " " + trimmedContent + CELL_PADDING;
		}

		return CELL_PADDING + trimmedContent + CELL_PADDING;
	}

	private void dumpTable()
	{
		filterEmptyColumns();
		rows.forEach(this::dumpTableRow);
		rows.clear();
		columnWidth.clear();
	}

	private void filterEmptyColumns()
	{
		final int widthOfEmptyCell = CELL_PADDING.length() * 2; // Cell padding is applied both left and right
		for (int i = columnWidth.size() - 1; i >= 0; i--)
		{
			int width = columnWidth.get(i);
			if (width == widthOfEmptyCell)
			{
				columnWidth.remove(i);
				for (List<String> row : rows)
				{
					if (row.size() > i)
					{
						row.remove(i);
					}
				}
			}
		}

		if (columnWidth.isEmpty())
		{
			rows.clear();
		}
	}

	private void dumpTableRow(final List<String> row)
	{
		ps.print(COLUMN_SEPARATOR);
		for (int i = 0; i < columnWidth.size(); i++)
		{
			dumpTableCell(row, i);
			ps.print(COLUMN_SEPARATOR);
		}
		ps.println();
	}

	private void dumpTableCell(final List<String> row, final int columnIndex)
	{
		final String cellContent = row.size() <= columnIndex ? "" : row.get(columnIndex);

		ps.print(cellContent);

		for (int x = columnWidth.get(columnIndex) - cellContent.length(); x > 0; x--)
		{
			ps.print(' ');
		}
	}
}