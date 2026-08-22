package com.renzoproject.calc.core.mechanical.duct;

import com.renzoproject.calc.core.exception.CalculationException;

import java.util.List;

/**
 * Resolves the recommended maximum airflow velocity (m/s) by duct location, acoustic rating
 * (NC/RC), and duct shape -- acoustic design targets, not code-mandated limits (see
 * {@code reference/duct/README.md}).
 *
 * <p>NOT called internally by {@link DuctSizingCalculator} -- {@code maxVelocity} is a direct
 * input to that calculator (same design as {@code PipeVelocityCalculator}, which likewise never
 * calls a velocity-limit resolver itself). This resolver exists purely so an API/frontend layer
 * can offer a suggested default before the caller submits a {@code VELOCITY}-mode request.
 */
public interface DuctVelocityLimitResolver {

	/**
	 * @throws CalculationException if no matching row is found for the given
	 *                               {@code ductLocation}/{@code ncRcRating} combination
	 */
	double resolveMaxVelocity(String ductLocation, int ncRcRating, DuctShape shape);

	/**
	 * All raw rows, as published. Intended for displaying the table itself (e.g. a frontend
	 * reference table), as opposed to {@link #resolveMaxVelocity} which resolves a single
	 * location/rating/shape combination.
	 */
	List<DuctVelocityLimitRow> allEntries();

}
