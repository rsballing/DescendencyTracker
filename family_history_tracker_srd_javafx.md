# Family History Pedigree & Descendancy Tracker
## Software Requirements Document and Software Design Specification

**Primary target stack:** Java 21, JavaFX, SQLite, IntelliJ IDEA  
**Future expansion target:** Android companion app with offline-first sync architecture

## 1. Executive Summary
This document specifies a private, local-first family-history application for personal manual data entry and line management. The application is designed to help the user work from self backward through ancestors, then inspect and manage the descendancy of each ancestor in an ordered way.

The core working model is **pedigree-indexed descendancy management**:
- the user starts from self,
- navigates backward through ancestors,
- and sees for each ancestor a summary badge describing the condition of that ancestor's descendant line.

Version 1 is a desktop application using Java 21 with JavaFX and SQLite. The Version 1 architecture must also be deliberately shaped so that a later Android client can reuse the same domain concepts, repository contracts, and synchronization model without requiring a redesign of the core business logic.

## 2. Product Vision
Build a private, highly ordered family-history workbench that helps the user engage with individual people and ancestor lines personally while also maintaining a clear operational picture of:
- which ancestor lines are complete for now,
- which have open work now,
- which are waiting on the 110-year rule,
- which are likely to become actionable within 1, 2, 5, or 10 years,
- and which lines are incomplete, unresolved, or externally managed.

The product is not intended to be an automated scraping or reservation system. It is meant to support deliberate, personally engaged manual work.

## 3. Technology Recommendation
### Version 1 Recommendation
**Recommended primary stack:** Java 21 + JavaFX + SQLite + JDBC.

### Alternative Desktop Stack
**Alternative:** Python + PySide/PyQt + SQLite.

### Future Mobile Stack Recommendation
For a future Android version, the preferred mobile implementation is:
- Kotlin
- Android Jetpack architecture
- Room or SQLite-backed local persistence
- repository-based data layer
- sync service/API for cross-device convergence

Java is preferred for Version 1 because it aligns with the intended IntelliJ environment, is durable for long-term desktop maintenance, and supports a clean local-first workflow. The future Android client should be treated as a separate client application that shares domain rules and data contracts rather than as a direct extension of the JavaFX UI.

## 4. Cross-Platform Strategy and Versioning Approach
### Version 1
Desktop-only local application:
- JavaFX UI
- local SQLite database
- manual entry workflow
- import/export and local backups
- no cloud dependency required

### Version 2
Android companion application and optional sync capability:
- Android device keeps its own local database
- desktop keeps its own local database
- both synchronize through a defined sync protocol or backend service
- no direct multi-device access to one shared live SQLite file

### Design Principle
The Version 1 system shall be built so that:
- UI code does not contain core business logic,
- persistence details are hidden behind repositories/services,
- domain rules are reusable,
- entities have stable IDs and sync metadata,
- and future synchronization can be added without replacing the data model.

## 5. Scope
### In Scope for Version 1
- Manual entry of persons, parent-child relationships, and spouse relationships
- Pedigree view rooted in the user
- Descendancy detail view for any selected ancestor
- Per-person ordinance statuses for baptism, confirmation, initiatory, endowment, and sealed to parents
- Per-spouse sealing status for each spouse relationship
- Automatic computation of 110-year milestones
- Ancestor-line summary status badges
- Near-term horizon summaries for lines becoming relevant in 1, 2, 5, or 10 years
- Local persistence in SQLite
- CSV import/export and backup
- Notes, review status, and line-management metadata
- Schema and architecture support for future sync metadata

### Explicitly Out of Scope for Version 1
- Direct FamilySearch synchronization
- Automated scraping or reservation automation
- Multi-user real-time collaboration
- Cloud account management
- Mobile-first UI
- Shared live SQLite access from multiple devices

### Planned for Future Versions
- Android companion app
- Optional backend/API sync
- Cross-device conflict resolution
- device-aware sync state and audit history

