# Makkal Nagar Municipal Corporation — Grievance Management and Priority Resolution System

> **Tagline:** *"Your Voice. Our Responsibility."*  
> **Course:** Second-Year Engineering Mini-Project  
> **Stack:** Java 25 LTS &bull; Spring Boot 3.5 &bull; Spring Security &bull; Hibernate JPA &bull; MySQL 8 / MariaDB &bull; Flyway &bull; HTML5 / CSS3 / JavaScript &bull; Bootstrap 5 &bull; Chart.js

---

## 1. Project Overview

The **Makkal Nagar Grievance Management System** is a full-stack, enterprise-grade civic technology web application designed for a municipal corporation. The system allows citizens to submit civic complaints across four municipal domains, tracks complaint lifecycles transparently, and provides municipal administrators with an automated, algorithmically driven priority resolution mechanism.

### Key Municipal Domains:
1. **Water Supply Department** (`WATER`): Pipeline bursts, water contamination, low pressure, broken distribution valves.
2. **Roads and Infrastructure Department** (`ROAD`): Pothole clusters, asphalt erosion, open stormwater trenches, culvert damage.
3. **Streetlight and Electrical Department** (`STREETLIGHT`): Snapped overhead electrical lines, dark lanes, flickering LEDs, leaning lamp posts.
4. **Sanitation and Waste Management Department** (`WASTE`): Garbage heaps, overflowing dumper bins, illegal open dumping, blocked drainage gutters.

---

## 2. Object-Oriented Programming (OOP) Concepts

This project explicitly demonstrates all fundamental OOP principles:

### A. Encapsulation
- All complaint and department attributes are declared `private` in domain entities and classes (e.g., `Grievance`, `Department`, `Citizen`).
- The `Grievance.updateStatus(ComplaintStatus, remarks, actor)` method is the **exclusive entry point** for modifying a complaint's state. It strictly enforces the permitted state transition lifecycle (`REGISTERED -> ASSIGNED -> IN_PROGRESS -> RESOLVED`) and prevents unauthorized state jumps (e.g. bypassing to `RESOLVED` throws `IllegalStateException`).
- Department capacity is protected by `Department.setCapacity(int)`, ensuring capacity cannot be reduced below current active assignments.

### B. Inheritance
- `Grievance` serves as an abstract base class containing common fields (`complaintId`, `citizen`, `severity`, `affectedCount`, `arrivalOrder`, `location`, `status`, `history`).
- Four concrete grievance classes extend `Grievance`:
  - `WaterGrievance`
  - `RoadGrievance`
  - `StreetlightGrievance`
  - `WasteGrievance`

### C. Polymorphism
- The abstract method `public abstract String responsibleDepartment();` is declared in `Grievance`.
- Each subclass polymorphically overrides this method:
  - `WaterGrievance` returns `"Water Supply Department"`
  - `RoadGrievance` returns `"Roads and Infrastructure Department"`
  - `StreetlightGrievance` returns `"Streetlight and Electrical Department"`
  - `WasteGrievance` returns `"Sanitation and Waste Management Department"`
- Subclasses can be treated uniformly as `Grievance` instances while delegating department resolution dynamically at runtime.

### D. Abstraction
- `GrievancePriorityPolicy` is defined as an abstract policy contract exposing `public abstract Comparator<Grievance> comparator();`.
- `SeverityImpactArrivalPolicy` provides the concrete implementation of the three-tier ranking rules.
- The `Department` waiting queue relies on the abstract policy, remaining decoupled from internal priority scoring algorithms.

### E. Composition
- A `Department` **has-a** `PriorityQueue<Grievance>` (waiting queue) and **has-a** `List<Grievance>` (active assigned cases).
- A `Grievance` **has-a** `Citizen` and **has-a** `List<StatusUpdate>` (audit history trail).

---

## 3. Data Structures & Algorithm Analysis

| Data Structure | Code Location | Purpose | Time Complexity |
|---|---|---|---|
| **`PriorityQueue<Grievance>`** | `Department.waitingQueue` | Maintains queued complaints waiting for department capacity, ranked by priority | $O(\log n)$ insertion (`offer`), $O(\log n)$ extraction (`poll`), $O(1)$ peek |
| **`HashMap<Category, Department>`** | `DepartmentQueueService.categoryToDepartmentMap` | Maps category to responsible municipal department for immediate routing | $O(1)$ average lookup and insertion |
| **`HashSet<String>`** | `ComplaintService.duplicateCandidateKeys` | Quick lookup of active composite keys (`Category\|Location`) to flag duplicate issues | $O(1)$ average search and insertion |
| **`ArrayList<T>`** | `Grievance.history`, Report rows | Maintains ordered, indexable audit trails and exportable report collections | $O(1)$ amortized append, $O(1)$ indexed access |
| **Multi-Tier Priority Sorting** | `SeverityImpactArrivalPolicy` | Ranks waiting queue: Severity &rarr; Impact &rarr; Arrival order | $O(n \log n)$ total ordering |
| **Aging Sorting Algorithm** | `ReportService.getPendingAgeReport` | Sorts open grievances descending by duration to identify neglected issues | $O(n \log n)$ via `TimSort` |

