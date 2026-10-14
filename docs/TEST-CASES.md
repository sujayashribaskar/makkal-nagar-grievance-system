# Automated Test Cases & Verification Report
## Makkal Nagar Municipal Corporation — Grievance Management System

This document catalogs the 15 automated test specifications implemented and verified using JUnit 5, Spring Boot Test, and MockMvc.

| # | Test Case Identifier | Method / Class | Requirement Description | Status |
|---|----------------------|----------------|-------------------------|--------|
| **1** | `TC-01-COMPLAINT-REG` | `ComplaintServiceIntegrationTest.testComplaintRegistrationAndIdGeneration` | Registers citizen grievance, creates citizen profile, validates mandatory fields | **PASSED** |
| **2** | `TC-02-UNIQUE-ID` | `ComplaintServiceIntegrationTest.testComplaintRegistrationAndIdGeneration` | Generates unique formatted ID (`MN-2026-XXXX`) via atomic sequence counter | **PASSED** |
| **3** | `TC-03-DEPT-MAP` | `ComplaintServiceIntegrationTest.testCategoryToDepartmentMapping` | Routes `WATER -> Water Supply`, `ROAD -> Roads & Infra`, `STREETLIGHT -> Electrical`, `WASTE -> Sanitation` | **PASSED** |
| **4** | `TC-04-SEV-ORDER` | `PriorityQueueAndOrderingTest.testSeverityOrdering` | Verifies `CRITICAL > HIGH > MEDIUM > LOW` ranking in PriorityQueue | **PASSED** |
| **5** | `TC-05-AFFECTED-TIE` | `PriorityQueueAndOrderingTest.testAffectedCountTieBreaking` | Equal severity tie-break: higher affected-person count ranks first | **PASSED** |
| **6** | `TC-06-ARRIVAL-TIE` | `PriorityQueueAndOrderingTest.testArrivalOrderTieBreaking` | Equal severity and affected count: earlier arrival order ranks first (FIFO determinism) | **PASSED** |
| **7** | `TC-07-DEPT-CAPACITY` | `ComplaintServiceIntegrationTest.testCapacityEnforcementAndResolutionDispatch` | Rejects immediate active assignment when active cases >= capacity; enqueues in PriorityQueue | **PASSED** |
| **8** | `TC-08-QUEUE-DISPATCH`| `ComplaintServiceIntegrationTest.testCapacityEnforcementAndResolutionDispatch` | Automatically pulls highest priority waiting complaint when active slot is freed | **PASSED** |
| **9** | `TC-09-INVALID-TRANS` | `GrievanceDomainOopTest.testInvalidStatusTransition` | Rejects illegal status jumps (`REGISTERED -> RESOLVED`, `RESOLVED -> IN_PROGRESS`), throws `IllegalStateException` | **PASSED** |
| **10**| `TC-10-COMPLAINT-RES` | `ComplaintServiceIntegrationTest.testComplaintResolution` | Marking complaint `RESOLVED` requires notes, sets `resolved_at`, and frees department capacity | **PASSED** |
| **11**| `TC-11-DUP-DETECTION` | `ComplaintServiceIntegrationTest.testDuplicateDetectionAndReview` | Flags potential duplicate candidates sharing Category & Location or Open Issue Reference without auto-deletion | **PASSED** |
| **12**| `TC-12-DUP-CONFIRM` | `ComplaintServiceIntegrationTest.testDuplicateDetectionAndReview` | Operator can confirm or dismiss duplicate relationships; persists decision and reviewer in MySQL | **PASSED** |
| **13**| `TC-13-AUTH-PROTECT` | `AdminControllerSecurityTest.testUnauthorizedAdminRequest` | Public endpoints permit all; unauthenticated requests to `/api/admin/**` return HTTP 401 Unauthorized | **PASSED** |
| **14**| `TC-14-DASHBOARD-STAT`| `AdminControllerSecurityTest.testAuthorizedAdminRequest` | Validates admin can retrieve real database counts and Chart.js series data | **PASSED** |
| **15**| `TC-15-CAPACITY-CHANGE`| `DepartmentCapacityTest.testCapacityChangeValidation` | Rejects capacity reduction below current active assignment count; accepts valid capacity expansions | **PASSED** |

---

### Test Execution Command
To run all automated test suites from the command line:

```powershell
cd makkal-nagar-grievance-system/backend
mvn test
```

### Verification Result Summary
- **Tests run**: 15
- **Failures**: 0
- **Errors**: 0
- **Skipped**: 0
- **Build Outcome**: `BUILD SUCCESS`
