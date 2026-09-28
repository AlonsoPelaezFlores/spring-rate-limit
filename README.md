# spring-rate-limit

![Java](https://img.shields.io/badge/Java-17%2B-orange)
![Spring Framework](https://img.shields.io/badge/Spring%20Framework-7-6DB33F)
![Redis](https://img.shields.io/badge/Redis-optional-DC382D)
![License](https://img.shields.io/badge/license-MIT-blue)

Annotation-driven rate limiting for Spring applications, based on the **token bucket** algorithm.

Put `@RateLimit` on a method and every call is checked against a named profile before it executes. When the limit is exceeded, the call is rejected with a `RateLimitExceededException`.

Two storage backends are available:

| Backend | Use it when | How state is kept |
|---|---|---|
| **In-memory** (default) | Your application runs as a **single instance** | A `ConcurrentHashMap` of buckets inside the JVM |
| **Redis** | Your application runs as **several instances** behind a load balancer | One shared bucket per key in Redis, updated atomically by a Lua script |

---

## Table of contents

- [Features](#features)
- [How it works](#how-it-works)
- [Tech stack](#tech-stack)
- [Package structure](#package-structure)
- [Prerequisites](#prerequisites)
- [Installation](#installation)
- [Configuration](#configuration)
- [Running Redis with Docker](#running-redis-with-docker)
- [Usage](#usage)
- [Errors](#errors)
- [Running the tests](#running-the-tests)
- [Current limitations](#current-limitations)
- [Roadmap](#roadmap)
- [License](#license)

---

## Features

- A single `@RateLimit` annotation, applied with Spring AOP. Your business code stays unchanged.
- Named profiles defined in a `rate-limits.properties` file, so one profile can be reused on many methods.
- A global limit per profile, or a limit per key (for example per user) with `keyParamIndex`.
- A thread-safe in-memory backend that needs no configuration.
- A distributed Redis backend: the token bucket runs as one **atomic Lua script** on Redis's own clock, so several instances can share the limit without race conditions.
- A pluggable storage contract (`RateLimitStore`).

## How it works

### Token bucket

Each profile describes a bucket:

- **`capacity`**: the maximum number of tokens the bucket holds. This is the largest burst allowed.
- **`refillTokens`**: how many tokens are added back per refill period.
- **`refillPeriodMillis`**: the length of the refill period, in milliseconds.

Every call to an annotated method consumes **one token**. If there is at least one token left, the call proceeds. If the bucket is empty, the call is rejected. Tokens are refilled lazily and proportionally to the time elapsed, and the bucket never goes above `capacity`.

Example: `capacity=10`, `refillTokens=10`, `refillPeriodMillis=1000` means bursts of up to 10 calls, then a steady rate of 10 calls per second.

### Request flow

```
  caller
    │
    ▼
┌────────────────────┐   1. read profile   ┌─────────────────────────┐
│  RateLimitAspect   │ ──────────────────▶ │ RateLimitConfigRegistry │  (loaded from rate-limits.properties)
│  @Around           │                     └─────────────────────────┘
│  @RateLimit        │   2. resolve key: "<profile>" or "<profile>:<argument>"
│                    │   3. try to consume a token
│                    │ ──────────────────▶ RateLimitService ──▶ RateLimitStore
└────────────────────┘                                              │
    │                                              ┌────────────────┴────────────────┐
    │ allowed → proceed()                          ▼                                 ▼
    │ denied  → RateLimitExceededException   InMemoryRateLimitStore          RedisRateLimitStore
    ▼                                        (Bucket per key, JVM)           (rate_limit.lua, EVALSHA)
 target method
```

## Tech stack

| Technology | Version | Role |
|---|---|---|
| Java | 17+ | Language (required by Spring Framework 7) |
| Maven | 3.6.3+ | Build and dependency management |
| Spring Framework (`spring-context`) | 7.0.5 | IoC container and `@Configuration` / `@Bean` wiring |
| Spring AOP (`spring-aop`) | 7.0.5 | Proxy-based interception of annotated methods |
| AspectJ (`aspectjweaver`) | 1.9.25.1 | `@Aspect` / `@Around` annotations and pointcut expressions |
| Jedis | 5.2.0 | Redis client, used with a `JedisPool` connection pool |
| Redis + Lua | Redis 5+ | Shared bucket state and the atomic token bucket script |
| Docker | any | Easiest way to run Redis locally |
| JUnit 5 | 5.11.4 | Unit and integration tests |

> This is a plain **Spring Framework** library, not a Spring Boot starter. It works in Spring Boot applications too, but it has to be imported explicitly (see [Configuration](#configuration)).

## Package structure

```
src/main/java/com/github/alonsopelaezflores/ratelimit
├── RateLimit.java                    ← public API: the annotation
├── RateLimitExceededException.java   ← public API: thrown when a limit is exceeded
├── config/
│   └── RateLimitAutoConfig.java      ← entry point: import this into your Spring context
├── store/
│   └── RateLimitStore.java           ← extension point: storage contract
└── internal/                         ← implementation details
    ├── RateLimitAspect.java          ← AOP interceptor
    ├── RateLimitService.java         ← delegates to the active store
    ├── RateLimitConfig.java          ← one profile (capacity, refillTokens, refillPeriodMillis)
    ├── RateLimitConfigLoader.java    ← parses rate-limits.properties
    ├── RateLimitConfigRegistry.java  ← profile lookup by name
    ├── RateLimitConstants.java       ← property keys and defaults
    ├── Bucket.java                   ← in-memory token bucket
    ├── InMemoryRateLimitStore.java   ← single-instance backend
    └── RedisRateLimitStore.java      ← distributed backend (Jedis + Lua)

src/main/resources
└── rate_limit.lua                    ← token bucket executed atomically inside Redis
```

**Why this layout.** The packages separate what you use from how it works:

- **`ratelimit`, `config` and `store`** are the only packages your code should reference. They contain the annotation, the exception, the configuration to import, and the interface to implement if you want your own backend.
- **`internal`** contains the whole implementation: the aspect, the config parsing, the buckets and the stores. It can change between versions without breaking you, as long as you don't depend on it directly.

## Prerequisites

- **JDK 17** or newer
- **Maven 3.6.3+**
- A Spring application (Spring Framework 7 / Spring Boot 4)
- **Docker**, only if you want the Redis backend. Install it from the [official Docker docs](https://docs.docker.com/get-started/get-docker/) (Docker Desktop on Windows and macOS, Docker Engine on Linux).

## Installation

Install it into your local Maven repository:

```bash

git clone https://github.com/AlonsoPelaezFlores/spring-rate-limit.git
cd spring-rate-limit
mvn clean install -DskipTests
```

Then add it to your project's `pom.xml`:

```xml
<dependency>
    <groupId>com.github.alonsopelaezflores</groupId>
    <artifactId>spring-rate-limit</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

## Configuration

### 1. Import the configuration

```java
@Configuration
@Import(RateLimitAutoConfig.class)
public class AppConfig {
}
```

This registers the aspect, the profile registry and the store, and enables AspectJ auto-proxying.

### 2. Create `rate-limits.properties`

Create a file named exactly **`rate-limits.properties`** on the classpath (for example `src/main/resources/rate-limits.properties`). This file is **required**: the application fails at startup if it is missing.

Each profile is declared with the prefix `ratelimit.<profile>.`:

```properties
# Global limit: 10 calls per second shared by all callers
ratelimit.search.capacity=10
ratelimit.search.refillTokens=10
ratelimit.search.refillPeriodMillis=1000

# Per-user limit: 3 attempts per minute per user
ratelimit.login.capacity=3
ratelimit.login.refillTokens=3
ratelimit.login.refillPeriodMillis=60000
```

| Property | Required | Description |
|---|---|---|
| `ratelimit.<profile>.capacity` | yes | Maximum tokens in the bucket (maximum burst) |
| `ratelimit.<profile>.refillTokens` | yes | Tokens added per refill period |
| `ratelimit.<profile>.refillPeriodMillis` | yes | Refill period in milliseconds |

If any of the three properties is missing, startup fails with an error that names the profile and the missing property.

### 3. Choose a backend

**In-memory (default).** No extra configuration is needed. If `store.type` is not set, the in-memory backend is used.

**Redis.** Add the store properties to the same file:

```properties
store.type=redis
store.redis.host=localhost
store.redis.port=6379
```

| Property | Default | Description |
|---|---|---|
| `store.type` | `memory` | `memory` or `redis` |
| `store.redis.host` | `localhost` | Redis host (only used when `store.type=redis`) |
| `store.redis.port` | `6379` | Redis port (only used when `store.type=redis`) |

With `store.type=redis`, you can leave out `host` and `port` if Redis runs on `localhost:6379`.

## Running Redis with Docker

One command downloads the official image and starts Redis on port 6379:

```bash

docker run -d -p 6379:6379 --name redis-ratelimit redis:latest
```

Check that it is running:

```bash

docker exec redis-ratelimit redis-cli ping
# PONG
```

Later, use `docker stop redis-ratelimit` and `docker start redis-ratelimit` to stop and restart it.

## Usage

### Global limit per profile

All calls share one bucket:

```java
@Service
public class SearchService {

    @RateLimit(name = "search")
    public List<Result> search(String query) {
        // ...
    }
}
```

### Limit per key with `keyParamIndex`

`keyParamIndex` is the zero-based position of the method parameter used as the bucket key. Each distinct value gets its own bucket:

```java
@Service
public class AuthService {

    @RateLimit(name = "login", keyParamIndex = 0)   // one bucket per userId
    public Token login(String userId, String password) {
        // ...
    }
}
```

Buckets are scoped per profile: the same `userId` has independent buckets in `login` and in any other profile. In Redis, keys are stored as `ratelimit:<profile>:<value>`.

### Handling the exception

When the limit is exceeded, `RateLimitExceededException` (a `RuntimeException`) is thrown and the method is **not** executed. In a Spring MVC application you can map it to HTTP `429 Too Many Requests`:

```java
@RestControllerAdvice
public class RateLimitExceptionHandler {

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<String> handle(RateLimitExceededException ex) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(ex.getMessage());
    }
}
```

## Errors

| Situation | Exception | When |
|---|---|---|
| Rate limit exceeded | `RateLimitExceededException` | Method call |
| `rate-limits.properties` not found | `IllegalStateException` | Startup |
| Profile missing a required property | `IllegalStateException` | Startup |
| `@RateLimit(name = ...)` references an unknown profile | `IllegalArgumentException` | First call to that method |
| `keyParamIndex` out of range for the method | `IllegalArgumentException` | Method call |
| The parameter used as key is `null` | `IllegalArgumentException` | Method call |
| `store.type=redis` but Redis is unreachable | Jedis connection exception | Startup |

## Running the tests

The test configuration uses the Redis backend, so start Redis first:

```bash

docker run -d -p 6379:6379 --name redis-ratelimit redis:latest
mvn clean test
```

The suite covers the in-memory bucket (including concurrency), properties parsing and validation, per-key and per-profile isolation through the aspect, and Redis-specific behaviour (key naming and recovery after a script cache flush).

## Current limitations

This is an initial version. Known limitations:

- **Token bucket only.** No other algorithms (fixed window, sliding window, leaky bucket).
- **`keyParamIndex` is positional.** The key is taken from the parameter at a fixed position and converted with `String.valueOf`. Consequences:
  - If you reorder the method's parameters, you must update the index.
  - You cannot use a field of an object parameter (for example `request.getUserId()`). Use a parameter whose `toString()` is a meaningful key, such as a `String` or `Long` id.
  - There is no SpEL support and no access to HTTP context such as the client IP or headers.
- **In-memory buckets never expire.** Every distinct key keeps its bucket for the life of the JVM, so memory grows with the number of keys (for example with many different user ids).
- **Redis backend:**
  - It uses a `JedisPool` with default pool settings, and no password or TLS configuration.
  - There is no fallback: if Redis is unavailable at startup the application fails, and if Redis goes down at runtime, annotated calls fail.
- **Configuration comes only from the classpath file.** It does not integrate with `application.properties` / `application.yml`, Spring profiles or environment variables, and there is no Spring Boot auto-configuration.

## Roadmap

- Key resolution with SpEL (for example `key = "#request.userId"`).
- Expiration of idle in-memory buckets.
- Redis authentication, TLS and pool tuning options.
- Configurable fail-open or fail-closed behaviour when Redis is unavailable.
- A Spring Boot starter with `@ConfigurationProperties` and auto-configuration.
- More algorithms (sliding window).

## License

Distributed under the [MIT License](LICENSE).

Author: **Alonso Peláez Flores**, [@AlonsoPelaezFlores](https://github.com/AlonsoPelaezFlores)
