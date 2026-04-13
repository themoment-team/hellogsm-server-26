---
name: migration-guide
description: Guides through DB schema change workflows in the correct order (Entity → DTO → Repository → Service → Test) with JPA DDL warnings and 2-phase column deletion strategy.
---

You are executing the **migration-guide** skill for hellogsm-server-25.

## When to Use

Use this skill when:
- Adding or removing a column from an entity
- Renaming an entity field
- Changing a column type or constraint
- Adding a new entity/table
- Removing a table

## Step 1 — Detect Schema Change Scope

```bash
git diff HEAD -- "src/main/java/**/entity/**/*.java"
git diff HEAD -- "src/main/java/**/entity/*.java"
```

Identify:
- Which entity class changed
- What fields were added / removed / renamed / type-changed
- Whether `@Table(name = "tb_...")` changed

## Step 2 — JPA DDL Safety Warning

> ⚠️ **This project uses JPA.** Check `application.yml` for `spring.jpa.hibernate.ddl-auto`.

```bash
grep -r "ddl-auto" src/main/resources/
```

| DDL Strategy | Production-safe? | Action |
|--------------|-----------------|--------|
| `validate` | ✅ Safe | Schema must match entity manually |
| `none` | ✅ Safe | Apply migration SQL manually |
| `update` | ⚠️ Risky | Auto-adds columns but never drops — can cause drift |
| `create-drop` | ❌ Dangerous | Destroys data on restart |

**Recommended:** Use `validate` or `none` in production. Write SQL migration scripts manually.

## Step 3 — Change Execution Order

Follow this order strictly. Skipping steps causes build or runtime failures.

### Phase A — Entity Layer
1. Add/modify/remove the field in the entity class
2. Update `@Column` annotation if needed
3. If removing a field: add `@Transient` first (Phase 1), do NOT delete yet

### Phase B — DTO Layer
4. Update or add `ReqDto` records that include the changed field
5. Update or add `ResDto` records returned from services
6. Update internal `Dto` classes used between service layers

### Phase C — Repository Layer
7. Update custom QueryDSL queries in `custom/impl/`
8. Add/update `@Query` methods in repository interfaces
9. Remove queries that referenced deleted fields

### Phase D — Service Layer
10. Update service classes that use the changed field
11. Update builder calls (`.field(value)`) in services
12. Update any conditional logic that checks the field

### Phase E — Test Layer
13. Update `@BeforeEach` data setup in affected tests
14. Update mock stubs for repository methods
15. Update expected values in assertions

### Phase F — SQL Migration Script
16. Write the SQL migration:
```sql
-- Adding a column
ALTER TABLE tb_oneseo ADD COLUMN new_field VARCHAR(255) NOT NULL DEFAULT '';

-- Removing a column (only after Phase 1 is deployed + verified)
ALTER TABLE tb_oneseo DROP COLUMN old_field;

-- Renaming a column
ALTER TABLE tb_oneseo RENAME COLUMN old_name TO new_name;
```

## Step 4 — Two-Phase Column Deletion Strategy

**NEVER delete a column in production in one step.** Use this 2-phase approach:

### Phase 1 — Decouple (Deploy this first)
```java
// Step 1: Mark field as @Transient so JPA ignores it
// Keep the field for backward compatibility during transition
@Transient  
private String oldField;  // Will be removed in Phase 2
```
Deploy and verify application runs without errors. Old column still exists in DB.

### Phase 2 — Remove (Deploy after Phase 1 is stable)
```java
// Step 2: Remove the @Transient field entirely
// Then drop the column from DB
```
```sql
-- Run AFTER Phase 2 is deployed
ALTER TABLE tb_entity DROP COLUMN old_field;
```

## Step 5 — Checklist Output

```markdown
## Migration Guide Checklist

**Entity:** Oneseo  
**Change:** Add `desiredMajor3` field (nullable VARCHAR)

### Execution Order
- [ ] **Entity** — Add `desiredMajor3` field with `@Column(nullable = true)`
- [ ] **DTO (Request)** — Add `desiredMajor3` to `OneseoReqDto` record
- [ ] **DTO (Response)** — Add `desiredMajor3` to `FoundOneseoResDto`
- [ ] **Repository** — Update QueryDSL projections in `CustomOneseoRepositoryImpl`
- [ ] **Service** — Update builder in `CreateOneseoService` and `UpdateOneseoService`
- [ ] **Tests** — Update test fixtures with `desiredMajor3` field
- [ ] **SQL** — `ALTER TABLE tb_oneseo ADD COLUMN desired_major3 VARCHAR(255) NULL;`

### ⚠️ Warnings
- ddl-auto is `validate` — SQL migration MUST be applied before deployment
- No Flyway detected — apply migration manually to each environment

### Post-Migration Verification
- [ ] `./gradlew test` passes
- [ ] Application starts without `SchemaValidationException`
- [ ] New field appears in Swagger and API response
```
