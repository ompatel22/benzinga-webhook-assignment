# Benzinga - Backend Java Assignment - Webhook Receiver

This project is a webhook receiver built with Java and Spring Boot. It is designed to accept JSON log payloads using POST HTTP requests, batch them in memory, and forward them asynchronously to an external endpoint based on configurable batch size and batch interval parameters while handling retries.

## Core Features

* **Asynchronous Batching:** Batches log payloads asynchronously in-memory based on size (`APP_BATCH_SIZE`) or a time interval (`APP_BATCH_INTERVAL_MS`).
* **Resilient Retries:** Automatically retries failed POST requests to the external endpoint 3 times with a 2-second delay.
* **Backpressure Handling:** Uses a bounded in-memory queue to prevent `OutOfMemoryError`. If the queue is full, it returns `503 Service Unavailable`, signaling the client to slow down.
* **Graceful Shutdown:** On application exit, it ensures all pending logs in the queue are flushed and sent to the external endpoint, preventing data loss.
* **Thread-Safe:** Manages batch triggers (by size or time) in a thread-safe manner to prevent race conditions.
* **Dockerized:** Includes a multi-stage `Dockerfile` for a lightweight, production-ready container.
* **Kubernetes-Ready:** Includes `deployment.yaml` and `service.yaml` for easy deployment.

## Flow Overview

```
Client → /log endpoint → BlockingQueue → Batch Scheduler/Trigger(by size) → POST batch → External Endpoint
```

## API Endpoints

### Health Check

This endpoint checks the health of the application.

* **Endpoint:** `GET /healthz`
* **Success Response (200 OK):**
  ```
  OK
  ```

### Log Receiver (Webhook Endpoint)

Receives and accepts a single log payload for batch processing.

* **Endpoint:** `POST /log`
* **Content-Type:** `application/json`
* **Example Payload:**
  ```json
  {
    "user_id": 1,
    "total": 1.65,
    "title": "delectus aut autem",
    "meta": {
      "logins": [
        {
          "time": "2020-08-08T01:52:50Z",
          "ip": "0.0.0.0"
        }
      ],
      "phone_numbers": {
        "home": "555-1212",
        "mobile": "123-5555"
      }
    },
    "completed": false
  }
  ```
* **Success Response (200 OK):**
  ```
  Log Payload Accepted
  ```
* **Error Response (503 Service Unavailable):**

  Returned if the internal processing queue is full, indicating the client should retry later.
  ```
  Queue is full, please retry later
  ```

The payloads are stored until either the batch size limit is reached or the batch interval time has elapsed. Once one of these conditions is met, the collected payloads are forwarded to the configured POST endpoint. The stored data is then cleared after sending the batch. If forwarding payloads to the configured endpoint fails, the application retries up to 3 times with an interval of 2 seconds. If all 3 attempts fail, the error is logged and application is exited.

## Configuration

The application is configured using environment variables. Default values are provided in `application.properties`.

| Environment Variable | Default Value | Description |
|:---|:---|:---|
| `APP_BATCH_SIZE` | `5` | The number of logs to collect before sending a batch. |
| `APP_BATCH_INTERVAL_MS` | `10000` | The max time (in ms) to wait before sending a batch, even if not full. |
| `APP_POST_ENDPOINT` | `https://1f69832a9cfbacc44e7bg1n4rdayyyyyb.oast.me` | The external URL to POST the log batches to. |

## How to Run

### 1. Locally by Cloning (without Docker and Kubernetes)

#### Prerequisites
- Java 
- Maven

#### Steps

1. **Clone the Repository**
   ```bash
   git clone https://github.com/ompatel22/benzinga-webhook-assignment.git
   cd benzinga-webhook-assignment
   ```

2. **Build the Application**
   ```bash
   mvn clean package
   ```

