-- Create "devices" table
CREATE TABLE "devices" (
  "id" uuid NOT NULL,
  "os" text NULL,
  "screen" text NULL,
  "created_at" timestamptz NOT NULL,
  "updated_at" timestamptz NOT NULL,
  PRIMARY KEY ("id")
);
-- Create index "devices_updated_at_idx" to table: "devices"
CREATE INDEX "devices_updated_at_idx" ON "devices" ("updated_at");
