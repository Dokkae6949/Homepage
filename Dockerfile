# --- Stage 1: Build the Rust binary ---
FROM rust:1.75-alpine AS builder

# Install build dependencies
RUN apk add --no-cache musl-dev openssl-dev openssl-libs-static pkgconfig

WORKDIR /app

# Copy manifests
COPY Cargo.toml ./

# Create dummy main to build dependencies
RUN mkdir src && echo "fn main() {}" > src/main.rs
RUN cargo build --release
RUN rm -rf src

# Copy source code
COPY src ./src
COPY migrations ./migrations
COPY templates ./templates
COPY locales ./locales
COPY static ./static

# Build the actual application
RUN touch src/main.rs && cargo build --release

# --- Stage 2: Run the app ---
FROM alpine:3.19

# Install runtime dependencies
RUN apk add --no-cache libgcc openssl

WORKDIR /app

# Copy the built binary
COPY --from=builder /app/target/release/chat_app /app/chat_app

# Copy static assets
COPY --from=builder /app/static /app/static
COPY --from=builder /app/templates /app/templates
COPY --from=builder /app/locales /app/locales
COPY --from=builder /app/migrations /app/migrations

# Expose port
EXPOSE 3000

# Run the app
ENTRYPOINT ["/app/chat_app"]
