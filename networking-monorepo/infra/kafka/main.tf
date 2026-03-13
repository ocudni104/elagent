terraform {
  required_providers {
    kafka = {
      source  = "Mongey/kafka"
      version = "~> 0.11"
    }
  }
}

provider "kafka" {
  bootstrap_servers = ["kafka:9092"]
}