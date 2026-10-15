package spark;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SaveMode;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.types.StructType;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.apache.spark.sql.functions.*;

/**
 * File I/O questions:
 *  1. Read CSV with an explicit schema (why not inferSchema?)
 *  2. Handle bad / corrupt records: PERMISSIVE vs DROPMALFORMED vs FAILFAST
 *  3. Write Parquet partitioned by a column; save modes
 *  4. Partition pruning when reading back
 *  5. Control number of output files (coalesce / repartition / maxRecordsPerFile)
 *
 * TRICK: inferSchema reads the data TWICE - give a schema in production.
 *        Corrupt-record column must be declared in the schema as STRING.
 *
 * NOTE (Windows): writing needs Hadoop winutils.exe (HADOOP_HOME). Reading works without it.
 */
public class ReadWriteFiles {

    static Dataset<Row> readCsv(SparkSession spark, String mode) throws Exception {
        // from the classpath -> works whatever the IDE's working directory is
        String csv = Path.of(ReadWriteFiles.class.getResource("/data/employees.csv").toURI()).toString();
        StructType schema = StructType.fromDDL(
                "id INT, name STRING, dept STRING, salary INT, join_date DATE, _corrupt_record STRING");
        return spark.read()
                .option("header", "true")
                .option("mode", mode)                                   // PERMISSIVE | DROPMALFORMED | FAILFAST
                .option("columnNameOfCorruptRecord", "_corrupt_record")
                .schema(schema)
                .csv(csv);
    }

    public static void main(String[] args) throws Exception {
        SparkSession spark = SparkUtil.session("ReadWriteFiles");

        SparkUtil.title("PERMISSIVE - bad row kept, raw line in _corrupt_record");
        Dataset<Row> permissive = readCsv(spark, "PERMISSIVE").cache();   // cache needed to query corrupt col
        permissive.show(false);

        SparkUtil.title("Only the bad records");
        permissive.filter(col("_corrupt_record").isNotNull()).select("_corrupt_record").show(false);

        SparkUtil.title("DROPMALFORMED - bad row removed");
        readCsv(spark, "DROPMALFORMED").drop("_corrupt_record").show();

        SparkUtil.title("FAILFAST - throws on first bad row");
        try {
            readCsv(spark, "FAILFAST").drop("_corrupt_record").collect();
        } catch (Exception e) {
            System.out.println("Failed as expected: " + e.getClass().getSimpleName());
        }

        Path out = Files.createTempDirectory("spark-out");
        try {
            Dataset<Row> clean = permissive.filter(col("_corrupt_record").isNull()).drop("_corrupt_record");

            // partitionBy -> folders dept=Eng/, dept=Sales/ ; coalesce(1) -> 1 file per folder
            clean.coalesce(1).write()
                    .mode(SaveMode.Overwrite)                             // Append | Overwrite | ErrorIfExists | Ignore
                    .partitionBy("dept")
                    .option("maxRecordsPerFile", 1000)
                    .parquet(out.resolve("emp").toString());

            SparkUtil.title("Read back with partition pruning (only dept=Eng folder is scanned)");
            Dataset<Row> eng = spark.read().parquet(out.resolve("emp").toString()).filter(col("dept").equalTo("Eng"));
            eng.show();
            eng.explain();                                                // look for PartitionFilters
        } catch (Exception e) {
            System.out.println("Write skipped (on Windows set HADOOP_HOME with winutils.exe): " + e.getMessage());
        }

        spark.stop();
    }
}
