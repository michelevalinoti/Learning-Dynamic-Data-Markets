import org.apache.commons.math3.distribution.*;

public class Seller {

	// HOMOGENEOUS PARAMETERS OF SELLERS
	private final double delta;		 // interest rate
	private String costDistribution; // name of cost distribution

	// TYPE OF THE SELLER
	private final int arrivalTime; // arrival time of the seller
	private final double cost;	   // cost of the seller
	private String sellerStrategy; /* strategy of the seller; possible values:
									  "myopic"
									  "strategic" */

	// REPORTS OF THE SELLER
	private int reportedTime = 0;
	private Boolean participate = false;

	private double currentPromisedPayment = 0;
	private double expectedFuturePayment = 0;

	public double receivedPayment = 0;
	public double sellerSurplus = 0;

	public Seller(int arrival, String strategy, double delta, String distribution, double[] parameters) throws Exception {

		this.delta = delta;
		this.costDistribution = distribution;

		this.arrivalTime = arrival;
		this.sellerStrategy = strategy;

		double randomCost = 0;
		if (costDistribution == "uniform") {
			double lowerBound = parameters[0];
			double upperBound = parameters[1];
			randomCost = computeUniformCost(costDistribution, (double) lowerBound, (double) upperBound);
		}
		else if (costDistribution == "exponential") {
			double rate = parameters[0];
			randomCost = computeExponentialCost(costDistribution, (double) rate);
		};
		this.cost = randomCost;
	}
	

	public void participateToSellingPeriod(int time) {

		if (sellerStrategy == "myopic") {
			myopicParticipation(time);
		}
		if (sellerStrategy == "strategic") {
			strategicParticipation(time);
		}
	}

	public void myopicParticipation(int time) {

		this.participate = true;
		this.reportedTime = time;
	}

	public void strategicParticipation(int time) {

		if (this.currentPromisedPayment < this.expectedFuturePayment) {
			this.participate = false;
		} else {
			this.participate = true;
			this.reportedTime = time;
		};
	}

	public int getArrivalTime() {

		return this.arrivalTime;
	}

	public double getCost() {

		return this.cost;
	}

	public int getReportedTime() {

		return this.reportedTime;
	}

	public Boolean getParticipationDecision() {

		return this.participate;
	}

	public void setCurrentPromisedPayment(double currentPromisedPayment) {

		this.currentPromisedPayment = currentPromisedPayment;
	}

	public void setFutureExpectedPayment(double futureExpectedPayment) {

		this.expectedFuturePayment = futureExpectedPayment;
	}

	public double getReceivedPayment() {

		return this.receivedPayment;
	}

	public void setReceivedPayment(double receivedPayment) {

		this.receivedPayment = receivedPayment;
	}

	public void setSellerSurplus() {

		this.sellerSurplus = Math.pow(delta, reportedTime-arrivalTime)*receivedPayment-cost;

	}

	public double computeUniformCost(String costDistribution, double upperBound, double lowerBound) {

		double costDraw = 0;
		if (costDistribution == "uniform") {
			if (lowerBound >= 0 && upperBound > lowerBound) {
				costDraw = generateUniformDraw(lowerBound, upperBound);
			} else if (lowerBound >= 0 && lowerBound > upperBound) {
				costDraw = generateUniformDraw(upperBound, lowerBound);
			} else {
				new Exception("Support of the uniform distribution must be positive");
			}
			;
		} else {
			new Exception(
					"Distribution of sellers not recognised. You should insert here either 'uniform' and bounds of the support of the distribution");
		}
		;

		return costDraw;
	}

	public double computeExponentialCost(String costDistribution, double rate) {

		double costDraw = 0;
		if (costDistribution == "exponential") {
			if (rate > 0) {
				costDraw = generateExponentialDraw(rate);
			} else {
				new Exception("Rate of the exponential distribution must be positive");
			}
			;
		} else {
			new Exception(
					"Distribution of sellers cost not recognised. You should insert here either 'exponential' and the rate of exponential distribution");
		}
		;

		return costDraw;
	}

	public static double generateUniformDraw(double lowerBound, double upperBound) {

		UniformRealDistribution uniform = new UniformRealDistribution(lowerBound, upperBound);
		return uniform.sample();
	}

	public static double generateExponentialDraw(double lambda) {

		ExponentialDistribution exponential = new ExponentialDistribution(lambda);
		return exponential.sample();
	}

	public static double F(double p, String distribution, double[] parameters) {

		double cumulative = 0;

		if (distribution == "uniform") {

			double lowerBound = parameters[0];
			double upperBound = parameters[1];

			if (p < lowerBound) {
				cumulative = 0;
			} else if (p > upperBound) {
				cumulative = 1;
			} else {
				cumulative = (p-lowerBound)/(upperBound-lowerBound);
			};
		} else if (distribution == "exponential") {

			double lambda = parameters[0];

			if (p < 0) {
				cumulative = 0;
			} else {
				cumulative = 1 - Math.exp(-lambda * p);
			};
		};

		return cumulative;
	}

	public static double f(double p, String distribution, double[] parameters) {

		double density = 0;

		if (distribution == "uniform") {

			double lowerBound = parameters[0];
			double upperBound = parameters[1];

			if (p < lowerBound || p > upperBound) {
				density = 0;
			} else {
				density = 1;
			};
		} else if (distribution == "exponential") {

			double lambda = parameters[0];

			if (p < 0) {
				density = 0;
			} else {
				density = lambda * Math.exp(-lambda * p);
			};
		};

		return density;
	}

}
