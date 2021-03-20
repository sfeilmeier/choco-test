import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solution;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.expression.discrete.arithmetic.ArExpression;
import org.chocosolver.solver.objective.ParetoOptimizer;
import org.chocosolver.solver.variables.IntVar;

import com.github.sh0nk.matplotlib4j.Plot;
import com.github.sh0nk.matplotlib4j.PythonExecutionException;

public class SolverTest {

	private final static int NO_OF_PERIODS = 24;
	// TODO apply in Wh calculations
	private final static int MINUTES_PER_PERIOD = 60;

	private final static int GRID_SELL_REVENUE_CONST = 10;
	private final static int[] GRID_SELL_REVENUE = IntStream.of(new int[NO_OF_PERIODS])
			.map(i -> GRID_SELL_REVENUE_CONST).toArray();

	private final static int GRID_BUY_COST_CONST = 30;
	private final static int[] GRID_BUY_COST = IntStream.of(new int[NO_OF_PERIODS]).map(i -> GRID_BUY_COST_CONST)
			.toArray();

	/**
	 * Grid Feed-In Limit, e.g. 70 % law
	 */
	private final static int GRID_SELL_LIMIT = 2000;

	private final static int GRID_BUY_LIMIT = 30000;

	/**
	 * PV-Production 20./21.11.2020; fems888; one value per hour
	 */
	private final static int[] PRODUCTIONS = new int[] { 0, 0, 0, 0, 0, 0, 1, 105, 951, 1467, 3669, 6077, 6865, 4555,
			5432, 697, 47, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 138, 770, 1436, 3437, 5579, 6036, 5672, 4888, 1663,
			106, 0, 0, 0, 0, 0, 0, 0 };
//	private final static int[] PRODUCTIONS = IntStream.of(new int[NO_OF_PERIODS]).map(i -> 5000).toArray();

	/**
	 * Consumption 20./21.11.2020; fems888; one value per hour
	 */
	private final static int[] CONSUMPTIONS = new int[] { 710, 1180, 1207, 1149, 1239, 1187, 1435, 3852, 2936, 2981,
			3459, 3928, 3560, 3560, 2634, 3253, 3289, 2776, 2198, 1299, 1359, 840, 1299, 233, 442, 171, 434, 192, 437,
			443, 663, 3012, 3685, 4498, 3010, 3743, 5544, 3086, 2077, 2852, 3004, 2310, 1343, 1727, 750, 994, 2330,
			1318 };
//	private final static int[] CONSUMPTIONS = IntStream.of(new int[NO_OF_PERIODS]).map(i -> 1000).toArray();

	private final static int ESS_INITIAL_ENERGY = 6000; // [Wh]
//	private final static int ESS_MIN_ENERGY = 0; // [Wh]
//	private final static int ESS_MAX_ENERGY = 12000; // [Wh]
	private final static int ESS_MAX_CHARGE = 9000; // [W]
	private final static int ESS_MAX_DISCHARGE = 9000; // [W]
//	private final static int ESS_EFFICIENCY = 90; // [%, 0-100]

