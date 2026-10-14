-- Flyway Migration: V3__seed_synthetic_complaints.sql
-- 34 Comprehensive Synthetic Grievances across 4 Departments and 5 Locations

-- 1. Seed Citizens (Fictional citizen profiles)
INSERT INTO citizens (id, citizen_reference, name, phone, location) VALUES
(1, 'CIT-1001', 'Ramanathan Swaminathan', '9840112345', 'Anna Nagar'),
(2, 'CIT-1002', 'Meenakshi Sundaram', '9840223456', 'Gandhi Street'),
(3, 'CIT-1003', 'Kavitha Rajendran', '9840334567', 'Nehru Colony'),
(4, 'CIT-1004', 'Arunkumar Velusamy', '9840445678', 'Lake View Road'),
(5, 'CIT-1005', 'Suresh Balakrishnan', '9840556789', 'Market Area'),
(6, 'CIT-1006', 'Priya Senthil', '9840667890', 'Anna Nagar'),
(7, 'CIT-1007', 'Dinesh Karthik', '9840778901', 'Gandhi Street'),
(8, 'CIT-1008', 'Ananya Natarajan', '9840889012', 'Nehru Colony'),
(9, 'CIT-1009', 'Vijay Raghavan', '9840990123', 'Lake View Road'),
(10, 'CIT-1010', 'Deepa Jayakumar', '9840101234', 'Market Area'),
(11, 'CIT-1011', 'Balamurugan Periasamy', '9840212345', 'Anna Nagar'),
(12, 'CIT-1012', 'Shanthi Subramanian', '9840323456', 'Gandhi Street')
ON DUPLICATE KEY UPDATE name = VALUES(name);

-- 2. Seed Complaints (Covering Water, Road, Streetlight, Waste)
-- Note: Department IDs: 1 = WATER, 2 = ROAD, 3 = STREETLIGHT, 4 = WASTE
INSERT INTO complaints (id, complaint_id, citizen_id, category, description, location, severity, affected_count, arrival_order, status, department_id, open_issue_ref, resolution_notes, resolved_at, created_at) VALUES
-- WATER SUPPLY DEPARTMENT (Capacity = 5; 5 Active assignments = FULL CAPACITY)
(1, 'MN-2026-0001', 1, 'WATER', 'Severe pipeline burst near main overhead water tank causing massive drinking water flooding.', 'Anna Nagar', 'CRITICAL', 120, 1, 'IN_PROGRESS', 1, 'PIPE-BURST-01', NULL, NULL, '2026-10-01 08:30:00'),
(2, 'MN-2026-0002', 2, 'WATER', 'Low pressure contaminated water supply with yellowish color and foul odor.', 'Gandhi Street', 'HIGH', 45, 2, 'ASSIGNED', 1, NULL, NULL, NULL, '2026-10-01 09:15:00'),
(3, 'MN-2026-0003', 3, 'WATER', 'Main distribution valve broken, leaking thousands of liters per hour continuously.', 'Nehru Colony', 'HIGH', 60, 3, 'IN_PROGRESS', 1, NULL, NULL, NULL, '2026-10-01 10:00:00'),
(4, 'MN-2026-0004', 4, 'WATER', 'Drinking water supply completely disrupted for the past 48 hours in sector 4.', 'Lake View Road', 'HIGH', 50, 4, 'ASSIGNED', 1, NULL, NULL, NULL, '2026-10-01 10:45:00'),
(5, 'MN-2026-0005', 5, 'WATER', 'Submerged public water tap leaking onto pedestrian sidewalk creating mud.', 'Market Area', 'MEDIUM', 30, 5, 'ASSIGNED', 1, NULL, NULL, NULL, '2026-10-01 11:30:00'),

