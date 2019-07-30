package learningdynamicdatamarkets;

import java.util.*;
import org.apache.commons.math3.distribution.*;

public class Market {

	public int time = 0; // time period (from 0 to the time at which number of databases is N)
	
	// (TRUE) PARAMETERS OF THE MODEL
	private final double rate;	// (true) arrival rate of sellers - to be learnt by 'UncertainRatePlatform'
	private final int N; 		// maximum number of databases
	private final double gamma; // buyers' discount factor
	private final double delta; // interest rate
	private double rho;			// (true) parameter of the value function - to be learnt by 'UncertainValuePlatform'
	private double lambda;		// (true) rate parameter of the exponential distribution of the value parameter - to be learnt by 'UncertainStochasticValuePlatform'
	private String valueType;	// choose value type, either "deterministic" or "stochastic"
	
	// STATE VARIABLE(S)
	private int databases = 0;		// current number of allocated databases
	
	private Boolean sale = false;	// flag showing whether today at least one seller decided to sell a database
	private Boolean submit = false;	// flag showing whether today at least one buyer decided to buy a database
	
	// AGENTS PRESENT IN THE MARKET OR ALREADY ALLOCATED
	private ArrayList<Seller> currentSellers;	// current sellers present in the market
	private ArrayList<Buyer> currentBuyers; 	// current buyers present in the market
	private ArrayList<Seller> allocatedSellers; // already allocated sellers in the market
	private ArrayList<Buyer> allocatedBuyers; 	// already allocated buyers in the market

	public Platform platform;
	
	public Market(double rate,
				  int N,
				  double gamma,
				  double delta,
				  String costDistribution,
				  double[] costParameters) {
		
		this.rate = rate;
		this.N = N;
		this.gamma = gamma;
		this.delta = delta;
		
		this.platform = new FullInformationPlatform(rate, N, gamma, delta, costDistribution, costParameters);
		createAgents();	
	}
	
	public Market(double rate,
				  int N,
				  double gamma,
				  double delta,
				  String costDistribution,
				  double[] costParameters,
				  double thetaHigh,		// highest possible arrival rate
				  double thetaLow,		// lowest possible arrival rate
				  double priorBelief,	// prior belief b_0, i.e. expected rate is thetaHigh*b_0 + thetaLow*(1-b_0)
				  String paymentPolicy 	// payment policy in the (rate) learning environment, e.g. "MBP", "LBP", "ICLBP".
				  ) {

		this.rate = rate;
		this.N = N;
		this.gamma = gamma;
		this.delta = delta;
		
		this.platform = new UncertainRatePlatform(N, gamma, delta, costDistribution, costParameters, thetaHigh, thetaLow, priorBelief, paymentPolicy);
		createAgents();
	}
	
	public Market(double rate,
				  double trueParameter,
				  int N,
				  double gamma,
				  double delta,
				  String costDistribution,
				  double[] costParameters,
				  String valueType, 		// type of the value function, either "deterministic" or "stochastic"
				  double[] priorParameters, /* initial (lowerRho, upperRho) in the deterministic case
				  							   initial (alpha_0, beta_0) in the stochastic case
				  							*/
				   
				  String pricingPolicy		// pricing (and payment) policy, either "passive" or "quasi-active"
				  ) {

		this.rate = rate;
		this.N = N;
		this.gamma = gamma;
		this.delta = delta;
		this.valueType = valueType;
		
		if(valueType == "deterministic") {
			this.rho = trueParameter;
		}
		else if(valueType == "stochastic") {
			this.lambda = trueParameter;
		};
		
		if(valueType == "deterministic") {
			this.platform = new UncertainValuePlatform(rate, N, gamma, delta, costDistribution, costParameters, valueType, priorParameters, pricingPolicy);
		}
		else if(valueType == "stochastic") {
			this.platform = new UncertainStochasticValuePlatform(rate, N, gamma, delta, costDistribution, costParameters, valueType, priorParameters, pricingPolicy);
		}
		
		createAgents();
	}
	
	public void createAgents() {
		
		this.currentSellers = new ArrayList<Seller>();
		this.currentBuyers = new ArrayList<Buyer>();
		this.allocatedSellers = new ArrayList<Seller>();
		this.allocatedBuyers = new ArrayList<Buyer>();
	}
	
	public void trade() throws Exception {
		
		initializePolicies();
		do {
			System.out.println("--- TIME " + time + " ---");
			onePeriodTrade();
		//} while(getDatabases()<N); // end simulation when the maximum number of databases is reached
		} while(time<100);
	}

