# Skills globales

Copias de las skills de Claude Code que sirven en **cualquier** proyecto. Las originales viven en `~/.claude/skills/` de la cuenta de Andrés; aquí quedan para instalarlas en otra cuenta.

| Skill | Cuándo usarla |
|---|---|
| `traspaso-de-sesion` | Al despedirse de una sesión larga: actualizar docs, vault y memoria. |
| `auditar-pr` | Auditar uno o varios PRs en solo lectura (versión genérica; este repo tiene la suya en `.claude/skills/auditar-pr/`). |
| `github-proteger-rama` | Proteger `main` con rulesets y dejar un solo administrador. |
| `trello-via-navegador` | Leer y editar tarjetas de Trello sin conector, con el navegador integrado. |
| `canva-editar-diapositivas` | Revisar y editar presentaciones de Canva (incluye código coloreado como imagen). |
| `entrenar-exposicion-oral` | Corregir prácticas habladas y preparar preguntas del profesor. |

## Instalar
Desde la raíz del repo:
```bash
mkdir -p ~/.claude/skills
cp -R docs/skills-globales/trello-via-navegador ~/.claude/skills/
```
Repite con cada carpeta que quieras. Después reinicia Claude Code.

No incluyen credenciales. Lo que dependa de una sesión (Trello, Canva) usa la sesión abierta de quien las ejecuta.
