# Grouped dependency target-multiplicity label

Requirements and implementation plan (as settled before implementation) for showing the
target multiplicity of a grouped dependency (GD) box as a small text label next to the box.

## Requirements

1. When the GD has a target multiplicity set (`destMultiplicity` non-empty), a small label
   showing it is drawn **next to the box**.
2. When there is no target multiplicity, **no label** is drawn and the box looks exactly as
   it does today.
3. The label is placed on a **side that has no connections**, so it **rotates with the box
   orientation** (a grouped dependency's fan connections always attach on two opposite sides:
   the target/bulge side and the source/spine side; the two perpendicular sides stay free).
4. The label **touches the box without intersecting it**: gap of 0 between the box edge and the
   label for the labels above/below a left/right pointing box, and a 2 px gap for the labels on
   the perpendicular sides of an up/down pointing box (so the label does not hug the box there).
5. The box itself — its bounds, outline, "D"/"X" glyph and the fan attachment points — must
   look and behave exactly as it does today.
6. **Double-clicking the label** opens the target-multiplicity dialog (the same dialog the
   box opens today).
7. **Double-clicking the box itself no longer opens that dialog.**

## Label placement rule

The label side is the clockwise-perpendicular side from the target side:

| target side (D points) | label side |
|------------------------|------------|
| `RIGHT`                | above (TOP)    |
| `TOP`                  | right of box   |
| `LEFT`                 | below (BOTTOM) |
| `BOTTOM`               | left of box    |

The text itself always stays horizontal; only the side the label orbits to changes.

## Why a child figure directly in the box is not enough (clipping)

draw2d clips every figure to the **bounds of that figure's children**:
`Figure.paintChildren` builds the paint clip from `child.getBounds()` and the box figure is
itself only painted because its parent clipped to the box bounds. `Graphics.clipRect` only
ever shrinks the clip. A label placed *outside* the box's own rectangle would therefore be
clipped away entirely, even though it is a child of the box figure.

So the label must render **inside the box figure's own bounds**. The figure bounds are
therefore enlarged by a label strip on the label side, and the box's *visual box* is drawn
inset: the visual box keeps today's exact position and size, and the strip (unpainted
background + text) occupies the extra room.

## Implementation

### `seg.jUCMNav/src/seg/jUCMNav/figures/GroupedDependencyFigure.java`

- Add a child `Label` (created in the constructor with
  `ColorManager.LINKREFLABEL` foreground, invisible by default).
- `labelSideForTargetSide(int)` — the clockwise-perpendicular mapping above.
- `setLabelText(String)` — stores the text, sets/clears it on the label, shows/hides the
  label, then `recomputeSize()`.
- `getVisualBox()` — today's box rectangle (existing `getOrientationSize` dimensions at the
  box's unchanged corner, offset for top/left strips).
- `recomputeSize()` — figure size = visual box size plus, when the label is visible, the
  label's preferred width (left/right strip) or height (top/bottom strip) on the label side,
  with a 2 px `LABEL_GAP` added to the strips on the perpendicular sides (up/down pointing box).
- `layoutLabel()` — places the label abutted to the visual box edge (plus `LABEL_GAP` on the
  perpendicular sides), centered along it.
- `setTargetSide(int)` — keeps reporting/repainting/`fireAnchorsMoved()`, but routes the size
  change through `recomputeSize()` and calls `layoutLabel()`.
- `getLabelBounds()` — the label's bounds translated to absolute diagram coordinates, or
  `null` when the label is hidden (for double-click hit testing).
- `containsPoint(int, int)` — hit-testing override: the figure is only interactive where it
  paints, i.e. the visual box and (when visible) the label text itself. The rest of the
  enlarged strip the label runs in is empty background, so hovering/selecting/dragging there
  falls through to the diagram (and the empty ends of the top/bottom strips of an up/down
  pointing box are not interactive either). The label text stays a hit target so
  double-clicking it still reaches the edit part. The mouse coordinates arrive in the figure's
  parent space; because this figure defines no local coordinate system (`useLocalCoordinates()`
  is false) the label's bounds already live in that same space, so no shift is applied.
- `getHandleBounds()` — implements `org.eclipse.gef.handles.HandleBounds`, returning
  `getVisualBox()`. GEF sizes the gray drag-ghost preview (`NonResizableEditPolicy`), the focus
  rectangle, handle placement and snap-guide rectangles from this rectangle instead of the full
  figure bounds, so they stay box-sized and never grow to include the label strip. The visual
  box is in the same space as `getBounds()`, satisfying the `HandleBounds` contract.
- `fillShape`, `outlineShape`, `drawD`, `drawX` and both anchor inner classes
  (`GroupedDependencySideAnchor`, `GroupedDependencyFixedAnchor`) switch from `getBounds()`
  to `getVisualBox()`, so the painted box, glyph and fan anchor points are unchanged.

### `seg.jUCMNav/src/seg/jUCMNav/editparts/GroupedDependencyEditPart.java`

- `refreshVisuals()` — reordered to:
  1. `setLabelText(DependencyMultiplicity.toDisplay(getDef().getDestMultiplicity()))`
     (renders e.g. `[1..*]`; empty when there is no multiplicity; same formatting rule as
     the `LinkRefEditPart` multiplicity labels),
  2. impossibility check + `refreshFanLinkVisibility()` as today,
  3. `updateTargetOrientation()` (may flip the side, which moves the strip),
  4. set figure bounds from the box location + the figure's computed size,
  5. `layoutLabel()` + `validate()`.
  Refreshes on dialog/undo/redo already work: the edit part is an adapter on the grouped
  dependency definition (the auto/manual-orientation refresh fix), so a `destMultiplicity`
  SET notification reaches `refreshVisuals()`.
- `performRequest(Request)` — on `REQ_OPEN` (a double-click), open
  `openMultiplicityDialog()` **only** when a `SelectionRequest` location falls inside
  `getNodeFigure().getLabelBounds()`; a double-click on the box itself now does nothing.
  The `REQ_OPEN` branch returns either way so no other open behaviour is triggered.

### Messages / locale

- No new message keys: the dialog title/labels
  (`MultiplicityDialog.titleTarget`, `MultiplicityDialog.labelTarget`) and the label text from
  `DependencyMultiplicity.toDisplay` already exist.

## Verification

- Compile + fast gate (`mvn -B -o verify -pl seg.jUCMNav,seg.jUCMNav.tests -Dtest=...`),
  then the full two-module suite.
- Manual checks:
  - set a target multiplicity → `[x..y]` appears adjacent to the box on the free side;
  - clear it → label disappears and the box is pixel-identical to before;
  - rotate the box through all four orientations → the strip moves to the perpendicular free
    side, and the box, its D/X glyph and the fan attachment points are unchanged;
  - double-click the label → dialog opens; double-click the box → nothing happens.