	public void initializePolicies() {
		
		System.out.println(" --- Compute payment policies --- ");
		platform.computeValuesAndPayments(); /*some platforms/policies can compute all the possible payments in advance
												e.g. fullInformationPlatform, UncertainRatePaltform with LBP. */
		System.out.println(" --- Compute future total payments --- ");
		platform.computeFutureTotalPayments(); // same as above
		
	}

	// check from here
	
	public void onePeriodTrade() throws Exception {
		
		System.out.println("There are currently " + getDatabases() + " databases.");
		if(platform.getBelief() >= 0) {
			System.out.println("The current belief is " + platform.getBelief());
		if(platform.getUpperRho() > 0) {
			if(valueType == "deterministic") {
			System.out.println("The value of lower rho is " + platform.getLowerRho());
			System.out.println("The value of upper rho is " + platform.getUpperRho());
			}
			else if(valueType == "stochastic") {
			System.out.println("The value of alpha is " + platform.getAlpha());
			System.out.println("The value of beta is " + platform.getBeta());
			};
			System.out.println("The value of rho is " + platform.getExpectedRho());
			};
		}
		platform.setCurrentPayment();
		platform.setTransitionProbability();
		double todayPayment = platform.getCurrentPayment();
		System.out.println("Promised payment to the seller(s) today is: " + todayPayment);
		if(databases<N) {
		platform.setExpectedFuturePayment();
		double tomorrowExpectedPayment = platform.getExpectedFuturePayment();
		arrivalOfSeller(todayPayment, tomorrowExpectedPayment);
		platform.setExpectedFutureTotalPayments();
		double FTP = platform.getExpectedFutureTotalPayments();
		System.out.println("Expected future total payments are equal to " + FTP);
		platform.setCurrentPostedPrice();
		double todayPrice = platform.getCurrentPostedPrice();
		//if(alpha > 0 && beta > 0) {
		//}
		System.out.println("Posted price for a query is: " + todayPrice);
		arrivalOfBuyer(todayPrice);
		if(sale) {
			updateDatabases();
		}
		else {
			updatePessimisticBelief();
		}
		//setHyperparameters(todayPrice, todayPayment);
		};
		time = time + 1;
		System.out.println("Deficit of the platform is: " + platform.getDeficit());
		System.out.println("Average deficit of the platform is: " + platform.getAverageDeficit());
		platform.setDiscountedPaymentsAndPrices();
	}

	public void arrivalOfSeller(double todayPayment, double expectedFuturePayment) throws Exception {
		//
		System.out.println("The probability of a transition - if sellers are myopic - is: " + platform.getTransitionProbability());
		int bernoulliOutcome = generateBernoulliOutcome(rate);
		if (bernoulliOutcome == 1) {
			System.out.println("A seller has arrived!");
			Seller seller = new Seller(time, "myopic", delta, platform.getCostDistribution(), platform.getCostParameters());
			if (seller.getCost() <= todayPayment) {
				currentSellers.add(seller);
				sale = true;
				platform.setSale(true);
			}
			else {
				System.out.println("Payment too low for the seller!");
				sale = false;
				platform.setSale(false);
			};
		}
		else {
			System.out.println("No seller has arrived");
			sale = false;
			platform.setSale(false);
		};
		System.out.println("There are " + currentSellers.size() + " seller(s) in the market.");
		//
		System.out.println("For the sellers in the market, they expect a (discounted) payment equal to " + expectedFuturePayment + " tomorrow.");
		ArrayList<Seller> eligibleSellers = new ArrayList<Seller>();
		if (currentSellers.isEmpty() == false) {
			for (Seller currentSeller : currentSellers) {
				currentSeller.setCurrentPromisedPayment(todayPayment);
				currentSeller.setFutureExpectedPayment(expectedFuturePayment);
				currentSeller.participateToSellingPeriod(time);
				if (currentSeller.getParticipationDecision() == true) {
					eligibleSellers.add(currentSeller);
					//
					System.out.println("Seller arrived at time " + currentSeller.getArrivalTime() + " decided to report her arrival.");
				}
				else {
					//
					System.out.println("Seller arrived at time " + currentSeller.getArrivalTime() + " decided to delay her report.");
				};
			}
			if (eligibleSellers.isEmpty() == false) {
				System.out.println("There are " + eligibleSellers.size() + " eligible seller(s) in the market.");
				int chosenSellerIndex = platform.allocationPolicy(eligibleSellers);
				Seller chosenSeller = eligibleSellers.get(chosenSellerIndex);
				chosenSeller.setReceivedPayment(todayPayment);
				chosenSeller.setSellerSurplus();
				platform.setPaymentsToSellersSoFar(todayPayment);
				allocatedSellers.add(chosenSeller);
				currentSellers.remove(chosenSeller);
				System.out.println("There are " + currentSellers.size() + " seller(s) in the market.");
				sale = true;
				platform.setSale(true);
			}
			else {
				sale = false;
				platform.setSale(false);
			};
		}
	}

