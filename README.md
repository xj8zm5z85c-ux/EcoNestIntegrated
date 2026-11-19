# EcoNest Integrated Application

This is the combined Apache NetBeans Maven project for the EcoNest group assignment. It brings the supplied Task 1–9 Java modules into one project and provides one launcher for opening each feature.

## Open and run in Apache NetBeans

1. Install/select JDK 17 or newer in NetBeans.
2. Choose **File → Open Project** and select the `EcoNestIntegrated` folder (the folder containing `pom.xml`).
3. Allow Maven to download the Jakarta Mail / Eclipse Angus dependency when the network is available.
4. Run the project. The entry point is `lk.econest.app.EcoNestApplication`.

From a terminal with Maven installed, run `mvn compile` to compile, and `mvn exec:java` to launch.

## Modules included

- Task 1: product catalogue
- Task 2: client management
- Task 3: inventory and supplier stock-in
- Task 4: delivery-agent management
- Tasks 5–6: order processing, agent assignment, and delivery status
- Task 7: monthly sales and inventory reports
- Tasks 8–9: dispatch and delivery-assignment email notifications

The feature windows keep their original task packages and can be opened from the EcoNest launcher. Their original independent `main` methods are retained.

## Database script

The group's MariaDB/MySQL schema is included at `src/main/resources/db/EcoNestMainDatabase.sql`. Import it using XAMPP/phpMyAdmin as described in that SQL file's original README or in the team's database setup notes.

**Current integration limitation:** the supplied task modules use different storage implementations (CSV, in-memory data, serialized files, or their own order store). This combined project makes them available from one application, but does not yet migrate all module data access to the shared `econest_db` database. For example, a client added in Task 2 is not automatically shared with Tasks 5–6. The schema is included, but importing it alone does not connect these modules to it. Do not treat data in one module as synchronized with another until the repositories are converted to shared JDBC-backed repositories.

## Local data and email

- Task 1 stores the catalogue in `%USERPROFILE%\\.econest\\products.csv`.
- Tasks 3 and 4 store their records in the user's `.econest` folder.
- Tasks 5–6 store their demo data in `%USERPROFILE%\\.econest\\data.ser`.
- Task 7 uses the project's `data` directory (`inventory.csv` and `sales.csv`).
- SMTP credentials are entered at runtime in the notification screen. Use a Gmail App Password if using Gmail; never commit it or share it with teammates.

## Source provenance

The Java files were copied from the team's `task 01` through `task 08,09` source folders into this Maven project without removing the originals. Module source filenames and their public type names use task prefixes (for example, `Task3InventoryFrame.java` and `Tasks56EcoNestApp.java`) so their ownership is visible in NetBeans. `EcoNestApplication.java` is the shared launcher. Keep the task folders as the source backup until the group confirms the integrated project.
