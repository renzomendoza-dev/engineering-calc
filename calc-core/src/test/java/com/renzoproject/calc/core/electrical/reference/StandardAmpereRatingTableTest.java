package com.renzoproject.calc.core.electrical.reference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

/** PEC 2017 Table 2.40.1.6(A) (printed pages 85-86) and the rule of 2.40.1.4(B) and (C) (pages 83-84). */
class StandardAmpereRatingTableTest {

	private final StandardAmpereRatingTable table = new StandardAmpereRatingTable();

	@Test
	void theTable_isAsPrinted() {
		assertEquals(List.of(15, 20, 25, 30, 35, 40, 45, 50, 60, 70, 80, 90, 100, 110, 125, 150, 175, 200, 225, 250, 300, 350,
				400, 450, 500, 600, 700, 800, 1000, 1200, 1600, 2000, 2500, 3000, 4000, 5000, 6000), table.ratings());
		assertEquals(List.of(1, 3, 6, 10, 601), table.additionalFuseRatings());
		assertFalse(table.ratings().contains(601), "601 A is a fuse rating only");
		assertEquals(800, table.nextHigherRatingMaximumAmps());
		assertTrue(table.standard().contains("2.40.1.6(A)"));
	}

	@Test
	void anAmpacityBetweenRatings_mayTakeTheNextOneUp() {
		assertEquals(30, table.largestPermittedRating(28, true));
		assertEquals(60, table.largestPermittedRating(55, true));
		assertEquals(80, table.largestPermittedRating(75, true));
		assertEquals(15, table.largestPermittedRating(12, true));
	}

	@Test
	void anAmpacityThatIsAStandardRating_takesThatRating() {
		// 2.40.1.4(B)(2): the next higher rating applies only where the ampacity does not correspond with one.
		assertEquals(30, table.largestPermittedRating(30, true));
		assertEquals(800, table.largestPermittedRating(800, true));
	}

	@Test
	void theNextRatingUp_isAllowedOnlyUpTo800Amperes() {
		// (B)(3): 790 A may take 800 A. Above 800 A, (C): the device may not exceed the ampacity.
		assertEquals(800, table.largestPermittedRating(790, true));
		assertEquals(800, table.largestPermittedRating(805, true));
		assertEquals(1000, table.largestPermittedRating(1100, true));
		assertEquals(6000, table.largestPermittedRating(7000, true));
	}

	@Test
	void aMultiReceptacleBranchCircuit_mayNotTakeTheNextRatingUp() {
		// (B)(1): the device may not exceed the ampacity, so the largest standard rating not above it.
		assertEquals(25, table.largestPermittedRating(28, false));
		assertEquals(30, table.largestPermittedRating(30, false));
		assertNull(table.largestPermittedRating(12, false), "no standard rating is 12 A or less");
	}

}