-- WAITING FOR WATER SUPPLY CAPACITY (Demonstrates Priority Ordering & Tie-Breaking)
-- Queue Item 1: CRITICAL (Highest Severity wins over all)
(6, 'MN-2026-0006', 6, 'WATER', 'Major sewage seepage entering municipal drinking water main line. Immediate health hazard!', 'Anna Nagar', 'CRITICAL', 90, 6, 'REGISTERED', 1, NULL, NULL, NULL, '2026-10-01 12:00:00'),
-- Queue Item 2: HIGH with affected_count = 80 (Wins tie-break over Queue Item 3 with affected_count = 25)
(7, 'MN-2026-0007', 7, 'WATER', 'Apartment block feeder pipe fractured under road slab.', 'Gandhi Street', 'HIGH', 80, 7, 'REGISTERED', 1, NULL, NULL, NULL, '2026-10-01 12:30:00'),
-- Queue Item 3: HIGH with affected_count = 25, arrival_order = 8 (Wins tie-break over Queue Item 4 with equal affected_count 25 but later arrival_order 9)
(8, 'MN-2026-0008', 8, 'WATER', 'Supply valve blocked by mineral sediment causing zero flow.', 'Nehru Colony', 'HIGH', 25, 8, 'REGISTERED', 1, NULL, NULL, NULL, '2026-10-01 13:00:00'),
-- Queue Item 4: HIGH with equal affected_count = 25, arrival_order = 9 (Deterministic tie-break by arrival order)
(9, 'MN-2026-0009', 9, 'WATER', 'Ruptured domestic connection line eroding roadway embankment.', 'Lake View Road', 'HIGH', 25, 9, 'REGISTERED', 1, NULL, NULL, NULL, '2026-10-01 13:15:00'),
-- Queue Item 5: LOW severity waiting
(10, 'MN-2026-0010', 10, 'WATER', 'Minor dripping at community water outlet.', 'Market Area', 'LOW', 10, 10, 'REGISTERED', 1, NULL, NULL, NULL, '2026-10-01 13:30:00'),

-- RESOLVED WATER COMPLAINTS
(11, 'MN-2026-0011', 11, 'WATER', 'Old corroded pipe replaced in 2nd cross street.', 'Anna Nagar', 'MEDIUM', 35, 11, 'RESOLVED', 1, NULL, 'New 110mm PVC line laid and pressure restored. Verified by field engineer.', '2026-10-02 16:00:00', '2026-09-28 09:00:00'),
(12, 'MN-2026-0012', 12, 'WATER', 'Air-lock in distribution pipe cleared.', 'Gandhi Street', 'LOW', 12, 12, 'RESOLVED', 1, NULL, 'Air-release valve inspected and purged. Flow normalized.', '2026-10-03 11:30:00', '2026-09-29 10:15:00'),

-- ROADS AND INFRASTRUCTURE DEPARTMENT (Capacity = 5; Active = 4, Waiting = 2, Resolved = 2)
(13, 'MN-2026-0013', 1, 'ROAD', 'Deep pothole cluster on primary bus route causing vehicular accidents and gridlock.', 'Anna Nagar', 'CRITICAL', 250, 13, 'IN_PROGRESS', 2, NULL, NULL, NULL, '2026-10-01 14:00:00'),
(14, 'MN-2026-0014', 2, 'ROAD', 'Stormwater trench left open without barricades or warning markers near school.', 'Gandhi Street', 'HIGH', 150, 14, 'IN_PROGRESS', 2, NULL, NULL, NULL, '2026-10-01 14:20:00'),
(15, 'MN-2026-0015', 3, 'ROAD', 'Asphalt eroded exposing sharp aggregate and damaging two-wheelers.', 'Nehru Colony', 'MEDIUM', 40, 15, 'ASSIGNED', 2, NULL, NULL, NULL, '2026-10-01 14:40:00'),
(16, 'MN-2026-0016', 4, 'ROAD', 'Damaged concrete speed bump with protruding rebar.', 'Lake View Road', 'MEDIUM', 30, 16, 'ASSIGNED', 2, NULL, NULL, NULL, '2026-10-01 15:00:00'),
-- Waiting in Roads queue:
(17, 'MN-2026-0017', 5, 'ROAD', 'Caved-in manhole collar near vegetable market intersection.', 'Market Area', 'HIGH', 180, 17, 'REGISTERED', 2, NULL, NULL, NULL, '2026-10-01 15:30:00'),
(18, 'MN-2026-0018', 6, 'ROAD', 'Uneven gravel spreading across pedestrian footpath.', 'Anna Nagar', 'LOW', 15, 18, 'REGISTERED', 2, NULL, NULL, NULL, '2026-10-01 15:45:00'),
-- Resolved Roads:
(19, 'MN-2026-0019', 7, 'ROAD', 'Cracked road junction patched with cold-mix asphalt.', 'Gandhi Street', 'MEDIUM', 65, 19, 'RESOLVED', 2, NULL, 'Road patch leveled and compacted with roller.', '2026-10-04 17:00:00', '2026-09-29 11:00:00'),
(20, 'MN-2026-0020', 8, 'ROAD', 'Sunken culvert repaired.', 'Nehru Colony', 'HIGH', 100, 20, 'RESOLVED', 2, NULL, 'Slab replaced and re-tarred. Traffic moving normally.', '2026-10-05 14:00:00', '2026-09-30 08:30:00'),

