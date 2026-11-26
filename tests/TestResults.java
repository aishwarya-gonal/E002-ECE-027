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

        // BR-03: Credit expiry checks
String txn0226 = rows.stream()
    .filter(s -> s.contains("TXN-0226,"))
    .findFirst().orElse("");

String txn0250 = rows.stream()
    .filter(s -> s.contains("TXN-0250,"))
    .findFirst().orElse("");

check(!txn0226.isEmpty() && txn0226.split(",", -1)[7].equals("CREDIT"),
      "BR-03: Credit valid on expiry date");

check(!txn0250.isEmpty() && txn0250.split(",", -1)[7].equals("WALLET"),
      "BR-03: Expired credit not used");
// BR-01: Swap price checks
String txn0082 = rows.stream()
    .filter(s -> s.startsWith("TXN-0082,"))
    .findFirst().orElse("");

String txn0084 = rows.stream()
    .filter(s -> s.startsWith("TXN-0084,"))
    .findFirst().orElse("");

check(!txn0082.isEmpty() && txn0082.split(",", -1)[6].equals("40.00"),
      "BR-01: Minimum swap price is 40");

check(!txn0084.isEmpty() && txn0084.split(",", -1)[6].equals("40.80"),
      "BR-01: Swap price calculation");
// BR-02: Reject battery packs below 90% SoC
List<String> inputRows =
    Files.readAllLines(Path.of("data/transactions.csv"));

String[] lowSocIds = {"TXN-0002", "TXN-0003", "TXN-0004"};

for (String id : lowSocIds) {
    String input = inputRows.stream()
        .filter(s -> s.startsWith(id + ","))
        .findFirst().orElse("");

    String output = rows.stream()
        .filter(s -> s.startsWith(id + ","))
        .findFirst().orElse("");

    check(!input.isEmpty()
        && Integer.parseInt(input.split(",", -1)[6]) < 90
        && !output.isEmpty()
        && output.split(",", -1)[4].equals("DECLINED")
        && output.split(",", -1)[5].equals("NO_CHARGED_PACK"),
        "BR-02: Reject low-SoC pack " + id);
}
// BR-05: Grade B damage checks
String gradeB = rows.stream()
    .filter(s -> s.startsWith("TXN-0086,"))
    .findFirst().orElse("");

String[] damageFields = gradeB.split(",", -1);

check(damageFields.length > 10
      && damageFields[9].equals("200.00")
      && damageFields[10].equals("36.00"),
      "BR-05: Grade B damage charge and GST");
      System.out.println("----------------------");
        System.out.println("ALL " + passed + " TESTS PASSED");
    }
}