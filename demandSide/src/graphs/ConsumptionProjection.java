package graphs;

import static repast.simphony.essentials.RepastEssentials.GetParameter;

import org.apache.commons.math3.util.FastMath;

import consumers.Consumer;
import consumers.Consumers;
import consumers.Pareto;
import demandSide.RecessionsHandler;
import repast.simphony.context.space.continuous.ContextSpace;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.continuous.SimpleCartesianAdder;
import repast.simphony.space.continuous.StickyBorders;

public class ConsumptionProjection {

	private static final double MAX_X = 100, MAX_Y = 100, MAX_Z = 100;
	private static final double MIN_X = 0, MIN_Y = 0, MIN_Z = 0;

	private ContinuousSpace<Consumer> space;

	public ConsumptionProjection(Consumers consumers) {

		double[] dims = new double[3];
		dims[0] = MAX_X + 0.1;
		dims[1] = MAX_Y + 0.1;
		dims[2] = MAX_Z + 0.1;

		space = new ContextSpace<Consumer>("ConsumptionProjection", new SimpleCartesianAdder<Consumer>(),
				new StickyBorders(), dims);

		consumers.addProjection(space);

	}

	public void update(Consumer c) {

		space.moveTo(c, 0.0, margUtilToCoord(c.getRawWelfareParam()), 0.0);		

		c.getChosenFirm().ifPresent(f -> space.moveTo(c, priceToCoord(f.getPrice()),
				margUtilToCoord(c.getRawWelfareParam()), qualityToCoord(f.getQuality())));
	}

	private int margUtilToCoord(double margUtilOfQuality) {

		return (int) FastMath.min(FastMath.round(margUtilOfQuality / getMaxUtilToDraw() * (MAX_Y - MIN_Y)) + MIN_Y,
				MAX_Y);
	}

	private double getMaxUtilToDraw() {
		// Assign minimum Marginal Utility of Quality for the segment
		double acumProb = (double) GetParameter("margUtilPercentToDraw");

		double gini = (double) GetParameter("gini");
		double lambda = (1.0 + gini) / (2.0 * gini);

		double minimum = RecessionsHandler.getWelfareParamPerceivedByFirms(Consumers.getMinRawWelfareParam());

		return Pareto.inversePareto(acumProb, minimum, lambda);
	}

	private double priceToCoord(double d) {
		return (d - Scale.getMinPrice()) / (Scale.getMaxPrice() - Scale.getMinPrice())
				* (MAX_X - MIN_X) + MIN_X;
	}

	private double qualityToCoord(double d) {
		return MAX_Z - ((d - Scale.getMinQuality())
				/ (Scale.getMaxQuality() - Scale.getMinQuality()) * (MAX_Z - MIN_Z) + MIN_Z);
	}

}
