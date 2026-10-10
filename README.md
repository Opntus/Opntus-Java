# Opntus — Java web application
The application's server has been converted to Java 17 + Spring Boot 3.5.16. It remains a web app; it is not an Android APK. No Node.js process, TypeScript backend, or Railway Bun Function is needed to run this Java edition.

## Run the included JAR
Install Java 17 or newer. From this folder:
```
java -jar opntus-1.0.0.jar
```
Open http://localhost:8080. Windows users can also run run-windows.bat. The UI loads without Supabase configuration, but real sign-in requires the Supabase setup below. No fake accounts or successful logins are provided.

## Supabase email/password setup — no Google Cloud required
1. Create a Supabase project.
2. In Authentication settings, enable Email provider and KEEP Confirm email enabled. Verified email is required, including for Admin access.
3. Set Authentication URL Configuration Site URL to http://localhost:8080 locally (use your HTTPS domain in production). Signup confirmation returns here; sign in manually after confirming.
4. Find the Project URL and publishable key in the project Connect/API settings. Use the publishable key (or legacy anon key), never service_role or a secret key.
5. Set these environment variables before starting Java. .env files are examples and are not loaded automatically.
PowerShell:
```
$env:SUPABASE_URL="https://YOUR-PROJECT.supabase.co"
$env:SUPABASE_PUBLISHABLE_KEY="YOUR_PUBLISHABLE_KEY"
java -jar opntus-1.0.0.jar
```
Create an account in the app, confirm the email link, then sign in. opntus.1@gmail.com becomes Admin only after that exact email is confirmed; vikashpalanisamy005@gmail.com is not allowlisted.
Passwords are sent over the same-origin Java server to Supabase Auth; they are not saved in the app database. Java checks the authenticated user through Supabase /auth/v1/user rather than trusting browser claims. Tokens are not exposed to the browser; a local HttpOnly session is used. Sessions expire after at most one hour (or 30 minutes idle), then users sign in again. Logout clears the local app session. This edition has no automatic refresh or password recovery UI; admins can use Supabase account-management tools for recovery.
Use HTTPS in production. Supabase controls authentication rate limits. Configure SMTP for reliable signup confirmation delivery before opening public registration; default test mail delivery is restricted. No Supabase project has been connected or created by this code change.

## What's Java and what stays browser code
All authentication, authorization, profiles, Listener applications, Admin approval and permissions, availability, request matching, session ownership, messaging, WebRTC signaling, history, ratings, reports, blocking and audit operations run in Java. Browser interfaces still require HTML/CSS/JavaScript; Java cannot replace the browser's native DOM, camera, microphone and WebRTC APIs. These browser assets are bundled inside the JAR and served by Spring Boot. src/main/resources/static contains the complete executable browser assets; no frontend server or build is required to run or rebuild the Java application.

## Source structure
- OpntusApplication.java: application entry point.
- SecurityConfig.java: authentication, sessions, CSRF, security headers and logout.
- OpntusController.java: authenticated Java REST endpoints.
- OpntusService.java: transactional domain logic and database access.
- WebController.java: page routing, login, health endpoint and CSRF tokens.
- ApiException.java: consistent API errors.
- src/main/resources/schema.sql: repeatable fresh database setup and indexes.
- src/main/resources/static/: UI, full logo, styles, browser calling and CSRF integration.
- src/test/java/: authentication, CSRF, permissions and end-to-end conversation tests.

## Build from source
Install Maven 3.9+ and Java 17+:
```
mvn clean package
java -jar target/opntus-1.0.0.jar
```
Tests run as part of package. IntelliJ IDEA, Eclipse or VS Code can import pom.xml as a Maven project. Browser bundles are included, so Maven does not require Node.js.

## Persistence and hosting
By default, data is stored in opntus.db in the current working directory. A server process restart keeps database records but ends login sessions; users sign in again. One JVM with a single JDBC connection is supported by this SQLite edition. Use the included Dockerfile for a conventional container service. Mount persistent storage at /data and set DATABASE_URL=jdbc:sqlite:/data/opntus.db. Set SERVER_SERVLET_SESSION_COOKIE_SECURE=true behind HTTPS. Forwarded headers are intended for a trusted reverse proxy; do not expose an untrusted proxy chain.

For multiple replicas migrate to a shared transactional database and shared session storage; this is not a distributed SQLite design. This Java application cannot run inside Cloudflare Workers or the current Railway Bun Function. A Java-capable host is needed; hosting cost is not guaranteed to be zero.

## Preserved feature boundaries
Text chat and request updates use 2.5-second browser polling. WebRTC audio/video signaling, permission handling, mute/camera controls and media cleanup are retained; restricted networks still need a production TURN relay and two-person browser tests. Real payments, GPS tracking, physical meetups, OTP check-in and group meetup booking are disabled, exactly as before. Listener identity checks are an Admin process; the app does not pretend to perform automated identity verification. Terms/privacy notices are early-access drafts needing review before public launch.

## Migration status
This deliverable is the Java source and runnable JAR, not a replacement of either live website. Existing Railway and ChatGPT Sites deployments were not changed or shut down. Historical databases have NOT been exported or copied. Supabase users have new supabase:UUID identities; old Google users are not automatically merged. The Java schema is compatible with the core tables, but identity mappings must be verified before importing older user records. Do not attach a live production database file concurrently to two applications.

## Editing the browser UI
frontend-source/ contains the editable React/TypeScript browser source, not a backend. Java serves its compiled assets. For UI changes only, install Node 24, run npm install and npm run build in frontend-source/. Copy dist/ into src/main/resources/static/, preserve csrf-fetch.js, add its script tag before the module script in index.html, then run mvn clean package. No Node server is used in this edition.
