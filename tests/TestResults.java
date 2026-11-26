package tests;


import java.nio.file.*;
import java.util.*;

public class TestResults {

    static int passed = 0;

    static void check(boolean condition, String message) {
        if (!condition) {
            throw new RuntimeException("FAILED: " + message);
        }
        passed++;
        System.out.println("PASSED: " + message);
    }

    public static void main(String[] args) throws Exception {

        Process process = new ProcessBuilder(
            "java", "-cp", "src", "Main"
        ).inheritIO().start();

        int exitCode = process.waitFor();
        check(exitCode == 0, "Main program runs");

        List<String> rows =
            Files.readAllLines(Path.of("swap_log.csv"));

        check(rows.size() == 568, "567 transaction records");

        int successful = 0;
        int declined = 0;
        Map<String, Integer> reasons = new HashMap<>();

        for (int i = 1; i < rows.size(); i++) {
            String[] fields = rows.get(i).split(",", -1);

            if (fields.length < 6) {
                continue;
            }

            if (fields[4].equals("OK")) {
                successful++;
            } else if (fields[4].equals("DECLINED")) {
                declined++;
                reasons.merge(fields[5], 1, Integer::sum);
            }
        }

        check(successful == 555, "555 successful transactions");
        check(declined == 12, "12 declined transactions");

        check(reasons.getOrDefault("NO_CHARGED_PACK", 0) == 3,
              "NO_CHARGED_PACK");

        check(reasons.getOrDefault("INSUFFICIENT_BALANCE", 0) == 4,
              "INSUFFICIENT_BALANCE");

        check(reasons.getOrDefault("SUSPENDED", 0) == 2,
              "SUSPENDED");

        check(reasons.getOrDefault("PACK_LIMIT", 0) == 2,
              "PACK_LIMIT");

        check(reasons.getOrDefault("NOT_HELD", 0) == 1,
              "NOT_HELD");

        check(Files.exists(Path.of("station_daily.csv")),
              "Station report exists");

        check(Files.exists(Path.of("rider_statement.csv")),
              "Rider statement exists");

        System.out.println("----------------------");
        System.out.println("ALL " + passed + " TESTS PASSED");
    }
}