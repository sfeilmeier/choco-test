package io.openems.ojalgo;

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
import static java.math.BigDecimal.ONE;

import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.ojalgo.optimisation.Expression;
import org.ojalgo.optimisation.ExpressionsBasedModel;
import org.ojalgo.optimisation.Optimisation.Result;
import org.ojalgo.optimisation.Variable;

import com.github.sh0nk.matplotlib4j.Plot;
import com.github.sh0nk.matplotlib4j.PythonExecutionException;

public class OjalgoTest {

	public void run() throws IOException, PythonExecutionException {
		ExpressionsBasedModel model = new ExpressionsBasedModel();

		// Initialize Periods
		final Period[] periods = new Period[NO_OF_PERIODS];
		for (int i = 0; i < periods.length; i++) {
			Period p = Period.from(i, periods.length, MINUTES_PER_PERIOD);
			periods[i] = p;

			/*
			 * Production
			 */
			p.production.power = model.addVariable("Production_" + p.name) //
					.lower(0) //
					.upper(PRODUCTIONS[i]);

			/*
			 * Consumption
			 */
			p.consumption.power = model.addVariable("Consumption_" + p.name) //
					.level(CONSUMPTIONS[i]);

			/*
			 * Energy Storage
			 */
			p.ess.power = model.addVariable("ESS_" + p.name + "_Power") //
					.lower(ESS_MAX_CHARGE * -1) //
					.upper(ESS_MAX_DISCHARGE);
			p.ess.discharge.power = model.addVariable("ESS_" + p.name + "_Discharge_Power") //
					.lower(0) //
					.upper(ESS_MAX_DISCHARGE);
			p.ess.charge.power = model.addVariable("ESS_" + p.name + "_Charge_Power") //
					.lower(0) //
					.upper(ESS_MAX_CHARGE);
			model.addExpression("ESS_" + p.name + "_ChargeDischargePower_Expr") //
					.set(p.ess.power, ONE) //
					.set(p.ess.discharge.power, ONE.negate()) //
					.set(p.ess.charge.power, ONE) //
					.level(0);

			// sum energy
			p.ess.energy = model.addVariable("ESS_" + p.name + "_Energy") //
					.lower(ESS_MIN_ENERGY * 60 /* [Wm] */) //
					.upper(ESS_MAX_ENERGY * 60 /* [Wm] */);

			if (i == 0) {
				model.addExpression("ESS_" + p.name + "_Energy_Expr_1st") //
						.set(p.ess.energy, ONE) //
						.set(p.ess.power, MINUTES_PER_PERIOD) //
						.level(ESS_INITIAL_ENERGY * 60);
			} else {
				model.addExpression("ESS_" + p.name + "_Energy_Expr") //
						.set(periods[i - 1].ess.energy, ONE) //
						.set(p.ess.power, MINUTES_PER_PERIOD * -1) //
						.set(p.ess.energy, ONE.negate()) //
						.level(0);
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
			p.grid.power = model.addVariable("Grid_" + p.name + "_Power"); //
			model.addExpression("Grid_" + p.name + "_Power_Expr") //
					.set(p.grid.power, ONE) //
					.set(p.production.power, ONE) //
					.set(p.ess.power, ONE) //
					.set(p.consumption.power, ONE.negate()) //
					.level(0);
			p.grid.buy.power = model.addVariable("Grid_" + p.name + "_Buy_Power") //
					.lower(0) //
					.upper(GRID_BUY_LIMIT);
			p.grid.sell.power = model.addVariable("Grid_" + p.name + "_Sell_Power") //
					.lower(0) //
					.upper(GRID_SELL_LIMIT);
			model.addExpression("Grid_" + p.name + "_BuySellPower_Expr") //
					.set(p.grid.power, ONE) //
					.set(p.grid.buy.power, ONE.negate()) //
					.set(p.grid.sell.power, ONE) //
					.level(0);
			p.grid.buy.cost = GRID_BUY_COST[i];
			p.grid.sell.revenue = GRID_SELL_REVENUE[i];
		}

		// Grid Buy Cost
		Variable gridBuyCostSum = model.addVariable("Grid_Buy_Cost_Sum");
		Expression gridBuyCostSumExpr = model.addExpression("Grid_Buy_Cost_Sum_Expr") //
				.set(gridBuyCostSum, ONE.negate());
		for (Period p : periods) {
			gridBuyCostSumExpr.set(p.grid.buy.power, p.grid.buy.cost);
		}
		gridBuyCostSumExpr.level(0);

		// Grid Sell Revenue
		Variable gridSellRevenueSum = model.addVariable("Grid_Sell_Revenue_Sum");
		Expression gridSellRevenueSumExpr = model.addExpression("Grid_Sell_Revenue_Sum_Expr") //
				.set(gridSellRevenueSum, ONE.negate()); //
		for (Period p : periods) {
			gridSellRevenueSumExpr.set(p.grid.sell.power, p.grid.sell.revenue);
		}
		gridSellRevenueSumExpr.level(0);

		// Grid Exchange Cost
		model.addExpression("Grid Exchange Cost Sum") //
				.set(gridBuyCostSum, ONE) //
				.set(gridSellRevenueSum, ONE.negate()) //
				.weight(ONE);

		// Total Production
//		Expression productionSum = model.addExpression("Production Sum") //
//				.weight(ONE);
//		for (Period p : periods) {
//			productionSum.set(p.production.power, ONE);
//		}

//		System.out.println(model);

		// Find optimal solution
//		Result result = model.maximise();
		Result result = model.minimise();

		System.out.println("The state: " + result.getState());
		System.out.println("The full result: " + result);

		for (Variable var : new Variable[] { gridBuyCostSum, gridSellRevenueSum }) {
			System.out.println(var.getName() + ": " + var.getValue());
		}

		for (int i = 0; i < periods.length; i++) {
			Period p = periods[i];
			System.out.println(String.format(
					"%2d | %s %5.0f | %s %5.0f | %s %5.0f | %s %5.0f | %s %5.0f | %s %5.0f | %s %5.0f | %s %5.0f | %s %5.0f",
					i, //
					"Grid", p.grid.power.getValue().doubleValue(), //
					"GridBuy", p.grid.buy.power.getValue().doubleValue(), //
					"GridSell", p.grid.sell.power.getValue().doubleValue(), //
					"Production", p.production.power.getValue().doubleValue(), //
					"Consumption", p.consumption.power.getValue().doubleValue(), //
					"ESS", p.ess.power.getValue().doubleValue(), //
					"ESSCharge", p.ess.charge.power.getValue().doubleValue(), //
					"ESSDischarge", p.ess.discharge.power.getValue().doubleValue(), //
					"ESSEnergy", p.ess.energy.getValue().doubleValue() / 60 //
			));
		}

		// Plot results
		this.plot(periods);
	}

	private void plot(Period[] periods) throws IOException, PythonExecutionException {
		Plot plt = Plot.create();
		plt.plot() //
				.add(Arrays.stream(PRODUCTIONS) //
						.limit(NO_OF_PERIODS) //
						.boxed().collect(Collectors.toList())) //
				.label("Max Production") //
				.linestyle("--");
		plt.plot() //
				.add(Stream.of(periods) //
						.map(p -> p.production.power.getValue()) //
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
						.map(p -> p.grid.power.getValue()) //
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
						.map(p -> p.ess.power.getValue()) //
						.collect(Collectors.toList())) //
				.label("ESS") //
				.linestyle("-");
		plt.plot() //
				.add(Stream.of(periods) //
						.map(p -> p.ess.energy.getValue().doubleValue() / 60) //
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
