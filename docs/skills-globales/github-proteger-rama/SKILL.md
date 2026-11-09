---
name: github-proteger-rama
description: Proteger la rama principal de un repo de GitHub con rulesets (PR obligatorio con aprobaciones, check de CI obligatorio, sin borrar ni forzar push) y dejar a una sola persona como administradora que puede saltarse la revisión. Úsala cuando el usuario pida proteger main, exigir revisiones, cambiar permisos del equipo o verificar la protección.
---

# Proteger la rama principal

## Antes de cambiar nada
1. Revisa lo que existe: `gh api repos/<dueño>/<repo>/rulesets` y `gh api repos/<dueño>/<repo>/branches/main/protection` (la clásica puede dar 404). **No supongas** que algo no existe: ya hubo un caso en que la regla clásica sí estaba.
2. Pide al usuario confirmar: número de aprobaciones, nombre del check de CI, quién puede saltarse la revisión.
3. Es un cambio de configuración del repo: confirma antes de aplicarlo.

## Dos rulesets (así quedó en Café Smart)
- `main-1`: sin borrar la rama y sin `push --force` (`deletion`, `non_fast_forward`).
- `main-2`: PR obligatorio con 1 aprobación, descartar aprobaciones viejas al cambiar el PR, y el check `build` obligatorio.

```bash
gh api -X POST repos/<dueño>/<repo>/rulesets --input - <<'JSON'
{
  "name": "main-2",
  "target": "branch",
  "enforcement": "active",
  "conditions": {"ref_name": {"include": ["~DEFAULT_BRANCH"], "exclude": []}},
  "bypass_actors": [{"actor_id": 5, "actor_type": "RepositoryRole", "bypass_mode": "pull_request"}],
  "rules": [
    {"type": "pull_request", "parameters": {
      "required_approving_review_count": 1,
      "dismiss_stale_reviews_on_push": true,
      "require_code_owner_review": false,
      "require_last_push_approval": false,
      "required_review_thread_resolution": false}},
    {"type": "required_status_checks", "parameters": {
      "strict_required_status_checks_policy": false,
      "required_status_checks": [{"context": "build"}]}}
  ]
}
JSON
```
- `actor_id: 5` es el rol **Admin**. `bypass_mode: "pull_request"` significa que el administrador solo se salta la revisión **dentro de un PR**, no con `push` directo. Para que nadie haga push directo, no pongas bypass en modo `always`.
- Si el repo es privado en un plan sin rulesets, la API lo dirá; no insistas.

## Un solo administrador
- Deja a una persona (el líder) como Admin y baja al resto a **Write**: `gh api -X PUT repos/<dueño>/<repo>/collaborators/<usuario> -f permission=push`.
- Con rol Write nadie se salta la revisión, aunque pueda crear ramas y PRs.

## Verificar
- `gh api repos/<dueño>/<repo>/rulesets` y revisa `bypass_actors`, `rules` y `enforcement`.
- Comprueba el nombre exacto del check en un PR real: `gh pr checks <n>`; si no coincide con `build`, el PR quedará esperando para siempre.
- Prueba con un PR pequeño: sin aprobación no debe dejar fusionar.

## Qué hacer cuando el CI falla por red
Si el fallo es una descarga de Gradle o Maven, no es del PR: `gh run rerun <id> --failed`.
