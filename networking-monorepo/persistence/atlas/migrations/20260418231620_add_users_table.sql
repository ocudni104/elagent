-- Create "users" table
CREATE TABLE "users" (
  "id" uuid NOT NULL,
  "email" text NOT NULL,
  "provider" text NOT NULL,
  "provider_subject" text NOT NULL,
  "created_at" timestamptz NOT NULL,
  "last_login_at" timestamptz NOT NULL,
  PRIMARY KEY ("id"),
  CONSTRAINT "users_email_key" UNIQUE ("email"),
  CONSTRAINT "users_provider_subject_key" UNIQUE ("provider", "provider_subject")
);
-- Create index "users_provider_subject_idx" to table: "users"
CREATE INDEX "users_provider_subject_idx" ON "users" ("provider", "provider_subject");