## 6. Core User Stories
- Start from self and move backward through ancestors
- See a summary badge for each ancestor's descendant line
- Open an ancestor and work descendants in an ordered outline
- Manually enter FamilySearch IDs, names, dates, spouses, and children
- Record ordinance statuses for each person and each spouse-specific sealing
- Identify descendants blocked by the 110-year rule or opening soon
- Mark lines as mine, shared, monitor-only, or externally managed
- Use fast keyboard entry
- Export and back up work easily
- In the future, continue work on Android and have changes reconcile safely with desktop data

## 7. Functional Requirements
### Person Management
1. Create, edit, soft delete, and restore a person record
2. Store unique internal ID and optional FamilySearch PID
3. Store names, sex, living/deceased, dates, notes, reviewed status
4. Store timestamps and sync metadata for each person row

### Relationship Management
1. Support parent-child relationships
2. Support multiple spouse relationships per person
3. Preserve child ordering where supplied
4. Support notes on spouse links and parent-child links
5. Store timestamps and sync metadata on relationship rows

### Ordinance Tracking
1. Track baptism, confirmation, initiatory, endowment, sealed to parents
2. Track sealed to spouse per spouse relationship
3. Use enums rather than plain booleans
4. Allow notes and status dates
5. Allow future mapping of status changes into audit/sync events

### Ancestor Line Summary
1. Compute summary status for each ancestor
2. Aggregate descendants into counts for open, blocked, soon-eligible, complete, unresolved
3. Display visible badge in pedigree
4. Store or compute next likely availability date
5. Preserve enough metadata to re-run summary logic after sync merges

### Horizon Forecasting
1. Compute 110-year milestone
2. Classify into available now, within 1 year, within 2 years, within 5 years, within 10 years, blocked, unknown
3. Summarize upward to ancestor line

### Search, Filtering, and Reporting
1. Search by name, PID, notes
2. Filter by badge, ordinance status, horizon status, reviewed status, stewardship status
3. Produce open-now, opening-soon, unresolved-data, and stewardship reports

### Import, Export, and Backup
1. Store in local SQLite
2. Export/import CSV
3. Back up database file
4. Export line summaries
5. Export/import portable JSON or CSV structures later if needed for sync bootstrap or migration

### Future Sync-Readiness Requirements
1. The system shall assign stable IDs to every entity and relationship
2. The system shall track record version and update timestamps
3. The system shall support soft delete rather than irreversible hard delete in normal UI flows
4. The system shall support sync-status metadata per row
5. The system shall avoid UI code directly manipulating raw database state
6. The system shall support eventual introduction of a remote sync service without redesigning the main domain model

## 8. Non-Functional Requirements
- Fast keyboard-centric entry
- Immediate-feeling local UI
- Fully functional offline
- Maintainable modular package structure
- Single-file backup model for Version 1
- Safe deletion workflow
- Architecture separation between UI, domain logic, persistence, and sync contracts
- Deterministic and testable summary logic
- Future cross-platform portability of core data concepts

## 9. Business Rules and Domain Logic
### Date Precision
- Support exact dates, month-year, year-only, unknown
- Never display guessed precision as known precision

### Ordinance Status Enum
- COMPLETE
- OPEN
- BLOCKED_110
- SOON_1Y
- SOON_2Y
- SOON_5Y
- SOON_10Y
- NOT_APPLICABLE
- UNKNOWN

### Ancestor Badge Priority
1. OPEN_NOW
2. OPENING_SOON
3. WAITING_110
4. UNRESOLVED
5. COMPLETE_FOR_NOW
6. NOT_REVIEWED

### Stewardship
Optional values:
- MINE
- SHARED
- EXTERNALLY_MANAGED
- MONITOR_ONLY
- UNASSIGNED

### Sync Status Enum
Recommended values:
- LOCAL_ONLY
- DIRTY_CREATE
- DIRTY_UPDATE
- DIRTY_DELETE
- SYNCED
- CONFLICT
- ERROR

## 10. Data Model
### Core Tables
- `person`
- `parent_child_link`
- `spouse_link`
- `person_ordinance_status`
- `line_stewardship`
- `line_summary_cache`
- `note`
- `audit_log`
- `device_registry` (future-ready)
- `sync_cursor` or `sync_state` (future-ready)

### Sync-Friendly Record Metadata
Every mutable business table should include or conceptually support these fields:
- `stable_uuid`
- `created_at`
- `updated_at`
- `deleted_at` or soft-delete flag
- `version`
- `sync_status`
- `last_synced_at`
- `last_modified_by_device`

