# Syncing type and instance models

Status: **idea/research only — not the current goal.** Written down for later
reference. No implementation yet, no commits.

Related shipped work: commit `0eea8619` ("feat(grl): generate instance models
and jump back to their type model", on `master`, not pushed) introduced the
**copy-based** "Generate Instance Model" feature. This document explores the
follow-up idea: an instance model that **shares definitions** with its type
model so edits sync automatically.

## The feature idea

Today "Generate Instance Model" deep-copies every actor / intentional element
/ element link definition into fresh objects (`GenerateInstanceModelCommand`,
`copyActor` / `copyIntentionalElement` / `copyElementLink`). The two models are
fully independent: editing the type model does not update the generated
diagram, and evaluating the type model does not evaluate the instance model.

The proposed feature: a **linked instance model** mode where the generated
diagram's refs point at the *same* definitions as the type model (shared
`Actor` / `IntentionalElement` / `ElementLink` definitions). Edits made to the
type model propagate into the generated diagram automatically, and evaluation
values are identical everywhere. This gives the fast type-model round-trip
workflow: iterate on definitions, see every linked diagram update live.

## Research results (verified against the code)

### The metamodel already supports it

- `GRLspec` owns all definitions globally: `actors`, `intElements`, `links`
  (`model/grl.ecore`).
- A `GRLGraph` contains only **refs**: `contRefs`, `nodes`, `connections`
  (`urncore.ecore:113-118`).
- Every ref→def pointer is **non-containment** with an unbounded opposite:
  `ActorRef.contDef` → `Actor.contRefs`, `IntentionalElementRef.def` →
  `IntentionalElement.refs`, `LinkRef.link` → `ElementLink.refs`. No
  uniqueness constraints, no containment clash (EMF only forbids two
  containers, and defs have one, refs have one). Nothing forbids two diagrams
  referencing the same definition.

### Layout is per-ref, so stacked diagrams keep independent layouts

- Position lives on the refs: `x/y/width/height` on `IURNContainerRef` /
  `IURNNode`; IE width/height in `WIDTH`/`HEIGHT` metadata.
- Link bendpoints live on the **`LinkRef`** (`LinkRef.bendpoints`, containment);
  endpoints are refs. `ElementLink` definitions carry no geometry.

### Rendering refresh is already wired for shared definitions

Ref edit parts register `eAdapter`s directly on the *definition*:

- `IntentionalElementEditPart.activate()` → `getDef().eAdapters().add(this)`
- `LinkRefEditPart.activate()` → the shared `ElementLink` as well
- `ActorRefEditPart.activate()` → `contDef`
- `LabelEditPart` (drives `ConnectionLabelEditPart`) → also `getLink()`

Consequences:
- name change → text updates in all open editors;
- type / decompositionType change → icon, AND/OR/XOR label, colors, and
  strategy re-evaluation fire everywhere;
- contribution strength / correlation change → connection label recomputes live.
- `MultiPageTabManager.createPages()` creates all page editors up-front, so
  definitions changes reach inactive pages too.

### The "doesn't update until moved" stall is a narrow, pre-existing bug

The **Outline / navigator tree icon** caches after a type change:

- `IntentionalElementRefTreeEditPart.getImage()` only recomputes while
  `super.getImage() == null` (`:72`), and the `setImage(null)` invalidation is
  commented out in `notifyChanged` (`:116-121`).
- Diagram figures update fine; only the tree icon stays stale.
- A shared-definition world would hit this constantly — fix it first.

Note: the "reused element" icon branch in `getImage()` only triggers when the
def lives in a *different* URN spec (`:73`). Same-model linked refs stay on the
normal icon path.

### Evaluation is definition-keyed — all diagrams show the same values

- `EvaluationStrategyManager` keys `evaluations` by `IntentionalElement`
  (the def), `PropagationGRLStrategyAlgorithm` iterates
  `GRLspec.getIntElements()`.
- Value displayed on any ref = `evaluations.get(ref.getDef())`.
- Actor values are keyed by the `Actor` def.
- There is **no per-ref / per-"instance" concept** (`StrategicActorInstance`
  has no matches in the codebase).

