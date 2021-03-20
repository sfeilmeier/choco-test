import org.chocosolver.solver.Model;
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.solver.variables.IntVar;

public class Grid {

	public final EnergySink sell = new EnergySink();
	public final EnergySource buy = new EnergySource();

	public final IntVar power;

	public Grid(Model model, IntVar power, IntVar buyPower, IntVar sellPower) {
		this.power = power;
		this.sell.power = sellPower;
		this.buy.power = buyPower;

		BoolVar gridIsBuy = power.gt(0).boolVar();
		model.ifThenElse(gridIsBuy, //
				model.arithm(buy.power, "=", power), //
				model.arithm(buy.power, "=", 0) //
		);
		model.ifThenElse(gridIsBuy, //
				model.arithm(sell.power, "=", 0), //
				model.arithm(sell.power, "=", model.intAbsView(power)) //
		);
	}
}
