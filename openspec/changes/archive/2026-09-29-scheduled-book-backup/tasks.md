## 1. Config & export path

- [x] 1.1 Add `financial-cloud.backup.schedule.*` properties (+ binding class); default enabled=false
- [x] 1.2 Add `BookBackupService.exportForSystem(bookId)` (same ZIP, system audit); unit-test format magic / size > 0 on fixture if available
- [x] 1.3 Implement `ScheduledBookBackupJob` (list enabled books, write ZIP, retain-N, last-run state); verify single-flight

## 2. Admin API & UI

- [x] 2.1 `GET /api/book/backup/schedule/status` and `POST .../schedule/run` for instance admin
- [x] 2.2 Books page: show schedule summary + run-now when permitted
- [x] 2.3 Update `01-account-book.md` / gap / roadmap / backlog; acceptance notes; archive