So with shared defs, every diagram shows identical, single-evaluated values
automatically, and a definition edit triggers one re-evaluation +
`refreshDiagrams` across all diagrams. This is the fast round-trip the idea
wants — but it also means a linked instance model **cannot diverge
semantically** from the type model. That is inherent to sharing.

## Proposed implementation plan (for later)

1. **Fix the outline/navigator icon cache** (`IntentionalElementRefTreeEditPart`):
   re-enable the `setImage(null)` invalidation in `notifyChanged` (dispose old
   image, null it, fall through to `refreshVisuals` → `getImage` recompute).
   Add a type-change regression test. Worth doing regardless of the feature.

2. **Add a linked mode to `GenerateInstanceModelCommand`** (a
   `linkDefinitions` flag or sibling command; flag recommended — `redo`,
   `analyze`, marker logic, and the marker-before-`specDiagrams.add` ordering
   are all shared):
   - create the new `GRLGraph`, new **refs** only (`ActorRef`→same `Actor` def,
     `IntentionalElementRef`→same def, `LinkRef`→same `ElementLink`);
   - copy geometry from the source refs (or auto-layout);
   - keep the instance marker metadata (`INSTANCE_MODEL`,
     `INSTANCE_MODEL_SOURCE`), the orange icon, and `goToTypeModel`;
   - `undo()` / `testPostConditions()` must become mode-aware: linked mode
     creates no defs, so the `createdIntElements.size()` postcondition must
     branch.

3. **Dialog** (`GenerateInstanceModelDialog`): radio pair "Copies / Linked".
   In linked mode the per-actor count becomes layout-only (N refs to one def,
   all visually identical).

4. **Action** (`GenerateInstanceModelAction`): pass the flag through; one new
   message key EN+FR.

5. **Tests** (extend `GenerateInstanceModelCommandTest`):
   - `assertSame` on defs between source and linked graph (actors, IEs, links);
   - per-diagram geometry independence;
   - cross-diagram refresh: def edit → EMF adapter → both diagrams repaint;
   - mode-aware undo; marker save/load round-trip;
   - copy-mode regression (existing tests stay green).

6. **Gate**: `xvfb-run -a mvn -B clean verify`.

## Open decisions (not yet answered)

1. **Both modes or replace?** Recommended: keep copy mode, add linked mode
   behind a toggle.
2. **Counts in linked mode**: keep the spinner as layout-only multiplicity, or
   drop it.
3. **Semantic consequence**: linked == single evaluation everywhere, no
   divergence from the type model. Confirm this is acceptable for the intended
   fast round-trip use.

## Known risk

Deleting a type-model definition (or whole type model) referenced by a linked
diagram leaves dangling refs; EMF cleans the opposite ref→def but not
geometry-dependent UI assumptions. Copy mode has no such coupling. Options:
(a) document as a v1 limitation (recommended), or (b) add a delete-guard that
finds linked diagrams via `INSTANCE_MODEL_SOURCE` and refuses/notifies.

## Key files

- `seg.jUCMNav/src/seg/jUCMNav/model/commands/create/GenerateInstanceModelCommand.java`
- `seg.jUCMNav/src/seg/jUCMNav/editparts/treeEditparts/IntentionalElementRefTreeEditPart.java`
- `seg.jUCMNav/src/seg/jUCMNav/views/dialogs/GenerateInstanceModelDialog.java`
- `seg.jUCMNav/src/seg/jUCMNav/actions/GenerateInstanceModelAction.java`
- Definitions: `model/grl.ecore`, `model/urncore.ecore`
- Evaluation: `seg/jUCMNav/strategies/EvaluationStrategyManager.java`,
  `PropagationGRLStrategyAlgorithm.java`
- Renderers/adapters: `editparts/IntentionalElementEditPart.java`,
  `LinkRefEditPart.java`, `ActorRefEditPart.java`, `LabelEditPart.java`,
  `editors/MultiPageTabManager.java`
- Tests: `seg.jUCMNav.tests/src/seg/jUCMNav/tests/model/GenerateInstanceModelCommandTest.java`