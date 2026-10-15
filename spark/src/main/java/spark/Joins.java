package spark;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;

import static org.apache.spark.sql.functions.*;

/**
 * Join questions:
 *  1. All join types (inner, left, right, full, left_semi, left_anti, cross)
 *  2. Employees with no matching department (anti join)
 *  3. Departments with no employees
 *  4. Employee -> manager name (self join)
 *  5. Broadcast join (small table) - check physical plan
 *  6. Avoiding duplicate columns after join
 *  7. Employees earning more than their manager
 *
 * TRICK: left_semi = "WHERE EXISTS" (only left columns), left_anti = "WHERE NOT EXISTS".
 *        Join on column NAME (String/Seq) -> key appears once; join on expression -> key appears twice.
 */
public class Joins {

    static void allJoinTypes(Dataset<Row> emp, Dataset<Row> dept) {
        for (String type : new String[]{"inner", "left", "right", "full", "left_semi", "left_anti"}) {
            SparkUtil.title(type + " join");
            emp.join(dept, emp.col("dept").equalTo(dept.col("dept")), type).show();
        }
        SparkUtil.title("cross join (first 5)");
        emp.select("name").crossJoin(dept.select("location")).show(5);
    }

    // 2. Employees whose dept is not in departments
    static Dataset<Row> employeesWithoutDept(Dataset<Row> emp, Dataset<Row> dept) {
        return emp.join(dept, emp.col("dept").equalTo(dept.col("dept")), "left_anti");
    }

    // 3. Departments with no employees
    static Dataset<Row> deptsWithoutEmployees(Dataset<Row> emp, Dataset<Row> dept) {
        return dept.join(emp, dept.col("dept").equalTo(emp.col("dept")), "left_anti");
    }

    // 4. Self join: alias both sides, LEFT so top managers still appear
    static Dataset<Row> employeeManager(Dataset<Row> emp) {
        return emp.as("e")
                .join(emp.as("m"), col("e.manager_id").equalTo(col("m.id")), "left")
                .select(col("e.name").as("employee"),
                        coalesce(col("m.name"), lit("-- none --")).as("manager"));
    }

    // 7. Earning more than manager
    static Dataset<Row> earnsMoreThanManager(Dataset<Row> emp) {
        return emp.as("e")
                .join(emp.as("m"), col("e.manager_id").equalTo(col("m.id")))
                .filter(col("e.salary").gt(col("m.salary")))
                .select(col("e.name"), col("e.salary"), col("m.name").as("manager"), col("m.salary").as("mgr_salary"));
    }

    // 5. Broadcast hint -> BroadcastHashJoin instead of SortMergeJoin (no shuffle of the big side).
    //    Auto-broadcast threshold: spark.sql.autoBroadcastJoinThreshold (default 10MB).
    static Dataset<Row> broadcastJoin(Dataset<Row> emp, Dataset<Row> dept) {
        return emp.join(broadcast(dept), "dept");
    }

    // 6. Join on column name -> single "dept" column (no ambiguity)
    static Dataset<Row> joinUsingColumnName(Dataset<Row> emp, Dataset<Row> dept) {
        return emp.join(dept, "dept");   // same as SQL: JOIN ... USING (dept)
    }

    public static void main(String[] args) {
        SparkSession spark = SparkUtil.session("Joins");
        Dataset<Row> emp = SparkUtil.employees(spark);
        Dataset<Row> dept = SparkUtil.departments(spark);

        allJoinTypes(emp, dept);

        SparkUtil.title("Employees without a department");
        employeesWithoutDept(emp, dept).show();

        SparkUtil.title("Departments without employees");
        deptsWithoutEmployees(emp, dept).show();

        SparkUtil.title("Employee -> manager (self join)");
        employeeManager(emp).show();

        SparkUtil.title("Earns more than manager");
        earnsMoreThanManager(emp).show();

        SparkUtil.title("Broadcast join plan (look for BroadcastHashJoin)");
        broadcastJoin(emp, dept).explain();

        SparkUtil.title("Join USING column - dept appears once");
        joinUsingColumnName(emp, dept).show();

        spark.stop();
    }
}
