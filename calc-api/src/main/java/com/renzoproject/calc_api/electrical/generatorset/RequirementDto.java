package com.renzoproject.calc_api.electrical.generatorset;

/**
 * One Chapter 7 requirement to check off, mirroring calc-core's {@code GeneratorSetSizingResult.Requirement}.
 *
 * @param citation the PEC section it comes from
 * @param text     the requirement in words
 */
public record RequirementDto(String citation, String text) {

}
