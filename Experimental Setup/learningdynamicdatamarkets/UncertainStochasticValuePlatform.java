package learningdynamicdatamarkets;

public class UncertainStochasticValuePlatform extends UncertainValuePlatform {

	public UncertainStochasticValuePlatform(double rate, int N, double gamma, double delta, String distribution, double[] parameters, String buyerType, double[] priorParameters, String policy) {
		
		super(rate, N, gamma, delta, distribution, parameters, buyerType, priorParameters, policy);
		
		this.alpha = priorParameters[0];
		this.beta = priorParameters[1];
	}
	
	
	public double computeExpectedRho(double lowerRho, double upperRho) {
		
		return beta/(alpha-1);
	}
}
