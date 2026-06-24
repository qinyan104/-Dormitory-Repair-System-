# Commit Message Convention

Use [Conventional Commits](https://www.conventionalcommits.org/) for all commit messages.

## Format

```
<type>(<scope>): <short summary>

[optional body]

[optional footer]
```

## Types

| Type       | Usage                                    |
|------------|------------------------------------------|
| `feat`     | A new feature                            |
| `fix`      | A bug fix                                |
| `docs`     | Documentation only changes               |
| `style`    | Formatting, missing semicolons, etc.     |
| `refactor` | Code change that neither fixes nor adds  |
| `test`     | Adding or correcting tests               |
| `chore`    | Build process, deps, CI, tooling         |
| `perf`     | Performance improvement                  |
| `security` | Security hardening                       |

## Scopes

| Scope       | Area                               |
|-------------|------------------------------------|
| `backend`   | `dormitory-repair-backend/`        |
| `frontend`  | `dormitory-repair-frontend/`       |
| `deploy`    | Docker / Compose / CI              |
| `infra`     | GitHub Actions, configs, tooling   |
| `docs`      | README, CHANGELOG, AGENTS.md       |

## Examples

```
feat(backend): add idempotency-key support on repair creation
fix(frontend): prevent double submit on slow network
security(backend): enforce JWT secret at startup
docs: add API documentation via Swagger UI
chore(infra): add GitHub Actions Maven CI workflow
refactor(backend): extract DeepSeekClient from AiService
```
