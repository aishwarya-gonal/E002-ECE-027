E-Scooter Battery-Swap Station Pricing and Deposit Engine

Project Overview

This project is a Java 17-based E-Scooter Battery-Swap Station Pricing and Deposit Engine. It processes rider transactions, calculates battery-swap charges, manages wallet credits and deposits, validates business rules, and generates rider statements and daily station reports.

Technologies Used

- Java 17
- CSV files for input and output
- Object-oriented programming
- Java Collections and BigDecimal for calculations

Project Structure

E002-ECE-027/
├── data/
│   ├── commands.txt
│   ├── credit_packs.csv
│   ├── inventory_updates.csv
│   ├── packs.csv
│   ├── rider_deposits.csv
│   ├── riders.csv
│   ├── stations.csv
│   └── transactions.csv
├── src/
│   └── Main.java
├── rider_statement.csv
├── station_daily.csv
├── swap_log.csv
└── README.md

Main Features

- Battery-swap price calculation
- Battery state-of-charge validation
- Rider wallet and credit management
- Deposit handling
- Damage and overdue charge calculation
- Transaction validation and decline handling
- Rider statements and daily station reports

Input Files

The "data" folder contains the input CSV files and transaction command file required to run the project.

Output Files

- "swap_log.csv" – transaction details and results.
- "station_daily.csv" – daily station activity and revenue.
- "rider_statement.csv" – rider balances, deposits and transaction summaries.

How to Run

1. Install Java Development Kit (JDK) 17.
2. Open the project folder in Visual Studio Code.
3. Open the terminal in the project directory.
4. Compile the Java program:

javac src/Main.java

5. Run the program:

java -cp src Main

Project Objective

To develop a reliable command-line engine that manages e-scooter battery swapping, pricing, deposits, rider credits and transaction reporting according to eight defined business rules.

## How to Run Tests

Run these commands from the project root directory:

```bash
javac -cp src -d . tests/TestResults.java
java -cp . tests.TestResults