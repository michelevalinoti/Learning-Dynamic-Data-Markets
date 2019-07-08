import java.util.*;

public class UncertainRatePlatform extends Platform {

	private double thetaHigh;	// lowest possible arrival rate
	private double thetaLow;	// highest possible arrival rate
	private String policy;		// chosen policy, e.g. "MBP", "LBP", "ICLBP"

	private double[][] LBPValues;
	private double[][] LBPPayments;
	private double[][] LBPFutureTotalPayments;
			
	public int M = 9981;
	
	public UncertainRatePlatform(double thetaHigh, double thetaLow, double priorBelief, double rate, int N, double gamma, double delta, String distribution,
			double[] parameters, String policy) {

		super(rate, N, gamma, delta, distribution, parameters);
		
		this.thetaHigh = thetaHigh;
		this.thetaLow = thetaLow;
		
		this.belief = priorBelief;
		
		this.policy = policy;
		
		this.LBPValues = new double[N+1][M];
		this.LBPPayments = new double[N+1][M];
		this.LBPFutureTotalPayments = new double[N+1][M];
	}
	
	public void computeValuesAndPayments() {
	
		if(policy == "LBP") {
			Map<String, double[][]> LBPValuesAndPayments = LearningBayesianPolicyPayment.computeLBPValuesAndPayments(N, gamma, thetaHigh, thetaLow, costDistribution, costParameters);
			this.LBPValues = LBPValuesAndPayments.get("LBPValues");
			this.LBPPayments = LBPValuesAndPayments.get("LBPPayments");
			for(int k=0;k<N;k++) {
			System.out.println(LBPPayments[k][500]);}
		};

	}
	
	public void computeFutureTotalPayments() {
		
		if(policy == "LBP") {
			this.LBPFutureTotalPayments = LearningBayesianPolicyPayment.computeFutureTotalPayments(N, gamma, delta, thetaHigh, thetaLow, costDistribution, costParameters, getLBPPayments());
		}
		
	}
	
	public void setTransitionProbability() {
		
		this.transitionProbability = theta(getBelief(), thetaHigh, thetaLow)*Seller.F(currentPayment, costDistribution, costParameters);
	}

	public void setCurrentPayment() {
		
		double currentPayment = 0;
		if(policy == "MBP") {
			currentPayment = MyopicBayesianPolicyPayment.computeMLPPayment(databases, belief, thetaHigh, thetaLow, N, gamma, delta, costDistribution, costParameters);
		}
		else if(policy == "LBP") {
			currentPayment = interpolator(belief, LBPPayments[databases]);
		
		};
		
		this.currentPayment = currentPayment;
		setTransitionProbability();
		
	}
	
	public void setExpectedFuturePayment() {
		
		double expectedFuturePayment = 0;
		if(policy == "MBP") {
			 expectedFuturePayment = MyopicBayesianPolicyPayment.computeMLPPayment(databases, belief+db0(belief, thetaHigh, thetaLow, currentPayment, costDistribution, costParameters), thetaHigh, thetaLow, N, gamma, delta, costDistribution, costParameters);
			 expectedFuturePayment *= delta;	 
		}
		else if(policy == "LBP") {
			
			expectedFuturePayment = interpolator(belief+UncertainRatePlatform.db0(belief, thetaHigh, thetaLow, currentPayment, costDistribution, costParameters), LBPPayments[databases]);
			expectedFuturePayment *= delta;
		};
		
		this.expectedFuturePayment = expectedFuturePayment;
	}
	
	public void setExpectedFutureTotalPayments() {
		
		double expectedFutureTotalPayments = 0;
		if(policy == "MBP") {
			double[] MBPPayments = Payment.computeFullInformationValuesAndPayments(rate, N, gamma, costDistribution, costParameters).get("fullInformationPayments");
			expectedFutureTotalPayments = Payment.computeFutureTotalPayments(UncertainRatePlatform.theta(belief, thetaHigh, thetaLow), N, gamma, delta, costDistribution, costParameters, MBPPayments)[databases];
		}
		else if(policy == "LBP"){
			expectedFutureTotalPayments = interpolator(belief,LBPFutureTotalPayments[databases]);
		};
		this.expectedFutureTotalPayments = expectedFutureTotalPayments;
		
	}
	
