package com.renzoproject.calc.core.mechanical.standpipe;

import com.renzoproject.calc.core.exception.CalculationException;
import com.renzoproject.calc.core.mechanical.HazenWilliams;
import com.renzoproject.calc.core.mechanical.pipe.PipeDimensionResolver;

/**
 * The pipe of a standpipe segment: its actual inside diameter, its fittings' equivalent length
 * (rescaled for the segment's C and for a bore other than Schedule 40 steel), and its nominal size
 * read as inches. Part of {@link StandpipeHydraulicCalculator}.
 */
final class StandpipePipeData {

	private final PipeDimensionResolver dimensionResolver;
	private final StandpipeReferenceResolver referenceResolver;

	StandpipePipeData(PipeDimensionResolver dimensionResolver, StandpipeReferenceResolver referenceResolver) {
		this.dimensionResolver = dimensionResolver;
		this.referenceResolver = referenceResolver;
	}

	double insideDiameterInch(StandpipeSegment segment) {
		return dimensionResolver.resolve(segment.pipeMaterial(), segment.schedule(), segment.nominalSize())
				.internalDiameter().to(StandpipeUnits.INCH).getValue().doubleValue();
	}

	/**
	 * Equivalent length of the segment's fittings, ft: the chart's value rescaled for the segment's C
	 * (11.3.2.2, which is {@code (C/120)^1.85}) and for an inside diameter other than Schedule 40
	 * steel (11.3.2.5, {@code (actual ID / Schedule 40 ID)^4.87}). An explicit length is used as given.
	 */
	double fittingsLengthFt(StandpipeSegment segment, double insideDiameter) {
		double total = 0;
		for (StandpipeFitting fitting : segment.fittings()) {
			if (fitting.explicitEquivalentLengthFt() != null) {
				total += fitting.quantity() * fitting.explicitEquivalentLengthFt();
				continue;
			}
			StandpipeFittingRow row = referenceResolver.resolveFitting(fitting.fittingType());
			Double published = row.equivalentLengthFtByNominalSize().get(segment.nominalSize());
			if (published == null) {
				throw new CalculationException("segment " + segment.label() + ": " + row.label() + " has no published equivalent length for "
						+ segment.nominalSize() + " in.; give an explicit length");
			}
			double cAdjustment = HazenWilliams.equivalentLengthAdjustmentFactor(segment.cFactor(), referenceResolver.cFactorBasis());
			total += fitting.quantity() * published * cAdjustment * diameterAdjustment(segment, insideDiameter);
		}
		return total;
	}

	private double diameterAdjustment(StandpipeSegment segment, double insideDiameter) {
		boolean steelSchedule40 = (segment.pipeMaterial().equalsIgnoreCase("BI") || segment.pipeMaterial().equalsIgnoreCase("GI"))
				&& "SCH40".equalsIgnoreCase(segment.schedule());
		if (steelSchedule40) {
			return 1.0;
		}
		double schedule40 = dimensionResolver.resolve("BI", "SCH40", segment.nominalSize())
				.internalDiameter().to(StandpipeUnits.INCH).getValue().doubleValue();
		return HazenWilliams.diameterAdjustmentFactor(insideDiameter, schedule40);
	}

	/** "2-1/2" gives 2.5, "4" gives 4, "3/4" gives 0.75. */
	static double nominalInches(String nominalSize) {
		try {
			double total = 0;
			for (String part : nominalSize.trim().split("-")) {
				if (part.contains("/")) {
					String[] fraction = part.split("/");
					total += Double.parseDouble(fraction[0]) / Double.parseDouble(fraction[1]);
				} else {
					total += Double.parseDouble(part);
				}
			}
			return total;
		} catch (RuntimeException e) {
			throw new CalculationException("Cannot read the nominal size: " + nominalSize);
		}
	}

}