	public void run() throws IOException, PythonExecutionException {
		Instant start = Instant.now();

		Model model = new Model("EMS MILP");

		// Initialize Periods
		final Period[] periods = new Period[NO_OF_PERIODS];
		for (int i = 0; i < periods.length; i++) {
			Period p = Period.from(i, periods.length, MINUTES_PER_PERIOD);
			periods[i] = p;

			// PV-Production per period. P_PV(t)
//			p.production = model.intVar("Production_" + p.name, 0, PRODUCTIONS[i]);
			p.production.power = model.intVar("Production_" + p.name, PRODUCTIONS[i]);

			// Consumption per period. P_HH(t)
			p.consumption.power = model.intVar("Consumption_" + p.name, CONSUMPTIONS[i]);

			// Energy Storage
			IntVar dischargePower = model.intVar("ESS_" + p.name + "_Discharge", 0, ESS_MAX_DISCHARGE);
			IntVar chargePower = model.intVar("ESS_" + p.name + "_Charge", 0, ESS_MAX_CHARGE);
			p.ess = new Ess(model, chargePower, dischargePower);

			// Balancing (with limit)
			ArExpression excess = p.consumption.power.sub(p.production.power);
			p.ess.power.eq(excess.min(1000).max(-2000)).post();

//			p.essEnergy = model.intVar("ESS_Energy_ " + p.name, ESS_MIN_ENERGY, ESS_MAX_ENERGY);
//			p.essEnergy = model.intVar("ESS_Energy_ " + p.name, IntVar.MIN_INT_BOUND, IntVar.MAX_INT_BOUND);

//			ArExpression thisEssDischargeEnergy = p.ess.discharge.power.mul(MINUTES_PER_PERIOD);
//			if (i == 0) {
//				p.essEnergy.eq(thisEssEnergy.add(ESS_INITIAL_ENERGY)).post();
//			} else {
//				Period previousP = periods[i - 1];
//				p.essEnergy.eq(thisEssEnergy.add(previousP.essEnergy)).post();
//			}
//
//			ArExpression thisEssEnergy = p.ess.min(0).mul(ESS_EFFICIENCY).div(100) // Charge
//					.add( //
//							p.ess.max(0).mul(100 + 100 - ESS_EFFICIENCY).div(100) // Discharge
//					).mul(-1); // invert charge/discharge
//			ArExpression thisEssEnergy = p.ess.mul(MINUTES_PER_PERIOD); // [Watt-Minutes]
//			if (i == 0) {
//				p.essEnergy.eq(thisEssEnergy.add(ESS_INITIAL_ENERGY)).post();
//			} else {
//				Period previousP = periods[i - 1];
//				p.essEnergy.eq(thisEssEnergy.add(previousP.essEnergy)).post();
//			}

			// Do not allow Discharging to Grid, i.e. max discharge power is Consumption
			// minus Production
//			p.ess.le(p.consumption.sub(p.production).max(0)).post();

			/*
			 * Grid Buy/Sell
			 */
			IntVar gridPower = p.consumption.power.add(p.ess.charge.power).sub(p.production.power)
					.sub(p.ess.discharge.power).intVar();
			IntVar gridBuyPower = model.intVar("GridBuy_" + p.name, 0, GRID_BUY_LIMIT);
			IntVar gridSellPower = model.intVar("GridSell_" + p.name, 0, GRID_SELL_LIMIT);
			p.grid = new Grid(model, gridPower, gridBuyPower, gridSellPower);
//
//			p.gridSellRevenue = p.grid.mul(-1).max(0).mul(GRID_SELL_REVENUE[i]).intVar();
//			p.gridBuyCost = p.grid.max(0).mul(GRID_BUY_COST[i]).intVar();
		}

		// Total Grid exchange
//		IntVar sumGridSellRevenue = model.intVar("Grid Sum Sell Revenue", 0, IntVar.MAX_INT_BOUND);
//		model.sum(Stream.of(periods).map(p -> p.gridSellRevenue).toArray(IntVar[]::new), "=", sumGridSellRevenue)
//				.post();
//
//		IntVar sumGridBuyCost = model.intVar("Grid Sum Buy Cost", 0, IntVar.MAX_INT_BOUND);
//		model.sum(Stream.of(periods).map(p -> p.gridBuyCost).toArray(IntVar[]::new), "=", sumGridBuyCost).post();

		// Total Production
		IntVar sumProduction = model.intVar("Production Sum", 0, IntVar.MAX_INT_BOUND);
		model.sum(Stream.of(periods).map(p -> p.production.power).toArray(IntVar[]::new), "=", sumProduction).post();

//		IntVar totalRevenue = sumGridSellRevenue.sub(sumGridBuyCost).intVar();

		// Find optimal solution
		Solver solver = model.getSolver();

//		ParetoOptimizer po = new ParetoOptimizer(Model.MAXIMIZE, new IntVar[] { sumProduction, totalRevenue });
		ParetoOptimizer po = new ParetoOptimizer(Model.MAXIMIZE, new IntVar[] { sumProduction });
		solver.plugMonitor(po);

		while (solver.solve()) {
			System.out.print(".");
		}
		System.out.println();

		List<Solution> paretoFront = po.getParetoFront();
		System.out.println("The pareto front has " + paretoFront.size() + " solutions : ");
		final Solution solution;
		if (paretoFront.size() > 0) {
			solution = paretoFront.get(0);
		} else {
			solution = null;
		}

		System.out.println("Duration: " + Duration.between(start, Instant.now()).getSeconds() + "s");

//		solver.limitTime("10s");
		// then run the resolution
//		Solution solution = solver.findOptimalSolution(totalCost, false);
		if (solution != null) {
			System.out.println(solution.toString());
//			for (IntVar var : new IntVar[] { sumGridBuyCost, sumGridSellRevenue, totalRevenue }) {
//				System.out.println(var.getName() + ": " + solution.getIntVal(var));
//			}

			for (int i = 0; i < periods.length; i++) {
				Period p = periods[i];
				System.out.println(i + ":" //
//						+ " Grid: " + solution.getIntVal(p.grid) //
						+ " Production: " + solution.getIntVal(p.production.power) //
						+ " Consumption: " + solution.getIntVal(p.consumption.power) //
						+ " ESS: " + solution.getIntVal(p.ess.power)); //
//				System.out.println(i + ": " + solution.getIntVal(p.grid) + " Revenue: "
//						+ solution.getIntVal(p.gridSellRevenue) + " Cost: " + solution.getIntVal(p.gridBuyCost));
				for (IntVar var : new IntVar[] {}) {
					System.out.println(var.getName() + ": " + solution.getIntVal(var));
				}
			}

			this.plot(periods, solution);
		} else {
			System.out.println(model);
			System.out.println("No Solution!");
		}
	}

