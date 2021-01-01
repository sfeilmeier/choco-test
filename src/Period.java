import org.chocosolver.solver.variables.IntVar;

public class Period {

	private final static int MINUTES_PER_DAY = 1440;

	public static Period from(int thisIndex, int maxIndex, int lengthInMinutes) {
		StringBuilder b = new StringBuilder();

		// More than one day?
		if (maxIndex * lengthInMinutes > MINUTES_PER_DAY) {
			b.append("D");
			final int day = thisIndex / (MINUTES_PER_DAY / lengthInMinutes);
			b.append(day);
			b.append("_");
			thisIndex -= day * (MINUTES_PER_DAY / lengthInMinutes); // normalize to first day from here
		}

		// Period of the day
		final int minute = thisIndex * lengthInMinutes;
		b.append(String.format("%02d", minute / 60));
		b.append(":");
		b.append(String.format("%02d", minute % 60));
		return new Period(b.toString());
	}

	public final String name;

	private Period(String name) {
		this.name = name;
	}

	public IntVar production = null;
	public IntVar consumption = null;

	public IntVar grid = null;
	public IntVar ess;

	public IntVar essEnergy;
	public IntVar gridSellRevenue = null;
	public IntVar gridBuyCost = null;

//	public IntVar productionPower = null;
//	public IntVar productionEnergy = null;
//	public IntVar productionRevenue = null;
//
//	public IntVar consumptionPower = null;
//	public IntVar consumptionEnergy = null;
//	public IntVar consumptionCost = null;
//
//	public IntVar gridBuyPower = null;
//	public IntVar gridBuyEnergy = null;
//	public IntVar gridBuyCost = null;
//
//	public IntVar gridSellPower = null;
//	public IntVar gridSellEnergy = null;
//	public IntVar gridSellRevenue = null;
//
//	public IntVar essChargePower = null;
//	public IntVar essChargeEnergy = null;
//	public IntVar essChargeRevenue = null;
//
//	public IntVar essDischargePower = null;
//	public IntVar essDischargeEnergy = null;
//	public IntVar essDischargeCost = null;
}
