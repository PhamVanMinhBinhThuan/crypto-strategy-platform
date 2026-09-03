# Shared UI Design System Reference

This document records the visual language of the approved prototype. It is a reference, not a mandate to add the prototype's styling framework to production.

## Product character

- Dark quantitative-research terminal.
- High information density without hiding primary actions.
- Strong distinction between durable status, realtime status and analytical values.
- Monospaced numerics where precise values benefit from fixed-width presentation.
- Consistent application shell across all five major screens.

## Prototype color tokens

The prototype source uses approximately these tokens:

| Token | Reference value | Intent |
|---|---:|---|
| main background | `#0B0E11` | application workspace |
| low surface | `#191C1F` | secondary surface |
| surface | `#1D2023` | card/panel |
| high surface | `#272A2E` | emphasized container |
| highest surface | `#323538` | hover/scroll/thumb |
| panel | `#1E2329` | analytical panel |
| panel border | `#2B3139` | subtle divider |
| primary | `#44E092` | positive/primary accent |
| primary container | `#02C076` | positive market/accent |
| tertiary | `#F6BE16` | warning/highlight |
| error reference | `#93000A` | error container |
| primary text | `#E1E2E7` | high-emphasis text |
| secondary text | `#BBCABD` | secondary text |
| negative candle reference | `#CF304A` | negative market movement |

Production should map these intentions into the existing F-011 styling system rather than importing Tailwind solely because the prototype used utility classes.

## Typography

- UI font reference: Inter/system sans-serif.
- Numeric/code reference: JetBrains Mono or the production mono fallback.
- Use tabular numeric presentation for prices, percentages, rankings and timestamps where practical.

## Layout principles

- Reference desktop canvas: 1440px.
- Production support target: 360px through 1440px+ according to F-011 requirements.
- Primary actions must remain usable at normal 100% browser zoom.
- Dense tables may use horizontal overflow on narrow screens; do not compress columns until critical content becomes unreadable.
- Do not recreate prototype Sidebar/TopBar in a business feature; reuse F-011 `ApplicationShell`.

## Reusable visual patterns

- Page header: title + concise context + relevant actions/status.
- Metric cards: label, precise value, optional supporting status; avoid decorative values unsupported by contracts.
- Status badges: text plus color/icon; never color-only.
- Data tables: deterministic columns, fixed numeric alignment, clear empty state.
- Analytical panels: bordered dark surfaces, restrained elevation, consistent spacing.
- Progress: explicit state label plus numeric progress when authoritative data exists.

## Market semantics

Green/red in the prototype communicates market/positive-negative meaning, but production status must always include text/icon semantics and must not rely on color alone.
