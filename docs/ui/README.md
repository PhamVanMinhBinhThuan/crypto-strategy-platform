# Crypto Strategy Platform — Shared UI Reference

`docs/ui/` is the shared visual and interaction reference for every browser UI feature in this repository.

This directory is intentionally feature-independent. F-012, F-013, F-014 and future frontend features should reference the same approved screens and prototype evidence instead of copying UI artifacts into each `specs/<feature>/` directory.

## Authority order

When two artifacts disagree, use this order:

1. `.specify/memory/constitution.md` and accepted ADRs.
2. Released capability/public contracts.
3. F-009 REST/WebSocket contracts and released OpenAPI/event documentation.
4. F-011 Frontend Foundation contract.
5. The current feature specification and clarified decisions.
6. The UI reference in this directory.

The UI reference is never business truth. It may guide layout, visual hierarchy, interaction placement and presentation states, but it must not invent backend fields, lifecycle transitions, calculations, authorization rules or transport semantics.

## Directory map

```text
docs/ui/
├── README.md                  # entry point for humans and Spec Kit
├── spec-kit-reference.md      # mandatory shared instructions for UI features
├── screen-map.md              # screen -> route -> feature owner
├── design-system.md           # shared visual language
├── interaction-states.md      # loading/error/degraded/realtime states
├── stitch-guide.md            # original Stitch workflow/design guidance
├── screens/                   # approved/reference screenshots
├── prototype/                 # read-only source reference from UI prototype
└── features/
    ├── F-012.md               # F-012 mapping to shared UI
    ├── F-013.md               # F-013 mapping to shared UI
    └── _template.md           # mapping template for future UI features
```

## Shared screens

| UI ID | Screen | Production route | Feature owner | Screenshot |
|---|---|---|---|---|
| UI-01 | Market Dashboard | `/market` | F-012 | `screens/Market Dashboard.png` |
| UI-02 | Strategy Composer | `/strategies` | F-012 | `screens/Stragegy Composer.png` |
| UI-03 | Backtest Results | `/backtests` | F-013 | `screens/Backtest Results.png` |
| UI-04 | Search & Leaderboard | `/search` | F-013 | `screens/Search & Leaderboard.png` |
| UI-05 | News Sentiment | `/news` | F-012 | `screens/New Sentiment.png` |

## What Spec Kit may learn from the prototype

Use `prototype/` for:

- layout and information hierarchy;
- card/panel/table structure;
- button and control placement;
- status badge placement;
- relative density and spacing;
- chart/table presentation intent;
- page-to-page visual consistency.

Do **not** copy prototype application architecture into production. In particular, do not use prototype code as authority for API calls, WebSocket behavior, authentication, Search orchestration, Backtest execution, Evaluation, Ranking or persistence.

## Production frontend rule

Production UI lives in `apps/web` and must reuse F-011 foundation boundaries. Prototype files under `docs/ui/prototype/` are design evidence only and are not part of the production build.

Before any frontend `/speckit-*` command, read `spec-kit-reference.md` and the relevant file under `features/`.
