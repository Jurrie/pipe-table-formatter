# pipe-table-formatter

This command-line tool will format the tables in JBehave story files.

For all command-line options, run the tool with the `--help` command-line option.

Two modes are available:
* Format a file or directory of files once
* Watch a directory of files and format when a file changes

## Format once
Use something like `java -jar ./PipeTableFormatter-1.0.0-SNAPSHOT-jar-with-dependencies.jar <filepath>`.
`<filepath>` can be the path to a file, or a directory.
If the path points to a directory, the directory is recursively scanned for files matching the file mask.

You can give multiple `<filepath>`s separated by a space.

### File mask
The default file mask is `*.story`. You can specify the file mask using the `-m` option.

## Watch a directory for changes
Use something like `java -jar ./PipeTableFormatter-1.0.0-SNAPSHOT-jar-with-dependencies.jar -w <directory>`.
The directory is recursively scanned for files matching the file mask. When such a file is changed (for example: you save it using [your favorite editor](https://eclipse.org)), the file is automatically formatted.

You can give multiple `<directory>`s separated by a space.

### Using this in a Git pre-commit hook
The format once mode is useful as a Git pre-commit hook. Use something like this:

    for file in $(git diff --name-only --cached)
    do
      java -jar ~/bin/PipeTableFormatter-1.0.0-SNAPSHOT-jar-with-dependencies.jar $file && git add $file
    done

## Using this as a dependency in other Java projects
You can also integrate this into your own Java project. Probably, you'll only need to use class `org.jurr.pipetableformatter.TableFormatter`.

## GraalVM native-image support
This project contains experimental support for GraalVM's native-image, meaning you can compile it to native code.
You'll need GraalVM installed on your system, and have the environment set up.
Basically, when you run `java -version`, you should see "GraalVM" in the output.
Then, running `mvn clean verify` will automatically create the native image.
GraalVM is automatically picked up, and the correct Maven profile is activated automatically.