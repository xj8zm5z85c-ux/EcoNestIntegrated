# EcoNest Integrated Application

EcoNest is a Java desktop application that brings the group's Task 1–9 features together behind one launcher. The modules share a MariaDB/MySQL database, so products, clients, stock, orders, deliveries, reports, and email audit records use the same source of truth.

All product prices, order totals, and sales reports are displayed and interpreted in **Sri Lankan rupees (LKR)**, regardless of the computer's regional settings. The database stores monetary amounts as decimal numbers without a currency code; switching the display to LKR does not convert or modify existing amounts.

## Contents

- [Prerequisites](#prerequisites)
- [Database setup](#database-setup)
- [Optional demo data](#optional-demo-data)
- [Database connection settings](#database-connection-settings)
- [Build and launch](#build-and-launch)
- [Using the application](#using-the-application)
- [Existing local data](#existing-local-data)
- [Troubleshooting](#troubleshooting)

## Prerequisites

- JDK 17 or newer.
- Apache Maven 3.9 or newer.
- MariaDB 10.6+ or MySQL 8.0.16+.
- A database account with permission to create/use `econest_db`, tables, views, and triggers.

The Maven project downloads MariaDB Connector/J and Jakarta Mail when dependencies are resolved.

### Verify Java and Maven on Windows

Open PowerShell and check:

```powershell
java -version
javac -version
mvn -version
```

Both `java` and `javac` should point to a JDK 17+ installation. If Maven is missing, download the **binary zip** from [Apache Maven](https://maven.apache.org/download.cgi), extract it somewhere under your user folder, and add its `bin` folder to your user `PATH`. For a temporary PowerShell setup (adjust the extracted folder name if needed):

```powershell
$env:MAVEN_HOME = "$HOME\tools\apache-maven-3.10.1"
$env:PATH = "$env:MAVEN_HOME\bin;$env:PATH"
mvn -version
```

That setup applies only to the current PowerShell window. For NetBeans or VS Code, add Maven's `bin` folder to the Windows **user** environment `PATH`, then restart the IDE so it inherits the change.

## Database setup

### New database

1. Start the MariaDB/MySQL service. With XAMPP, open the XAMPP Control Panel and click **Start** beside **MySQL** (XAMPP's MySQL module runs MariaDB).
2. From the repository root, import [`EcoNestMainDatabase.sql`](src/main/resources/db/EcoNestMainDatabase.sql). It creates the `econest_db` database, tables, indexes, views, and triggers.

   With the MySQL/MariaDB command-line client:

   ```text
   mysql -u root -p < src/main/resources/db/EcoNestMainDatabase.sql
   ```

   Omit `-p` if the local account has no password. If the client is not on `PATH` in a typical XAMPP installation, use `C:\xampp\mysql\bin\mysql.exe` from Command Prompt. The script contains triggers; if using a GUI importer, confirm it accepts the `DELIMITER` statements and creates all triggers.

3. Configure the application to use an account that can read and write the schema.

### Existing database from the previous schema

1. Back up `econest_db` before changing it.
   With the XAMPP client and a root account with no password:

   ```text
   C:\xampp\mysql\bin\mysqldump.exe -u root econest_db > econest_db_backup.sql
   ```

   Add `-p` if the account requires a password. Keep the backup outside the project repository because it contains business data.
2. Run [`V2__component_integration.sql`](src/main/resources/db/migrations/V2__component_integration.sql) **once** against the existing database. It adds stable client, agent, and order codes and the delivery schedule date.
3. Rerun the base [`EcoNestMainDatabase.sql`](src/main/resources/db/EcoNestMainDatabase.sql) script. This installs the current triggers and views.

Do not run the V2 migration on a fresh database created from the current base schema, or run it a second time. The application checks for the required schema columns at startup and reports when the schema is missing or outdated.

## Optional demo data

To populate an empty database with sample products, stock receipts, two clients, delivery agents, and example orders/deliveries, run [`demo_data.sql`](src/main/resources/db/demo_data.sql) after the schema is installed:

```text
mysql -u root -p econest_db < src/main/resources/db/demo_data.sql
```

In XAMPP, use `C:\xampp\mysql\bin\mysql.exe` if the client is not on `PATH`. The script uses `DEMO-` codes and `DEMO-` receipt references, and is safe to rerun without duplicating its records. It adds records; it does not clear or replace existing business data. Demo orders reserve inventory just like regular orders. Remove demo data only from a database dedicated to testing, after backing it up.

## Database connection settings

The default connection is:

| Setting | Default |
|---|---|
| URL | `jdbc:mariadb://localhost:3306/econest_db` |
| User | `root` |
| Password | empty |

Set environment variables before launching to override the defaults:

```powershell
$env:ECONEST_DB_URL = "jdbc:mariadb://localhost:3306/econest_db"
$env:ECONEST_DB_USER = "econest_app"
$env:ECONEST_DB_PASSWORD = "your-local-password"
```

The same values can be supplied as Java system properties (`econest.db.url`, `econest.db.user`, and `econest.db.password`) in the IDE's VM options. Do not put database passwords in source files or commit them.

## Build and launch

### Apache NetBeans

1. Open the folder containing `pom.xml` as a Maven project.
2. Set the project JDK to 17 or newer and allow Maven to reload/download dependencies.
3. Start the database and configure the connection as above.
4. Run `lk.econest.app.EcoNestApplication`.

### Command line

From the repository root:

```text
mvn clean compile
mvn exec:java
```

The Maven exec plugin is configured to launch `lk.econest.app.EcoNestApplication`. The launcher verifies both the database connection and required schema before showing the module menu. A connection/schema error is displayed rather than switching to local file storage.

Individual task entry points remain available for development, but use the same database configuration and schema.

## Using the application

Suggested first-use order:

1. Add products in **Product Catalogue**.
2. Set reorder levels and record incoming quantities in **Inventory**. Use a consistent supplier name to keep receipts under the same supplier.
3. Add clients in **Clients** and delivery agents in **Delivery Agents**.
4. Place orders in **Orders and Deliveries**. The database stores order lines and deducts stock atomically; an order is rejected if the current stock is insufficient.
5. Assign an agent and update delivery status. Reassignment is blocked after dispatch, and delivery status cannot move backwards.
6. Review current stock and order-derived sales in **Monthly Reports**. It is intentionally read-only so reports cannot diverge from the catalogue, stock receipts, or actual orders.
7. Use **Email Notifications** for an existing dispatched order or assigned delivery. The entered recipient/name/address must match the corresponding database record. Attempts are logged as `PENDING`, then updated to `SENT` or `FAILED`.

Product and agent removals deactivate records instead of deleting history. A product cannot be deactivated while it has stock or open orders. Client deletion may be rejected when an order references that client.

## Existing local data

Older CSV and serialized data files are left untouched and are **not automatically imported**. Back them up before switching to the shared database. The database starts with empty business tables; add or import the records you need before using the order workflow. In particular, do not delete the previous `.econest` data folder or the project `data` directory until you have confirmed the database contains the records you need.

## Troubleshooting

| Message/symptom | What to check |
|---|---|
| Connection refused / communications link failure | Make sure MariaDB/MySQL is running, the host and port in `ECONEST_DB_URL` are correct, and port `3306` is reachable. |
| Access denied | Check `ECONEST_DB_USER` and `ECONEST_DB_PASSWORD`, and grant that account access to `econest_db`. |
| Schema missing or outdated | Import the current base script, or perform the existing-database V2 migration followed by the base script. |
| Maven cannot resolve a dependency | Check network/proxy settings, then reload the Maven project in NetBeans or run `mvn clean compile` again. |
| Order rejected for insufficient inventory | Refresh the module from the launcher and check current on-hand stock in Inventory; the database prevents overselling. |
| Product cannot be removed | Deplete its stock and finish or cancel its open orders first; inactive products are hidden from new orders while history is retained. |

SMTP credentials are entered at runtime and are not stored in the database. For Gmail, use an App Password. Never commit or share the password.

## Included modules

| Task | Feature |
|---|---|
| 1 | Product catalogue |
| 2 | Client management |
| 3 | Inventory and supplier stock-in |
| 4 | Delivery-agent management |
| 5–6 | Order processing, agent assignment, and delivery status |
| 7 | Monthly sales and inventory reports |
| 8–9 | Dispatch and delivery-assignment email notifications |
