## How Atlas is working here

Atlas is the repo’s **database schema control plane**.

We are using the **open-source / OSS Atlas flow** with:

* one repo-level `persistence/atlas` area
* one root `schema.sql` file that composes the domain schema fragments
* one `atlas.hcl` at the Atlas root
* one migration history generated from that composed desired state
* Postgres as the target database

### Layout

```text
persistence/
  atlas/
    atlas.hcl
    schema.sql
    schemas/
      identity.sql
      app.sql
      insights.sql
      ...
    migrations/
      ...
```

### How it works

* `schema.sql` is the **composition root**
* it imports the domain schema files from `schemas/`
* Atlas reads that composed schema as the desired state
* Atlas compares desired state vs actual database state
* Atlas generates SQL migrations into `migrations/`
* Atlas applies those migrations to the database

### Example barrel file

`schema.sql`

```sql
-- atlas:import "./schemas/identity.sql"
-- atlas:import "./schemas/app.sql"
-- atlas:import "./schemas/insights.sql"
```

### Typical workflow

1. Edit a domain schema file, for example:

    * `schemas/app.sql`
    * `schemas/identity.sql`

2. The composed schema is picked up through `schema.sql`

3. Generate a migration:

```bash
atlas migrate diff <name> --env local
```

4. Apply it:

```bash
atlas migrate apply --env local
```

5. Inspect current state if needed:

```bash
atlas schema inspect --env local
atlas migrate status --env local
```

### Why this structure

This keeps:

* schema ownership separated by domain
* migration generation centralized
* future service/database splitting easier

If a domain is split later, we can extract that domain schema and start a dedicated migration track there, then handle data migration separately.

## Atlas OSS docs

Open-source Atlas getting started:
[https://atlasgo.io/getting-started](https://atlasgo.io/getting-started)

Atlas documentation:
[https://atlasgo.io/docs](https://atlasgo.io/docs)
