package io.openems.choco;
import org.chocosolver.solver.variables.IntVar;

public class EnergyStorage {

	public static class Charge {
		public IntVar power = null;
	}

	public static class Discharge {
		public IntVar power = null;
	}

	public final Charge charge = new Charge();
	public final Charge discharge = new Charge();

	public IntVar power = null;
	public IntVar energy = null;

	public EnergyStorage() {
	}

}
