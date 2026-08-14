package com.renzoproject.calc.core.electrical.reference;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HorsepowerRatingTest {

	private static final double DELTA = 1e-9;

	@Test
	void parseHp_plainFraction() {
		assertEquals(0.25, HorsepowerRating.parseHp("1/4"), DELTA);
		assertEquals(1.0 / 3.0, HorsepowerRating.parseHp("1/3"), DELTA);
	}

	@Test
	void parseHp_mixedNumber() {
		assertEquals(1.5, HorsepowerRating.parseHp("1 1/2"), DELTA);
		assertEquals(7.5, HorsepowerRating.parseHp("7 1/2"), DELTA);
	}

	@Test
	void parseHp_wholeNumber() {
		assertEquals(200.0, HorsepowerRating.parseHp("200"), DELTA);
		assertEquals(1.0, HorsepowerRating.parseHp("1"), DELTA);
	}

	@Test
	void fromLabel_populatesBothFields() {
		HorsepowerRating rating = HorsepowerRating.fromLabel("1 1/2");

		assertEquals("1 1/2", rating.label());
		assertEquals(1.5, rating.hpValue(), DELTA);
	}

}
