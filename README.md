# Leonbets Parser - Dockerized Spring Boot App (Java 21)

This project is a **Spring Boot application** for parsing and printing sports events and market data from the Leonbets API. It supports **reactive WebClient**, **non-blocking processing**, and can be run as a REST API or headless CLI-style parser.

---

## Run with Docker

This project uses a **multi-stage Dockerfile** to build and run everything inside the container — no Java or Maven required on your host.

### Build Docker Image

```bash
docker build -t leonbets-parser-app .
```

## Usage

### CLI Mode

```bash
docker run --rm leonbets-parser-app
```


### REST API Mode

```bash
docker run --rm -p 8080:8080 -e SPRING_AUTOSTART_PARSE=false leonbets-parser-app
```

#### Access the API:
GET http://localhost:8080/api/sports
