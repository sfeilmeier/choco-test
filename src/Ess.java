import org.chocosolver.solver.Model;
import org.chocosolver.solver.variables.IntVar;

public class Ess {

	public final EnergySink charge = new EnergySink();
	public final EnergySource discharge = new EnergySource();

	public final IntVar power;

	public Ess(Model model, IntVar chargePower, IntVar dischargePower) {
		this.charge.power = chargePower;
		this.discharge.power = dischargePower;

		this.power = dischargePower.sub(chargePower).intVar();

		model.ifThen(//
				model.arithm(dischargePower, ">", 0), //
				model.arithm(chargePower, "=", 0) //
		);
		model.ifThen(//
				model.arithm(chargePower, ">", 0), //
				model.arithm(dischargePower, "=", 0) //
		);
	}
}