### Illustrative Schema Skeleton
```sql
person(
    person_id INTEGER PRIMARY KEY,
    stable_uuid TEXT NOT NULL UNIQUE,
    fs_pid TEXT UNIQUE NULL,
    preferred_name TEXT NOT NULL,
    given_names TEXT NULL,
    surname TEXT NULL,
    sex TEXT NULL,
    is_living INTEGER NOT NULL DEFAULT 0,
    birth_date_text TEXT NULL,
    birth_date_sort TEXT NULL,
    death_date_text TEXT NULL,
    death_date_sort TEXT NULL,
    birth_date_precision TEXT NULL,
    death_date_precision TEXT NULL,
    reviewed_status TEXT NOT NULL DEFAULT 'NOT_REVIEWED',
    last_reviewed_on TEXT NULL,
    notes TEXT NULL,
    is_deleted INTEGER NOT NULL DEFAULT 0,
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL,
    deleted_at TEXT NULL,
    version INTEGER NOT NULL DEFAULT 1,
    sync_status TEXT NOT NULL DEFAULT 'LOCAL_ONLY',
    last_synced_at TEXT NULL,
    last_modified_by_device TEXT NULL
);

parent_child_link(
    link_id INTEGER PRIMARY KEY,
    stable_uuid TEXT NOT NULL UNIQUE,
    parent_person_id INTEGER NOT NULL,
    child_person_id INTEGER NOT NULL,
    parent_role TEXT NULL,
    child_order INTEGER NULL,
    notes TEXT NULL,
    is_deleted INTEGER NOT NULL DEFAULT 0,
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL,
    deleted_at TEXT NULL,
    version INTEGER NOT NULL DEFAULT 1,
    sync_status TEXT NOT NULL DEFAULT 'LOCAL_ONLY',
    last_synced_at TEXT NULL,
    last_modified_by_device TEXT NULL,
    UNIQUE(parent_person_id, child_person_id)
);

spouse_link(
    spouse_link_id INTEGER PRIMARY KEY,
    stable_uuid TEXT NOT NULL UNIQUE,
    person_a_id INTEGER NOT NULL,
    person_b_id INTEGER NOT NULL,
    marriage_date_text TEXT NULL,
    marriage_notes TEXT NULL,
    sealing_to_spouse_status TEXT NOT NULL DEFAULT 'UNKNOWN',
    sealing_status_date TEXT NULL,
    sealing_notes TEXT NULL,
    is_deleted INTEGER NOT NULL DEFAULT 0,
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL,
    deleted_at TEXT NULL,
    version INTEGER NOT NULL DEFAULT 1,
    sync_status TEXT NOT NULL DEFAULT 'LOCAL_ONLY',
    last_synced_at TEXT NULL,
    last_modified_by_device TEXT NULL
);

person_ordinance_status(
    person_id INTEGER PRIMARY KEY,
    baptism_status TEXT NOT NULL DEFAULT 'UNKNOWN',
    confirmation_status TEXT NOT NULL DEFAULT 'UNKNOWN',
    initiatory_status TEXT NOT NULL DEFAULT 'UNKNOWN',
    endowment_status TEXT NOT NULL DEFAULT 'UNKNOWN',
    sealed_to_parents_status TEXT NOT NULL DEFAULT 'UNKNOWN',
    ordinance_notes TEXT NULL,
    updated_at TEXT NOT NULL,
    version INTEGER NOT NULL DEFAULT 1,
    sync_status TEXT NOT NULL DEFAULT 'LOCAL_ONLY',
    last_synced_at TEXT NULL,
    last_modified_by_device TEXT NULL
);
```

## 11. User Interface Design
### Overall Layout
- Left navigation: pedigree tree rooted in the user
- Center content: selected ancestor/person detail
- Right or lower panel: summary, notes, quick actions
- Toolbar: add ancestor, add child, add spouse, search, filters, reports, backup/export

### Primary Screens
- Home Dashboard
- Pedigree View
- Ancestor Line View
- Person Editor
- Spouse/Relationship Editor
- Reports
- Settings

