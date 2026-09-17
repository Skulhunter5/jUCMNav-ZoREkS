# GD evaluation plan

General description of the final plan (as settled after deliberation) for implementing
evaluation of grouped dependencies (GD) in the GRL strategy propagation.

## Terminology (consistent with ordinary dependencies)

- **source** = the drawing-start end of a dependency (the goals that may be *restricted*).
- **target** = the drawing-end end (the goals that are *depended on*; their satisfaction
  gates the multiplicity).

Both ordinary and grouped dependencies follow this rule: **source goals are restricted based on
target goals** ("a source depends on a target to be satisfied"). In model terms a hand-drawn
`Dependency` has `src` = drawing-end and `dest` = drawing-start (see
`AddDependencyElementLinkCommand`): an element holding a link in `linksDest` (its drawing-start
side) is clipped by the evaluation of `link.getSrc()`. Generated instance-model fans follow the
same convention, so a denied box restricts the source-side copies while the target-side copies
keep their own evaluation.

## 1. Model

- `grl.GroupedDependency` extends `GRLLinkableElement`: a hub that enters the strategy
  evaluation order like any other linkable element. It carries one attribute,
  `destMultiplicity` (a plain string), plus a `refs` list.
- `grl.GroupedDependencyRef` extends `GRLNode`: an inline graph node that points back
  to its `GroupedDependency` definition (`def`, required, inverse of `refs`). It gives
  the diagram a concrete place to draw a group fan while the hub itself holds the
  multiplicity and the incoming links.

## 2. Multiplicity semantics

- `destMultiplicity` uses the canonical form `x..y`; either bound may be `*` (unbounded);
  the end has no multiplicity when the string is empty/absent. As for an ordinary dependency it
  labels the target (drawing-end) side: it names how many target instances the group may depend
  on, and it is what restricts the source side when unmet.
- All parsing, validation and display live in one helper,
  `seg.jUCMNav.model.util.DependencyMultiplicity` (`isValid`, `parseBounds`, `toDisplay`,
  `normalizeStored`).
- A multiplicity with `lower > upper` is "unsatisfiable" — it demands more satisfied
  contributors than exist. Only instance-model generation can produce one (clamping the
  upper bound to the copy count). The diagram renders these as impossible boxes with an
  "X" and hides the box-to-target fans.

## 3. Scheduling inside a strategy algorithm

- A GD hub is scheduled like an intentional element, with one difference in *when* it may
  run: it becomes eval-ready only after every element on the far (src) end of its incoming
  fans (`linksDest`, the box's target-side fans) has been evaluated, i.e. it waits on the
  target instances that will satisfy it, not on its outgoing fans. This mirrors how a node
  waits for all contributions: target instances (leaves) -> box -> source-side copies.
- A ready hub is handed to the model evaluation loop as a `GRLLinkableElement`.
- Generated fan links follow the ordinary-dependency convention (`src` = drawing-end,
  `dest` = drawing-start): a source copy holds its box fan in `linksDest`, so the standard
  dependency semantics clip the source copy to the box's value; the box holds its target
  fans in `linksDest`, so the multiplicity counts the target instances. The drawn
  `LinkRef` direction stays source -> box -> target.

## 4. Evaluation dispatch and the algorithm hook

- `EvaluationStrategyManager.evaluateModel()` routes every `GroupedDependency` it pops to a
  per-algorithm hook, `IGRLStrategyAlgorithm.getGroupedDependencyEvaluation(strategy,
  evaluations, groupedDependency)`, instead of the generic `getEvaluation(element)` used for
  intentional elements. The hook's contract is a result in [-100, 100].
- The interface provides a default that returns `SATISFICED` (a GD with no algorithm-specific
  handling is simply satisfied). Algorithms that care override it.
- The returned value is stored in the shared `evaluations` map as an `Evaluation` and synced
  to the element's qualitative evaluation, exactly like intentional-element results, so
  previews/colour painting work uniformly.

## 5. Propagation hook semantics (the decision that ends deliberation)

- The multiplicity is treated as a **hard boolean constraint** on the count of satisfied
  target-side instances, not as a weighted aggregation:
  - the box counts, across its `linksDest` fans, how many target instances (each fan's
    `src`, i.e. the drawing-end copies) evaluate `> 0`;
  - no (or invalid) multiplicity: `SATISFICED`;
  - fewer satisfied targets than `lower`, or more than `upper`: `scaleMin` (fully denied, -100);
  - otherwise: `SATISFICED`.
- The box's result then restricts the source side exactly like an ordinary dependency: each
  source copy holds its box fan in `linksDest` and `getEvaluation` clips it to the box's
  value (dependency min-semantics). A denied box therefore drives the source goals (and any
  decompositions above them) to `scaleMin`, while target copies keep their own evaluation.
- This "placeholder semantics" (met or not met) is a deliberate choice: the numeric value
  carries no intermediate information, only a threshold decision. `PropagationGRLStrategyAlgorithm`
  implements exactly this.

## 6. Diagram rendering

- Each GD renders as a box (`GroupedDependencyFigure`); the multiplicity is shown as `[x..y]`
  via `toDisplay`.
- Visual state is two-valued: satisfiable boxes draw their target fans, impossible boxes draw
  an "X" with the fans hidden (`refreshVisuals` flips between the two).
- Box orientation: auto-orientation responds only to moves of the instances on the far end of
  the fans; a manual orientation override is stored as element metadata on the definition.

## 7. Tests

- `DependencyMultiplicityTest` covers parsing, validation and display.
- `GenerateInstanceModelCommandTest` covers the generation-time clamping and the
  impossible-box case it produces.
- Strategy-level coverage exercises scheduling (a hub waits for its contributors) and the
  hook result.
- `GroupedDependencyEvaluationTest` covers the corrected direction: a denied box restricts
  the source-side copies (and the gates above them) to `scaleMin` while target copies keep
  their own evaluation, and a satisfied box imposes no restriction on the source side.

## 8. Open work / non-goals

- No algorithm-specific refinement of the hook beyond propagation semantics (other algorithms
  inherit the default `SATISFICED`).
- No change to instance-model generation *semantics*: the clamping that can yield an
  unsatisfiable multiplicity is intentional and its visual signal is the impossible box.
  Generation does orient each fan like a hand-drawn dependency (semantic `src` = drawing-end,
  `dest` = drawing-start) so ordinary and grouped dependencies share one source/target naming.