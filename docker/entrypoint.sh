#!/usr/bin/env sh
set -e

echo "🚀 Setting up deployment environment"

wait_for_db() {
  echo "⏳ Waiting for database at $DB_HOST:$DB_PORT..."
  while ! nc -z $DB_HOST $DB_PORT; do
    sleep 2
  done
  echo "✅ Database is ready!"
}