3. **Run the Application**

   **Using Maven:**
   ```bash
   mvn spring-boot:run
   ```

   **With Custom Configuration:**
   ```bash
   APP_BATCH_SIZE=<insert-batch-size> \
   APP_BATCH_INTERVAL_MS=<insert-batch-interval> \
   APP_POST_ENDPOINT=<insert-post-endpoint> \
   mvn spring-boot:run
   ```

   **OR**

   **Using Java:**
   ```bash
   java -jar target/webhook-example-0.0.1-SNAPSHOT.jar
   ```

   **With Custom Configuration:**
   ```bash
   APP_BATCH_SIZE=<insert-batch-size> \
   APP_BATCH_INTERVAL_MS=<insert-batch-interval> \
   APP_POST_ENDPOINT=<insert-post-endpoint> \
   java -jar target/webhook-example-0.0.1-SNAPSHOT.jar
   ```

4. **Test the Application**

   Use the following URLs for requesting using Postman or any other HTTP client:
    - Health Check: `GET http://localhost:8080/healthz`
    - Log Ingestion: `POST http://localhost:8080/log` (use example payload above)

### 2. Using Docker

#### Prerequisites
- Docker installed and running

#### Steps

1. **Build the Docker Image**
   ```bash
   cd benzinga-webhook-assignment
   docker build -t webhook-example .
   ```

2. **Run the Docker Container** 

   **This is not in detached mode so logs will be directly visible (environment variables are optional)**
   ```bash
    docker run --name webhook-example \
    -p 8080:8080 \
    -e APP_BATCH_SIZE=<insert-batch-size> \
    -e APP_BATCH_INTERVAL_MS=<insert-batch-interval> \
    -e APP_POST_ENDPOINT=<insert-post-endpoint> \
    webhook-example
   ```

   **OR run in detached mode and then see the logs (environment variables are optional)**
   ```bash
    docker run -d --name webhook-example \
    -p 8080:8080 \
    -e APP_BATCH_SIZE=<insert-batch-size> \
    -e APP_BATCH_INTERVAL_MS=<insert-batch-interval> \
    -e APP_POST_ENDPOINT=<insert-post-endpoint> \
    webhook-example
   ```
   ```bash
   docker logs webhook-example -f
   ```

3. **Test the Application**

   Use the following URLs for requesting using Postman or any other HTTP client:
    - Health Check: `GET http://localhost:8080/healthz`
    - Log Ingestion: `POST http://localhost:8080/log` (use example payload above)

### 3. Using Kubernetes

#### Prerequisites
- Kubernetes cluster (local or cloud)
- `kubectl` installed and configured
- For local setup: Minikube

#### Steps

1. **Start Minikube** (if using local setup)
   ```bash
   minikube start
   ```

2. **Apply Kubernetes related files**
   ```bash
   kubectl apply -f deployment.yaml
   kubectl apply -f service.yaml
   ```

3. **Verify Deployment & Service creation**

   Check created pod, service, deployment, and replicaset:
   ```bash
   kubectl get all
   ```

4. **Update Environment Variables** (optional)
   ```bash
   kubectl set env deployment/webhook-example APP_BATCH_SIZE=<insert-batch-size>
   kubectl set env deployment/webhook-example APP_BATCH_INTERVAL_MS=<insert-batch-interval>
   kubectl set env deployment/webhook-example APP_POST_ENDPOINT=<insert-post-endpoint>
   ```

5. **Get Service URL** (for Minikube)
   ```bash
   minikube service webhook-example --url
   ```

6. **View Application Logs** (open in another terminal)
   ```bash
   kubectl get pods
   kubectl logs <pod-name> -f
   ```

7. **Test the Application**

   Use the provided IP and port from step 5 for requesting using Postman or any other HTTP client:
    - Health Check: `GET <base-url>/healthz`
    - Log Ingestion: `POST <base-url>/log` (use example payload above)

**Note: If you are making any changes in the code and creating a new image and pushing it to your Docker Hub or any other registry, you can change the image property in the deployment.yaml file:**

   ```bash
   image: your-dockerhub-username/your-image-name:tag
   ```

## How to Test

Run the unit tests for the application:

> **Note:** Currently unit test cases are implemented only for WebhookController.

```bash
mvn clean test
```
