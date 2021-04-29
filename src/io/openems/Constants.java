package io.openems;

public class Constants {

	public final static int NO_OF_PERIODS = 24;
	public final static int MINUTES_PER_PERIOD = 60;

	/**
	 * Grid Feed-In Limit, e.g. 70 % law
	 */
	public final static int GRID_SELL_LIMIT = 30000;

	public final static int GRID_BUY_LIMIT = 30000;

	public final static int ESS_INITIAL_ENERGY = 6000; // [Wh]
	public final static int ESS_MIN_ENERGY = 0; // [Wh]
	public final static int ESS_MAX_ENERGY = 12000; // [Wh]
	public final static int ESS_MAX_CHARGE = 9000; // [W]
	public final static int ESS_MAX_DISCHARGE = 9000; // [W]
//	private final static int ESS_EFFICIENCY = 90; // [%, 0-100]
}