-- STREETLIGHT AND ELECTRICAL DEPARTMENT (Capacity = 5; Active = 3, Waiting = 2, Resolved = 2)
(21, 'MN-2026-0021', 9, 'STREETLIGHT', 'Live electrical wire snapped from pole and hanging low over public walkway!', 'Lake View Road', 'CRITICAL', 85, 21, 'IN_PROGRESS', 3, NULL, NULL, NULL, '2026-10-02 07:00:00'),
(22, 'MN-2026-0022', 10, 'STREETLIGHT', 'Entire market lane dark for 5 consecutive nights due to phase failure.', 'Market Area', 'HIGH', 110, 22, 'ASSIGNED', 3, NULL, NULL, NULL, '2026-10-02 08:00:00'),
(23, 'MN-2026-0023', 11, 'STREETLIGHT', 'Timer relay fault causing streetlights to remain on during broad daylight.', 'Anna Nagar', 'MEDIUM', 20, 23, 'ASSIGNED', 3, NULL, NULL, NULL, '2026-10-02 09:00:00'),
-- Waiting in Streetlight queue:
(24, 'MN-2026-0024', 12, 'STREETLIGHT', 'Two consecutive LED streetlight luminaires flickering and sparking.', 'Gandhi Street', 'HIGH', 40, 24, 'REGISTERED', 3, NULL, NULL, NULL, '2026-10-02 10:00:00'),
(25, 'MN-2026-0025', 1, 'STREETLIGHT', 'Corroded lamp post leaning dangerously towards parking lot.', 'Anna Nagar', 'MEDIUM', 25, 25, 'REGISTERED', 3, NULL, NULL, NULL, '2026-10-02 11:00:00'),
-- Resolved Streetlight:
(26, 'MN-2026-0026', 2, 'STREETLIGHT', 'Blown fuse replaced in distribution feeder pillar.', 'Gandhi Street', 'MEDIUM', 50, 26, 'RESOLVED', 3, NULL, 'Fuse replaced and load tested. All 12 luminaires functional.', '2026-10-04 20:30:00', '2026-09-30 16:00:00'),
(27, 'MN-2026-0027', 3, 'STREETLIGHT', 'Faulty photocell sensor replaced on pole #14.', 'Nehru Colony', 'LOW', 10, 27, 'RESOLVED', 3, NULL, 'New photocell fitted. Lights trigger at dusk properly.', '2026-10-05 18:00:00', '2026-10-01 17:00:00'),

-- DUPLICATE CANDIDATE TEST RECORD (Matches MN-2026-0001: Category WATER, Location Anna Nagar, Open Issue Ref PIPE-BURST-01)
(28, 'MN-2026-0028', 4, 'WATER', 'Massive drinking water pipe leakage flooding road near main tank.', 'Anna Nagar', 'CRITICAL', 115, 28, 'REGISTERED', 1, 'PIPE-BURST-01', NULL, NULL, '2026-10-02 11:30:00'),

