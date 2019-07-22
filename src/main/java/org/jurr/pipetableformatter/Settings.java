package org.jurr.pipetableformatter;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.beust.jcommander.Parameter;

final class Settings
{
	public static final Settings INSTANCE = new Settings();

	private static final String DEFAULT_FILE_MASK = "*.story";
	private static final String DEFAULT_DIRECTORY = ".";

	@Parameter(names = { "-h", "--help" }, description = "Show this help message", help = true)
	private boolean help;

	@Parameter(names = { "-m", "--file-mask" }, description = "File mask", required = false)
	private String fileMask = DEFAULT_FILE_MASK;

	@Parameter(names = { "-w", "--watch" }, description = "Watch for changes in given directories, and format on file change", required = false)
	private boolean watch = false;

	@Parameter(description = "Files to parse (or directories to traverse)", required = true)
	private List<String> filesOrDirectories;
	private List<Path> filesOrDirectoriesAsPath = null;

	private Settings()
	{
		filesOrDirectories = new ArrayList<>();
		filesOrDirectories.add(DEFAULT_DIRECTORY);
	}

	public boolean isHelp()
	{
		return help;
	}

	public String getFileMask()
	{
		return fileMask;
	}

	public boolean isWatch()
	{
		return watch;
	}

	public List<Path> getFilesOrDirectories()
	{
		if (filesOrDirectoriesAsPath == null)
		{
			filesOrDirectoriesAsPath = filesOrDirectories.stream().map(Paths::get).collect(Collectors.toList());
		}
		return filesOrDirectoriesAsPath;
	}
}