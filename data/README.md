# ECE-027 – E-Scooter Battery-Swap Station Pricing and Deposit Engine — Sample Data Pack
Prepared by 10 SECONDS for Team E002.

This pack is the input for an e-scooter battery-swap engine: 80 riders, 4 stations with 25 packs each, the packs riders already hold, prepaid credit packs, and ten days (2026-10-20 to 2026-10-29) of transactions plus station charging telemetry. Your engine reads `commands.txt`, processes transactions and inventory updates in time order, prices and settles every swap, tracks deposits and suspensions, and prints daily station reports and a rider statement.

Pack conventions where the brief is silent: SoC is a whole percent. Days held = transaction date − issue date (calendar days); more than 7 days is overdue. A credit pack is usable through its expiry date = purchase date + 30 days; the credit that expires first is used first; credit purchases are paid outside the wallet. On a swap the remaining deposit of the returned pack moves to the issued pack (it is not topped up). A RETURN (no issue) releases the remaining deposit to the wallet unless forfeited. A grade-C return is always a RETURN: the deposit is forfeited and the rider is SUSPENDED; suspended riders may still return packs but every SWAP or ISSUE is declined. An ISSUE (no return) holds a new Rs 2,000 deposit and is declined if the rider already has 2 packs. The station's issued pack must be at that station with SoC ≥ 90; issued_pack_id NONE means the station had none. Checks run in this order: NOT_HELD → SUSPENDED → PACK_LIMIT → NO_CHARGED_PACK → INSUFFICIENT_BALANCE. At the same timestamp, inventory updates are applied before transactions.

## Files
| File | What it holds | Rows |
|---|---|---|
| stations.csv | Swap stations | 4 |
| packs.csv | Every pack at the start of 2026-10-20: at a station (with SoC) or out with a rider (with issue time) | 190 |
| riders.csv | Riders with opening wallet | 80 |
| rider_deposits.csv | Deposit held per pack a rider has out | 90 |
| credit_packs.csv | Credit packs bought before the period | 6 |
| transactions.csv | Swaps, returns, issues and credit purchases in time order | 567 |
| inventory_updates.csv | Station telemetry: pack SoC after charging | 557 |
| commands.txt | Load commands, RUN, then daily station reports and the rider statement | 19 |

## Column reference
Conventions: dates `YYYY-MM-DD`; timestamps `YYYY-MM-DD HH:MM:SS` (24h, IST, no timezone suffix); money in Rs as plain decimals (no commas, no symbol); percentages as plain numbers (12.5 = 12.5%); booleans `true`/`false`; IDs uppercase with a fixed prefix.

### stations.csv
| Column | Type | Units / format | Allowed values | Notes |
|---|---|---|---|---|
| station_id | string | STN-n | STN-1 … STN-4 | Primary key |
| station_name | string | - |  |  |

### packs.csv
| Column | Type | Units / format | Allowed values | Notes |
|---|---|---|---|---|
| pack_id | string | PK-nnnn | unique | Primary key |
| location | string | STN-n or RDR-nnn |  | Where the pack is at the start |
| soc_pct | integer or NONE | % | 0–100 | NONE for packs out with riders |
| issued_at | timestamp or NONE | YYYY-MM-DD HH:MM:SS |  | When the rider took it (drives overdue) |

### riders.csv
| Column | Type | Units / format | Allowed values | Notes |
|---|---|---|---|---|
| rider_id | string | RDR-nnn | RDR-001 … RDR-080 | Primary key |
| rider_name | string | - |  |  |
| mobile | string | 10 digits | starts 6–9 | Not used by any rule |
| home_station_id | string | STN-n | must exist in stations.csv | Not used by any rule |
| wallet_balance_rs | decimal | Rs, 2 dp |  | Opening wallet |

### rider_deposits.csv
| Column | Type | Units / format | Allowed values | Notes |
|---|---|---|---|---|
| rider_id | string | RDR-nnn | must exist in riders.csv |  |
| pack_id | string | PK-nnnn | must exist in packs.csv |  |
| deposit_held_rs | decimal | Rs, 2 dp | 2000.00 |  |

### credit_packs.csv
| Column | Type | Units / format | Allowed values | Notes |
|---|---|---|---|---|
| credit_pack_id | string | CRP-nnn |  |  |
| rider_id | string | RDR-nnn | must exist in riders.csv |  |
| purchase_date | date | YYYY-MM-DD |  | Expires 30 days later |
| credits_remaining | integer | swaps |  |  |

