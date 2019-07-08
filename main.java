

public class main {

	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
		double rate = 1;
		int N = 100;
		double gamma = 0.6;
		double delta = 0.9;
		String distribution = "uniform";
		double[] uniformParams = new double[]{0.0, 1.0};
		double thetalow = 0.4;
		double thetahigh = 0.8;
		double prior = 0.5;
		String policy = "MBP"; // choose either LBP or MBP
		// uncomment the line below to create a market in the full information environment
		//Market market = new Market(rate, N, gamma, delta, distribution, uniformParams);
		// uncomment the line below to create a market in the uncertain rate environment
		Market market = new Market(thetahigh, thetalow, prior, rate, N, gamma, delta, distribution, uniformParams, policy);
		market.Trade();
		
	} 

}
