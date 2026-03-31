# Repository Guidelines

## Project Structure & Module Organization
This repository is a Gradle multi-module Java project. `app/` is the Spring Boot entrypoint and the only module that produces an executable JAR. Shared primitives live in `common/`. Feature modules live in `user/`, `booking/`, and `ticket/`; `user/` currently shows the intended hexagonal structure: `domain`, `application`, and `adapter/in/web`. Operational files live in `docker/`, while design notes and planning docs live in `knowledge-base/`.

Source code follows the standard Gradle layout: `src/main/java` and `src/main/resources`. Tests belong in `src/test/java` and `src/test/resources`.

## Build, Test, and Development Commands
Use the Gradle wrapper from the repo root.

- `.\gradlew.bat build` builds all modules and runs tests.
- `.\gradlew.bat test` runs the JUnit Platform test suite.
- `.\gradlew.bat :app:bootRun` starts the application locally.
- `.\gradlew.bat :app:bootJar` creates the runnable JAR for `app`.
- `docker compose up --build -d` starts PostgreSQL, Redis, RabbitMQ, the app, and Nginx.

## Coding Style & Naming Conventions
Use 4-space indentation and standard Java formatting. Packages stay lowercase under `com.multimodule.<module>`. Keep class names descriptive and suffix by role, for example `UserController`, `CreateUserService`, `UserRepository`, `UserResponse`.

Preserve module boundaries. Domain code should stay framework-free; Spring annotations belong in application or adapter layers. Prefer constructor injection with Lombok (`@RequiredArgsConstructor`) and use records for request/response DTOs where practical. MapStruct is the mapper standard in feature modules.

## Testing Guidelines
The build is configured for JUnit 5 (`useJUnitPlatform()`). `app/` includes Testcontainers dependencies for integration tests, and feature modules include ArchUnit for package-boundary checks. Name tests `*Test` and keep them in the same module as the code they verify. Run module-scoped tests with commands such as `.\gradlew.bat :user:test`.

## Commit & Pull Request Guidelines
Git history currently uses short, imperative subjects, for example: `Initial commit: multi-module Gradle app with hexagonal user module and infra`. Follow that pattern and mention the affected module when useful.

Pull requests should state scope, affected modules, local verification performed, and any config or API changes. Include request/response examples or screenshots when changing web behavior, and link the relevant issue or task.

## Configuration & Environment
The project targets Java 25 via Gradle toolchains. Keep secrets out of Git; use environment variables consumed by `application.yml` and `docker-compose.yml` for local runtime configuration.