### transactions.csv
| Column | Type | Units / format | Allowed values | Notes |
|---|---|---|---|---|
| txn_id | string | TXN-nnnn | unique | In time order |
| timestamp | timestamp | YYYY-MM-DD HH:MM:SS |  |  |
| txn_type | string | - | SWAP, RETURN, ISSUE, CREDIT_PURCHASE | CREDIT_PURCHASE = 30 swaps for Rs 3,000 |
| rider_id | string | RDR-nnn | must exist in riders.csv |  |
| station_id | string | STN-n or NONE |  | NONE for credit purchases |
| returned_pack_id | string | PK-nnnn or NONE |  |  |
| returned_soc_pct | integer or NONE | % | 0–100 | Drives the swap price |
| damage_grade | string | - | A, B, C, NONE |  |
| issued_pack_id | string | PK-nnnn or NONE |  | NONE = nothing issued (RETURN) or no charged pack available |

### inventory_updates.csv
| Column | Type | Units / format | Allowed values | Notes |
|---|---|---|---|---|
| update_id | string | UPD-nnnnn | unique |  |
| timestamp | timestamp | YYYY-MM-DD HH:MM:SS |  |  |
| station_id | string | STN-n |  |  |
| pack_id | string | PK-nnnn |  |  |
| soc_pct | integer | % | 0–100 | Replaces the pack's SoC |

## Command file format
`commands.txt` holds one command per line as `COMMAND_NAME|arg1|arg2|...`. Commands must be processed in file order, top to bottom. Lines starting with `#` are comments and blank lines are ignored.

| Command | Arguments (in order) | Meaning |
|---|---|---|
| LOAD_STATIONS / LOAD_PACKS / LOAD_RIDERS / LOAD_DEPOSITS / LOAD_CREDIT_PACKS | file | Load the opening state |
| LOAD_TRANSACTIONS / LOAD_INVENTORY_UPDATES | file | Load the two time-ordered feeds |
| RUN | (none) | Process both feeds merged by timestamp |
| STATION_REPORT | YYYY-MM-DD | Print the daily report of every station |
| RIDER_STATEMENT | YYYY-MM-DD | Print every rider's statement as of the end of that day |

`status` is OK or DECLINED; `reason` NONE or NOT_HELD, SUSPENDED, PACK_LIMIT, NO_CHARGED_PACK, INSUFFICIENT_BALANCE. `paid_by` is CREDIT, WALLET or NONE. `wallet_revenue_rs` is swap price + swap GST + damage charge + damage GST paid from wallets that day. `packs_ready_end` = packs at the station with SoC ≥ 90 at 23:59:59. Rider `status` is ACTIVE or SUSPENDED.

## Planted scenarios
Line numbers count the header row as line 1, so the first data row of a CSV is line 2. For commands.txt the line number is the physical line in the file (comment lines included).

- **Returned pack with SoC above 91% (price floors at Rs 40)** — transactions.csv line 83 (TXN-0082, RDR-064, 2026-10-21 09:12) returns a pack at 93% → 150 − 1.20 × 93 = 38.40 → floors at Rs 40.00. Compare transactions.csv line 85 (TXN-0084, RDR-010, 2026-10-21 09:47) at 91% → Rs 40.80.
- **Credit pack expiring on the same day as a swap** — RDR-060 has a credit pack bought 2026-09-23 (credit_packs.csv) that expires on 2026-10-23. transactions.csv line 227 (TXN-0226, RDR-060, 2026-10-23 18:05) is on the expiry date → paid by credit. transactions.csv line 251 (TXN-0250, RDR-060, 2026-10-24 08:40) is the next day → the credits are expired (forfeited) and the swap is paid from the wallet with GST.
- **Rider returning the final pack with grade C while already suspended** — RDR-024 holds two packs. transactions.csv line 135 (TXN-0134, RDR-024, 2026-10-22 11:30) returns one with grade C → that pack's deposit is forfeited and the rider is SUSPENDED; transactions.csv line 150 (TXN-0149, RDR-024, 2026-10-22 17:10) (a swap) is declined. transactions.csv line 259 (TXN-0258, RDR-024, 2026-10-24 10:15) then returns the final pack, again grade C, while already suspended → the return is accepted, the second deposit is forfeited too, and the rider stays SUSPENDED. Another grade-C return: transactions.csv line 194 (TXN-0193, RDR-073, 2026-10-23 12:45), followed by a declined request for a new pack transactions.csv line 252 (TXN-0251, RDR-073, 2026-10-24 09:00).
- **Pack returned exactly on day 7 (no overdue charge) and on day 8** — transactions.csv line 202 (TXN-0201, RDR-005, 2026-10-23 13:34) returns a pack issued exactly 7 days earlier (see packs.csv issued_at) → no overdue charge. transactions.csv line 107 (TXN-0106, RDR-056, 2026-10-21 16:06) returns one issued 8 days earlier → Rs 50 from the deposit. Other overdue packs: transactions.csv line 191 (TXN-0190, RDR-006, 2026-10-23 12:02) (9 days), transactions.csv line 113 (TXN-0112, RDR-029, 2026-10-21 17:02) (10 days), transactions.csv line 219 (TXN-0218, RDR-040, 2026-10-23 16:59) (12 days) and transactions.csv line 224 (TXN-0223, RDR-020, 2026-10-23 17:06) (52 days → 45 × 50 = 2,250, capped at the Rs 2,000 deposit).
- **Station with exactly one pack at 90% SoC** — STN-4 starts the period after an overnight outage: in packs.csv exactly one of its packs is at 90% and all others are below. transactions.csv line 2 (TXN-0001, RDR-076, 2026-10-20 06:20) takes that 90% pack (allowed, 90 is enough). transactions.csv line 3 (TXN-0002, RDR-039, 2026-10-20 06:34), transactions.csv line 4 (TXN-0003, RDR-015, 2026-10-20 06:51) and transactions.csv line 5 (TXN-0004, RDR-062, 2026-10-20 07:04) find no pack at 90% or above → NO_CHARGED_PACK (issued_pack_id is NONE). The other packs report 100% in inventory_updates.csv from 07:30.

