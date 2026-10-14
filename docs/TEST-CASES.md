# Automated Test Cases & Verification Report
## Makkal Nagar Municipal Corporation — Grievance Management System

This document catalogs the 22 automated test specifications implemented and verified using JUnit 5, Spring Boot Test, and MockMvc.

| # | Test Case Identifier | Method / Class | Requirement Description | Status |
|---|---|---|---|---|
| **1** | `TC-01-DUP-IDENTICAL` | `ComplaintServiceIntegrationTest.testIdenticalDescriptionsAtSameLocation` | Identical description & same location detected as duplicate (Confidence >= 0.85) | **PASSED** |
| **2** | `TC-02-DUP-WORDING` | `ComplaintServiceIntegrationTest.testSimilarDescriptionsDifferentWording` | Synonym and wording variation detected via NLP text similarity (Confidence >= 0.60) | **PASSED** |
| **3** | `TC-03-DUP-SAME-LOC-DIFF-ISSUE` | `ComplaintServiceIntegrationTest.testSameLocationUnrelatedComplaints` | Same location with completely unrelated issue is NOT flagged as duplicate | **PASSED** |
| **4** | `TC-04-DUP-DIFF-LOC-SIMILAR` | `ComplaintServiceIntegrationTest.testSimilarDescriptionsDifferentLocations` | Similar text at different locations is NOT flagged as duplicate | **PASSED** |
| **5** | `TC-05-DUP-DIFF-ID-SAME-ISSUE` | `ComplaintServiceIntegrationTest.testDifferentComplaintIdsSameIssue` | Different complaint IDs reporting the same issue are linked as duplicate candidates | **PASSED** |
| **6** | `TC-06-REPEATED-ID-REJECTION` | `ComplaintServiceIntegrationTest.testRepeatedComplaintIdRejection` | Duplicate/reused complaint ID is rejected as an ID conflict (throws `ConflictException`) | **PASSED** |
| **7** | `TC-07-DUP-MULTIPLE-CANDIDATES` | `ComplaintServiceIntegrationTest.testMultiplePossibleDuplicateCandidates` | Newly submitted issue matching multiple existing issues correctly links to all candidates | **PASSED** |
| **8** | `TC-08-DEPT-WORK-START` | `ComplaintServiceIntegrationTest.testDepartmentWorkStartedTransition` | `startDepartmentWork` transitions `ASSIGNED -> IN_PROGRESS` with event `DEPARTMENT_WORK_STARTED` | **PASSED** |
| **9** | `TC-09-VERIFIED-RESOLUTION-DISPATCH` | `ComplaintServiceIntegrationTest.testResolutionVerifiedAndAutoDispatch` | `recordVerifiedResolution` transitions `IN_PROGRESS -> RESOLVED`, frees slot, auto-dispatches waiting complaint | **PASSED** |
| **10**| `TC-10-INVALID-TRANSITION-REJECT` | `ComplaintServiceIntegrationTest.testInvalidStatusTransitionRejected` | Illegal transitions (e.g. `REGISTERED -> RESOLVED`) are rejected with `InvalidOperationException` | **PASSED** |
| **11**| `TC-11-MANUAL-STATUS-PROHIBITED` | `ComplaintServiceIntegrationTest.testManualStatusModificationProhibited` | Direct call to `updateStatus` is strictly prohibited and throws exception | **PASSED** |
| **12**| `TC-12-STATUS-HISTORY-PERSISTENCE` | `ComplaintServiceIntegrationTest.testStatusHistoryPersistedWithTriggeringEvent` | Validates MySQL persistence of `previous_status`, `new_status`, `triggering_event`, actor, and remarks | **PASSED** |
| **13**| `TC-13-SEV-ORDER` | `PriorityQueueAndOrderingTest.testSeverityOrdering` | Verifies `CRITICAL > HIGH > MEDIUM > LOW` ranking in PriorityQueue | **PASSED** |
| **14**| `TC-14-AFFECTED-TIE` | `PriorityQueueAndOrderingTest.testAffectedCountTieBreaking` | Equal severity tie-break: higher affected-person count ranks first | **PASSED** |
| **15**| `TC-15-ARRIVAL-TIE` | `PriorityQueueAndOrderingTest.testArrivalOrderTieBreaking` | Equal severity and affected count: earlier arrival order ranks first (FIFO determinism) | **PASSED** |
| **16**| `TC-16-EMPTY-QUEUE` | `PriorityQueueAndOrderingTest.testEmptyQueuePoll` | Polling empty PriorityQueue safely returns null | **PASSED** |
| **17**| `TC-17-PEEK-NO-REMOVAL` | `PriorityQueueAndOrderingTest.testQueuePeekDoesNotRemove` | Peeking returns highest priority item without dequeueing | **PASSED** |
| **18**| `TC-18-CAPACITY-CONSTRAINTS` | `DepartmentCapacityTest.testDepartmentCapacityConstraints` | Department capacity bounds enforcement | **PASSED** |
| **19**| `TC-19-UNAUTH-ADMIN-REJECT` | `AdminControllerSecurityTest.testUnauthorizedAdminRequest` | Unauthenticated requests to `/api/admin/**` return HTTP 401 Unauthorized | **PASSED** |
| **20**| `TC-20-DASHBOARD-TELEMETRY` | `AdminControllerSecurityTest.testAuthorizedAdminRequest` | Admin can retrieve real database counts, duplicate telemetry, and chart series | **PASSED** |
| **21**| `TC-21-OOP-DOMAIN-ENCAPSULATION` | `GrievanceDomainOopTest.testDomainEntityEncapsulation` | OOP encapsulation and immutable ID constraints | **PASSED** |
| **22**| `TC-22-OOP-INVALID-TRANSITION` | `GrievanceDomainOopTest.testInvalidStatusTransition` | Rejects illegal status jumps at domain entity level | **PASSED** |

---

### Test Execution Command
To run all automated test suites from the command line:

```powershell
cd backend
mvn test
```

### Verification Result Summary
- **Tests run**: 22
- **Failures**: 0
- **Errors**: 0
- **Skipped**: 0
- **Build Outcome**: `BUILD SUCCESS`
