package consumers;

import repast.simphony.random.RandomHelper;

import org.apache.commons.math3.util.FastMath;

import cern.jet.random.AbstractContinousDistribution;
import cern.jet.random.Exponential;

public class Pareto extends AbstractContinousDistribution {

	private static final long serialVersionUID = 1L;
	private Exponential implicitDistrib;
	private double minimum;

	Pareto(double lambda, double minimum) {
		this.minimum = minimum;
		implicitDistrib = RandomHelper.createExponential(lambda);
	}

	public static Pareto getPareto(double lambda, double minimum) {
		return new Pareto(lambda, minimum);
	}

	public double nextDouble() {
		return minimum * FastMath.exp(implicitDistrib.nextDouble());
	}

	public static double inversePareto(double acumProb, double minimum, double lambda) {
		return minimum / (1 - acumProb) * FastMath.exp(-1.0 / lambda);
	}

}