	public void arrivalOfBuyer(double todayPrice) {

		System.out.println("A buyer has arrived!");
		Buyer buyer = new Buyer(time, "strategic", gamma);
		platform.setExpectedFuturePostedPrice();
		double futurePrice = platform.getExpectedFuturePostedPrice();
		System.out.println("For buyers in the market, they expect to pay " + futurePrice + " tomorrow for a query.");
		platform.setCurrentPromisedSurplus(buyer.getArrivalTime(), time);
		double promisedSurplus = platform.getCurrentPromisedSurplus();
		platform.setExpectedFutureBuyerSurplus(buyer.getArrivalTime(), time);
		double futureSurplus = platform.getExpectedFutureBuyerSurplus();
		System.out.println("If the buyer submits today his query, he enjoys amount of utility: " + promisedSurplus);
		System.out.println("If the buyer submits tomorrow his query, he enjoys amount of utility: "+ platform.getExpectedFutureBuyerSurplus());
		currentBuyers.add(buyer);
		System.out.println("There are currently " + currentBuyers.size() + " buyer(s) in the market.");
		ArrayList<Buyer> eligibleBuyers = new ArrayList<Buyer>();
		if(currentBuyers.isEmpty() == false) {
			for(Buyer currentBuyer : currentBuyers) {
				currentBuyer.setExpectedFutureSurplus(futureSurplus);
				currentBuyer.setCurrentPromisedSurplus(promisedSurplus);
				currentBuyer.decideIfSubmitQuery(time);
				submit = false;
				if(currentBuyer.getAskDecision() == true) {
					eligibleBuyers.add(currentBuyer);
					submit = true;
				}	
			}
			for(Buyer eligibleBuyer: eligibleBuyers) {
				eligibleBuyer.setPaidPrice(todayPrice);
				eligibleBuyer.setBuyerSurplus(databases+1);
				allocatedBuyers.add(eligibleBuyer);
				currentBuyers.remove(eligibleBuyer);
				platform.setPaymentsFromBuyersSoFar(todayPrice);
			}
		}
		System.out.println("There are " + currentBuyers.size() + " remaining buyer(s) in the market.");
	}


	public int generateBernoulliOutcome(double p) {

		BinomialDistribution bernoulli = new BinomialDistribution(1, p);
		return bernoulli.sample();

	}
	
	
	
	public void updatePessimisticBelief() {
		
		double currentBelief = platform.getBelief();
		if(currentBelief >= 0) {
			platform.setBelief(currentBelief + UncertainRatePlatform.db0(currentBelief, platform.getThetaHigh(), platform.getThetaLow(), platform.getCurrentPayment(), platform.getCostDistribution(), platform.getCostParameters()));
		}
	}
	
	/*public void setHyperparameters(double todayPrice, double todayPayment) {
		
		double threshold = platform.computeThreshold(todayPrice, todayPayment);
		System.out.print("threshold is "+platform.computeThreshold(todayPrice, todayPayment));
		if(platform.getCurrentPromisedSurplus()>0) {
		if(alpha > 0 && beta > 0) {
		if(submit == false) {
			if(threshold > 0) {
			this.alpha = Math.max(alpha,threshold);
			//this.alpha = alpha + threshold;
			platform.setAlpha(this.alpha);}
		}
		else {
			if(threshold > 0) {
			//this.beta = beta+threshold;
			this.beta = Math.min(beta,threshold);
			platform.setBeta(this.beta);}
		}
		}
		}
	}*/
	
	public double getRate() {
		
		return this.rate;
	}
	
	public double getRho() {
		
		return this.rho;
	}
	
	public double getLambda() {
		
		return this.lambda;
	}
	
	public int getDatabases() {
		
		return this.databases;
	}
	
	public void updateDatabases() {
		
		this.databases += 1;
		platform.setDatabases(this.databases);
		platform.setTransitionProbability();
		double currentBelief = platform.getBelief();
		if(currentBelief >= 0) {
			platform.setBelief(currentBelief + UncertainRatePlatform.db1(currentBelief, platform.getThetaHigh(), platform.getThetaLow()));
			System.out.println("New belief is: " + platform.getBelief());
		}
	}
	
	
}
