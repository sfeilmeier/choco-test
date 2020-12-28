import java.io.IOException;
import java.util.Collections;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solution;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.expression.discrete.arithmetic.ArExpression;
import org.chocosolver.solver.variables.IntVar;

import com.github.sh0nk.matplotlib4j.Plot;
import com.github.sh0nk.matplotlib4j.PythonExecutionException;
import com.google.common.primitives.Ints;

public class SolverTest {

	private final static int LENGTH = 48;

	/**
	 * PV-Production 20./21.11.2020; fems888; one value per hour
	 */
	private final static int[] PRODUCTIONS = new int[] { 0, 0, 0, 0, 0, 0, 1, 105, 951, 1467, 3669, 6077, 6865, 4555,
			5432, 697, 47, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 138, 770, 1436, 3437, 5579, 6036, 5672, 4888, 1663,
			106, 0, 0, 0, 0, 0, 0, 0 };
	/**
	 * Consumption 20./21.11.2020; fems888; one value per hour
	 */
	private final static int[] CONSUMPTIONS = new int[] { 710, 1180, 1207, 1149, 1239, 1187, 1435, 3852, 2936, 2981,
			3459, 3928, 3560, 3560, 2634, 3253, 3289, 2776, 2198, 1299, 1359, 840, 1299, 233, 442, 171, 434, 192, 437,
			443, 663, 3012, 3685, 4498, 3010, 3743, 5544, 3086, 2077, 2852, 3004, 2310, 1343, 1727, 750, 994, 2330,
			1318 };

	/**
	 * Grid Feed-In Limit, e.g. 70 % law
	 */
	private final static int GRID_FEED_IN_LIMIT = -2000;

	public void run() throws IOException, PythonExecutionException {
		Model model = new Model("EMS MILP");

		// Initialize Periods
		final Period[] periods = new Period[LENGTH];
		for (int i = 0; i < periods.length; i++) {
			Period p = Period.from(i, periods.length, 60);
			periods[i] = p;

			// PV-Production per period. P_PV(t)
			p.production = model.intVar("Production_" + p.name, 0, PRODUCTIONS[i], true); // Formula (1)

			// Consumption per period. P_HH(t)
			p.consumption = model.intVar("Consumption_" + p.name, CONSUMPTIONS[i]); // Static value

			// Grid exchange power
			p.grid = p.consumption.sub(p.production).intVar();
		}

		// Apply Grid Feed-In Limit
		IntVar gridFeedInLimit = model.intVar("Grid Feed-In Limit", GRID_FEED_IN_LIMIT);
		model.min(gridFeedInLimit, Stream.of(periods).map(p -> p.grid).toArray(IntVar[]::new)).post();

		IntVar sumGrid = periods[0].grid.add(Stream.of(periods).map(p -> p.grid).toArray(ArExpression[]::new)).intVar();

		// Maximize Grid-Feed-In
		model.setObjective(Model.MINIMIZE, sumGrid);

		// Find optimal solution
		Solver solver = model.getSolver();
		Solution solution = new Solution(model);
		while (solver.solve()) {
			solution.record();
		}

		if (solution.exists()) {
			System.out.println(solution.toString());
			for (int i = 0; i < periods.length; i++) {
				Period p = periods[i];
				System.out.println(solution.getIntVal(p.consumption) + " - " + solution.getIntVal(p.production)
						+ " (max " + PRODUCTIONS[i] + ") = " + solution.getIntVal(p.grid));
			}

			this.plot(periods, solution);
		}
	}

	private void plot(Period[] periods, Solution solution) throws IOException, PythonExecutionException {
		Plot plt = Plot.create();
		plt.plot() //
				.add(Ints.asList(PRODUCTIONS)) //
				.label("Max Production") //
				.linestyle("--");
		plt.plot() //
				.add(Stream.of(periods) //
						.map(p -> solution.getIntVal(p.production)) //
						.collect(Collectors.toList())) //
				.label("Curtailed Production") //
				.linestyle("-");
		plt.plot() //
				.add(Ints.asList(CONSUMPTIONS)) //
				.label("Consumption") //
				.linestyle("-");
		plt.plot() //
				.add(Stream.of(periods) //
						.map(p -> solution.getIntVal(p.grid)) //
						.collect(Collectors.toList())) //
				.label("Grid") //
				.linestyle("-");
		plt.plot() //
				.add(Collections.nCopies(LENGTH, GRID_FEED_IN_LIMIT)) //
				.label("Grid Feed-In Limit") //
				.linestyle("--");
		plt.xlabel("Hour");
		plt.ylabel("Watt");
		plt.legend();
		plt.show();
	}
}
