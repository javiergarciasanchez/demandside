package graphs;


import org.apache.commons.math3.util.FastMath;

import firms.Firm;
import firms.Firms;
import repast.simphony.context.space.continuous.ContextSpace;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.continuous.SimpleCartesianAdder;
import repast.simphony.space.continuous.StickyBorders;

public class FirmsSalesProjection {

	private static final double MAX_X = 100, MAX_Y = 100, MAX_Z = 100;
	private static final double MIN_X = 0, MIN_Y = 0, MIN_Z = 0;
	private static final double MIN_PROFIT_TO_DRAW = -100.0,
			MAX_SALES_TO_DRAW = 1000.0;

	private ContinuousSpace<Firm> space;

	public FirmsSalesProjection(Firms firms) {

		double[] dims = new double[3];
		dims[0] = MAX_X + 0.1;
		dims[1] = MAX_Y + 0.1;
		dims[2] = MAX_Z + 0.1;

		space = new ContextSpace<Firm>("FirmsSalesProjection",
				new SimpleCartesianAdder<Firm>(), new StickyBorders(), dims);

		firms.addProjection(space);

	}

	public void update(Firm firm) {
		space.moveTo(firm, priceToCoord(firm.getPrice()),
				salesToCoord(firm.getSales()),
				qualityToCoord(firm.getQuality()));
	}

	private double priceToCoord(double price) {
		return (price - Scale.getMinPrice())
				/ (Scale.getMaxPrice() - Scale.getMinPrice()) * (MAX_X - MIN_X)
				+ MIN_X;
	}

	private double salesToCoord(double sales) {
		double salesCoord = FastMath.min(MAX_SALES_TO_DRAW,
				FastMath.max(sales, MIN_PROFIT_TO_DRAW)) + ( -MIN_PROFIT_TO_DRAW );
		return salesCoord / (MAX_SALES_TO_DRAW - MIN_PROFIT_TO_DRAW)
				 * (MAX_Y - MIN_Y) + MIN_Y;
	}

	private double qualityToCoord(double quality) {
		return MAX_Z
				- ((quality - Scale.getMinQuality())
						/ (Scale.getMaxQuality() - Scale.getMinQuality())
						* (MAX_Z - MIN_Z) + MIN_Z);
	}

}