-- SANITATION AND WASTE MANAGEMENT DEPARTMENT (Capacity = 5; Active = 4, Waiting = 2, Resolved = 3)
(29, 'MN-2026-0029', 5, 'WASTE', 'Massive uncollected garbage accumulation overflowing onto road for 6 days, breeding flies and rodents.', 'Market Area', 'CRITICAL', 200, 29, 'IN_PROGRESS', 4, 'GARBAGE-MK-01', NULL, NULL, '2026-10-02 12:00:00'),
(30, 'MN-2026-0030', 6, 'WASTE', 'Public dumper bin turned over and blocking residential colony access road.', 'Anna Nagar', 'HIGH', 75, 30, 'ASSIGNED', 4, NULL, NULL, NULL, '2026-10-02 12:30:00'),
(31, 'MN-2026-0031', 7, 'WASTE', 'Medical and sanitary waste dumped illegally on empty plot.', 'Gandhi Street', 'HIGH', 60, 31, 'IN_PROGRESS', 4, NULL, NULL, NULL, '2026-10-02 13:00:00'),
(32, 'MN-2026-0032', 8, 'WASTE', 'Green garden waste and tree branches blocking drainage gutter.', 'Nehru Colony', 'MEDIUM', 30, 32, 'ASSIGNED', 4, NULL, NULL, NULL, '2026-10-02 13:30:00'),
-- Waiting in Waste queue:
(33, 'MN-2026-0033', 9, 'WASTE', 'Daily door-to-door waste collection skipped for 3 days in lake perimeter avenue.', 'Lake View Road', 'HIGH', 95, 33, 'REGISTERED', 4, NULL, NULL, NULL, '2026-10-02 14:00:00'),
(34, 'MN-2026-0034', 10, 'WASTE', 'Overflowing community compost bin attracting stray animals.', 'Market Area', 'MEDIUM', 50, 34, 'REGISTERED', 4, NULL, NULL, NULL, '2026-10-02 14:30:00')
ON DUPLICATE KEY UPDATE description = VALUES(description);

-- Update arrival sequence to match highest seeded arrival order (34)
UPDATE arrival_sequences SET current_val = 34 WHERE sequence_name = 'complaint_arrival';

-- 3. Seed Assignment Records for active and resolved cases
INSERT INTO assignments (complaint_id, department_id, assigned_by, assigned_at, notes) VALUES
(1, 1, 'SYSTEM', '2026-10-01 08:35:00', 'Automated category assignment to Water Supply Department'),
(2, 1, 'SYSTEM', '2026-10-01 09:20:00', 'Automated category assignment to Water Supply Department'),
(3, 1, 'SYSTEM', '2026-10-01 10:05:00', 'Automated category assignment to Water Supply Department'),
(4, 1, 'SYSTEM', '2026-10-01 10:50:00', 'Automated category assignment to Water Supply Department'),
(5, 1, 'SYSTEM', '2026-10-01 11:35:00', 'Automated category assignment to Water Supply Department'),
(11, 1, 'SYSTEM', '2026-09-28 09:10:00', 'Assigned to field pipeline repair squad'),
(12, 1, 'SYSTEM', '2026-09-29 10:20:00', 'Assigned to valve maintenance team'),
(13, 2, 'SYSTEM', '2026-10-01 14:05:00', 'Automated assignment to Roads Department'),
(14, 2, 'SYSTEM', '2026-10-01 14:25:00', 'Automated assignment to Roads Department'),
(15, 2, 'SYSTEM', '2026-10-01 14:45:00', 'Automated assignment to Roads Department'),
(16, 2, 'SYSTEM', '2026-10-01 15:05:00', 'Automated assignment to Roads Department'),
(19, 2, 'SYSTEM', '2026-09-29 11:10:00', 'Assigned to asphalt crew'),
(20, 2, 'SYSTEM', '2026-09-30 08:40:00', 'Assigned to civil works team'),
(21, 3, 'SYSTEM', '2026-10-02 07:05:00', 'Automated assignment to Electrical Department'),
(22, 3, 'SYSTEM', '2026-10-02 08:05:00', 'Automated assignment to Electrical Department'),
(23, 3, 'SYSTEM', '2026-10-02 09:05:00', 'Automated assignment to Electrical Department'),
(26, 3, 'SYSTEM', '2026-09-30 16:10:00', 'Assigned to streetlight maintenance gang'),
(27, 3, 'SYSTEM', '2026-10-01 17:10:00', 'Assigned to sensor technician'),
(29, 4, 'SYSTEM', '2026-10-02 12:05:00', 'Automated assignment to Sanitation Department'),
(30, 4, 'SYSTEM', '2026-10-02 12:35:00', 'Automated assignment to Sanitation Department'),
(31, 4, 'SYSTEM', '2026-10-02 13:05:00', 'Automated assignment to Sanitation Department'),
(32, 4, 'SYSTEM', '2026-10-02 13:35:00', 'Automated assignment to Sanitation Department')
ON DUPLICATE KEY UPDATE assigned_by = VALUES(assigned_by);

