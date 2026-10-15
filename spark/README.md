# Spark (Java) interview solutions

Frequently asked Spark coding questions, solved with the Java API (Spark 3.5.3, Java 17).
Every file is self-contained: inline sample data, a `main` that prints each answer, local mode, no cluster.

## Run

**IntelliJ:** right-click `spark/pom.xml`, choose **Add as Maven Project** (one time only), then click the ▶ next to any `main`.
You don't need to edit any run configuration.

Spark 3.5 needs Java 11/17/21 plus `--add-opens` flags, and the project SDK here is JDK 25.
`SparkUtil.session()` detects this and relaunches the same class on a local JDK 17 with the flags, so the output
still shows in the IDE console. To choose the JDK yourself, set `SPARK_JAVA_HOME`.

**Command line:**

```bash
cd spark
mvn -q compile exec:java -Dexec.mainClass=spark.WindowFunctions
```

> Windows: anything that **writes** files (Parquet output in `ReadWriteFiles`, checkpoints in
> `StructuredStreaming`) needs Hadoop `winutils.exe` with `HADOOP_HOME` set. Everything else runs as is.

## Files

| File | Questions |
|------|-----------|
| `WordCount` | Word count with RDD / DataFrame / SQL, top K words |
| `RddBasics` | reduceByKey vs groupByKey, average per key (aggregateByKey, combineByKey), mapPartitions, broadcast + accumulator, lazy evaluation and lineage, repartition vs coalesce |
| `WindowFunctions` | Top N per group, Nth highest salary, row_number vs rank vs dense_rank, running total, lag/lead, moving average, % of group, above dept average, first/last per group |
| `Joins` | All join types, semi/anti joins, self join (employee-manager), earns more than manager, broadcast join |
| `Duplicates` | Find duplicates, distinct vs dropDuplicates, keep the latest record per key |
| `ConsecutiveAndSessions` | N consecutive login days (gaps and islands), longest streak, sessionization with a 30 min gap |
| `PivotUnpivot` | pivot, unpivot with `stack()` and `Dataset.unpivot` |
| `NestedJson` | Nested JSON, explode vs explode_outer vs posexplode, from_json/to_json, generic schema flattener, CSV string to rows |
| `NullHandling` | Null count per column, na.fill/drop, coalesce, forward fill, null-safe equality |
| `Scd2` | SCD Type 2 with plain DataFrames |
| `SkewAndPerformance` | Salted join, two-stage aggregation, partition distribution, cache/persist, AQE, explain plans |
| `UdfExamples` | UDF via DataFrame API and SQL, why built-in functions are preferred |
| `TypedDataset` | RDD vs DataFrame vs Dataset, bean encoders, groupByKey/mapGroups |
| `SparkSqlQuestions` | The classic questions in pure Spark SQL (2nd highest, MoM growth, relational division, ...) |
| `DateQuestions` | Mixed date formats, date parts, MoM growth, filling missing dates, time windows |
| `ReadWriteFiles` | CSV with a schema, corrupt records (PERMISSIVE/DROPMALFORMED/FAILFAST), partitioned Parquet, partition pruning |
| `StructuredStreaming` | Event-time windows, watermarks, output modes |

## Theory quick answers

- **Transformation vs action**: transformations (`map`, `filter`, `join`) are lazy and build a DAG. Actions (`count`, `collect`, `write`) trigger a job.
- **Narrow vs wide**: in a narrow transformation each output partition depends on one input partition (`map`, `filter`). A wide one needs a shuffle (`groupBy`, `join`, `distinct`) and starts a new **stage**.
- **Job, stage, task**: one action creates a job. The job is split into stages at shuffle boundaries, and each stage runs one task per partition.
- **Driver vs executor**: the driver plans and schedules work and holds `collect()` results. Executors run the tasks and cache data.
- **Catalyst / Tungsten**: Catalyst optimizes the logical plan (predicate pushdown, column pruning, constant folding) and then picks a physical plan. Tungsten handles off-heap binary rows and whole-stage codegen.
- **AQE** (on by default since 3.2): at runtime it coalesces small shuffle partitions, switches sort-merge joins to broadcast joins, and splits skewed partitions.
- **Join strategies**: Broadcast Hash (small side < `autoBroadcastJoinThreshold`, 10MB), Shuffle Hash, and Sort Merge (the default for two large tables).
- **cache vs persist**: `cache()` is `persist(MEMORY_AND_DISK)` for DataFrames. `persist` lets you choose the storage level. Both are lazy, so call `unpersist()` when you are done.
- **repartition vs coalesce**: `repartition` does a full shuffle and can increase or decrease the partition count. `coalesce` only merges partitions, so it is cheap but can only decrease the count.
- **partitionBy vs bucketBy**: `partitionBy` writes one folder per value, which allows partition pruning on read. `bucketBy` writes a fixed number of hashed files per table and can avoid the shuffle in later joins.
- **Spark OOM fixes**: avoid `collect()` on big data, raise `spark.sql.shuffle.partitions` or rely on AQE, fix skew, broadcast only small tables, and tune `spark.executor.memory`/`memoryOverhead`.
- **Fault tolerance**: lost partitions are recomputed from the lineage. Checkpointing truncates long lineages, and streaming queries use a checkpoint directory.
- **Small files problem**: compact with `coalesce`/`repartition` before writing, set `maxRecordsPerFile`, or use Delta `OPTIMIZE`.
