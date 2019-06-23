package net.adamcin.oakpal.cli;

import static net.adamcin.oakpal.core.Fun.result1;

import java.io.File;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import net.adamcin.oakpal.core.Nothing;
import org.jetbrains.annotations.NotNull;

/**
 * {@link Main} hosts the {@link #main(String[])} method, and as an object, captures ENV, stdin, stdout, and stderr as
 * abstracted variables for use by commands.
 */
final class Main implements Console {
    private final File cwd;
    private final Map<String, String> env;
    private final PrintStream stdout;
    private final PrintStream stderr;
    private final Map<File, DisposablePrinter> printers = new HashMap<>();

    Main(final File cwd,
         final Map<String, String> env,
         final PrintStream stdout,
         final PrintStream stderr) {
        this.cwd = cwd;
        this.env = env;
        this.stdout = stdout;
        this.stderr = stderr;
    }

    @Override
    public File getCwd() {
        return this.cwd;
    }

    @Override
    public Map<String, String> getEnv() {
        return Collections.unmodifiableMap(this.env);
    }

    @Override
    public IO<Nothing> printLine(final @NotNull Object object) {
        return () -> {
            stdout.println(object.toString());
            return Nothing.instance;
        };
    }

    @Override
    public IO<Nothing> printLineErr(final Object object) {
        return () -> {
            stderr.println(object.toString());
            return Nothing.instance;
        };
    }

    class DisposablePrinterImpl implements DisposablePrinter {
        final PrintWriter writer;

        DisposablePrinterImpl(final @NotNull PrintWriter writer) {
            this.writer = writer;
        }

        @Override
        public void dispose() {
            writer.close();
        }

        @Override
        public IO<Nothing> apply(final Object object) {
            return () -> {
                writer.println(object.toString());
                return Nothing.instance;
            };
        }
    }

    private static final DisposablePrinter SILENT_PRINTER = new DisposablePrinter() {
        @Override
        public void dispose() {

        }

        @Override
        public IO<Nothing> apply(final Object o) {
            return IO.empty;
        }
    };

    @Override
    public DisposablePrinter openPrinter(final @NotNull File outFile) {
        DisposablePrinter printer = result1((File file) -> new PrintWriter(file)).apply(outFile)
                .map(writer -> (DisposablePrinter) new DisposablePrinterImpl(writer))
                .getOrElse(SILENT_PRINTER);
        printers.put(outFile, printer);
        return printer;
    }

    @Override
    public void dispose() {
        printers.values().stream().forEach(DisposablePrinter::dispose);
        printers.clear();
    }

    int doMain(final @NotNull String[] args) {
        final Command command = new Command();
        return command.perform(this, args).get();
    }

    /**
     * Let's do a mental map:
     * <p>
     * 1. input?
     * <p>
     * 2. output?
     * <p>
     * 3. status code?
     *
     * @param args argv yo
     */
    public static void main(final String[] args) {
        StackTraceElement[] stack = new Exception().getStackTrace();
        final StackTraceElement last = stack[stack.length - 1];
        final boolean iStartedIt = Main.class.getName().equals(last.getClassName())
                && "main".equals(last.getMethodName());

        Main main = new Main(new File(".").getAbsoluteFile(),
                System.getenv(), System.out, System.err);

        if (iStartedIt) {
            System.setOut(System.err);
            Runtime.getRuntime().exit(main.doMain(args));
        } else {
            main.doMain(args);
        }
    }
}
