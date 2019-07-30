package learningdynamicdatamarkets;

import java.util.*;

public class UncertainValuePlatform extends Platform {

	protected final String valueType;
	protected final String pricingPolicy;
	protected double futurePostedPriceIfTransition = 0;

	protected double[] fullInformationValues;
	protected double[] fullInformationPayments;
	protected double[] fullInformationFutureTotalPayments;

	public UncertainValuePlatform(double rate, int N, double gamma, double delta, String distribution, double[] parameters,String valueType,  double[] priorParameters, String pricingPolicy) {

		super(rate, N, gamma, delta, distribution, parameters);
		
		this.valueType = valueType;
		this.pricingPolicy = pricingPolicy;
		
		this.lowerRho = priorParameters[0];
		this.upperRho = priorParameters[1];
	}
	
	public void setCurrentPayment() {
	
		this.currentPayment = MyopicSurvivalAnalysis.computeSurvivalAnalysisPayment(rate, N, databases, gamma, costDistribution, costParameters, expectedRho);
	}

	// check from here
	
	public void setExpectedFuturePayment() {
		
		double todayPrice = getCurrentPostedPrice();
		double todayPayment = getCurrentPayment();
		double threshold = computeThreshold(todayPrice, todayPayment);
		//double notDiscountedExpectedFuturePayment = transitionProbability*delayProbability*MyopicSurvivalAnalysis.computeSurvivalAnalysisPayment(rate, N, databases+1, gamma, costDistribution, costParameters, beta/(alpha+threshold-1));
		//notDiscountedExpectedFuturePayment += transitionProbability*(1-delayProbability)*MyopicSurvivalAnalysis.computeSurvivalAnalysisPayment(rate, N, databases+1, gamma, costDistribution, costParameters, (beta+threshold)/(alpha-1));
		//notDiscountedExpectedFuturePayment += (1-transitionProbability)*delayProbability*MyopicSurvivalAnalysis.computeSurvivalAnalysisPayment(rate, N, databases, gamma, costDistribution, costParameters, beta/(alpha+threshold-1));
		//notDiscountedExpectedFuturePayment += (1-transitionProbability)*(1-delayProbability)*MyopicSurvivalAnalysis.computeSurvivalAnalysisPayment(rate, N, databases, gamma, costDistribution, costParameters, (beta+threshold)/(alpha-1));
		//this.expectedFuturePayment = delta*notDiscountedExpectedFuturePayment;
	}
	
	public void setExpectedFutureTotalPayments() {
		
		double[] payments = MyopicSurvivalAnalysis.computeSurvivalAnalysisValuesAndPaymentsUpToStateK(rate, N, databases, gamma, costDistribution, costParameters, expectedRho).get("fullInformationPayments");
		double[] futurePayments = new double[N+1];
		for(int j = 0; j<databases; j++) {
			futurePayments[j] = 0;
		}
		for(int j = databases; j<= N; j++) {
			futurePayments[j] = payments[j-databases];
		}
		
		double expectedFutureTotalPayments = Payment.computeFutureTotalPaymentsAtStateK(rate, N, gamma, delta, costDistribution, costParameters, futurePayments, databases);
		this.expectedFutureTotalPayments = expectedFutureTotalPayments;
	}
	
	public void setCurrentPostedPrice() {
		
		if(pricingPolicy == "passive") {
			this.currentPostedPrice = Price.computePostedPrice(delta, expectedFutureTotalPayments, deficit);
		}
		else if(pricingPolicy == "quasi-active") {
			
			double futurePostedPrice = this.futurePostedPriceIfTransition;
			this.futurePostedPriceIfTransition = Price.computeSurvivalAnalysisPostedPrice(databases, rate, gamma, expectedRho, currentPayment, currentPostedPrice, costDistribution, costParameters);
			//System.out.println("FPPIFT: "+futurePostedPriceIfTransition);
			if(databases == 0) {
				this.currentPostedPrice = 0.0;
				//this.currentPostedPrice = Price.computePostedPrice(delta, expectedFutureTotalPayments, deficit);
				this.futurePostedPriceIfTransition = Price.computeSurvivalAnalysisPostedPrice(databases, rate, gamma, expectedRho, currentPayment, currentPostedPrice, costDistribution, costParameters);
			}
			else {
				System.out.println("SALE: "+sale);
				if(sale) {
				this.currentPostedPrice = futurePostedPrice;}
				else {
					this.currentPostedPrice = currentPostedPrice;
				};
			};
		};
	}
	
	public void setExpectedFuturePostedPrice() {
		
		this.expectedFuturePostedPrice = (transitionProbability*futurePostedPriceIfTransition+(1-transitionProbability)*currentPostedPrice); // since the process is a martingale
	}
	
	public void setExpectedFutureBuyerSurplus(int actualArrivalTime, int reportedArrivalTime) {
		
		this.expectedFutureBuyerSurplus = Buyer.futureExpectedSurplus(gamma, databases, actualArrivalTime, reportedArrivalTime, futurePostedPriceIfTransition, currentPostedPrice, transitionProbability)/gamma;
	}
	
	public void setDelayProbability(double price, double payment) {
		
		//this.delayProbability = 1/(alpha-1)*(1/Math.pow(beta, alpha-1)-1/Math.pow(beta+computeThreshold(price, payment),alpha-1));
	}
	
	public double computeThreshold(double price, double payment) {
		
		double threshold = 0;
		if(pricingPolicy=="passive") {
			threshold = ((1-gamma)*price)/(StochasticBuyer.upsilon(databases+1)*(1+(1-gamma)/(gamma*rate*Seller.F(payment, costDistribution, costParameters)))-StochasticBuyer.upsilon(databases));
		}
		else if(pricingPolicy =="quasi-active"){
			double bracketExpression = (1+(1-gamma)/(gamma*rate*Seller.F(payment, costDistribution, costParameters)));
			//System.out.println("BE "+bracketExpression);
			threshold = (bracketExpression*price-futurePostedPriceIfTransition)/(bracketExpression*StochasticBuyer.upsilon(databases)-StochasticBuyer.upsilon(databases+1));
		}
	
		return threshold;
	}

	public void setExpectedRho(double lowerRho, double upperRho) {
		
		this.expectedRho = 0.5*(lowerRho+upperRho);
	}
	
}