### Deterministic Three-Tier Priority Ordering Rule:
1. **Tier 1 (Severity)**: `CRITICAL` > `HIGH` > `MEDIUM` > `LOW`.
2. **Tier 2 (Impact Tie-Break)**: Within equal severity, higher affected-person count ranks first.
3. **Tier 3 (Arrival Tie-Break)**: Within equal severity and affected count, earlier arrival order (`arrival_order ASC`) ranks first.

---

## 4. Application Architecture

```
makkal-nagar-grievance-system/
├── backend/
│   ├── pom.xml                               # Maven project dependencies & plugins
│   └── src/
│       ├── main/
│       │   ├── java/com/makkalnagar/
│       │   │   ├── config/                   # Web & Application configuration
│       │   │   ├── controller/               # REST Controllers (Auth, Public, Admin, Depts, Reports)
│       │   │   ├── domain/                   # OOP Domain Hierarchy (Grievance, Subclasses, Dept)
│       │   │   ├── dto/                      # Request & Response DTOs with Bean Validation
│       │   │   ├── entity/                   # JPA Database Entities
│       │   │   ├── enums/                    # Category, Severity, ComplaintStatus, DuplicateStatus
│       │   │   ├── exception/                # GlobalExceptionHandler & Error Responses
│       │   │   ├── priority/                 # GrievancePriorityPolicy & Comparator implementation
│       │   │   ├── repository/               # Spring Data JPA Repositories
│       │   │   ├── security/                 # Spring Security, UserDetailsService, BCrypt
│       │   │   ├── service/                  # Business Logic (Complaints, Queues, Reports, Duplicates)
│       │   │   └── MakkalNagarApplication.java
│       │   └── resources/
│       │       ├── application.yml           # MySQL, Flyway, and port configurations
│       │       └── db/migration/             # Flyway SQL Migration scripts
│       │           ├── V1__create_schema.sql
│       │           ├── V2__seed_initial_data.sql
│       │           └── V3__seed_synthetic_complaints.sql
│       └── test/java/com/makkalnagar/        # 15 Automated Test Specifications
├── frontend/
│   ├── index.html                            # Public Municipal Home Portal
│   ├── login.html                            # Administrator Sign-In
│   ├── dashboard.html                        # Real-time Administrative Telemetry & Charts
│   ├── register-complaint.html               # Public Complaint Submission Form
│   ├── track-complaint.html                  # Public Transparent Tracking by ID
│   ├── complaints.html                       # Filterable Complaint Data Table
│   ├── complaint-details.html                # Detailed Inspection, Actions, Duplicate Reviews
│   ├── departments.html                      # Department Capacity Management & Queues
│   ├── reports.html                          # Analytical Reports & CSV Data Export
│   └── assets/
│       ├── css/style.css                     # Modern Civic Navy/Teal Design System
│       └── js/app.js                         # Unified Client API Library & Authentication
├── docs/
│   ├── ER-DIAGRAM.md                         # Relational Database Schema & Diagram
│   ├── UML-DIAGRAM.md                        # Object-Oriented Class Relationships & OOP Guide
│   ├── ARCHITECTURE.md                       # Multi-Tier System Architecture Document
│   └── TEST-CASES.md                         # 15 Test Specifications & Verification Outcomes
├── .gitignore
└── README.md
```

---

## 5. Database Schema & Flyway Migrations

The database is built on **MySQL 8 / MariaDB** and version-controlled via Flyway:

1. **`users`**: Administrative credentials hashed with BCrypt.
2. **`departments`**: Four municipal departments and configurable capacities.
3. **`citizens`**: Fictional citizen profiles identified by unique references (`CIT-XXXX`).
4. **`arrival_sequences`**: Thread-safe atomic counter guaranteeing safe arrival order indexing without race conditions.
5. **`complaints`**: Core persistent table storing category, description, severity, affected people, arrival order, status, and resolution details.
6. **`assignments`**: Complete audit log of department routing and reassignments.
7. **`status_updates`**: Immutable historical audit timeline of every state transition.
8. **`duplicate_links`**: Tracks flagged duplicate candidate pairs, review decisions (`FLAGGED`, `CONFIRMED`, `DISMISSED`), and reviewer notes.

