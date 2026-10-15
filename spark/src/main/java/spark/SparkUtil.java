package spark;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.RowFactory;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.types.StructType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Shared helpers: a local SparkSession and tiny inline DataFrames for every example.
 *
 * TRICK: build test DataFrames from a DDL string + rows -> no bean classes, no files.
 */
public final class SparkUtil {

    private SparkUtil() {}

    static SparkSession session(String app) {
        relaunchIfNeeded(StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).getCallerClass());
        SparkSession spark = SparkSession.builder()
                .appName(app)
                .master("local[*]")                              // all local cores, no cluster
                .config("spark.sql.shuffle.partitions", "4")     // default 200 is overkill locally
                .config("spark.ui.enabled", "false")
                .getOrCreate();
        spark.sparkContext().setLogLevel("ERROR");
        return spark;
    }

    // Same flags as .mvn/jvm.config (that file covers `mvn exec:java`, this covers IDE runs)
    private static final List<String> ADD_OPENS = Stream.of(
                    "java.lang", "java.lang.invoke", "java.lang.reflect", "java.io", "java.net", "java.nio",
                    "java.util", "java.util.concurrent", "java.util.concurrent.atomic",
                    "sun.nio.ch", "sun.nio.cs", "sun.security.action", "sun.util.calendar")
            .map(p -> "--add-opens=java.base/" + p + "=ALL-UNNAMED")
            .toList();

    /**
     * Makes the IDE's plain "Run main" work with no run-config setup.
     * Spark 3.5 needs Java 11/17/21 (Hadoop fails on 23+ with "getSubject is not supported")
     * plus --add-opens flags. If this JVM can't run Spark, start the same main class on a
     * compatible JDK with the flags, stream its output here and exit with its exit code.
     * Set SPARK_JAVA_HOME to pick the JDK explicitly.
     */
    private static void relaunchIfNeeded(Class<?> mainClass) {
        int version = Runtime.version().feature();
        boolean opened = Object.class.getModule().isOpen("sun.nio.ch", SparkUtil.class.getModule());
        if (version <= 21 && opened) return;
        if (Boolean.getBoolean("spark.interview.relaunched")) {
            throw new IllegalStateException("Relaunched JVM still can't run Spark: java " + version + ", add-opens=" + opened);
        }

        String javaHome = System.getenv("SPARK_JAVA_HOME");
        if (javaHome == null) javaHome = version <= 21 ? System.getProperty("java.home") : findCompatibleJdk();

        List<String> cmd = new ArrayList<>();
        cmd.add(Path.of(javaHome, "bin", "java").toString());
        cmd.addAll(ADD_OPENS);
        cmd.add("-Dspark.interview.relaunched=true");
        cmd.add("-cp");
        cmd.add(System.getProperty("java.class.path"));
        cmd.add(mainClass.getName());

        System.out.println("[SparkUtil] java " + version + " can't run Spark 3.5 as is -> relaunching on " + javaHome);
        try {
            System.exit(new ProcessBuilder(cmd).inheritIO().start().waitFor());
        } catch (IOException | InterruptedException e) {
            throw new IllegalStateException("Relaunch failed: " + cmd, e);
        }
    }

    // Looks in the usual JDK install folders for 17, then 11, then 21
    private static String findCompatibleJdk() {
        List<Path> roots = List.of(
                Path.of("C:/Program Files/Amazon Corretto"), Path.of("C:/Program Files/Java"),
                Path.of("C:/Program Files/Eclipse Adoptium"), Path.of("C:/Program Files/Microsoft"),
                Path.of(System.getProperty("user.home"), ".jdks"),
                Path.of("/usr/lib/jvm"), Path.of("/Library/Java/JavaVirtualMachines"));
        for (int wanted : new int[]{17, 11, 21}) {
            for (Path root : roots) {
                if (!Files.isDirectory(root)) continue;
                try (Stream<Path> dirs = Files.list(root)) {
                    Optional<Path> home = dirs
                            .filter(d -> majorVersion(d.getFileName().toString()) == wanted)
                            .map(d -> Files.isDirectory(d.resolve("Contents/Home")) ? d.resolve("Contents/Home") : d)
                            .filter(d -> Files.exists(d.resolve("bin/java")) || Files.exists(d.resolve("bin/java.exe")))
                            .findFirst();
                    if (home.isPresent()) return home.get().toString();
                } catch (IOException ignored) {
                }
            }
        }
        throw new IllegalStateException("No JDK 17/11/21 found. Install one or set SPARK_JAVA_HOME.");
    }

    // "jdk-17.0.2" -> 17, "openjdk-21.0.2" -> 21, "jdk1.8.0_202" -> 8
    private static int majorVersion(String dirName) {
        Matcher m = Pattern.compile("(\\d+)(?:\\.(\\d+))?").matcher(dirName);
        if (!m.find()) return -1;
        int major = Integer.parseInt(m.group(1));
        return major == 1 && m.group(2) != null ? Integer.parseInt(m.group(2)) : major;
    }

    // df(spark, "id INT, name STRING", new Object[]{1, "a"}, new Object[]{2, "b"})
    static Dataset<Row> df(SparkSession spark, String ddl, Object[]... rows) {
        StructType schema = StructType.fromDDL(ddl);
        List<Row> list = Arrays.stream(rows).map(RowFactory::create).toList();
        return spark.createDataFrame(list, schema);
    }

    static Object[] r(Object... values) {
        return values;
    }

    /** id, name, dept, salary, manager_id - used by most examples. */
    static Dataset<Row> employees(SparkSession spark) {
        return df(spark, "id INT, name STRING, dept STRING, salary INT, manager_id INT",
                r(1, "Alice", "Eng", 9000, null),
                r(2, "Bob", "Eng", 8000, 1),
                r(3, "Carol", "Eng", 8000, 1),
                r(4, "Dan", "Eng", 7000, 2),
                r(5, "Eve", "Sales", 6000, null),
                r(6, "Frank", "Sales", 5500, 5),
                r(7, "Grace", "Sales", 6000, 5),
                r(8, "Heidi", "HR", 4000, null),
                r(9, "Ivan", "Ops", 3000, 8));       // Ops has no row in departments
    }

    /** dept, location - Finance has no employees. */
    static Dataset<Row> departments(SparkSession spark) {
        return df(spark, "dept STRING, location STRING",
                r("Eng", "Bangalore"),
                r("Sales", "Mumbai"),
                r("HR", "Pune"),
                r("Finance", "Delhi"));
    }

    static void title(String text) {
        System.out.println("\n==== " + text + " ====");
    }
}
