package com.renzoproject.calc_api.electrical.generatorset;

/**
 * How load reaches an optional standby source (PEC 7.2.1.4(B)).
 *
 * @param key      what the request's {@code optionalTransfer} carries
 * @param label    the mode in words
 * @param citation the section that sets its capacity rule
 * @param text     the rule
 */
public record OptionalTransferOptionDto(String key, String label, String citation, String text) {

}
