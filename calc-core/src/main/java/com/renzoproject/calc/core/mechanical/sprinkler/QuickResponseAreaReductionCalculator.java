package com.renzoproject.calc.core.mechanical.sprinkler;

import com.renzoproject.calc.core.Calculator;

import java.util.ArrayList;
import java.util.List;

/**
 * Applies NFPA 13's design area reduction for quick-response sprinklers (Figure 19.3.3.2.3.1,
 * 2019 edition): up to 40 percent off the design area, shrinking with ceiling height and gone
 * above 20 ft.
 *
 * <p><b>What this does and does not do.</b> It reads the curve and reduces an area. It does not
 * decide whether the reduction is <i>permitted</i> -- the conditions in section 19.3.3.2.3 (system
 * type, hazard classification, extent of quick-response coverage, any floor on the reduced area)
 * are not encoded, and every result that applies a reduction says so. A reduction makes a design
 * less conservative, so that statement is not optional.
 *
 * <p>Separate from {@link SprinklerHydraulicCalculator} because the design area never enters the
 * hydraulics: demand is density times the coverage of the sprinklers the caller lists. This tells
 * a designer what area to build the remote area to; the sprinkler list is where it takes effect.
 */
public class QuickResponseAreaReductionCalculator
		implements Calculator<QuickResponseAreaReductionInput, QuickResponseAreaReductionResult> {

	private final QuickResponseAreaReductionResolver resolver;

	public QuickResponseAreaReductionCalculator(QuickResponseAreaReductionResolver resolver) {
		this.resolver = resolver;
	}

	@Override
	public QuickResponseAreaReductionResult calculate(QuickResponseAreaReductionInput input) {
		QuickResponseAreaReductionCurve curve = resolver.curve();
		List<String> notes = new ArrayList<>();

		double reductionPercent = 0.0;
		if (!input.quickResponseSprinklers()) {
			notes.add("No reduction: the figure covers quick-response sprinklers, and they were not stated.");
		} else {
			reductionPercent = curve.reductionPercentAt(input.ceilingHeightFt());
			if (reductionPercent == 0.0) {
				notes.add("No reduction: at " + format(input.ceilingHeightFt()) + " ft the ceiling is above the "
						+ format(curve.highCeilingFt()) + " ft the figure runs to, and it gives none beyond it.");
			}
		}

		boolean applied = reductionPercent > 0.0;
		if (applied) {
			notes.add("Reduction read from " + curve.standard() + ". The conditions that clause attaches to it "
					+ "-- system type, hazard classification, how widely quick-response sprinklers must be "
					+ "installed, and any minimum on the reduced area -- are not checked here. Confirm them "
					+ "against the printed section before using the reduced area.");
		}

		// Multiply before dividing: 1500 * (1 - 0.325) comes out as 1012.5000000000001, while
		// 1500 * 67.5 / 100 is exactly 1012.5, and these are figures people read and compare.
		double adjusted = input.designAreaSqFt() * (100.0 - reductionPercent) / 100.0;
		return new QuickResponseAreaReductionResult(
				input.designAreaSqFt(),
				input.ceilingHeightFt(),
				input.quickResponseSprinklers(),
				reductionPercent,
				adjusted,
				applied,
				curve.standard(),
				curve.confidence(),
				List.copyOf(notes));
	}

	private static String format(double value) {
		return value == Math.rint(value) ? Long.toString((long) value) : Double.toString(value);
	}

}
