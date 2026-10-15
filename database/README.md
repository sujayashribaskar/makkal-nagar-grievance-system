# Makkal Nagar — Database Setup & Architecture Guide

## Database Engine: MySQL 8+ / MariaDB 10.4+

The **Makkal Nagar Municipal Corporation Grievance Management System** uses a relational database schema managed via **Flyway Database Migrations**.

---

## 1. Quick Database Setup

### Step A: Start MySQL / MariaDB Server
Ensure MySQL or MariaDB is running on `localhost:3306`. If using XAMPP:
- Open XAMPP Control Panel and start MySQL, **or**
- Execute `C:\xampp\mysql\bin\mysqld.exe --defaults-file="C:\xampp\mysql\bin\my.ini" --console`

### Step B: Create Database
Open MySQL terminal or phpMyAdmin and execute:
```sql
CREATE DATABASE IF NOT EXISTS makkal_nagar_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### Step C: Spring Boot Automatic Migration
When the Spring Boot application boots, Flyway automatically executes the migration scripts located in:
`backend/src/main/resources/db/migration/`

---

## 2. Flyway Migration History

1. **`V1__create_schema.sql`**
   - Creates `users` (credentials with BCrypt hashing and roles).
   - Creates `departments` (department metadata and capacity).
   - Creates `citizens` (fictional citizen directory with citizen reference IDs).
   - Creates `arrival_sequences` (atomic arrival order sequence generator).
   - Creates `complaints` (all core fields, categories, severities, foreign keys, and indexes).
   - Creates `assignments` (history of department assignments).
   - Creates `status_updates` (audit trail of every status transition).
   - Creates `duplicate_links` (candidate pairs flagged for administrative review).

2. **`V2__seed_initial_data.sql`**
   - Configures the 4 official municipal departments:
     - `WATER`: Water Supply Department (Capacity: 5)
     - `ROAD`: Roads and Infrastructure Department (Capacity: 4)
     - `STREETLIGHT`: Streetlight and Electrical Department (Capacity: 4)
     - `WASTE`: Sanitation and Waste Management Department (Capacity: 4)
   - Initializes arrival sequence counter to `0`.

3. **`V3__seed_synthetic_complaints.sql`**
   - Inserts 34 comprehensive synthetic complaints covering:
     - All 4 municipal categories (`WATER`, `ROAD`, `STREETLIGHT`, `WASTE`).
     - All 5 configured locations: *Anna Nagar*, *Gandhi Street*, *Nehru Colony*, *Lake View Road*, *Market Area*.
     - All 4 severity levels: `CRITICAL`, `HIGH`, `MEDIUM`, `LOW`.
     - All lifecycle statuses: `REGISTERED`, `ASSIGNED`, `IN_PROGRESS`, `RESOLVED`.
     - Department full-capacity states and waiting queue priority ordering.
     - Priority tie-breaking examples (Severity > Affected Count > Arrival Order).

4. **`V4__automatic_status_and_duplicate_enhancements.sql`**
   - Adds `triggering_event` tracking to `status_updates`.
   - Adds `confidence_score` and `similarity_details` columns to `duplicate_links`.

---

## 3. Database Connection Configuration

Configuration in `application.yml` uses environment variable overrides with developer defaults:

| Variable | Default Value | Description |
| :--- | :--- | :--- |
| `DB_URL` | `jdbc:mysql://localhost:3306/makkal_nagar_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&createDatabaseIfNotExist=true` | JDBC Connection String |
| `DB_USERNAME` | `root` | Database user account |
| `DB_PASSWORD` | *(empty string)* | Database user password |

To set credentials in Windows PowerShell:
```powershell
$env:DB_USERNAME="root"
$env:DB_PASSWORD="your_password"
java -jar backend/target/makkal-nagar-backend-1.0.0.jar
```

---

## 4. Resetting Demonstration Data

To completely reset the database to a clean synthetic baseline:
```sql
DROP DATABASE IF EXISTS makkal_nagar_db;
CREATE DATABASE makkal_nagar_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```
Restart the Spring Boot backend to automatically rerun migrations and reseed data.
