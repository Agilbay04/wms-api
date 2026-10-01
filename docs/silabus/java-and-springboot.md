# Silabus Training: Java Spring Boot REST API

## Tech Stack
1. Java 17
2. Spring Boot 4.1.1
3. PostgreSQL
4. Redis
5. Mailpit & Spring Mail

* * *
## Prerequisite (Persyaratan Peserta)
Sebelum mengikuti training ini, peserta diharapkan sudah memahami dasar-dasar berikut:
1. Konsep Pemrograman Java: _Object-Oriented Programming_ (OOP), Collections Framework, Exception Handling, dan dasar Concurrency/Multithreading.
2. Konsep Database Relasional (RDBMS): Dasar SQL (JOIN, Indexing, Transaction) dan relasi antar tabel.
3. Konsep RESTful API: HTTP Methods (`GET`, `POST`, `PUT`, `DELETE`), Status Codes, JSON Data Format, dan HTTP Request Headers.
4. Dasar Command Line & Tools: Penggunaan Terminal/CMD, Git Version Control, serta Docker dasar.

* * *
## Quickstart & Environment Setup Guide
### Perangkat Lunak & Dependency Manager
1. **SDK:** JDK 17 (OpenJDK).
2. **IDE:** IntelliJ IDEA Community/Ultimate (Recommended) atau VS Code / Eclipse.
3. **Infrastructure:** Docker & Docker Desktop / Orbstack: Untuk infrastruktur PostgreSQL dan Redis.
4. **Build Tool:** Gradle 8.x+
5. **API Client:** Postman / Insomnia / Apidog
6. **DB Tool:** DBeaver / pgAdmin
### Cara Inisialisasi Project Spring Boot
1. Akses [https://start.spring.io](https://start.spring.io)
2. Konfigurasi project:
    1. Project: Gradle
    2. Java
    3. Spring Boot Version: `4.1.1`
    4. Metadata: Group `com.warehousing`, Artifact `wms-api`
    5. Java Version: `17` atau `21`
3. Tambahkan dependencies utama:
    1. Spring Web (REST API)
    2. Spring Data JPA (ORM & Postgres Access)
    3. PostgreSQL Driver
    4. Spring Data Reactive Redis / Spring Data Redis (Cache & Redis Streams)
    5. Spring Boot DevTools
    6. Lombok
    7. Validation (Spring Boot Starter Validation)
4. Klik generate, unduh file `.zip` lalu ekstrak dan buka di IDE pilihan.

* * *
## VsCode Extension (Optional)

1. Spring Boot Extension Pack - [https://marketplace.visualstudio.com/items?itemName=vmware.vscode-boot-dev-pack](https://marketplace.visualstudio.com/items?itemName=vmware.vscode-boot-dev-pack)
2. Extension Pack for Java - [https://marketplace.visualstudio.com/items?itemName=vscjava.vscode-java-pack](https://marketplace.visualstudio.com/items?itemName=vscjava.vscode-java-pack)

* * *
## Modul Training
### Modul 1: Dasar Pemrograman Java
1. Fundamental syntax Java: variable, data type, operator, control flow, method, class, object, package, dan access modifier.
2. Konsep _Object-Oriented Programming_ (OOP): encapsulation, inheritance, polymorphism, abstraction, dan interface.
3. Struktur data dan API dasar Java: Collections Framework, generics, enum, date/time API, dan Stream API dasar.
4. Exception Handling: checked exception, unchecked exception, custom exception, dan strategi propagasi error.
5. Dasar concurrency: Thread, ExecutorService, CompletableFuture, synchronization, dan konsep race condition.
### Modul 2: Environment Setup & Foundation
1. Setup project Spring Boot 4.1.1 dengan Spring Initializr (_Spring Web, Spring Data JPA, PostgreSQL Driver, Spring Data Redis, Lombok, Validation_).
2. Konfigurasi PostgreSQL dan Redis menggunakan Docker.
3. Arsitektur _Clean/Layered Architecture_ (Controller, Service, Repository, DTO, Mapper, Entity).
4. Standardisasi REST API: Global Response Envelope (`code`, `message`, `data`, `errors`) dan Global Exception Handling (`@RestControllerAdvice`).
### Modul 3: Autentikasi & Autorisasi (Spring Security + JWT)
1. Integrasi Spring Security 6 dengan Stateless Authentication berbasis JWT (_JSON Web Token_).
2. Implementasi Access Token & Refresh Token Flow.
3. Role-Based Access Control (RBAC) & Fine-Grained Authorization (`@PreAuthorize`).
4. Token Revocation, Blacklisting, dan Session State Management menggunakan Redis.
### Modul 4: Domain Data Modeling, Advanced Querying & Data Seeding
1. Desain Entity Relationship Diagram (ERD) dan pemetaan relasi JPA (`@OneToMany`, `@ManyToOne`, `@ManyToMany`).Implementasi RESTful CRUD dengan validasi input deklaratif (`@Valid`, Hibernate Validator).
2. Database Migration Tool menggunakan Flyway / Liquibase.
3. Database Seeding & Data Initialization Strategy:
    1. Implementasi Data Seeder berbasis Java menggunakan `CommandLineRunner` / `ApplicationRunner`.
    2. SQL-based initial seeding via Flyway Migration scripts (`R__seed_data.sql` / Repeatable Migrations).
    3. Environment-aware Data Seeding (mengisi mock data khusus untuk profile `dev` / `test` tanpa mengotori `prod`).
4. Dynamic Filtering, Multi-column Sorting, dan Pagination generik menggunakan Spring Data JPA Specification & Criteria API.
5. Database Migration Tool menggunakan Flyway atau Liquibase.
### Modul 5: Core Transactional Processing & Concurrency Handling
1. Pengelolaan Database Transaction (`@Transactional`) & Isolation Level pada kompleksitas bisnis.
2. Penanganan Concurrency & Race Condition pada data kritis (misal: saldo, kuota, atau stok):
    1. Pessimistic Locking (`@Lock(LockModeType.PESSIMISTIC_WRITE)`)
    2. Optimistic Locking dengan `@Version`
3. Event-Driven Application State Handling menggunakan Spring Application Events.
### Modul 6: Caching Strategy dengan Redis
1. Konfigurasi Spring Cache Abstraction berbasis Redis.
2. Deklaratif Caching (`@Cacheable`, `@CachePut`, `@CacheEvict`) untuk data yang sering dibaca.
3. Penanganan _Cache Stampede_, _Cache Penetration_, dan manajemen TTL (Time-To-Live) secara kontrok.
4. Serialization & Deserialization JSON kustom pada Redis Template.
### Modul 7: Async Queue Processing & Messaging via Redis Streams
1. Arsitektur Event-Driven Async Task menggunakan Redis Streams (`opsForStream()`):
    1. Publisher Pattern: Enqueue payload job ke Stream Key (`XADD`).
    2. Consumer Group Listener: Polling/consuming job secara terisolasi (`XREADGROUP`).
2. Heavy Background Processing Use Cases:
    1. Bulk Async Data Import (Excel/CSV Parsing & Batch Database Insert).
    2. Async Report Generation & File Export (PDF/Excel generation).
3. Notification Integration: Service pengiriman Email (_Spring Mail SMTP_) otomatis dipicu pasca-proses async.
4. Fault Tolerance: Acknowledgment system (`XACK`), pending entries list (PEL), dan retry/recovery mechanism.
### Modul 8: API Documentation & Testing
1. Dokumentasi API otomatis menggunakan OpenAPI 3 / Swagger UI.
2. Postman Collection

* * *
## Technical Implementation Checklist

Berikut adalah daftar capaian teknis (_deliverables_) yang harus diimplementasikan oleh peserta selama training berlangsung:

1. **Security & Access Control**
    - [ ] JWT Authentication (Access Token & Refresh Token)
    - [ ] Role-Based Access Control / Authorization (`@PreAuthorize`)
    - [ ] Password Hashing dengan BCrypt
    - [ ] Token Blacklisting / Revocation via Redis
2. **API Architecture & Data Handling**
    - [ ] Global Response Format Handler (Standardized JSON Envelope: `code`, `message`, `data`, `errors`)
    - [ ] Global Exception Handling (`@RestControllerAdvice` & `@ExceptionHandler`)
    - [ ] Request Body & Parameter Validation (`@Valid`, Hibernate Validator, Custom Annotations)
    - [ ] Generic Dynamic Pagination, Sorting, & Filtering (Spring Data JPA Specification)
3. **Database, Seeding & Concurrency**
    - [ ] Database Migration Setup (Flyway / Liquibase)
    - [ ] Database Seeder Component (`CommandLineRunner` / Flyway Repeatable Migrations)
    - [ ] Environment-based Seed Data Configuration (`@Profile("dev")`)
    - [ ] Database Transaction Management (`@Transactional`)
    - [ ] Pessimistic / Optimistic Locking untuk pencegahan Race Condition
4. **Performance & Async Queue Processing**
    - [ ] Caching Strategy via Redis (`@Cacheable`, `@CacheEvict`, TTL Configuration)
    - [ ] Redis Streams Publisher Service (`RedisTemplate.opsForStream().add()`)
    - [ ] Redis Streams Consumer Group Listener (`StreamMessageListenerContainer`)
    - [ ] Stream Acknowledgment (`XACK`) & Error/Retry Handling
    - [ ] Asynchronous Email Notification Service (Spring Mail)
5. **Documentation**
    - [ ] OpenAPI 3 / Swagger Integration & Custom Schema Annotations
    - [ ] Postman Collection

* * *
## Referensi & Dokumentasi Teknis
Gunakan rujukan & referensi berikut selama proses pembelajaran dan pengembangan:
### 1\. Dasar Pemrograman Java

| Referensi | Deskripsi | URL |
| ---| ---| --- |
| Java Documentation | Fundamental Java, OOP, Collections, Exception Handling, dan concurrency. | [dev.java](https://dev.java/learn/) |
| Java Language Specification | Spesifikasi bahasa Java, syntax, type system, class, interface, dan exception. | [JLS](https://docs.oracle.com/javase/specs/) |
| Java Tutorials | Panduan dasar bahasa Java, OOP, Collections, generics, dan exception handling. | [Oracle Java Tutorials](https://docs.oracle.com/javase/tutorial/) |
| Java Collections Framework | Struktur data bawaan Java seperti List, Set, Map, Queue, dan Iterator. | [Collections Framework](https://docs.oracle.com/javase/8/docs/technotes/guides/collections/overview.html) |
| Java Concurrency | Thread, executor, synchronization, concurrent collections, dan concurrency utilities. | [Concurrency](https://docs.oracle.com/javase/tutorial/essential/concurrency/) |
| Java Stream API | Pemrosesan collection secara fungsional menggunakan Stream API. | [Stream API](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/stream/package-summary.html) |

### 2\. Spring Boot & REST Foundation

| Referensi | Deskripsi | URL |
| ---| ---| --- |
| Spring Boot Reference | Konfigurasi aplikasi, dependency, application properties, dan struktur project. | [Spring Boot Docs](https://docs.spring.io/spring-boot/reference/) |
| Spring Initializr | Inisialisasi project Spring Boot dan pemilihan dependency. | [start.spring.io](https://start.spring.io/) |
| Spring Web MVC | REST Controller, HTTP request/response, routing, dan request handling. | [Spring MVC Docs](https://docs.spring.io/spring-framework/reference/web/webmvc.html) |
| Gradle User Manual | Build automation, dependency management, dan konfigurasi project. | [Gradle Docs](https://docs.gradle.org/current/userguide/userguide.html) |
| Spring Framework — Validation | Validasi request menggunakan Bean Validation dan custom validator. | [Validation Docs](https://docs.spring.io/spring-framework/reference/core/validation/beanvalidation.html) |

### 3\. REST API Design & Error Handling

| Referensi | Deskripsi | URL |
| ---| ---| --- |
| MDN — HTTP Request Methods | Konsep HTTP methods seperti GET, POST, PUT, PATCH, dan DELETE. | [HTTP Methods](https://developer.mozilla.org/en-US/docs/Web/HTTP/Methods) |
| MDN — HTTP Response Status Codes | Penggunaan HTTP status code untuk response API. | [HTTP Status Codes](https://developer.mozilla.org/en-US/docs/Web/HTTP/Status) |
| RFC 9110 — HTTP Semantics | Standar HTTP, semantics request/response, dan status code. | [RFC 9110](https://www.rfc-editor.org/rfc/rfc9110) |
| Spring Web MVC — Exception Handling | Penanganan exception menggunakan `@ExceptionHandler` dan `@ControllerAdvice`. | [Spring MVC Docs](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-exceptionhandler.html) |

### 4\. Security, Authentication & Authorization

| Referensi | Deskripsi | URL |
| ---| ---| --- |
| Spring Security Reference | Konsep autentikasi, otorisasi, filter chain, dan konfigurasi security. | [Spring Security Docs](https://docs.spring.io/spring-security/reference/) |
| Spring Security — JWT | Validasi JWT dan konfigurasi resource server. | [JWT Resource Server](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html) |
| Spring Security — Method Security | Implementasi otorisasi berbasis role dan `@PreAuthorize`. | [Method Security](https://docs.spring.io/spring-security/reference/servlet/authorization/method-security.html) |
| Spring Security — PasswordEncoder | Password hashing dan penggunaan BCrypt. | [Password Storage](https://docs.spring.io/spring-security/reference/servlet/authentication/passwords/password-encoder.html) |
| JWT Introduction | Struktur JWT, claims, dan mekanisme token. | [jwt.io](https://www.jwt.io/introduction) |
| OWASP Authentication Cheat Sheet | Praktik keamanan autentikasi dan session management. | [OWASP](https://cheatsheetseries.owasp.org/cheatsheets/Authentication_Cheat_Sheet.html) |
| OWASP JWT Cheat Sheet | Risiko JWT, token storage, dan strategi mitigasi. | [OWASP](https://cheatsheetseries.owasp.org/cheatsheets/JSON_Web_Token_for_Java_Cheat_Sheet.html) |

### 5\. Database, Data Modeling, Migration & Seeding

| Referensi | Deskripsi | URL |
| ---| ---| --- |
| PostgreSQL Documentation | SQL, relasi tabel, constraint, indexing, dan transaksi. | [PostgreSQL Docs](https://www.postgresql.org/docs/current/) |
| Spring Data JPA | Entity, repository, relasi JPA, query, dan persistence. | [Spring Data JPA](https://docs.spring.io/spring-data/jpa/reference/) |
| Jakarta Persistence | Standar ORM Java, entity mapping, dan relationship. | [Jakarta Persistence](https://jakarta.ee/specifications/persistence/) |
| Spring Data JPA — Specifications | Dynamic filtering menggunakan Specification dan Criteria API. | [Specifications](https://docs.spring.io/spring-data/jpa/reference/jpa/specifications.html) |
| Flyway Documentation | Database versioning dan migration scripts. | [Flyway Docs](https://documentation.red-gate.com/flyway) |
| Flyway — Repeatable Migrations | Migration untuk data atau objek database yang dapat dijalankan ulang. | [Repeatable Migrations](https://documentation.red-gate.com/flyway/reference/tutorials/tutorial-repeatable-migrations) |
| Liquibase Documentation | Alternatif Flyway untuk schema migration dan changelog. | [Liquibase Docs](https://docs.liquibase.com/) |
| Spring Boot — ApplicationRunner | Inisialisasi data menggunakan `CommandLineRunner` atau `ApplicationRunner`. | [Spring Boot Docs](https://docs.spring.io/spring-boot/reference/features/spring-application.html) |

### 6\. Transaction Management & Concurrency

| Referensi | Deskripsi | URL |
| ---| ---| --- |
| Spring Transaction Management | Konsep transaksi dan penggunaan `@Transactional`. | [Transaction Docs](https://docs.spring.io/spring-framework/reference/data-access/transaction.html) |
| Spring Data JPA — Transactions | Pengelolaan transaksi pada repository dan service. | [Transactionality](https://docs.spring.io/spring-data/jpa/reference/jpa/transactions.html) |
| Spring Data JPA — Locking | Pessimistic locking menggunakan `@Lock`. | [Locking](https://docs.spring.io/spring-data/jpa/reference/jpa/locking.html) |
| PostgreSQL — Explicit Locking | Row-level lock dan mekanisme locking database. | [Explicit Locking](https://www.postgresql.org/docs/current/explicit-locking.html) |
| PostgreSQL — Transaction Isolation | Isolation level dan concurrency behavior. | [Transaction Isolation](https://www.postgresql.org/docs/current/transaction-iso.html) |
| Spring Framework — Application Events | Event publishing dan event listener untuk application-level events. | [Application Events](https://docs.spring.io/spring-framework/reference/core/beans/context-introduction.html) |

### 7\. Caching & Redis

| Referensi | Deskripsi | URL |
| ---| ---| --- |
| Spring Cache Abstraction | Konsep caching dan integrasi cache pada Spring. | [Spring Cache](https://docs.spring.io/spring-framework/reference/integration/cache.html) |
| Spring Cache Annotations | Penggunaan `@Cacheable`, `@CachePut`, dan `@CacheEvict`. | [Cache Annotations](https://docs.spring.io/spring-framework/reference/integration/cache/annotations.html) |
| Spring Data Redis | Integrasi Redis, RedisTemplate, serialization, dan konfigurasi koneksi. | [Spring Data Redis](https://docs.spring.io/spring-data/redis/reference/) |
| Redis Data Types | Struktur data dan operasi dasar Redis. | [Redis Docs](https://redis.io/docs/latest/develop/data-types/) |
| Redis Key Expiration | TTL dan expiration pada key Redis. | [Redis Keyspace](https://redis.io/docs/latest/develop/use/keyspace/) |

### 8\. Asynchronous Processing, Redis Streams & Messaging

| Referensi | Deskripsi | URL |
| ---| ---| --- |
| Redis Streams | Konsep stream, consumer, dan consumer group. | [Redis Streams](https://redis.io/docs/latest/develop/data-types/streams/) |
| Redis — XADD | Menambahkan pesan ke Redis Stream. | [XADD](https://redis.io/docs/latest/commands/xadd/) |
| Redis — XREADGROUP | Membaca pesan menggunakan consumer group. | [XREADGROUP](https://redis.io/docs/latest/commands/xreadgroup/) |
| Redis — XACK | Acknowledgment pesan yang telah diproses. | [XACK](https://redis.io/docs/latest/commands/xack/) |
| Redis — XAUTOCLAIM | Mengambil alih pesan pending untuk recovery. | [XAUTOCLAIM](https://redis.io/docs/latest/commands/xautoclaim/) |
| Spring Data Redis | Implementasi Redis Streams melalui `RedisTemplate` dan `StreamMessageListenerContainer`. | [Spring Data Redis](https://docs.spring.io/spring-data/redis/reference/) |
| Spring Boot — Email | Pengiriman email menggunakan Spring Mail dan `JavaMailSender`. | [Spring Mail](https://docs.spring.io/spring-boot/reference/io/email.html) |
| Mailpit | SMTP testing server untuk pengujian email lokal. | [Mailpit Docs](https://mailpit.axllent.org/docs/) |

### 9\. API Documentation & API Testing

| Referensi | Deskripsi | URL |
| ---| ---| --- |
| OpenAPI Specification | Standar dokumentasi API, schema, endpoint, dan response. | [OpenAPI](https://spec.openapis.org/oas/latest.html) |
| springdoc-openapi | Integrasi OpenAPI dan Swagger UI pada Spring Boot. | [springdoc-openapi](https://springdoc.org/) |
| Swagger UI | Dokumentasi API interaktif. | [Swagger UI](https://swagger.io/tools/swagger-ui/) |
| Postman Collections | Pengelolaan dan pembagian kumpulan request API. | [Postman Docs](https://learning.postman.com/docs/use/use-collections/create-collections) |
| Postman Tests | Pengujian response API menggunakan test scripts. | [Postman Docs](https://learning.postman.com/docs/tests-and-scripts/write-scripts/test-scripts/) |

###