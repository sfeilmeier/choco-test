package io.openems.choco;

import static io.openems.Constants.CONSUMPTIONS;
import static io.openems.Constants.ESS_INITIAL_ENERGY;
import static io.openems.Constants.ESS_MAX_CHARGE;
import static io.openems.Constants.ESS_MAX_DISCHARGE;
import static io.openems.Constants.ESS_MAX_ENERGY;
import static io.openems.Constants.ESS_MIN_ENERGY;
import static io.openems.Constants.GRID_BUY_COST;
import static io.openems.Constants.GRID_BUY_LIMIT;
import static io.openems.Constants.GRID_SELL_LIMIT;
import static io.openems.Constants.GRID_SELL_REVENUE;
import static io.openems.Constants.MINUTES_PER_PERIOD;
import static io.openems.Constants.NO_OF_PERIODS;
import static io.openems.Constants.PRODUCTIONS;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solution;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.expression.discrete.arithmetic.ArExpression;
import org.chocosolver.solver.objective.ParetoOptimizer;
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.solver.variables.IntVar;

import com.github.sh0nk.matplotlib4j.Plot;
import com.github.sh0nk.matplotlib4j.PythonExecutionException;

public class SolverTest {

	public void run() throws IOException, PythonExecutionException {
		Instant start = Instant.now();

		Model model = new Model("EMS MILP");

		// Initialize Periods
		final Period[] periods = new Period[NO_OF_PERIODS];
		for (int i = 0; i < periods.length; i++) {
			Period p = Period.from(i, periods.length, MINUTES_PER_PERIOD);
			periods[i] = p;

			/*
			 * Production
			 */
			p.production.power = model.intVar("Production_" + p.name, 0, PRODUCTIONS[i]);
//			p.production.power = model.intVar("Production_" + p.name, PRODUCTIONS[i]);

			/*
			 * Consumption
			 */
			p.consumption.power = model.intVar("Consumption_" + p.name, CONSUMPTIONS[i]);

			/*
			 * Energy Storage
			 */
			p.ess.discharge.power = model.intVar("ESS_" + p.name + "_Discharge", 0, ESS_MAX_DISCHARGE);
			p.ess.charge.power = model.intVar("ESS_" + p.name + "_Charge", 0, ESS_MAX_CHARGE);

			// positive/negative power
			p.ess.power = p.ess.discharge.power.sub(p.ess.charge.power).intVar();
			model.ifThen(//
					model.arithm(p.ess.discharge.power, ">", 0), //
					model.arithm(p.ess.charge.power, "=", 0) //
			);
			model.ifThen(//
					model.arithm(p.ess.charge.power, ">", 0), //
					model.arithm(p.ess.discharge.power, "=", 0) //
			);

			// sum energy
			p.ess.energy = model.intVar("ESS_" + p.name + "_Energy", ESS_MIN_ENERGY * 60 /* [Wm] */,
					ESS_MAX_ENERGY * 60 /* [Wm] */);
//			p.ess.energy = model.intVar("ESS_" + p.name + "_Energy", IntVar.MIN_INT_BOUND, IntVar.MAX_INT_BOUND);
			ArExpression thisEssEnergy = p.ess.power.mul(MINUTES_PER_PERIOD).mul(-1);
			if (i == 0) {
				p.ess.energy.eq(thisEssEnergy.add(ESS_INITIAL_ENERGY)).post();
			} else {
				p.ess.energy.eq(thisEssEnergy.add(periods[i - 1].ess.energy)).post();
			}

			// Do not allow Discharging to Grid, i.e. max discharge power is Consumption
			// minus Production
//			p.ess.le(p.consumption.sub(p.production).max(0)).post();

			// Balancing (with limit)
//			ArExpression excess = p.consumption.power.sub(p.production.power);
//			p.ess.power.eq(excess.min(1000).max(-2000)).post();

			/*
			 * Grid
			 */
			p.grid.power = p.consumption.power //
					.add(p.ess.charge.power) //
					.sub(p.production.power) //
					.sub(p.ess.discharge.power).intVar();
			p.grid.buy.power = model.intVar("GridBuy_" + p.name, 0, GRID_BUY_LIMIT);
			p.grid.sell.power = model.intVar("GridSell_" + p.name, 0, GRID_SELL_LIMIT);

			// positive/negative power
			BoolVar gridIsBuy = p.grid.power.gt(0).boolVar();
			model.ifThenElse(gridIsBuy, //
					model.arithm(p.grid.buy.power, "=", p.grid.power), //
					model.arithm(p.grid.buy.power, "=", 0) //
			);
			model.ifThenElse(gridIsBuy, //
					model.arithm(p.grid.sell.power, "=", 0), //
					model.arithm(p.grid.sell.power, "=", model.intAbsView(p.grid.power)) //
			);

			// energy
//			p.grid.buy.cost = p.grid.buy.power.mul(MINUTES_PER_PERIOD).mul(GRID_BUY_COST[i]).intVar();
//			p.grid.sell.revenue = p.grid.sell.power.mul(MINUTES_PER_PERIOD).mul(GRID_SELL_REVENUE[i]).intVar();
		}

		// Total Grid exchange
		IntVar sumGridSellRevenue = model.intVar("Grid Sum Sell Revenue", 0, IntVar.MAX_INT_BOUND);
		model.scalar(//
				Stream.of(periods).map(p -> p.grid.sell.power).toArray(IntVar[]::new), //
				GRID_SELL_REVENUE, "=", sumGridSellRevenue).post();
		IntVar sumGridBuyCost = model.intVar("Grid Sum Buy Cost", 0, IntVar.MAX_INT_BOUND);
		model.scalar(//
				Stream.of(periods).map(p -> p.grid.buy.power).toArray(IntVar[]::new), //
				GRID_BUY_COST, "=", sumGridBuyCost).post();
		IntVar totalRevenue = sumGridSellRevenue.sub(sumGridBuyCost).intVar();

//		model.sum(Stream.of(periods).map(p -> p.gridSellRevenue).toArray(IntVar[]::new), "=", sumGridSellRevenue)
//				.post();
//
//		IntVar sumGridBuyCost = model.intVar("Grid Sum Buy Cost", 0, IntVar.MAX_INT_BOUND);
//		model.sum(Stream.of(periods).map(p -> p.gridBuyCost).toArray(IntVar[]::new), "=", sumGridBuyCost).post();

		// Total Production
		IntVar sumProduction = model.intVar("Production Sum", 0, IntVar.MAX_INT_BOUND);
		model.sum(Stream.of(periods).map(p -> p.production.power).toArray(IntVar[]::new), "=", sumProduction).post();

		// Find optimal solution
		Solver solver = model.getSolver();
		solver.limitTime("10s");

//		ParetoOptimizer po = new ParetoOptimizer(Model.MAXIMIZE,
//				new IntVar[] { totalRevenue, periods[periods.length - 1].ess.energy });
		ParetoOptimizer po = new ParetoOptimizer(Model.MAXIMIZE, new IntVar[] { totalRevenue });

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

//		solution = solver.findOptimalSolution(totalRevenue.add(periods[periods.length - 1].ess.energy).intVar(), true);
//		solution = solver.findOptimalSolution(totalRevenue, true);

		System.out.println("Duration: " + Duration.between(start, Instant.now()).getSeconds() + "s");

		// then run the resolution
//		Solution solution = solver.findOptimalSolution(totalCost, false);
		if (solution != null) {
			System.out.println(solution.toString());
//			sumGridBuyCost
//			totalRevenue
			for (IntVar var : new IntVar[] { sumGridSellRevenue, sumGridBuyCost }) {
				System.out.println(var.getName() + ": " + solution.getIntVal(var));
			}

			for (int i = 0; i < periods.length; i++) {
				Period p = periods[i];
				System.out.println(String.format("%2d | %s %5d | %s %5d | %s %5d | %s %5d | %s %5d", i, //
						"Grid", solution.getIntVal(p.grid.power), //
						"Production", solution.getIntVal(p.production.power), //
						"Consumption", solution.getIntVal(p.consumption.power), //
						"ESS", solution.getIntVal(p.ess.power), //
						"ESS", solution.getIntVal(p.ess.energy) //
				));
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
//						.map(p -> solution.getIntVal(p.grid.buy.cost) / 1000) //
//						.collect(Collectors.toList())) //
//				.label("GridBuyCost") //
//				.linestyle(":");
//		plt.plot() //
//				.add(Stream.of(periods) //
//						.map(p -> solution.getIntVal(p.grid.sell.revenue) / 1000) //
//						.collect(Collectors.toList())) //
//				.label("GridSellRevenue") //
//				.linestyle("--");
		plt.plot() //
				.add(Stream.of(periods) //
						.map(p -> solution.getIntVal(p.ess.power)) //
						.collect(Collectors.toList())) //
				.label("ESS") //
				.linestyle("-");
		plt.plot() //
				.add(Stream.of(periods) //
						.map(p -> solution.getIntVal(p.ess.energy) / 60) //
						.collect(Collectors.toList())) //
				.label("ESS Energy") //
				.linestyle(":");
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
