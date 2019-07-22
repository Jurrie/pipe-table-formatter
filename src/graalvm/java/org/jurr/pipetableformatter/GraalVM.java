package org.jurr.pipetableformatter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.oracle.svm.core.annotate.AutomaticFeature;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import org.graalvm.nativeimage.Feature;
import org.graalvm.nativeimage.RuntimeReflection;

@AutomaticFeature
class JCommanderRuntimeReflectionRegistrationFeature implements Feature
{
	@Override
	public void beforeAnalysis(final BeforeAnalysisAccess access)
	{
		RuntimeReflection.register(Settings.class);
		RuntimeReflection.register(Settings.class.getDeclaredFields());

		RuntimeReflection.register(com.beust.jcommander.validators.NoValueValidator.class);
		RuntimeReflection.registerForReflectiveInstantiation(com.beust.jcommander.validators.NoValueValidator.class);

		RuntimeReflection.register(com.beust.jcommander.validators.NoValidator.class);
		RuntimeReflection.registerForReflectiveInstantiation(com.beust.jcommander.validators.NoValidator.class);

		try
		{
			RuntimeReflection.register(com.beust.jcommander.converters.BooleanConverter.class);
			RuntimeReflection.register(com.beust.jcommander.converters.BooleanConverter.class.getConstructor(String.class));
		}
		catch (NoSuchMethodException | SecurityException e)
		{
			throw new PipeTableFormatterException("Error while setting up GraalVM stuff", e);
		}
	}
}

@TargetClass(Main.class)
final class MainSubstituted
{
	private MainSubstituted()
	{
	}

	@SuppressWarnings("squid:S106") // Suppress Sonar warning "Replace this use of System.out or System.err by a logger."
	@Substitute
	private static void watchDirectoriesForChanges()
	{
		System.err.println("The java.nio.file.WatchService is not yet supported in GraalVM's native-image.");
		System.err.println("Please see bug https://github.com/oracle/graal/issues/1253.");
	}

	@Substitute
	private static String getCurrentExecutable()
	{
		try
		{
			final Path symlinkSelf = Paths.get("/proc/self/exe");
			if (Files.isSymbolicLink(symlinkSelf))
			{
				final Path actualSelf = Files.readSymbolicLink(symlinkSelf);
				return actualSelf.getFileName().toString();
			}
		}
		catch (IOException e)
		{
			// Do nohting - return default
		}

		// We can not read /proc/sefl/exe - probably not running on a Unix with /proc filesystem
		// Return a default
		return Main.class.getCanonicalName();
	}
}