Extra traps (realistic things your validation should catch):

- **Third pack** — transactions.csv line 201 (TXN-0200, RDR-051, 2026-10-23 13:25) and transactions.csv line 484 (TXN-0483, RDR-048, 2026-10-28 16:05): riders already holding 2 packs ask for another pack without returning one → declined (limit 2).
- **Wallet cannot cover the swap** — transactions.csv line 86 (TXN-0085, RDR-013, 2026-10-21 10:20), transactions.csv line 186 (TXN-0185, RDR-074, 2026-10-23 11:20), transactions.csv line 311 (TXN-0310, RDR-058, 2026-10-25 12:20), transactions.csv line 428 (TXN-0427, RDR-066, 2026-10-27 13:20): wallet below price + GST and no valid credits → INSUFFICIENT_BALANCE (wallet never goes below zero).
- **Pack not held by the rider** — transactions.csv line 315 (TXN-0314, RDR-057, 2026-10-25 15:10) returns a pack that is out with a different rider → declined.
- **Damage grade B** — Five swaps return grade B packs: transactions.csv line 91 (TXN-0090, RDR-045, 2026-10-21 11:57), transactions.csv line 87 (TXN-0086, RDR-012, 2026-10-21 10:25), transactions.csv line 303 (TXN-0302, RDR-016, 2026-10-25 09:02), transactions.csv line 143 (TXN-0142, RDR-007, 2026-10-22 14:45), transactions.csv line 470 (TXN-0469, RDR-027, 2026-10-28 12:20) → Rs 200 + 18% GST from the wallet on top of the swap.
- **Counts** — This set has 12 declined transactions, 8 damaged returns (5 B, 3 C), 5 overdue packs and 20 credit purchases.

## What is NOT in this pack
Expected outputs are not provided for this set. Validate your engine with your own test suite against the rules in your brief (BR-01 … BR-08). A separate hidden input set with the same schema will be used at final evaluation; outputs are compared field-for-field, so follow the column formats above exactly in your own output.

## Output format expected at evaluation
Produce these files (CSV, header row, same conventions as above). Column order matters.

### swap_log.csv
One row per transaction in file order.

Columns in order: `txn_id`, `rider_id`, `station_id`, `txn_type`, `status`, `reason`, `swap_price_rs`, `paid_by`, `swap_gst_rs`, `damage_charge_rs`, `damage_gst_rs`, `overdue_charge_rs`, `deposit_forfeited_rs`, `deposit_released_rs`, `wallet_after_rs`, `credits_after`

### station_daily.csv
One row per station per date.

Columns in order: `date`, `station_id`, `swaps_ok`, `declines`, `wallet_revenue_rs`, `credits_redeemed`, `packs_at_station_end`, `packs_ready_end`

### rider_statement.csv
One row per rider, ordered by rider_id.

Columns in order: `rider_id`, `status`, `wallet_rs`, `packs_held`, `deposit_held_rs`, `credits_valid`, `credits_forfeited`, `swaps_ok`, `declines`, `damage_charges_rs`, `overdue_charges_rs`, `deposits_forfeited_rs`
