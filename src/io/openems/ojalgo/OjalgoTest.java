package io.openems.ojalgo;

import static io.openems.Constants.ESS_MAX_ENERGY;
import static io.openems.Constants.NO_OF_PERIODS;
import static java.math.BigDecimal.ONE;

import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Collectors;

import com.github.sh0nk.matplotlib4j.Plot;
import com.github.sh0nk.matplotlib4j.PythonExecutionException;

public class OjalgoTest {

	public void run() throws IOException, PythonExecutionException {
		int[] maxGridBuy = new int[NO_OF_PERIODS];
		int[] maxGridSell = new int[NO_OF_PERIODS];

		EnergyModel em = null;
		for (int i = 0; i < NO_OF_PERIODS; i++) {
			em = new EnergyModel();

			em.model.addExpression("End of 1st HLZ") //
					.set(em.periods[5].ess.energy, ONE) //
					.level(0);

			em.model.addExpression("Beginning of 2nd HLZ") //
					.set(em.periods[18].ess.energy, ONE) //
					.level(ESS_MAX_ENERGY * 60);

			em.model.addExpression("End of 2nd HLZ") //
					.set(em.periods[23].ess.energy, ONE) //
					.level(0);

			em.model.addExpression("Extreme Grid Power") //
					.set(em.periods[i].grid.power, ONE) //
					.weight(ONE);

			em.model.maximise();
			maxGridBuy[i] = em.periods[i].grid.buy.power.getValue().intValue();
			em.model.minimise();
			maxGridSell[i] = em.periods[i].grid.sell.power.getValue().intValue();
		}

		plot(maxGridBuy, maxGridSell);
//		em.print();
	}

	public void plot(int[] maxGridBuy, int[] maxGridSell) throws IOException, PythonExecutionException {
		Plot plt = Plot.create();

		plt.plot() //
				.add(Arrays.stream(maxGridBuy) //
						.boxed() //
						.collect(Collectors.toList())) //
				.label("Grid Buy") //
				.linestyle("-");
		plt.plot() //
				.add(Arrays.stream(maxGridSell) //
						.map(v -> v * -1) //
						.boxed() //
						.collect(Collectors.toList())) //
				.label("Grid Sell") //
				.linestyle("-");
		plt.xlabel("Hour");
		plt.ylabel("Watt");
		plt.legend();
		plt.show();
	}
}
