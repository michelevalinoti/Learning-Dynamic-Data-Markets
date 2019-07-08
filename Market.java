import java.util.*;
import org.apache.commons.math3.distribution.*;

public class Market {

	public int time = 0;
	
	// PARAMETERS OF THE MODEL
	private double rate;		// arrival rate of sellers
	private final int N; 		// maximum number of databases
	private final double gamma; // buyers' discount factor
	private final double delta; // interest rate

	private final String costDistribution; /* name of cost distribution; possible values:
											  "uniform": U(a,b)
											  "exponential" E(lambda) */
	private final double[] costParameters; /* parameters of the distribution above
											  [a,b] for "uniform"
										 	  [lambda] for "exponential" */
	
	// ---- Uncertain arrival case:
	private double thetaLow;	// lowest possible arrival rate
	private double thetaHigh;	// highest possible arrival rate
	private String policy;		// chosen policy, e.g. "MBP", "LBP", "ICLBP"
	
	// STATE VARIABLE(S)
	private int databases = 0;	// current number of allocated databases
	
	private Boolean sale = false;
	
	// AGENTS PRESENT IN THE MARKET OR ALREADY ALLOCATED
	private ArrayList<Seller> currentSellers;	// current sellers present in the market
	private ArrayList<Buyer> currentBuyers; 	// current buyers present in the market
	private ArrayList<Seller> allocatedSellers; // already allocated sellers in the market
	private ArrayList<Buyer> allocatedBuyers; 	// already allocated buyers in the market

	private Platform platform;
	
	public Market(double rate, int N, double gamma, double delta, String distribution,
			double[] parameters) {

		this.rate = rate;
		this.N = N;
		this.gamma = gamma;
		this.delta = delta;

		this.costDistribution = distribution;
		this.costParameters = parameters;

		this.currentSellers = new ArrayList<Seller>();
		this.currentBuyers = new ArrayList<Buyer>();
		
		this.allocatedSellers = new ArrayList<Seller>();
		this.allocatedBuyers = new ArrayList<Buyer>();

		this.platform = new FullInformationPlatform(rate, N, gamma, delta, costDistribution, costParameters);
	}
	
	public Market(double thetaHigh, double thetaLow, double priorBelief, double rate, int N, double gamma, double delta, String distribution,
			double[] parameters, String policy) {

		this.rate = rate;
		this.N = N;
		this.gamma = gamma;
		this.delta = delta;

		this.thetaHigh = thetaHigh;
		this.thetaLow = thetaLow;
		
		this.costDistribution = distribution;
		this.costParameters = parameters;

		this.policy = policy;
		
		this.currentSellers = new ArrayList<Seller>();
		this.currentBuyers = new ArrayList<Buyer>();
		
		this.allocatedSellers = new ArrayList<Seller>();
		this.allocatedBuyers = new ArrayList<Buyer>();

		this.platform = new UncertainRatePlatform(thetaHigh, thetaLow, priorBelief, rate, N, gamma, delta, distribution, parameters, policy);
	}
	
	public void Trade() throws Exception {
		
		initializePolicies();
		do {
			System.out.println("--- TIME " + time + " ---");
			onePeriodTrade();
		} while(getDatabases()<N);
		//} while(time<100);
	}

	public void initializePolicies() {
		
		System.out.println(" --- Compute Payment policies --- ");
		platform.computeValuesAndPayments();
		System.out.println(" --- Compute Future total payments --- ");
		platform.computeFutureTotalPayments();
		
	}

	public void onePeriodTrade() throws Exception {
		
		System.out.println("There are currently " + getDatabases() + " databases.");
		if(platform.getBelief() >= 0) {
			System.out.println("The current belief is " + platform.getBelief());
		}
		platform.setCurrentPayment();
		platform.setTransitionProbability();
		double todayPayment = platform.getCurrentPayment();
		System.out.println("Promised payment to the seller(s) is: " + todayPayment);
		if(databases<N) {
		platform.setExpectedFuturePayment();
		double tomorrowExpectedPayment = platform.getExpectedFuturePayment();
		arrivalOfSeller(todayPayment, tomorrowExpectedPayment);
		platform.setExpectedFutureTotalPayments();
		double FTP = platform.getExpectedFutureTotalPayments();
		System.out.println("Future total payments are equal to " + FTP);
		platform.setCurrentPostedPrice();
		double todayPrice = platform.getCurrentPostedPrice();
		System.out.println("Posted price for a query is: " + todayPrice);
		arrivalOfBuyer(todayPrice);
		if(sale) {
			updateDatabases();
		}
		else {
			updatePessimisticBelief();
		}
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
			Seller seller = new Seller(time, "myopic", delta, costDistribution, costParameters);
			if (seller.getCost() <= todayPayment) {
				currentSellers.add(seller);
			}
			else {
				sale = false;
			};
		}
		else {
			System.out.println("No seller has arrived");
			sale = false;
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
			}
			else {
				sale = false;
			};
		}
	}

	public void arrivalOfBuyer(double todayPrice) {

		System.out.println("A buyer has arrived!");
		Buyer buyer = new Buyer(time, "myopic", gamma);
		platform.setExpectedFuturePostedPrice();
		double futurePrice = platform.getExpectedFuturePostedPrice();
		System.out.println("For buyers in the market, they expect to pay " + futurePrice + " tomorrow for a query.");
		platform.setCurrentPromisedSurplus(buyer.getArrivalTime(), time);
		double promisedSurplus = platform.getCurrentPromisedSurplus();
		buyer.setCurrentPromisedSurplus(promisedSurplus);
		System.out.println("If the buyer submits today his query, he enjoys amount of utility: " + promisedSurplus);
		platform.setExpectedFutureBuyerSurplus(buyer.getArrivalTime(), time);
		double futureSurplus = platform.getExpectedFutureBuyerSurplus();
		buyer.setExpectedFutureSurplus(futureSurplus);
		currentBuyers.add(buyer);
		System.out.println("There are " + currentBuyers.size() + " buyer(s) in the market.");
		ArrayList<Buyer> eligibleBuyers = new ArrayList<Buyer>();
		if(currentBuyers.isEmpty() == false) {
			for(Buyer currentBuyer : currentBuyers) {
				currentBuyer.decideIfSubmitQuery(time);
				if(currentBuyer.getAskDecision() == true) {
					eligibleBuyers.add(currentBuyer);
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
		System.out.println("There are " + currentBuyers.size() + " buyer(s) in the market.");
	}


	public int generateBernoulliOutcome(double p) {

		BinomialDistribution bernoulli = new BinomialDistribution(1, p);
		return bernoulli.sample();

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
			platform.setBelief(currentBelief + UncertainRatePlatform.db1(currentBelief, thetaHigh, thetaLow));
			System.out.println("New belief is: " + platform.getBelief());
		}
	}
	
	public void updatePessimisticBelief() {
		
		double currentBelief = platform.getBelief();
		if(currentBelief >= 0) {
			platform.setBelief(currentBelief + UncertainRatePlatform.db0(currentBelief, thetaHigh, thetaLow, platform.getCurrentPayment(), costDistribution, costParameters));
		}
	}
	
	
}
