package com.renzoproject.calc.core.mechanical.pipe;

/**
 * A pipe size identified by catalog lookup, resolved via {@link PipeDimensionResolver} before
 * the math runs.
 *
 * @param nominalLabel matched against a size row's machine-friendly {@code nominalSize} key in
 *                      the reference JSON (e.g. {@code "1/2"}, {@code "1-1/4"}) — NOT the
 *                      human-readable {@code nominalLabel} JSON field (e.g.
 *                      {@code "1/2\" (DN15)"}). Field is named to match this record's own
 *                      public API; see {@code reference/pipes/README.md} for the JSON-side
 *                      naming.
 */
public record NominalSize(String material, String schedule, String nominalLabel) implements DiameterSpec {

}
