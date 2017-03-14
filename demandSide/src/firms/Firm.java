package firms;

import static repast.simphony.essentials.RepastEssentials.GetParameter;

import java.util.ArrayList;

import org.apache.commons.math3.util.FastMath;

import consumers.Consumer;
import consumers.Consumers;
import decisionTools.ImprovingDeltaOffer;
import decisionTools.NoPrice;
import decisionTools.OptimalPrice;
import demandSide.Market;
import demandSide.RunPriority;
import graphs.Scale;
import offer.Offer;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.essentials.RepastEssentials;
import repast.simphony.random.RandomHelper;

public class Firm {

	protected double minConsumerMargUtil = Consumers.getMinMargUtilOfQuality();
	protected double maxConsumerMargUtil = Consumers.getMaxMargUtilOfQuality();

	public StrategicPreference stratPref;
	private Offer offer;

	private int demand = 0;
	private double profit = 0.0;

	private double accumProfit = 0;
	private double autoRegressiveProfit = 0;
	private double fixedCost;
	private double born;
	private ArrayList<Consumer> notYetKnownBy;
	private int triedBy = 0;

	// Cost Scale is the same for all firms. It could be changed easily
	private static double costParameter;
	private static double currentProfitWeight;

	protected static long firmIDCounter;

	private long firmIntID = firmIDCounter++;
	private String ID = "Firm " + firmIntID;

	public static void resetStaticVars() {
		// resets static variables
		costParameter = (Double) GetParameter("costParameter");
		currentProfitWeight = (Double) GetParameter("currentProfitWeight");
		firmIDCounter = 1;
	}

	public Firm() {

		Market.firms.add(this);

		offer = new Offer();

		stratPref = new StrategicPreference(this);

		notYetKnownBy = new ArrayList<Consumer>();

		fixedCost = (Double) Market.firms.getFixedCostDistrib().nextDouble();

		born = RepastEssentials.GetTickCount();

		if (!makeInitialOffer())
			Market.firms.remove(this);

	}

	private boolean makeInitialOffer() {

		// getRandomInitialQuality takes care that q should be different for
		// different firms.
		double q = getRandomInitialQuality();
		double p;

		try {
			p = getOptimalPriceGivenRealQ(q);
		} catch (NoPrice e) {
			return false;
		}

		offer.setPrice(p);
		offer.setQuality(q);
		Market.firms.addToFirmLists(this);
		initializeConsumerKnowledge();
		return true;

	}

	/*
	 * Gets the price that maximizes profit given quality.
	 * 
	 * Profit depends on quantity which depends on who are the neighbors. Thus
	 * depending which competitors are expelled, we have different profit
	 * functions.
	 * 
	 * For every possible set of competitors we get a temporary optimal price
	 * 
	 * The price that produces the highest profit among all set of competitors
	 * is the optimal price
	 * 
	 * The different set of competitors are determined by reducing price. The
	 * lower the price the more competitors are expelled The lowest meaningful
	 * price is marginal cost
	 */
	private double getOptimalPriceGivenRealQ(double q) throws NoPrice {

		double perceivedQ = getPerceivedQuality(q);
		double cost = getUnitCost(q);
		FirmsSegments seg = Market.firms.perceivedQSegments;

		return OptimalPrice.get(seg, perceivedQ, cost);

	}

	private double getRandomInitialQuality() {
		double q = RandomHelper.nextDoubleFromTo(0.0, Offer.getMaxInitialQuality());

		if (Market.firms.firmsByQ.containsKey(q))
			
			if (q > Double.MIN_NORMAL)
				q = q - Double.MIN_NORMAL;
			else
				q = q + Double.MIN_NORMAL;

		return q;
	}

	@ScheduledMethod(start = 1, priority = RunPriority.RESET_DEMAND_PRIORITY, interval = 1)
	public void resetDemand() {
		demand = 0;
	}

	@ScheduledMethod(start = 1, priority = RunPriority.MAKE_OFFER_PRIORITY, interval = 1)
	public void makeOffer() {

		if (Market.firms.perceivedQSegments.contains(this))
			setNextOffer();

		else {
			// Choose a price to reenter
			double p;
			try {
				p = getOptimalPriceGivenRealQ(offer.getQuality());
				offer.setPrice(p);
			} catch (NoPrice e) {
				// No price to reenter, sets cost as price
				offer.setPrice(getUnitCost(getQuality()));
			}
		}

		updateConsumerKnowledge();

	}

	protected void setNextOffer() {

		offer = Offer.checkedAdd(this, getOffer(), ImprovingDeltaOffer.get(this, Market.firms.perceivedQSegments));

	}

	public double getPerceivedQuality(double q) {
		return q * getPerceptionDiscount();
	}

	private double getPerceptionDiscount() {
		// Weighted average of discounted quality factor and real quality factor
		// real q weighted by consumers already tried the firm
		// disc q weighted by consumers didn't try the firm

		double notTriedBy = Market.consumers.size() - triedBy;

		double avgDiscountFactor = (Double) GetParameter("qualityDiscountMean");
		return (triedBy + avgDiscountFactor * notTriedBy) / (triedBy + notTriedBy);
	}

