# Changelog

All notable changes to the dormitory-repair project are documented here.

## [1.0.0] — 2026-06-24

### Added
- API documentation via Swagger UI (`/swagger-ui/index.html`)
- Health check with real DB/Redis connectivity probes (`/api/health`, `/live`, `/ready`)
- Structured JSON logging (auto-selected in `prod`/`docker` profiles)
- HTTP security headers: CSP, HSTS, X-Frame-Options, X-Content-Type-Options, Referrer-Policy, Permissions-Policy
- `Idempotency-Key` header support on repair order creation
- User soft-delete: delete now sets `status=0` instead of physical removal
- Repair status enum (`RepairStatusEnum`) and user role enum (`UserRoleEnum`)
- `DeepSeekClient` component extracted from `AiService`
- Integration smoke tests for enums and state machine
- GitHub Actions CI workflow (Maven build + test)
- Commit message convention guide
- README now documents Capacitor Android APK build process

### Changed
- JWT secret is now mandatory at startup (was falling back to hardcoded default)
- CORS tightened from `*` wildcard to explicit origin patterns
- `@Transactional` scoped to individual write methods instead of class-level on controller
- `RedisConfig`: removed dead `CacheManager` configuration (no `@Cacheable` usage)
- `AiService` reduced from 27KB to ~500 lines via `DeepSeekClient` extraction
- `buildOrderView()` removed duplicate fields (`description`, `processRemark`)
- `RepairOrderServiceImpl` uses `RepairStatusEnum` for state machine validation
- Frontend Vite config: vendor chunk splitting for better caching
- Login rate-limiter: explicit TTL refresh after increment

### Fixed
- Missing exception handlers for HTTP method/missing-param/media-type errors
- `StringRedisTemplate.increment()` without TTL refresh in login rate limiter
- AGENTS.md out-of-sync with actual code state

## [0.0.1] — 2026-05-02

Initial project scaffold with core features:
- Student repair order CRUD
- Admin dispatch and management
- Worker acceptance and completion
- AI integration (DeepSeek classify + recommend)
- WebSocket real-time notifications
- Redis captcha
- Excel export
- AOP operation logging
- Docker Compose deployment