-- 4. Seed Status Update History for audit trail
INSERT INTO status_updates (complaint_id, previous_status, new_status, changed_by, remarks, created_at) VALUES
(1, NULL, 'REGISTERED', 'CITIZEN', 'Complaint registered online by citizen', '2026-10-01 08:30:00'),
(1, 'REGISTERED', 'ASSIGNED', 'SYSTEM', 'Capacity available in Water Supply Department', '2026-10-01 08:35:00'),
(1, 'ASSIGNED', 'IN_PROGRESS', 'admin', 'Field crew dispatched with excavation excavator', '2026-10-01 09:00:00'),

(2, NULL, 'REGISTERED', 'CITIZEN', 'Complaint registered online by citizen', '2026-10-01 09:15:00'),
(2, 'REGISTERED', 'ASSIGNED', 'SYSTEM', 'Capacity available in Water Supply Department', '2026-10-01 09:20:00'),

(3, NULL, 'REGISTERED', 'CITIZEN', 'Complaint registered online by citizen', '2026-10-01 10:00:00'),
(3, 'REGISTERED', 'ASSIGNED', 'SYSTEM', 'Capacity available in Water Supply Department', '2026-10-01 10:05:00'),
(3, 'ASSIGNED', 'IN_PROGRESS', 'admin', 'Replacement flange valve ordered and in transit', '2026-10-01 11:00:00'),

(6, NULL, 'REGISTERED', 'CITIZEN', 'Complaint registered online. Water Supply Department at maximum capacity (5/5). Enqueued in priority waiting queue.', '2026-10-01 12:00:00'),

(11, NULL, 'REGISTERED', 'CITIZEN', 'Complaint registered', '2026-09-28 09:00:00'),
(11, 'REGISTERED', 'ASSIGNED', 'SYSTEM', 'Assigned to Water Supply Department', '2026-09-28 09:10:00'),
(11, 'ASSIGNED', 'IN_PROGRESS', 'admin', 'Work begun on site', '2026-09-29 10:00:00'),
(11, 'IN_PROGRESS', 'RESOLVED', 'admin', 'New 110mm PVC line laid and pressure restored. Verified by field engineer.', '2026-10-02 16:00:00'),

(28, NULL, 'REGISTERED', 'CITIZEN', 'Complaint registered online. Flagged as possible duplicate of MN-2026-0001 (Same category WATER, same location Anna Nagar, shared issue reference PIPE-BURST-01).', '2026-10-02 11:30:00')
ON DUPLICATE KEY UPDATE remarks = VALUES(remarks);

-- 5. Seed Duplicate Links Table (Flagged Duplicate Candidate for Operator Review)
INSERT INTO duplicate_links (source_complaint_id, target_complaint_id, status, reason, created_at) VALUES
(28, 1, 'FLAGGED', 'Matches open issue ref PIPE-BURST-01 in Anna Nagar for category WATER', '2026-10-02 11:30:00')
ON DUPLICATE KEY UPDATE reason = VALUES(reason);