	private void plot(Period[] periods, Solution solution) throws IOException, PythonExecutionException {
		Plot plt = Plot.create();
		plt.plot() //
				.add(Arrays.stream(PRODUCTIONS) //
						.limit(NO_OF_PERIODS) //
						.boxed().collect(Collectors.toList())) //
				.label("Max Production") //
				.linestyle("--");
		plt.plot() //
				.add(Stream.of(periods) //
						.map(p -> solution.getIntVal(p.production.power)) //
						.collect(Collectors.toList())) //
				.label("Curtailed Production") //
				.linestyle("-");
		plt.plot() //
				.add(Arrays.stream(CONSUMPTIONS) //
						.limit(NO_OF_PERIODS) //
						.boxed().collect(Collectors.toList())) //
				.label("Consumption") //
				.linestyle("-");
		plt.plot() //
				.add(Stream.of(periods) //
						.map(p -> solution.getIntVal(p.grid.power)) //
						.collect(Collectors.toList())) //
				.label("Grid") //
				.linestyle("-");
//		plt.plot() //
//				.add(Stream.of(periods) //
//						.map(p -> solution.getIntVal(p.gridBuyCost)) //
//						.collect(Collectors.toList())) //
//				.label("GridBuyCost") //
//				.linestyle(":");
//		plt.plot() //
//				.add(Stream.of(periods) //
//						.map(p -> solution.getIntVal(p.gridSellRevenue)) //
//						.collect(Collectors.toList())) //
//				.label("GridSellRevenue") //
//				.linestyle("--");
		plt.plot() //
				.add(Stream.of(periods) //
						.map(p -> solution.getIntVal(p.ess.power)) //
						.collect(Collectors.toList())) //
				.label("ESS") //
				.linestyle("-");
//		plt.plot() //
//				.add(Stream.of(periods) //
//						.map(p -> solution.getIntVal(p.essEnergy)) //
//						.collect(Collectors.toList())) //
//				.label("ESS Energy") //
//				.linestyle(":");
//		plt.plot() //
//				.add(Collections.nCopies(NO_OF_PERIODS, GRID_SELL_LIMIT)) //
//				.label("Grid Sell Limit") //
//				.linestyle("-.");
//		plt.plot() //
//				.add(Collections.nCopies(NO_OF_PERIODS, GRID_BUY_LIMIT)) //
//				.label("Grid Buy Limit") //
//				.linestyle("-.");
		plt.xlabel("Hour");
		plt.ylabel("Watt");
		plt.legend();
		plt.show();
	}
}
