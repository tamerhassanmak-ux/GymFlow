# Updated performance observations

Final patched candidate, local Ubuntu/OpenJDK 21, SQLite JDBC synthetic workload. Measurements from evidence/release-fix/legacy-final.log; single sample, warm JVM/filesystem, not a capacity or percentile guarantee.

| Executed workload | Observation |
|---|---|
| Four concurrent workers, 100 payment insert/delete pairs total | 439.022 ms, all operations completed |
| Batch insert 5000 synthetic members | 49.347 ms |
| Materialize all 5002 members | 103.415 ms |
| 100 managed connection open/close cycles | 56.231 ms |
| Whole 35-case legacy workflow Java run | 4.945 s wall time |
| Final clean Maven build including 44 JUnit tests | 28.336 s (dependency-fixed build) |

No final-candidate RSS, CPU profile, p95/p99 response times or long-duration resource-growth measurements were collected. Previous process measurements belong to the earlier candidate and are not claimed here. Passing connection cycles are not proof of leak freedom.

Failure-injection/consistency tests verify active managed-connection recovery refusal, financial rollback and concurrent inserts; the 5-second restore lock refusal is expected behavior. UI latency, cross-process writers, very large exports, server databases, disk/network failure and sustained stress remain unexecuted. No performance SLA was supplied or inferred.