### Person Editor Sections
- Core identity
- Dates and precision
- Ordinances
- Spouses with one sealing status per spouse link
- Children list
- Notes and reviewed status

### Future Mobile UX Notes
The Android companion app should not attempt to mirror the desktop layout exactly. Instead, it should reuse the same domain concepts with mobile-friendly screens such as:
- ancestor list
- person detail tabs or sections
- focused descendant-line drill-down
- offline edit queue and sync status visibility

## 12. Software Architecture
Use a layered architecture with clean separation between presentation, domain, data access, and sync readiness.

### Logical Layers
- UI / presentation layer
- Application/service layer
- Domain layer
- Persistence/repository layer
- Infrastructure layer
- Sync contract layer

### Recommended Multi-Module Structure
```text
family-history-tracker/
  ├─ desktop-app/          # JavaFX entry point and screens
  ├─ core-domain/          # entities, enums, value objects, business rules
  ├─ application-services/ # use cases, orchestration, summary services
  ├─ persistence-sqlite/   # SQLite repositories, migrations, DAO layer
  ├─ sync-contracts/       # DTOs, sync metadata, conflict models
  ├─ shared-test-fixtures/ # representative data sets and test helpers
  └─ docs/                 # schema notes and architecture docs
```

### Suggested Package Structure (single-repo view)
```text
com.example.familytracker
  ├─ app
  ├─ domain
  ├─ application
  ├─ persistence
  ├─ sync
  ├─ ui
  └─ util
```

### Key Domain Types
- Person
- ParentChildLink
- SpouseLink
- PersonOrdinanceStatus
- AncestorLineSummary
- LineStewardship
- PartialDate
- HorizonBucket
- AncestorBadgeStatus
- SyncState
- DeviceId
- ConflictResolutionProposal

### Core Design Rules
- JavaFX controllers/view-models may depend on application services, never directly on JDBC code
- Application services may depend on repository interfaces, not UI classes
- Repository implementations may depend on SQLite/JDBC details
- Domain logic must be testable without JavaFX or Android
- Future Android client should be able to implement its own presentation layer over equivalent repository/service contracts

## 13. Key Algorithms
### 110-Year Classification
1. If birth date is unknown, classify as UNKNOWN unless overridden
2. Compute milestone = birth date + 110 years
3. Compare milestone with current date
4. Map time remaining to OPEN / SOON / BLOCKED buckets

### Ancestor Aggregation
1. Traverse descendants for selected ancestor
2. Inspect person ordinance statuses
3. Inspect spouse sealing statuses
4. Classify each tracked item
5. Aggregate counts and next opening date
6. Apply badge priority

### Future Conflict-Merge Outline
1. Compare local row version and remote row version
2. If only one side changed, accept changed side
3. If both changed in non-overlapping fields, merge when safe
4. If both changed in overlapping fields, mark CONFLICT
5. Preserve audit trail and surface conflict to user review tools

## 14. Validation Rules
- Preferred name required
- FamilySearch PID unique if provided
- No self-parent or self-spouse
- Prevent direct cycles
- Birth date should not follow death date
- Prevent duplicate spouse links
- Use soft delete and confirmations
- Stable UUID required on all sync-capable rows
- Version must increment on each persisted update

## 15. Reporting Requirements
- Open Now Report
- Opening Soon Report
- Waiting on 110 Report
- Unresolved Data Report
- Line Stewardship Report
- Future Sync Health Report
  - unsynced records
  - conflicted records
  - device last sync timestamps

## 16. Persistence, Files, Backup, and Sync Strategy
### Version 1 Storage Model
- Local SQLite database file
- Versioned schema migrations
- In-app timestamped backup
- CSV exports
- No binary blobs in Version 1

### Explicit Storage Constraint
Do **not** design the system around two devices opening the same live SQLite file from a shared folder or network location. Instead, design around one local database per client and eventual synchronization between clients.

### Future Sync Topology
Recommended future topology:
- desktop client with local database
- Android client with local database
- optional lightweight sync service/API
- per-client sync queue and conflict handling

### Future Sync Modes
Potential phases:
1. manual export/import
2. optional file-based interchange package
3. API-based synchronization
4. richer conflict resolution UI

