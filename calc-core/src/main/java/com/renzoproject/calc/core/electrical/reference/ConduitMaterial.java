package com.renzoproject.calc.core.electrical.reference;

/**
 * Raceway (conduit) material a conductor is run in.
 *
 * <p>PEC Table 10.1.1.9 only splits reactance into two columns — "PVC/Aluminum Conduits"
 * combined vs. "Steel Conduit" — so {@link ConductorImpedanceTable} uses the same reactance
 * value for {@link #PVC} and {@link #ALUMINUM}. Resistance columns, however, are split into
 * three distinct values (PVC, Aluminum Conduit, Steel Conduit) and are looked up separately
 * per material.
 */
public enum ConduitMaterial {
	PVC,
	ALUMINUM,
	STEEL
}
