-- Create "sessions" table
CREATE TABLE "sessions" (
  "id" uuid NOT NULL,
  "user_id" uuid NOT NULL,
  "tenant_id" uuid NULL,
  "device_id" uuid NULL,
  "created_at" timestamptz NOT NULL,
  "absolute_expires_at" timestamptz NOT NULL,
  "revoked_at" timestamptz NULL,
  PRIMARY KEY ("id")
);
-- Create index "sessions_absolute_expires_at_idx" to table: "sessions"
CREATE INDEX "sessions_absolute_expires_at_idx" ON "sessions" ("absolute_expires_at");
-- Create index "sessions_revoked_at_idx" to table: "sessions"
CREATE INDEX "sessions_revoked_at_idx" ON "sessions" ("revoked_at");
-- Create index "sessions_user_id_idx" to table: "sessions"
CREATE INDEX "sessions_user_id_idx" ON "sessions" ("user_id");
