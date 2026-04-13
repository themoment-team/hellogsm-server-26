---
name: migration-guide
description: Guides through DB schema change workflows for hellogsm-server-25 in the correct order (Entity → DTO → Repository → Service → Test) with JPA DDL warnings and 2-phase column deletion strategy.
---

You are executing the **migration-guide** skill for hellogsm-server-25.

## When to Use
- Adding/removing/renaming a column in a JPA entity
- Changing a column type or constraint
- Adding or removing a table

## Step 1 — Detect Change Scope

```bash
git diff HEAD -- "src/main/java/**/entity/**/*.java"
```

Identify: entity, fields added/removed/renamed/type-changed.

## Step 2 — JPA DDL Warning

```bash
grep -r "ddl-auto" src/main/resources/
```

| DDL Strategy | Production-safe? |
|--------------|-----------------|
| `validate` | ✅ Apply SQL manually |
| `none` | ✅ Apply SQL manually |
| `update` | ⚠️ Never drops columns — can cause drift |
| `create-drop` | ❌ Destroys data |

## Step 3 — Execution Order

1. **Entity** — add/modify/remove field
2. **DTO (Request)** — update ReqDto records
3. **DTO (Response)** — update ResDto records
4. **Repository** — update QueryDSL queries in `custom/impl/`
5. **Service** — update builder calls and logic
6. **Tests** — update fixtures, stubs, assertions
7. **SQL** — write `ALTER TABLE` migration

## Step 4 — Two-Phase Column Deletion

**Never delete a production column in one step.**

### Phase 1 — Decouple
```java
@Transient  // Mark as transient — JPA ignores it
private String oldField;  // Remove in Phase 2
```
Deploy and verify. Old column still in DB.

### Phase 2 — Remove
```java
// Remove the field entirely
```
```sql
ALTER TABLE tb_entity DROP COLUMN old_field;
```

## Step 5 — Checklist Output

```markdown
## Migration Checklist

**Entity:** {EntityName}  
**Change:** {description}

- [ ] Entity — {specific change}
- [ ] DTO (Request) — {specific change}
- [ ] DTO (Response) — {specific change}
- [ ] Repository — {specific change}
- [ ] Service — {specific change}
- [ ] Tests — {specific change}
- [ ] SQL — `{ALTER TABLE statement}`

### ⚠️ Warnings
- {ddl-auto warning if applicable}
```