package io.openems.choco;
import org.chocosolver.solver.variables.IntVar;

public class EnergyGrid {

	public static class Buy {
		public IntVar power = null;
//		public IntVar cost = null;
	}

	public static class Sell {
		public IntVar power = null;
//		public IntVar revenue = null;
	}

	public final Sell sell = new Sell();
	public final Buy buy = new Buy();

	public IntVar power;

	public EnergyGrid() {

	}
}
