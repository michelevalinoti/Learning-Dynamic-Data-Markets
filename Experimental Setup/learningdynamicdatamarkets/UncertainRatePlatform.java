package learningdynamicdatamarkets;

import java.util.*;

public class UncertainRatePlatform extends Platform {

	private final String paymentPolicy; // chosen policy, e.g. "MBP", "LBP", "ICLBP"

	private double[][] LBPValues; 	// 2-array of MDP values nu[k][b], with k=#databases, b=belief
	private double[][] LBPPayments; // as above, for payments p[k][b]
	private double[][] LBPFutureTotalPayments; // as above, for future total payments ftp[k][b]

	public int M = 99801; // length of the array along dimension of beliefs

	public UncertainRatePlatform(int N, double gamma, double delta, String distribution,
			double[] parameters, double thetaHigh, double thetaLow, double priorBelief, String paymentPolicy) {

		super(0, N, gamma, delta, distribution, parameters);

		this.thetaHigh = thetaHigh;
		this.thetaLow = thetaLow;
		this.belief = priorBelief;

		this.paymentPolicy = paymentPolicy;

		this.LBPValues = new double[N+1][M];
		this.LBPPayments = new double[N+1][M];
		this.LBPFutureTotalPayments = new double[N+1][M];
	}

	public void computeValuesAndPayments() {

		if(paymentPolicy == "LBP" || paymentPolicy == "ICLBP") {
			Map<String, double[][]> LBPValuesAndPayments = LearningBayesianPolicyPayment.computeLBPValuesAndPayments(N, gamma, thetaHigh, thetaLow, costDistribution, costParameters);
			this.LBPValues = LBPValuesAndPayments.get("LBPValues");
			this.LBPPayments = LBPValuesAndPayments.get("LBPPayments");	
		}
	}

	public void computeFutureTotalPayments() {

		if(paymentPolicy == "LBP") {
			this.LBPFutureTotalPayments = LearningBayesianPolicyPayment.computeFutureTotalPayments(N, gamma, delta, thetaHigh, thetaLow, costDistribution, costParameters, getLBPPayments());
		}
	}

	public void setTransitionProbability() {

		this.transitionProbability = theta(getBelief(), thetaHigh, thetaLow)*Seller.F(currentPayment, costDistribution, costParameters);
	}

	public void setCurrentPayment() {

		double lastPayment = getCurrentPayment();
		double currentPayment = 0;
		if(paymentPolicy == "MBP") {
			currentPayment = MyopicBayesianPolicyPayment.computeMLPPayment(databases, belief, thetaHigh, thetaLow, N, gamma, delta, costDistribution, costParameters);
		}
		else if(paymentPolicy == "LBP") {
			currentPayment = interpolator(belief, LBPPayments[databases]);
		}
		else if(paymentPolicy == "ICLBP") {
			double currentPaymentAccordingToLBP = interpolator(belief, LBPPayments[databases]);
			double presentValueOfLastPayment = lastPayment/delta;
			currentPayment = Math.min(currentPaymentAccordingToLBP, presentValueOfLastPayment);
		};

		this.currentPayment = currentPayment;
	}

	// check until here
	
	public double[][] getLBPValues(){

		return this.LBPValues;
	}	

	public double[][] getLBPPayments(){

		return this.LBPPayments;
	}

	public double[][] getLBPFutureTotalPayments() {

		return this.LBPFutureTotalPayments;
	}

	public void setExpectedFuturePayment() {

		double expectedFuturePayment = 0;
		if(paymentPolicy == "MBP") {
			expectedFuturePayment = MyopicBayesianPolicyPayment.computeMLPPayment(databases, belief+db0(belief, thetaHigh, thetaLow, currentPayment, costDistribution, costParameters), thetaHigh, thetaLow, N, gamma, delta, costDistribution, costParameters);
			expectedFuturePayment *= delta;	 
		}
		else if(paymentPolicy == "LBP") {

			expectedFuturePayment = interpolator(belief+UncertainRatePlatform.db0(belief, thetaHigh, thetaLow, currentPayment, costDistribution, costParameters), LBPPayments[databases]);
			expectedFuturePayment *= delta;
		};

		this.expectedFuturePayment = expectedFuturePayment;
	}

	public void setExpectedFutureTotalPayments() {

		double expectedFutureTotalPayments = 0;
		if(paymentPolicy == "MBP") {
			//double[] MBPPayments = Payment.computeFullInformationValuesAndPayments(rate, N, gamma, costDistribution, costParameters).get("fullInformationPayments");
			//expectedFutureTotalPayments = Payment.computeFutureTotalPayments(UncertainRatePlatform.theta(belief, thetaHigh, thetaLow), N, gamma, delta, costDistribution, costParameters, MBPPayments)[databases];
		}
		else if(paymentPolicy == "LBP"){
			expectedFutureTotalPayments = interpolator(belief,LBPFutureTotalPayments[databases]);
		};
		this.expectedFutureTotalPayments = expectedFutureTotalPayments;

	}

	public void setExpectedFuturePostedPrice() {

		if(paymentPolicy == "MBP") {
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
			double MBPFuturePostedPrice = transitionProbability*Price.computePostedPrice(delta, expectedFutureTotalPaymentsAtNextState, deficit+currentPayment);
			MBPFuturePostedPrice += (1-transitionProbability)*Price.computePostedPrice(delta, expectedFutureTotalPaymentsAtTheSameState, deficit); 
			MBPFuturePostedPrice *= delta;
			this.expectedFuturePostedPrice = MBPFuturePostedPrice;
		}
		else if(paymentPolicy == "LBP") {
			this.expectedFuturePostedPrice = currentPostedPrice;
		}
	}

	// checked from here

	public static double db1(double b, double thetaHigh, double thetaLow) {

		double deltaTheta = thetaHigh-thetaLow;
		return b*(1-b)*deltaTheta/theta(b, thetaHigh, thetaLow);
	}

	public static double db0(double b, double thetaHigh, double thetaLow, double payment, String distribution, double[] parameters) {

		double deltaTheta = thetaHigh-thetaLow;
		double cumulative = Seller.F(payment, distribution, parameters);
		return -b*(1-b)*deltaTheta*cumulative/(1-theta(b, thetaHigh, thetaLow)*cumulative);
	}

	public static double theta(double b, double thetaHigh, double thetaLow) {

		return thetaHigh*b + thetaLow*(1-b);
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
			double ratio = differenceWithUpperIndex/(differenceWithUpperIndex+differenceWithLowerIndex);
			interpolatedValue = ratio*policyArray[lowerIndex]+ratio*policyArray[upperIndex];
		};

		return interpolatedValue;
	}
	
	// to here

}
