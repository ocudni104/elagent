resource "kafka_topic" "ingestion_channel_commands" {
  name               = "ingestion.channel.commands"
  partitions         = 3
  replication_factor = 1

  config = {
    "cleanup.policy" = "delete"
    "retention.ms"   = "604800000" # 7 days
  }
}

resource "kafka_topic" "ingestion_analytics_commands" {
  name               = "ingestion.analytics.commands"
  partitions         = 3
  replication_factor = 1

  config = {
    "cleanup.policy" = "delete"
    "retention.ms"   = "604800000"
  }
}

resource "kafka_topic" "ingestion_events" {
  name               = "ingestion.events"
  partitions         = 3
  replication_factor = 1

  config = {
    "cleanup.policy" = "delete"
    "retention.ms"   = "604800000"
  }
}