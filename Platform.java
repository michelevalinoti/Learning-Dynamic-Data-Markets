import java.util.*;

public class Platform {

	// PARAMETERS OF THE MODEL
	protected final double rate;  // arrival rate of sellers
	
	protected final int N; 		  // maximum number of databases
	protected final double gamma; // buyers' discount factor
	protected final double delta; // interest rate

	protected final String costDistribution; /* name of cost distribution; possible values:
										  	 "uniform": U[a,b]
									         "exponential": E(lambda) */
	protected final double[] costParameters; /* parameters of the distribution above
									   	 [a,b] for "uniform"
									     [lambda] for "exponential" */

	// STATE VARIABLE(S)
	protected int databases = 0;
	protected double belief;		// current belief about the state of the world to be high

	protected double transitionProbability = 0;
	
	// (POSTED) PAYMENTS FOR SELLERS
	protected double currentPayment = 0;
	protected double expectedFuturePayment = 0;
	protected double expectedFutureTotalPayments = 0;

	// POSTED PRICES FOR BUYERS
	protected double currentPostedPrice = 0;
	protected double expectedFuturePostedPrice = 0;
	protected double currentPromisedBuyerSurplus = 0;
	protected double expectedFutureBuyerSurplus = 0;

	// GIVEN AND RECEIVED PAYMENTS
	protected double paymentsToSellersSoFar = 0;
	protected double paymentsFromBuyersSoFar = 0;

	protected double discountedPaymentsToSellersSoFar = 0;
	protected double discountedPaymentsFromBuyersSoFar = 0;
	
	protected double deficit = 0;
	protected double averageDeficit = 0;

	public Platform(double rate, int N, double gamma, double delta, String distribution, double[] parameters) {

		this.rate = rate;
		this.N = N;
		this.gamma = gamma;
		this.delta = delta;
		this.costDistribution = distribution;
		this.costParameters = parameters;
	}
	
	public void computeValuesAndPayments() {
		
	}
	
	public void computeFutureTotalPayments() {
	
	}
	
	public int allocationPolicy(ArrayList<Seller> eligibleSellers) {
		
		Random random = new Random();
		int chosenSellerIndex = random.nextInt(eligibleSellers.size());
		return chosenSellerIndex;
	}
	
	public int getDatabases() {
		
		return this.databases;
	}
	
	public void setDatabases(int numberDatabases) {
		
		this.databases = numberDatabases;
	}
	
	public double getBelief() {
		
		return this.belief;
	}

	public void setBelief(double newBelief) {
		
		this.belief = newBelief;
	}
	
	public double getTransitionProbability() {
		
		return this.transitionProbability;
	}

	public void setTransitionProbability() {
		
		this.transitionProbability = rate*Seller.F(currentPayment, costDistribution, costParameters);
	}
	
	public double getCurrentPayment() {
		
		return this.currentPayment;
	}
	
	public void setCurrentPayment() {
		
	}
	
	public double getExpectedFuturePayment() {
		
		return this.expectedFuturePayment;
	}

	public void setExpectedFuturePayment() {
		
	}
	
	public double getExpectedFutureTotalPayments() {
		
		return this.expectedFutureTotalPayments;
	}
	
	public void setExpectedFutureTotalPayments() {
		
	}
	
	public double getCurrentPostedPrice() {
		
		return this.currentPostedPrice;
	}

	public void setCurrentPostedPrice() {
		/*System.out.println("FTP: " +getExpectedFutureTotalPayments());
		System.out.println("payments from buyers: " + getDiscountedPaymentsFromBuyersSoFar());
		System.out.println("payments to sellers: " + getDiscountedPaymentsToSellersSoFar());*/
		//this.currentPostedPrice = Price.computePostedPrice(delta, getExpectedFutureTotalPayments(), getDiscountedPaymentsToSellersSoFar(), getDiscountedPaymentsFromBuyersSoFar());
		this.currentPostedPrice = Price.computePostedPrice(delta, expectedFutureTotalPayments, deficit);

	}
	
	public double getExpectedFuturePostedPrice() {
		
		return this.expectedFuturePostedPrice;
	}
	
	public void setExpectedFuturePostedPrice() {
		
	}
	
	public double getCurrentPromisedSurplus() {
		
		return this.currentPromisedBuyerSurplus;
	}
	
	public void setCurrentPromisedSurplus(int actualArrivalTime, int reportedArrivalTime) {
		
		this.currentPromisedBuyerSurplus = Buyer.surplus(gamma, databases, actualArrivalTime, reportedArrivalTime, currentPostedPrice);
	}
	
	public double getExpectedFutureBuyerSurplus() {
		
		return this.expectedFutureBuyerSurplus;
	}
	
	public void setExpectedFutureBuyerSurplus(int actualArrivalTime, int reportedArrivalTime) {
		
		this.expectedFutureBuyerSurplus = Buyer.futureExpectedSurplus(gamma, databases, actualArrivalTime, reportedArrivalTime, expectedFuturePostedPrice, transitionProbability);
	}
	
	public double getPaymentsToSellersSoFar() {
		
		return this.paymentsToSellersSoFar;
	}
	
	public double getPaymentsFromBuyersSoFar() {
		
		return this.paymentsFromBuyersSoFar;
	}
	
	public double getDiscountedPaymentsToSellersSoFar() {
		
		return this.discountedPaymentsToSellersSoFar;
	}
	
	public double getDiscountedPaymentsFromBuyersSoFar() {
		
		return this.discountedPaymentsFromBuyersSoFar;
	}
	public void setDiscountedPaymentsAndPrices() {
		
		this.discountedPaymentsToSellersSoFar /= delta;
		this.discountedPaymentsFromBuyersSoFar /= delta;
		this.deficit /= delta;
		this.averageDeficit = deficit/databases;
		
	}
	public void setPaymentsToSellersSoFar(double lastPayment) {
		
		this.paymentsToSellersSoFar += lastPayment;
		this.discountedPaymentsToSellersSoFar += lastPayment;
		this.deficit += lastPayment;
	}

	public void setPaymentsFromBuyersSoFar(double lastPayment) {
		
		this.paymentsFromBuyersSoFar += lastPayment;
		this.discountedPaymentsFromBuyersSoFar += lastPayment;
		this.deficit -= lastPayment;
	}
	
	public double getDeficit() {
		
		return this.deficit;
	}
	
	public double getAverageDeficit() {
		
		return this.averageDeficit;
	}
	
}
