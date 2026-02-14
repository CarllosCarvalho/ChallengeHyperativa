# Hyperativa Card API

REST API for registration and lookup of credit card numbers with secure storage.

## Technologies

- **Java 21** + **Spring Boot 3.2.5**
- **Spring Security** + **JWT** (stateless authentication)
- **Spring Data JPA** (persistence)
- **H2** (development) / **PostgreSQL** (production)
- **AES-256-GCM** (card data encryption)
- **SHA-256** (hash for indexed lookups)
- **SpringDoc OpenAPI** (Swagger documentation)
- **Docker** + **Docker Compose**

## Architecture and Design Decisions

### Data Security
- Card numbers are **encrypted with AES-256-GCM** before storage (with a random IV per record)
- A **SHA-256 hash** is stored in an indexed column for efficient lookups without the need to decrypt
- Each card has a **public UUID** (`externalId`) returned in queries, avoiding exposure of internal IDs

### Scalability
- Indexed hash lookup (O(1) in the database) instead of decrypting all records
- Batch upload via TXT file with transactional processing
- Stateless (JWT) — allows horizontal scaling without shared sessions

### Logging
- All requests are logged in the database (`request_logs`) with method, URI, user, status, and duration
- Application logs via SLF4J/Logback

## Setup

### Prerequisites
- Java 21+
- Maven 3.9+

### Run locally (in-memory H2)

```bash
mvn clean install
mvn spring-boot:run
```

The API will be available at `http://localhost:8080`

### Run with Docker + MySQL

```bash
docker-compose up --build
```

### Run tests

```bash
mvn test
```

## Endpoints

### Swagger UI
Access: `http://localhost:8080/swagger-ui.html`

### Authentication

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/auth/register` | Register new user |
| POST | `/api/v1/auth/login` | Authenticate and obtain JWT token |

**Default user:** `admin` / `admin123`

### Cards (requires JWT token)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/cards` | Register a card |
| POST | `/api/v1/cards/batch` | Batch TXT file upload |
| GET | `/api/v1/cards/search?cardNumber=` | Search card by number |

## Usage Examples (cURL)

### 1. Authenticate
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "admin", "password": "admin123"}'
```

Response:
```json
{
  "token": "eyJhbGciOiJIUzI1...",
  "type": "Bearer",
  "expiresIn": 3600
}
```

### 2. Register card
```bash
curl -X POST http://localhost:8080/api/v1/cards \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <TOKEN>" \
  -d '{"cardNumber": "4456897999999999"}'
```

Response:
```json
{
  "externalId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "message": "Card registered successfully"
}
```

### 3. Batch upload (TXT file)
```bash
curl -X POST http://localhost:8080/api/v1/cards/batch \
  -H "Authorization: Bearer <TOKEN>" \
  -F "file=@DESAFIO-HYPERATIVA.txt"
```

Response:
```json
{
  "batchId": "LOTE0001",
  "totalProcessed": 10,
  "totalSuccess": 8,
  "totalErrors": 2,
  "errors": ["Line 6: invalid card number '4456897999999999124'", ...]
}
```

### 4. Search card
```bash
curl -X GET "http://localhost:8080/api/v1/cards/search?cardNumber=4456897999999999" \
  -H "Authorization: Bearer <TOKEN>"
```

Response (found — HTTP 200):
```json
{
  "externalId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "message": "Card found"
}
```

Response (not found — HTTP 404): empty body

## TXT File Format

```
DESAFIO-HYPERATIVA           20180524LOTE0001000010   // Header
C1     4456897922969999                               // Card line
C2     4456897999999999                               // ...
LOTE0001000010                                        // Footer
```

- **Header:** [01-29] Name, [30-37] Date, [38-45] Batch, [46-51] Record count
- **Lines:** [01] "C" identifier, [02-07] Numbering, [08-26] Card number
- **Footer:** [01-08] Batch, [09-14] Record count

## Project Structure

```
src/main/java/com/hyperativa/cardapi/
├── CardApiApplication.java          # Main
├── config/                          # Security, OpenAPI, initialization
├── controller/                      # REST controllers
├── dto/                             # Request/Response objects
├── entity/                          # JPA entities
├── exception/                       # Global exception handler
├── filter/                          # JWT and logging filters
├── repository/                      # Spring Data repositories
├── service/                         # Business logic
└── util/                            # Encryption, parser, JWT
```