	@ScheduledMethod(start = 1, priority = RunPriority.NEXT_STEP_FIRM_PRIORITY, interval = 1)
	public void nextStep() {
		// This is run after all offers are made and consumers have chosen
		// Calculates profit, accumProfit and kills the firm if necessary

		// Calculate profits of period
		profit = calcProfit();

		accumProfit += getProfit();

		if (born == RepastEssentials.GetTickCount())
			// Entry moment
			autoRegressiveProfit = getProfit();
		else
			autoRegressiveProfit = currentProfitWeight * getProfit() + (1 - currentProfitWeight) * autoRegressiveProfit;

		if (isToBeKilled())
			Market.toBeKilled.add(this);

		else
			// Updates Projections of results
			updateProjections();

	}

	@ScheduledMethod(start = 1, priority = RunPriority.UPDATE_PROJECTIONS_PRIORITY, interval = 1)
	public void updateProjections() {
		Scale.update(this);
		Market.firms2DProjection.update(this);
		Market.firmsDemandProjection.update(this);
		Market.firmsProfitProjection.update(this); //
		Market.margUtilProjection.update(this);
	}

	private void initializeConsumerKnowledge() {

		notYetKnownBy.addAll(Market.consumers);

		// Take out of the list the initial "knower's"
		getFromIgnorance((int) FastMath.round((Double) GetParameter("initiallyKnownByPerc") * Market.consumers.size()));

	}

	private void getFromIgnorance(int amount) {
		Consumer c;

		for (int k = 0; (k < amount) && !notYetKnownBy.isEmpty(); k++) {

			int i = RandomHelper.getUniform().nextIntFromTo(0, notYetKnownBy.size() - 1);

			c = notYetKnownBy.get(i);
			notYetKnownBy.remove(i);
			c.addToKnownFirms(this);

		}
	}

	private void updateConsumerKnowledge() {
		Consumers consumers = Market.consumers;
		int mktSize = consumers.size();

		// if all Consumers know the firm then return
		if (notYetKnownBy.isEmpty())
			return;

		// Take some consumers out of "ignorance" and let them know this
		// firm
		// Knowledge is spread using the logistic growth equation (Bass
		// model with 100% imitators)

		double alreadyK = mktSize - notYetKnownBy.size();

		int knownByIncrement = (int) FastMath
				.round(Market.firms.diffusionSpeedParam * alreadyK * (1.0 - alreadyK / mktSize));

		knownByIncrement = FastMath.min(mktSize, knownByIncrement);
		knownByIncrement = FastMath.max(1, knownByIncrement);

		getFromIgnorance(knownByIncrement);

	}

	private double calcProfit() {
		return (getPrice() - getUnitCost(getQuality())) * getDemand() - fixedCost;
	}

	public double getUnitCost(double quality) {
		// Cost grows quadratically with quality
		return FastMath.pow(quality / costParameter, 2.0);
	}

	public double getMarginalCostOfQuality(double q) {
		return 2.0 / FastMath.pow(costParameter, 2.0) * q;
	}

	private boolean isToBeKilled() {
		// Returns true if firm should exit the market
		return (autoRegressiveProfit < Market.firms.minimumProfit);
	}

	public void killFirm() {

		Market.firms.removeFromFirmLists(this);

		// Remove firm from consumers lists
		Market.consumers.forEach((c) -> c.removeTraceOfFirm(this));

		Market.firms.remove(this);

	}

	public void addNewConsumer() {
		triedBy++;
	}

	public void setDemand(int i) {
		demand = i;
	}

	/*
	 * Setters to probe DO NOT USE FOR CODE
	 */
	public void setPrice(double p) {
		// Market.firms.removeFromFirmLists(this);

		Offer o = getOffer();
		o.setPrice(p);

		Market.firms.addToFirmLists(this);
	}

	public void setQuality(double q) {
		// Market.firms.removeFromFirmLists(this);

		Offer o = getOffer();
		o.setQuality(q);

		Market.firms.addToFirmLists(this);
	}

	public double getExpectedMargin() {
		return Market.firms.perceivedQSegments.getExpectedDemand(this)
				* (offer.getPrice() - getUnitCost(offer.getQuality()));
	}

	public double getExpectedProfit() {
		return getExpectedMargin() - fixedCost;
	}

	/*
	 * Getters to probe
	 */

	public int getDemand() {
		return demand;
	}

	public Offer getOffer() {
		return offer;
	}

	public double getProfit() {
		return profit;
	}

	public double getPrice() {
		return offer.getPrice();
	}

	public double getQuality() {
		return offer.getQuality();
	}

	public double getAge() {
		return RepastEssentials.GetTickCount() - born;
	}

	public String getFirmType() {
		return "Firm";
		// return getClass().toString().substring(16, 17);
	}

	public String getFirmID() {
		return getFirmType() + " " + getFirmNumID();
	}

	public long getFirmIntID() {
		return firmIntID;
	}

	public String getFirmNumID() {
		return Long.toString(firmIntID);
	}

	public double getAccumProfit() {
		return accumProfit;
	}

	public double getAutoRegressiveProfit() {
		return autoRegressiveProfit;
	}

	public double getFixedCost() {
		return fixedCost;
	}

	public double getGrossMargin() {
		return (getPrice() - getUnitCost(getQuality())) / getPrice();
	}

	public double getMargin() {
		return getProfit() / (getDemand() * getPrice());
	}

	public String toString() {
		return ID;
	}

}
