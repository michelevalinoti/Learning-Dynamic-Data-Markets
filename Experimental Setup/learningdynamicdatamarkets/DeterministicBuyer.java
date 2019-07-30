package learningdynamicdatamarkets;

public class DeterministicBuyer extends Buyer {
	
	private double trueRho;
	
	public DeterministicBuyer(int arrival, String strategy, double gamma, double trueRho) {

		super(arrival, strategy, gamma);
		
		this.trueRho = trueRho;
		
	}

	public double value(int k) {
		
		return trueRho*upsilon(k);
	}
	
	public static double upsilon(int k) {
		
		return Math.sqrt(k);
	}
	
	public double getTrueRho() {
		
		return this.trueRho;
	}
}
