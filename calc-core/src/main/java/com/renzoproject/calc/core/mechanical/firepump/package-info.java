/**
 * Fire pump sizing calculators (NFPA 20-aligned): demand determination, standard capacity
 * rounding, pump curve compliance validation, and driver power/motor sizing.
 *
 * <p><b>Units: this package works in psi and GPM (US fire-protection convention) throughout,
 * not the SI-first convention used elsewhere in this application.</b> The electrical
 * calculators and {@code mechanical.pipe}'s {@code PipeVelocityCalculator} all compute in SI
 * internally. This is a deliberate choice for this package specifically, not an
 * inconsistency: NFPA 20 — the entire source of truth here — is itself written in psi/GPM/ft,
 * and every published pump curve, standard capacity table, and industry rule-of-thumb constant
 * (head = psi * 2.31, water horsepower = GPM * head / 3960) is expressed that way. Converting
 * to SI internally would mean re-deriving those constants for no benefit, and would make
 * cross-checking this code against an NFPA 20 table or a manufacturer's pump curve sheet
 * (both published in psi/GPM) needlessly error-prone.
 *
 * <p>Indriya quantity types are still used throughout — callers can supply values in whatever
 * unit is convenient, same as {@code mechanical.pipe}. psi and GPM are this package's internal
 * working units and canonical result units, exposed via
 * {@link com.renzoproject.calc.core.mechanical.firepump.FirePumpUnits}.
 */
package com.renzoproject.calc.core.mechanical.firepump;
