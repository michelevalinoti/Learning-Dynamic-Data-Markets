import java.util.*;

public class FullInformationPlatform extends Platform {

	private double[] fullInformationValues; 			 // values from nu(0) to nu(N)
	private double[] fullInformationPayments; 			 // payments from p(1) to p(N), including p(N+1) = 0;
	private double[] fullInformationFutureTotalPayments; // future total payments from ftp(0) to ftp(N-1), including ftp(N) = 0;

	public FullInformationPlatform(double rate, int N, double gamma, double delta, String distribution, double[] parameters) {

		super(rate, N, gamma, delta, distribution, parameters);
		
		this.belief = -1;
		
		this.fullInformationPayments = new double[N+1]; 
		this.fullInformationValues = new double[N+1]; 
		this.fullInformationFutureTotalPayments = new double[N+1];

	}
	
	public void computeValuesAndPayments() {
		
		Map<String, double[]> fullInformationValuesAndPayments = Payment.computeFullInformationValuesAndPayments(rate, N, gamma, costDistribution, costParameters);
		this.fullInformationValues = fullInformationValuesAndPayments.get("fullInformationValues");
		this.fullInformationPayments = fullInformationValuesAndPayments.get("fullInformationPayments");
		
	}
	
	public void computeFutureTotalPayments() {
		
		this.fullInformationFutureTotalPayments = Payment.computeFutureTotalPayments(rate, N, gamma, delta, costDistribution, costParameters, fullInformationPayments);
	}
	
	public void setTransitionProbability() {
		
		this.transitionProbability = rate*Seller.F(currentPayment, costDistribution, costParameters);
	}
	
	public void setCurrentPayment() {
		
		this.currentPayment = fullInformationPayments[databases];
	}
	
	public void setExpectedFuturePayment() {
		
		double notDiscountedExpectedFuturePayment = fullInformationPayments[databases+1];
		this.expectedFuturePayment = delta*notDiscountedExpectedFuturePayment;
	}
	
	public void setExpectedFutureTotalPayments() {
		
		double expectedFutureTotalPayments = fullInformationFutureTotalPayments[databases];
		this.expectedFutureTotalPayments = expectedFutureTotalPayments;
	}
	
	public void setExpectedFuturePostedPrice() {
		
		this.expectedFuturePostedPrice = currentPostedPrice; // since the process is a martingale
	}
	
	public double[] getFullInformationValues(){
		
		return this.fullInformationValues;
	}

	public double[] getFullInformationPayments() {
		
		return this.fullInformationPayments;
		
	}
	
}