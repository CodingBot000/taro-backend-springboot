# Spring Service

Spring Boot backend for the tarot frontend. The frontend only calls this API; Hugging Face / Gradio secrets stay here.

## Required environment variables

- `HF_SPACE_URL`
- `HF_TOKEN`

## Optional environment variables

- `PORT`
- `APP_VERSION`
- `CORS_ALLOWED_ORIGINS`
- `RATE_LIMIT_REQUESTS_PER_MINUTE`
- `HF_API_PREFIX`
- `HF_CONNECT_TIMEOUT`
- `HF_READ_TIMEOUT`
- `HF_GENERATE_READING_API_NAME`
- `HF_BACKEND_VERSION_API_NAME`
- `JAVA_OPTS`

## Local development

1. Copy the example env file.

```bash
cp .env.example .env
```

2. Fill in the real `HF_TOKEN` and, if needed, change `HF_SPACE_URL`.

3. Run Spring Boot with the env file loaded into the shell.

```bash
set -a
source .env
set +a
./gradlew bootRun
```

## Docker Compose

```bash
cp .env.example .env
docker compose up --build
```

`docker-compose.yml` reads the same `.env` file and passes the variables into the Spring container.

## Production

Provide the same variables through your deployment environment or a production env file that is not committed.

Health check:

```bash
curl http://localhost:8080/health
```

Version check:

```bash
curl http://localhost:8080/api/version
```