	public void setExpectedFuturePostedPrice() {
		
		if(policy == "MBP") {
			double[] MBPPayments = Payment.computeFullInformationValuesAndPayments(rate, N, gamma, costDistribution, costParameters).get("fullInformationPayments");
			double newOptimisticBelief = belief+db1(belief, thetaHigh, thetaLow);
			double newPessimisticBelief = belief+db0(belief,thetaHigh,thetaLow, currentPayment, costDistribution, costParameters);
			double expectedFutureTotalPaymentsAtNextState = 0;
			if(databases<99) {
				expectedFutureTotalPaymentsAtNextState = Payment.computeFutureTotalPayments(theta(newOptimisticBelief, thetaHigh, thetaLow), N, gamma, delta, costDistribution, costParameters, MBPPayments)[databases+1];
			}
			else {
				expectedFutureTotalPaymentsAtNextState = 0;
			}
			double expectedFutureTotalPaymentsAtTheSameState = Payment.computeFutureTotalPayments(theta(newPessimisticBelief, thetaHigh, thetaLow), N, gamma, delta, costDistribution, costParameters, MBPPayments)[databases];
			double expectedFutureTotalPaymentAtTheNextTime = transitionProbability*expectedFutureTotalPaymentsAtNextState+(1-transitionProbability)*expectedFutureTotalPaymentsAtTheSameState;
			expectedFutureTotalPaymentAtTheNextTime *= delta;
			double MBPFuturePostedPrice = transitionProbability*Price.computePostedPrice(delta, expectedFutureTotalPaymentAtTheNextTime, discountedPaymentsToSellersSoFar+currentPayment, discountedPaymentsFromBuyersSoFar);
			this.expectedFuturePostedPrice = MBPFuturePostedPrice;
		}
		else if(policy == "LBP") {
		this.expectedFuturePostedPrice = currentPostedPrice;
		}
	}
	
	public double[][] getLBPValues(){
		
		return this.LBPValues;
	}	
	
	public double[][] getLBPPayments(){
		
		return this.LBPPayments;
	}
	
	public double[][] getLBPFutureTotalPayments() {
			
		return this.LBPFutureTotalPayments;
	}
	
	public static double theta(double b, double thetaHigh, double thetaLow) {
		
		return thetaHigh*b + thetaLow*(1-b);
	}
	
	public static double db1(double b, double thetaHigh, double thetaLow) {
		
		double deltaTheta = thetaHigh-thetaLow;
		return b*(1-b)*deltaTheta/theta(b, thetaHigh, thetaLow);
	}
	
	public static double db0(double b, double thetaHigh, double thetaLow, double payment, String distribution, double[] parameters) {
		
		double deltaTheta = thetaHigh-thetaLow;
		double F = Seller.F(payment, distribution, parameters);
		return -b*(1-b)*deltaTheta*F/(1-theta(b, thetaHigh, thetaLow)*F);
	}
	
	public static double interpolator(double b, double[] policyArray) {
		
		int M = policyArray.length;
		int lowerIndex = (int) Math.floor(b*M);
		int upperIndex = lowerIndex + 1;
		double differenceWithLowerIndex = b*M - lowerIndex;
		double differenceWithUpperIndex = upperIndex - b*M;
		double interpolatedValue = 0;
		if(lowerIndex<0) {
			interpolatedValue = policyArray[0];
		}
		else if(upperIndex>M-1) {
			interpolatedValue = policyArray[M-1];
		}
		else {
		interpolatedValue = differenceWithUpperIndex/(differenceWithUpperIndex+differenceWithLowerIndex)*policyArray[lowerIndex]+differenceWithLowerIndex/(differenceWithUpperIndex+differenceWithLowerIndex)*policyArray[upperIndex];
		};
		
		return interpolatedValue;
	}

}
