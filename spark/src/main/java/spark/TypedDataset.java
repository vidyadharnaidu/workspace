package spark;

import org.apache.spark.api.java.function.FilterFunction;
import org.apache.spark.api.java.function.MapFunction;
import org.apache.spark.api.java.function.MapGroupsFunction;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Encoder;
import org.apache.spark.sql.Encoders;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import scala.Tuple2;

import java.io.Serializable;

/**
 * Q: RDD vs DataFrame vs Dataset? Show a typed Dataset in Java.
 *
 * RDD       - low level, no schema, no Catalyst optimizer, Java objects (GC heavy)
 * DataFrame - Dataset<Row>, schema + Catalyst + Tungsten, errors at RUNTIME (column names are strings)
 * Dataset<T>- typed: compile-time safety, still optimized, but lambdas are opaque to Catalyst
 *
 * TRICK: Java Dataset needs an Encoder - Encoders.bean(Class) for POJOs (public no-arg ctor + getters/setters).
 *        In Java, cast lambdas to FilterFunction/MapFunction to resolve the Scala/Java overload ambiguity.
 */
public class TypedDataset {

    public static class Employee implements Serializable {
        private int id;
        private String name;
        private String dept;
        private int salary;

        public Employee() {}

        public int getId() { return id; }
        public void setId(int id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDept() { return dept; }
        public void setDept(String dept) { this.dept = dept; }
        public int getSalary() { return salary; }
        public void setSalary(int salary) { this.salary = salary; }
    }

    public static void main(String[] args) {
        SparkSession spark = SparkUtil.session("TypedDataset");
        Encoder<Employee> enc = Encoders.bean(Employee.class);

        // DataFrame -> Dataset<Employee> (columns matched by name; manager_id ignored by bean)
        Dataset<Employee> ds = SparkUtil.employees(spark).drop("manager_id").as(enc);

        SparkUtil.title("Typed filter + map");
        ds.filter((FilterFunction<Employee>) e -> e.getSalary() > 6000)
                .map((MapFunction<Employee, String>) e -> e.getName().toUpperCase() + " (" + e.getDept() + ")",
                        Encoders.STRING())
                .show();

        SparkUtil.title("groupByKey + mapGroups: max salary per dept");
        ds.groupByKey((MapFunction<Employee, String>) Employee::getDept, Encoders.STRING())
                .mapGroups((MapGroupsFunction<String, Employee, Tuple2<String, Integer>>) (dept, it) -> {
                    int max = Integer.MIN_VALUE;
                    while (it.hasNext()) max = Math.max(max, it.next().getSalary());
                    return new Tuple2<>(dept, max);
                }, Encoders.tuple(Encoders.STRING(), Encoders.INT()))
                .toDF("dept", "max_salary")
                .show();

        SparkUtil.title("Back to untyped DataFrame / RDD");
        Dataset<Row> df = ds.toDF();
        System.out.println("DataFrame columns: " + String.join(", ", df.columns()));
        System.out.println("RDD count: " + ds.javaRDD().count());

        spark.stop();
    }
}
