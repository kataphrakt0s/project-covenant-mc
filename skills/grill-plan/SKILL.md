---
name: grill-plan
description: Work through Project Covenant design decisions one question at a time, checking the existing game model and recording agreed decisions. Use when the user asks to grill a plan, says "grill me", or wants to walk down a design tree.
---

# Grill Plan

Adapted from the shared grill-plan skill for this repository's documentation.

## Before asking

Read `AGENTS.md`, `CONTEXT.md`, and the foundational GDD at
`Project-Covenant/Project Covenant - GDD.md`. Follow the relevant system notes and
inspect the current implementation. Answer questions from the code when possible
instead of asking the user to explain existing behavior.

Distinguish implemented behavior, roadmap ideas, and newly agreed requirements.
Call out disagreements between the user's description, documentation, and code
without treating the current implementation as a constraint on their design intent.

## Work down the design tree

Ask one question at a time and wait for the answer before asking the next.
Include a recommended answer and its concrete trade-off. Resolve decisions that
other choices depend on first; follow each branch until behavior and scope are
clear enough to implement.

Use concrete gameplay scenarios to sharpen vague terms: what the player sees,
what triggers the behavior, who else sees it, and what happens when the action
fails or is interrupted. Offer alternatives only when they materially change the
design. Reuse accepted answers rather than repeatedly asking for confirmation.

Keep the discussion within the user's requested scope. Challenge assumptions
against existing class identities, resource rules, and player expectations. Do not
invent new systems simply to make a prototype more general.

## Record decisions as they settle

Update the relevant existing design document as each decision is agreed, such as
`docs/RPG_SKILLS.md` and the matching Obsidian system note. Clearly distinguish
planned behavior from implemented behavior. Keep open questions in the discussion
until resolved rather than turning documentation into an interview transcript.

Preserve this project's `CONTEXT.md` structure: it contains current scope,
ownership, toolchain, and workflow information, not only a glossary. Update it
when a settled decision changes those contracts; do not replace it with the shared
skill's generic glossary template. Use consistent domain terms in the existing docs.

Create an ADR in `docs/adr/` only for a decision that is costly to reverse,
surprising without its rationale, and the result of a real trade-off. A short
paragraph stating the context, decision, and reason is sufficient. Number new
ADRs after the highest existing entry and create the directory only when needed.

## Handoff to implementation

When the relevant branches are resolved, summarize the agreed behavior and
acceptance criteria. Continue with implementation when the user's instructions
authorize it, following `skills/covenant-development/SKILL.md`. Grilling itself
does not require a new approval step for work already authorized.
