# Prototype Source — Read-Only UI Evidence

This directory contains selected source files copied from the `crypto-strategy-lab` prototype so Spec Kit and reviewers can inspect the visual/component structure without importing the prototype application architecture into production.

## Included

- five main page components;
- shared/common presentational components;
- market/strategy/backtest/search/news components;
- prototype navigation/layout source for visual reference;
- relevant TypeScript type declarations;
- prototype CSS tokens/styles;
- small presentation utility helpers.

## Intentionally excluded

- `App.tsx` and `main.tsx`;
- Vite configuration/package/build wiring;
- prototype `AppContext` business simulation;
- mock data and timer-driven execution state;
- mock Search/Backtest orchestration;
- experimental AI Strategy workspace not currently part of the canonical shared screen map.

## Rule

Files here do not need to compile and MUST NOT be imported by `apps/web`.

When a prototype component conflicts with the released API or F-011 foundation, the released production contract wins.
