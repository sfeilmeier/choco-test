package io.openems;

import java.util.stream.IntStream;

public class Constants {

	public final static int NO_OF_PERIODS = 24;
	public final static int MINUTES_PER_PERIOD = 60;

	public final static int GRID_SELL_REVENUE_CONST = 10;
	public final static int[] GRID_SELL_REVENUE = IntStream.of(new int[NO_OF_PERIODS]).map(i -> GRID_SELL_REVENUE_CONST)
			.toArray();

	public final static int GRID_BUY_COST_CONST = 30;
	public final static int[] GRID_BUY_COST = IntStream.of(new int[NO_OF_PERIODS]).map(i -> GRID_BUY_COST_CONST)
			.toArray();

	/**
	 * Grid Feed-In Limit, e.g. 70 % law
	 */
	public final static int GRID_SELL_LIMIT = 30000;

	public final static int GRID_BUY_LIMIT = 30000;

	/**
	 * PV-Production 20./21.11.2020; fems888; one value per hour
	 */
	public final static int[] PRODUCTIONS = new int[] { 0, 0, 0, 0, 0, 0, 1, 105, 951, 1467, 3669, 6077, 6865, 4555,
			5432, 697, 47, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 138, 770, 1436, 3437, 5579, 6036, 5672, 4888, 1663,
			106, 0, 0, 0, 0, 0, 0, 0 };
//	private final static int[] PRODUCTIONS = IntStream.of(new int[NO_OF_PERIODS]).map(i -> 5000).toArray();

	/**
	 * Consumption 20./21.11.2020; fems888; one value per hour
	 */
	public final static int[] CONSUMPTIONS = new int[] { 710, 1180, 1207, 1149, 1239, 1187, 1435, 3852, 2936, 2981,
			3459, 3928, 3560, 3560, 2634, 3253, 3289, 2776, 2198, 1299, 1359, 840, 1299, 233, 442, 171, 434, 192, 437,
			443, 663, 3012, 3685, 4498, 3010, 3743, 5544, 3086, 2077, 2852, 3004, 2310, 1343, 1727, 750, 994, 2330,
			1318 };
//	private final static int[] CONSUMPTIONS = IntStream.of(new int[NO_OF_PERIODS]).map(i -> 1000).toArray();

	public final static int ESS_INITIAL_ENERGY = 6000; // [Wh]
	public final static int ESS_MIN_ENERGY = 0; // [Wh]
	public final static int ESS_MAX_ENERGY = 7000; // [Wh]
	public final static int ESS_MAX_CHARGE = 9000; // [W]
	public final static int ESS_MAX_DISCHARGE = 9000; // [W]
//	private final static int ESS_EFFICIENCY = 90; // [%, 0-100]
}