---

## 6. Setup & Execution Instructions

### Prerequisites
- **Java**: JDK 25 LTS (OpenJDK Adoptium 25).
- **Maven**: Apache Maven 3.9+ (Installed at `C:\Users\Sujayashri baskar\tools\apache-maven-3.9.6\bin\mvn.cmd`).
- **MySQL / MariaDB**: MySQL 8.x or MariaDB 10.4+ running on port `3306` (XAMPP MySQL is active).

### Step 1: Database Setup
Ensure MySQL is running on port 3306. Create the database:

```sql
CREATE DATABASE IF NOT EXISTS makkal_nagar_db;
```

*(Note: The database `makkal_nagar_db` is already created and pre-configured on localhost:3306).*

### Step 2: Environment Variables (Optional)
The application works out-of-the-box with default local development settings. If required, customize via environment variables:

| Variable | Default Value | Description |
|---|---|---|
| `DB_URL` | `jdbc:mysql://localhost:3306/makkal_nagar_db` | MySQL connection string |
| `DB_USERNAME` | `root` | Database username |
| `DB_PASSWORD` | *(empty)* | Database password |

### Step 3: Start Backend Application
Navigate to the `backend/` directory and run:

```powershell
cd makkal-nagar-grievance-system/backend
& "C:\Users\Sujayashri baskar\tools\apache-maven-3.9.6\bin\mvn.cmd" spring-boot:run
```

The Spring Boot backend will start on **`http://localhost:8080`**.
On startup:
- Flyway automatically applies all 3 migrations (`V1`, `V2`, `V3`).
- All 34 synthetic complaints, 4 departments, and default admin user are initialized.
- In-memory `PriorityQueue` structures are populated from persistent MySQL records.

### Step 4: Access Frontend Application
You can access the portal in either of two ways:
1. **Served directly by Spring Boot**: Open `http://localhost:8080/index.html` in your browser.
2. **Open directly in browser**: Open `makkal-nagar-grievance-system/frontend/index.html` directly in Google Chrome / Edge. The unified `app.js` will automatically route API calls to `http://localhost:8080`.

---

## 7. Default Credentials & Demonstration Guide

### Administrator Login:
- **URL**: `http://localhost:8080/login.html`
- **Username**: `admin`
- **Password**: `Admin@Makkal2026`

### Demonstration Scenarios to Showcase:

1. **Submit a Complaint**:
   - Go to `register-complaint.html`.
   - Submit a new complaint (e.g., Water Supply at Anna Nagar, Severity: `CRITICAL`).
   - Notice the unique generated ID (e.g., `MN-2026-0035`).

2. **Full Department Capacity & Priority Waiting Queue**:
   - The *Water Supply Department* is initialized with capacity 5 and has 5 active complaints.
   - When a new water complaint is submitted, it is placed in the **Priority Waiting Queue** (`REGISTERED` status).
   - View `departments.html` and click **"View Waiting Queue"** to see grievances prioritized strictly by `CRITICAL > HIGH > MEDIUM > LOW`.

3. **Complaint Resolution & Automated Capacity Dispatch**:
   - Go to `complaint-details.html?id=1` (Active Water complaint `MN-2026-0001`).
   - Click **"Resolve Grievance"** and enter resolution notes.
   - Upon resolution, a capacity slot is freed, and the backend **automatically pulls and assigns the #1 ranked waiting complaint** from the PriorityQueue!

4. **Duplicate Candidate Detection**:
   - Go to `complaint-details.html?id=28` (`MN-2026-0028`).
   - Scroll down to **"Possible Duplicate Issues"** to see `MN-2026-0001` flagged for review due to sharing Category `WATER`, Location `Anna Nagar`, and open issue reference `PIPE-BURST-01`.
   - Click **"Confirm Duplicate"** or **"Dismiss"** to record the administrative decision in MySQL.

5. **Download CSV Report**:
   - Go to `reports.html` and click **"Download CSV Export"** to download all database records as a clean CSV spreadsheet.

---

## 8. REST API Reference

### Public APIs
- `POST /api/public/complaints`: Submit a new grievance.
- `GET /api/public/complaints/{complaintId}/track`: Retrieve public timeline (citizen phone and internal notes sanitized).
- `GET /api/public/stats`: Homepage municipal statistics counters.

### Authentication APIs
- `POST /api/auth/login`: Authenticate with username and password.
- `POST /api/auth/logout`: Invalidate session.
- `GET /api/auth/me`: Retrieve current logged-in user details.

