public class Price {

	public static double computePostedPrice(double delta, double expectedFutureTotalPayments, double discountedPaymentsSoFar, double discountedPricesSoFar) {

		double postedPrice = (1-delta)*(expectedFutureTotalPayments + discountedPaymentsSoFar - discountedPricesSoFar);
		return postedPrice;
		
	}
	
	public static double computePostedPrice(double delta, double expectedFutureTotalPayments, double deficit) {

		double postedPrice = (1-delta)*(expectedFutureTotalPayments + deficit);
		return postedPrice;
		
	}

}