## 17. Testing Strategy
- Unit tests for date classification and badge logic
- Unit tests for descendant traversal and aggregation
- Repository tests with temporary SQLite
- Migration tests across schema versions
- Sync metadata tests
- Conflict-detection tests
- UI smoke tests
- Golden data sets for representative line conditions

## 18. Phased Implementation Plan
### Phase 0 - Foundation for Long-Term Architecture
- Create multi-module project skeleton
- Define domain enums and value objects
- Define repository interfaces
- Add sync metadata fields to schema from the start

### Phase 1 - Core Person and Relationship Modeling
- Person CRUD
- Parent-child links
- Spouse links
- basic navigation

### Phase 2 - Ordinance Tracking
- person ordinance statuses
- spouse-specific sealing statuses
- validation and notes

### Phase 3 - Pedigree and Ancestor-Line UI
- rooted pedigree navigation
- person editor
- ancestor-line page

### Phase 4 - Summary Logic
- 110-year calculations
- horizon buckets
- ancestor badge generation
- next-availability summaries

### Phase 5 - Reports and Filters
- open-now reports
- opening-soon reports
- stewardship reports

### Phase 6 - Backup and Import/Export
- CSV export/import
- timestamped backups
- integrity checks

### Phase 7 - Refinement and Hardening
- keyboard optimization
- usability cleanup
- test coverage improvement
- migration hardening

### Phase 8 - Sync Preparation Layer
- portable DTOs
- sync queue schema
- conflict markers
- import/export reconciliation support

### Phase 9 - Version 2 Android Companion
- Android client with local persistence
- sync API or interchange layer
- conflict handling UX
- selective offline-first workflows

## 19. Acceptance Criteria
1. Create root person for user
2. Add ancestors and navigate pedigree
3. Show badge per ancestor
4. Open descendant line outline
5. Record ordinance statuses
6. Record separate sealing statuses per spouse
7. Classify descendants by 110-year horizon
8. Generate open-now and opening-soon reports
9. Persist data across restarts
10. Back up and export data
11. Use repository/service boundaries rather than direct UI-to-database coupling
12. Include stable IDs and sync metadata in the schema
13. Preserve ability to add an Android companion without redesigning domain logic

## 20. Future Cross-Platform and Sync Requirements
1. The system shall separate domain logic from UI logic
2. The system shall use repository interfaces rather than direct UI database access
3. The system shall assign stable IDs to all entities and relationships
4. The system shall track per-record version and sync metadata
5. The system shall support eventual addition of a remote sync service
6. The system shall not depend on sharing a live SQLite database file between devices
7. The system shall support conflict marking and later conflict resolution flows
8. The system shall support Android as a future client using the same business rules and data contracts
9. The system shall support one local database per device as the normal operating model
10. The system shall allow manual-only desktop use even if sync is never implemented

## 21. AI Build Handoff Notes
- Build Version 1 as a local Java 21 + JavaFX + SQLite desktop app
- Keep architecture clean and modular
- Use enums for ordinance, badge, horizon, stewardship, and sync statuses
- Treat spouse-specific sealing as relationship data
- Make pedigree view the main navigation
- Make ancestor-line summaries a first-class domain concept
- Add sync metadata to the schema now, even though live sync is deferred
- Use repository interfaces and application services so a future Android client can reuse the same domain concepts
- Do not add web sync or FamilySearch integration in Version 1
- Do not design around a shared live SQLite file

## 22. Suggested Initial Prompt for a Coding AI
Build a Java 21 + JavaFX desktop application using SQLite for a local family-history tracker. The app must be rooted in the user, show a pedigree view backward to ancestors, and attach to each ancestor a summary badge computed from that ancestor's descendant line. The app must support manual entry of persons, parent-child links, spouse links, FamilySearch PIDs, partial dates, person-level ordinance statuses, and spouse-specific sealing statuses. Use a layered, sync-ready architecture with clear packages, schema migrations, stable UUIDs, record versioning, and repository interfaces. Add sync metadata to the schema from the beginning, but do not implement live sync in Version 1. Start by scaffolding the project, database, domain enums, core entities, repositories, and a minimal person editor, then proceed in phases.
