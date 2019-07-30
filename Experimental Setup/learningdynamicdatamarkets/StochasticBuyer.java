package learningdynamicdatamarkets;

import org.apache.commons.math3.distribution.ExponentialDistribution;

public class StochasticBuyer extends Buyer {
	
	public double trueLambda;
	
	public StochasticBuyer(int arrival, String strategy, double gamma, double trueLambda) {

		super(arrival, strategy, gamma);
		
		this.trueLambda = trueLambda;
		
	}
	
	public double getTrueLambda() {
		
		return this.trueLambda;
	}
	
	public double value(int k) {

		return generateExponentialDraw()*upsilon(k);
		
	}
	
	public static double upsilon(int k) {
		
		return Math.sqrt(k);
	}
	
	public double generateExponentialDraw() {

		ExponentialDistribution exponential = new ExponentialDistribution(trueLambda);
		return exponential.sample();
		
	}

}
