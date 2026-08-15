package com.renzoproject.calc.core.mechanical.pump;

import com.renzoproject.calc.core.mechanical.pipe.PipePressureLossCalculator;
import com.renzoproject.calc.core.mechanical.pipe.PipePressureLossInput;
import com.renzoproject.calc.core.mechanical.pipe.PipePressureLossResult;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

/**
 * Stub {@link PipePressureLossCalculator} for {@link PumpTDHCalculatorTest}: returns a queue of
 * pre-canned results in call order, and records every {@link PipePressureLossInput} it was
 * actually invoked with — so tests can both (a) drive known head-loss values without coupling
 * to real friction-factor math, and (b) confirm {@link PumpTDHCalculator} genuinely delegates to
 * this calculator (correct per-segment fields) rather than reimplementing the math itself.
 *
 * <p>Subclasses the real {@link PipePressureLossCalculator} (constructor-injected with nulls,
 * since {@link #calculate} is fully overridden and never touches the inherited resolver fields)
 * rather than implementing a separate interface, because {@link PumpTDHCalculator}'s constructor
 * is explicitly typed to the concrete class — {@code PipePressureLossCalculator} has no
 * interface of its own in this codebase to substitute instead.
 */
class StubPipePressureLossCalculator extends PipePressureLossCalculator {

	private final Queue<PipePressureLossResult> results;
	private final List<PipePressureLossInput> capturedInputs = new ArrayList<>();

	StubPipePressureLossCalculator(List<PipePressureLossResult> results) {
		super(null, null, null);
		this.results = new ArrayDeque<>(results);
	}

	@Override
	public PipePressureLossResult calculate(PipePressureLossInput input) {
		capturedInputs.add(input);
		if (results.isEmpty()) {
			throw new IllegalStateException("Stub ran out of canned results -- test provided fewer results than calls made");
		}
		return results.poll();
	}

	List<PipePressureLossInput> capturedInputs() {
		return capturedInputs;
	}

}
