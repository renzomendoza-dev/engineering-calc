package com.renzoproject.calc.core.electrical.reference;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * One locked-rotor current entry, from either PEC Table 4.30.14.5(A) (single-phase) or
 * 4.30.14.5(B) (Design B/C/D polyphase — Design A has no published locked-rotor limit and so
 * has no data file at all, not a gap).
 *
 * <p>Deliberately excludes each source table's discriminator field ({@code phaseType} /
 * {@code designLetters}) since lookups go through separate table classes per source table,
 * not a shared enum-discriminated one; {@code @JsonIgnoreProperties} lets those source-only
 * fields pass through during deserialization without needing to be modeled here.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record LockedRotorEntry(String sizeLabel, int voltage, double lockedRotorAmps) {

}