### Administrative APIs (Requires `ROLE_ADMIN`)
- `GET /api/admin/complaints`: Search and filter complaints with pagination.
- `GET /api/admin/complaints/{id}`: Detailed grievance audit information.
- `PATCH /api/admin/complaints/{id}/status`: Transition status (`REGISTERED -> ASSIGNED -> IN_PROGRESS -> RESOLVED`).
- `POST /api/admin/complaints/{id}/assign`: Assign to department.
- `POST /api/admin/complaints/{id}/reassign`: Administrative department reassignment.
- `GET /api/admin/complaints/{id}/duplicates`: List flagged duplicate candidates.
- `POST /api/admin/complaints/{id}/duplicates/{dupId}/confirm`: Confirm duplicate link.
- `POST /api/admin/complaints/{id}/duplicates/{dupId}/dismiss`: Dismiss duplicate candidate.
- `GET /api/admin/complaints/waiting/{category}`: Inspect ordered in-memory `PriorityQueue` elements.
- `GET /api/admin/departments`: Department capacities, active assignments, and queues.
- `PATCH /api/admin/departments/{id}/capacity`: Update capacity with validation.
- `GET /api/admin/dashboard/summary`: Dashboard counters and top queue items.
- `GET /api/admin/dashboard/charts`: Series data for Chart.js.
- `GET /api/admin/reports/departments`: Workload and utilization report.
- `GET /api/admin/reports/pending-age`: Pending complaint aging analysis.
- `GET /api/admin/reports/categories`: Category breakdown and resolution rates.
- `GET /api/admin/reports/status`: Lifecycle status counts.
- `GET /api/admin/reports/resolution-time`: Average resolution durations.
- `GET /api/admin/reports/export.csv`: Export all complaints to CSV.

---

## 9. Automated Testing & Verification

Run the test suite:

```powershell
cd makkal-nagar-grievance-system/backend
& "C:\Users\Sujayashri baskar\tools\apache-maven-3.9.6\bin\mvn.cmd" test
```

### Test Outcomes:
- **15 Tests Executed & Passed**:
  1. Complaint Registration
  2. Unique Complaint ID Generation
  3. Category-to-Department Mapping
  4. Severity Ordering
  5. Affected-Person Tie Breaking
  6. Arrival-Order Tie Breaking
  7. Department Capacity Enforcement
  8. Waiting Queue Auto-Dispatch on Resolution
  9. Invalid Status Transition Rejection
  10. Complaint Resolution & Mandatory Notes
  11. Duplicate Candidate Detection
  12. Duplicate Confirmation & Dismissal
  13. Unauthorized Administrative Request Protection (HTTP 401)
  14. Authorized Dashboard Statistics Retrieval
  15. Capacity Reduction Validation

---

## 10. Git Setup & GitHub Repository Instructions

To initialize the repository and push to GitHub:

```bash
cd makkal-nagar-grievance-system
git init
git add .
git commit -m "feat: complete Makkal Nagar Government Grievance Management System"
git branch -M main
git remote add origin https://github.com/<your-username>/makkal-nagar-grievance-system.git
git push -u origin main
```

---

## 11. Common Errors & Troubleshooting

1. **MySQL Connection Refused (`CommunicationsException`)**:
   - Ensure MySQL / MariaDB service is running in XAMPP on port `3306`.
   - Verify database `makkal_nagar_db` exists (`CREATE DATABASE makkal_nagar_db;`).
2. **HTTP 401 Unauthorized on Admin Pages**:
   - You must log in via `login.html` first using `admin` / `Admin@Makkal2026`.
3. **Capacity Reduction Error (`400 Bad Request`)**:
   - You cannot set department capacity lower than the current number of active assignments. Resolve or reassign active cases before decreasing capacity.

---

## 12. Project Demonstration Checklist

- [x] Public Homepage with live municipal counters (`index.html`)
- [x] Citizen Complaint Registration with instant ID feedback (`register-complaint.html`)
- [x] Public Citizen Tracking without exposing private data (`track-complaint.html`)
- [x] Admin Login with BCrypt authentication (`login.html`)
- [x] Interactive Dashboard with Chart.js analytics (`dashboard.html`)
- [x] Filterable All Complaints table with pagination (`complaints.html`)
- [x] Complaint Details with Status Audit Trail & Actions (`complaint-details.html`)
- [x] Department Capacity Management & In-Memory PriorityQueue viewer (`departments.html`)
- [x] Analytical Reports & CSV Data Export (`reports.html`)
- [x] 34 Realistic Synthetic Complaints across 4 Departments and 5 Locations
- [x] 15 Automated Tests with 100% Pass Rate
- [x] Comprehensive Documentation, ER Diagram, UML Diagram, and Architecture Specs
