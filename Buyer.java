import java.lang.Math;

public class Buyer {

	// HOMOGENEOUS PARAMETERS OF BUYERS
	private final double gamma;

	// TYPE OF THE BUYER
	private final int arrivalTime;
	private String buyerStrategy;

	// REPORTS OF THE BUYER
	private int reportedTime = 0;
	private Boolean submit = false;

	public double currentPromisedSurplus = 0;
	public double expectedFutureSurplus = 0;

	public double paidPrice = 0;
	public double buyerSurplus = 0;

	public Buyer(int arrival, String strategy, double gamma) {

		this.arrivalTime = arrival;
		this.gamma = gamma;
		this.buyerStrategy = strategy;
	}

	public void decideIfSubmitQuery(int time) {
		if (buyerStrategy == "myopic") {
			myopicParticipation(time);
		}
		else if(buyerStrategy == "strategic") {
			strategicParticipation(time);
		};
	}

	public void myopicParticipation(int time) {

		this.submit = true;
		this.reportedTime = time;
	}

	public void strategicParticipation(int time) {

		if (this.currentPromisedSurplus < this.expectedFutureSurplus) {
			this.submit = false;
		}
		else {
			this.submit = true;
			this.reportedTime = time;
		};

	}

	public int getArrivalTime() {

		return this.arrivalTime;
	}

	public int getReportedTime() {

		return this.reportedTime;
	}

	public Boolean getAskDecision() {

		return this.submit;
	}

	public void setCurrentPromisedSurplus(double currentPromisedSurplus) {

		this.currentPromisedSurplus = currentPromisedSurplus;
	}

	public void setExpectedFutureSurplus(double expectedFutureSurplus) {

		this.expectedFutureSurplus = expectedFutureSurplus;
	}

	public double getPaidPrice() {

		return this.paidPrice;
	}

	public void setPaidPrice(double paymentFromBuyer) {

		this.paidPrice = paymentFromBuyer;
	}

	public double getBuyersSurples() {

		return this.buyerSurplus;
	}

	public void setBuyerSurplus(int k) {

		double surplus = V(k) - paidPrice;
		surplus = Math.pow(gamma, reportedTime-arrivalTime)*surplus;
		this.buyerSurplus = surplus;
	}

	public static double surplus(double gamma, int k, int actualArrivalTime, int reportedArrivalTime, double price){

		double surplus = V(k) - price;
		surplus = Math.pow(gamma, reportedArrivalTime-actualArrivalTime)*surplus;
		return surplus;
	}

	public static double futureExpectedSurplus(double gamma, int k, int actualArrivalTime, int reportedArrivalTime, double price, double transitionProbability) {

		double expectedSurplus = gamma*transitionProbability*surplus(gamma, k+1, actualArrivalTime, reportedArrivalTime+1, price);
		expectedSurplus += gamma*(1-transitionProbability)*surplus(gamma, k, actualArrivalTime, reportedArrivalTime+1, price);
		return expectedSurplus;
	}

	public static double V(int k) {

		return Math.sqrt(k);
	}
}
