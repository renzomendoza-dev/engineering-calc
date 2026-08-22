/**
 * Water storage sizing calculators: domestic (NPC-based) and fire protection (NFPA 13-based)
 * cistern/tank volume.
 *
 * <p>{@link com.renzoproject.calc.core.mechanical.storage.DomesticWaterStorageCalculator} and
 * {@link com.renzoproject.calc.core.mechanical.storage.FireWaterStorageCalculator} share this
 * package and its {@code reference/storage/} data folder but are otherwise unrelated -- neither
 * calls the other, and there is no shared base class. They also deliberately use different unit
 * conventions: domestic storage works in SI (litres, litres/second) like the rest of
 * {@code mechanical.pipe}, while fire water storage is GPM-native, matching
 * {@code mechanical.firepump}'s established NFPA convention (re-exposing
 * {@code FirePumpUnits.GPM} rather than converting to SI internally). Both report their final
 * volume as an Indriya {@code Quantity<Volume>} -- this package's first use of the standard JSR-385
 * {@code Volume} quantity type in calc-core (unlike {@code VolumetricFlowRate}, {@code Volume} is
 * part of the core quantity set, so no custom marker interface was needed).
 */
package com.renzoproject.calc.core.mechanical.storage;
