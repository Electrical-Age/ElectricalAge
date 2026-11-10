#!/bin/sh
# Checks the spec against the code: the proofs, then the Lean limiter against the Java one.
# Needs lake (from elan) on the PATH, and what the Gradle build needs (see the top-level CLAUDE.md). A few minutes.
# Usage: fluidcore/spec/check.sh [--keep]    (--keep: use build/limiter-vectors.txt as it is, without running Java)
set -eu
cd "$(dirname "$0")"
spec=$(pwd)
root=$(cd ../.. && pwd)
out="$root/fluidcore/build"
vectors="$out/limiter-vectors.txt"
mkdir -p "$out"

if [ "${1:-}" != "--keep" ]; then
    # The Java tests write a sample of their Limiter calls: made-up lists, and the random networks with rings.
    # A second run with 2 scaling passes instead of 16 reaches the branches the first rarely does.
    echo "== recording Limiter calls from the Java tests"
    rm -f "$vectors"
    cd "$root"
    if ! { FLUIDCORE_VECTORS="$vectors" FLUIDCORE_LOOPS=1 ./gradlew --no-daemon --console=plain :fluidcore:test --rerun \
            --tests 'mods.eln.fluidcore.LimiterTest' --tests 'mods.eln.fluidcore.MixNetworkTest' \
            --tests 'mods.eln.fluidcore.NetworkTest.randomLevelNetworksConserveStayPositiveAndSettle' \
        && FLUIDCORE_VECTORS="$vectors" FLUIDCORE_LOOPS=1 FLUIDCORE_LIMITER_PASSES=2 ./gradlew --no-daemon --console=plain :fluidcore:test --rerun \
            --tests 'mods.eln.fluidcore.MixNetworkTest' \
            --tests 'mods.eln.fluidcore.NetworkTest.randomLevelNetworksConserveStayPositiveAndSettle'; } > "$out/spec-vectors.log" 2>&1; then
        tail -n 30 "$out/spec-vectors.log"
        echo "FAILED: the Java tests"
        exit 1
    fi
    cd "$spec"
fi

echo "== proofs"
# (Mathlib's prebuilt files, rather than building it here)
lake exe cache get > "$out/spec-cache.log" 2>&1
if ! lake build FluidSpec limitercheck > "$out/spec-build.log" 2>&1; then
    grep -v "consider adding" "$out/spec-build.log" | tail -n 40
    echo "FAILED: the proofs or the worked examples do not check"
    exit 1
fi
if grep -q "declaration uses .sorry." "$out/spec-build.log"; then
    echo "FAILED: an unfinished proof"
    exit 1
fi
if ! lake env lean FluidSpec/Audit.lean > "$out/spec-axioms.txt"; then
    cat "$out/spec-axioms.txt"
    echo "FAILED: the list of theorems does not check"
    exit 1
fi
# Every theorem may rest on Lean's three standard axioms and nothing else (an unfinished proof shows as sorryAx).
if grep -v "^'.*' depends on axioms: \[propext, Classical.choice, Quot.sound\]$" "$out/spec-axioms.txt" \
    | grep -v "^'.*' does not depend on any axioms$" | grep -v "^'.*' depends on axioms: \[propext\(, Quot.sound\)\?\]$" | grep .; then
    echo "FAILED: a theorem rests on something else (above)"
    exit 1
fi
echo "$(grep -c . "$out/spec-axioms.txt") theorems, standard axioms only"

echo "== Lean limiter against Java, bit for bit"
lake exe limitercheck "$vectors"

echo "== control: the same with one division done as a multiplication by the inverse (must differ)"
if lake exe limitercheck "$vectors" --control > "$out/spec-control.txt"; then
    echo "FAILED: the comparison did not notice the changed arithmetic"
    exit 1
fi
tail -n 1 "$out/spec-control.txt"
if ! tail -n 1 "$out/spec-control.txt" | grep -q "mismatches [1-9]"; then
    echo "FAILED: the control stopped for another reason"
    exit 1
fi
echo "spec: all checks passed"
