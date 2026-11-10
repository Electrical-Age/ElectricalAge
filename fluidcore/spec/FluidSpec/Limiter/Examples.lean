import Mathlib
import FluidSpec.Limiter.Impl
/-
The limiter run on exact fractions, on small cases worked by hand: a check that the definitions say what they are
meant to say. Nodes have one mass and the energy (C = 2), so node n's mass is cell 2n and its energy cell 2n + 1.
-/
namespace FluidSpec.Limiter.Examples
open FluidSpec.Limiter

def mass (a b : Nat) (x : ℚ) (edge : Option Nat := none) : Move ℚ := ⟨some a, some b, 0, x, edge⟩

def energy (a b : Nat) (x : ℚ) (edge : Option Nat := none) : Move ℚ := ⟨some a, some b, 1, x, edge⟩

def start (l : List ℚ) (i : Nat) : ℚ := l.getD i 0

def cells (n : Nat) (f : Nat → ℚ) : List ℚ := (List.range n).map f

def go (nodes : Nat) (y0 : List ℚ) (moves : List (Move ℚ)) : List ℚ × List ℚ × Nat :=
  let r := apply 2 (2 * nodes) 4 1 16 64 (start y0) moves
  (cells (2 * nodes) r.1, cells (2 * nodes) r.2.1, r.2.2)

/-- Nothing to limit: a has 5 and sends 3. -/
example : True := trivial
#guard go 2 [5, 0, 0, 0] [mass 0 1 3] = ([2, 0, 3, 0], [1, 1, 1, 1], 0)

/-- A transit node that over-sends: b has 1, receives 3 and is asked for 7; it sends the 4 it has. -/
example : True := trivial
#guard go 3 [5, 0, 1, 0, 0, 0] [mass 0 1 3, mass 1 2 7] = ([2, 0, 0, 0, 4, 0], [1, 1, 4/7, 1, 1, 1], 1)

/--
The case that left a mass at -0.83 before: a and b, both empty, send each other 10 and b sends 1 on to c. Cutting
never settles (each cut of one undoes the other's), so both are silenced, and nothing moves.
-/
example : True := trivial
#guard go 3 [0, 0, 0, 0, 0, 0] [mass 0 1 10, mass 1 0 10, mass 1 2 1] = ([0, 0, 0, 0, 0, 0], [0, 1, 0, 1, 1, 1], 18)

/--
Silenced, then raised: the same ring, with half a kilogram reaching b from d. In the exact answer a and b each send
half of what they were asked, and c gets the 0.5. Cutting approaches that from above without arriving, so both are
silenced; raising then approaches it from below, a and b in turn. After the 64 passes c gets 0.476 (95 %), the rest
stays in b, and nothing is negative or lost.
-/
def ring : List ℚ × List ℚ × Nat :=
  go 4 [0, 0, 0, 0, 0, 0, 5, 0] [mass 0 1 10, mass 1 0 10, mass 1 2 1, mass 3 1 (1/2)]

#guard ring.2.2 = 18
#guard ring.1.all (0 ≤ ·)
#guard ring.1.sum = 5
#guard 476/1000 < ring.1.getD 4 0 ∧ ring.1.getD 4 0 < 477/1000
#guard ring.1.getD 6 0 = 9/2

/--
Energy follows the mass: a has 1 kg and is asked for 4 on edge 0, which also carries 100 J with the flow; 7 J are
conducted beside it. A quarter of the mass goes, so a quarter of the carried energy, and all of the conducted.
-/
example : True := trivial
#guard go 2 [1, 500, 0, 0] [mass 0 1 4 (some 0), energy 0 1 100 (some 0), energy 0 1 7]
  = ([0, 468, 1, 32], [1/4, 1, 1, 1], 1)

end FluidSpec.Limiter.Examples
