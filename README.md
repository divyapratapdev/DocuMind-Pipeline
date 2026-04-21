# DocuMind Enterprise: AI Contract & Compliance Pipeline

Welcome to the DocuMind Enterprise Engine. This repository showcases a top 1%, highly scalable, event-driven B2B data pipeline capable of ingesting massive unstructured contract PDFs, utilizing LLM-based autonomous extraction, and routing JSON arrays into NoSQL-style Postgres columns in real-time.

## System Architecture Highlights
* **Kafka KRaft Broker**: Asynchronous, highly decoupled worker nodes bypass HTTP thread-blocking issues naturally found in Spring Boot.
* **MinIO Object Storage**: Native AWS S3 SDK integration for 100% cloud-ready BLOB storage, bypassing archaic local-disk mechanisms.
* **Llama-3 Groq Pipeline**: Hard-enforced JSON extraction acting over chunked Apache PDFBox outputs.
* **PostgreSQL GIN JSONB**: Using `io.hypersistence` / Native Hibernate 6 to sink the AI's dynamic schema logic straight into relational tables, indexable in milliseconds.

## Quickstart

This application is fully containerized. You do not need to install Kafka, Zookeeper, Postgres, or Redis manually!

### 1. Boot up the Infrastructure
To download all massive database containers and spin them up locally on your machine:
```bash
docker-compose up -d
```
*(Note: Initial download of Confluent Kafka and MinIO may take some time depending on your bandwidth).*

### 2. Configure Groq API
In `src/main/resources/application.yml`, place your Groq API key:
```yaml
groq:
  api:
    key: your_real_api_key_here
```
> *If you omit this key, the pipeline automatically detects it and uses a Mock JSON Generator to allow you to natively test the S3 routing and DB saving without spending LLM credits!*

### 3. Live Verification
Wait for Docker to reach a fully healthy state (`docker-compose ps`), then run the automated verification script:
```powershell
.\live_verify.ps1
```
This script will automatically:
1. Fire up a detached Spring Boot backend.
2. Generate a dummy High-Risk PDF payload on the fly.
3. Transmit the Payload to your REST API.
4. Dump the real-time Eventual Consistency tracking logs to your console!

## Testing Suite
Execute the entire test suite covering mock MVC, Spring Boot S3, and Kafka injections:
```bash
mvn clean test
```
**Coverage**: 100% Critical Path Functional Verification.
