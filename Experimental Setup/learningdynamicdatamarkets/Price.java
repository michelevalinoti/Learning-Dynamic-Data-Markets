package learningdynamicdatamarkets;

public class Price {

	public static double computePostedPrice(double delta, double expectedFutureTotalPayments, double discountedPaymentsSoFar, double discountedPricesSoFar) {

		double postedPrice = (1-delta)*(expectedFutureTotalPayments + discountedPaymentsSoFar - discountedPricesSoFar);
		return postedPrice;
		
	}
	
	public static double computePostedPrice(double delta, double expectedFutureTotalPayments, double deficit) {

		double postedPrice = (1-delta)*(expectedFutureTotalPayments + deficit);
		return postedPrice;
		
	}

	public static double computeSurvivalAnalysisPostedPrice(int k, double rate, double gamma, double rho, double currentPayment, double currentPostedPrice, String distribution, double[] parameters) {
		
		double bracketExpression = (1+(1-gamma)/(gamma*rate*Seller.F(currentPayment, distribution, parameters)));
		return bracketExpression*currentPostedPrice - rho*(bracketExpression*StochasticBuyer.upsilon(k)-StochasticBuyer.upsilon(k+1));
	}
	
	
}