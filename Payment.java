import java.util.Map;

import java.util.HashMap;

import de.linearbits.newtonraphson.Constraint2D;
import de.linearbits.newtonraphson.Function2D;
import de.linearbits.newtonraphson.NewtonRaphson2D;
import de.linearbits.newtonraphson.Vector2D;

public class Payment{

	public static Map<String, double[]> computeFullInformationValuesAndPayments(double rate, int N, double gamma, String costDistribution, double[] costParameters) {

		Map<String, double[]> fullInformationValuesAndPayments = new HashMap<String, double[]>();
		double[] fullInformationValues = new double[N+1]; 
		double[] fullInformationPayments = new double[N+1]; 

		fullInformationValues[N] = Buyer.V(N)/(1-gamma); // value in the terminal state
		fullInformationPayments[N] = 0;					 // future payment when it is reached state N is equal to 0
		/*
		 * loop over the states of number of databases for j=k, it is computed
		 * fullInformationValues[k-1] equal to value(k) fullInformationValues[k-1] equal
		 * to p(k)
		 */
		
		for (int j = N; j > 0; j--) {
			
			Vector2D jStepSolution = computeOneStepPayments(rate, gamma, costDistribution, costParameters, j, fullInformationValues[j]);
			fullInformationValues[j-1] = jStepSolution.x;
			fullInformationPayments[j-1] = jStepSolution.y;
			
			if (costDistribution == "uniform") {
				double upperBound = costParameters[1];
				if (Double.isNaN(fullInformationPayments[j-1]) || fullInformationPayments[j-1] > upperBound) {
					fullInformationPayments[j-1] = upperBound;
				};
			};
		}

		fullInformationValuesAndPayments.put("fullInformationValues", fullInformationValues);
		fullInformationValuesAndPayments.put("fullInformationPayments", fullInformationPayments);

		return fullInformationValuesAndPayments;
	}

	public static Vector2D computeOneStepPayments(double rate, double gamma, String costDistribution, double[] costParameters, int k, double valueAtStateK) {
		// Eq. (2.23) in Master Thesis
		Function2D focFullInformation = new Function2D() {
			public Double evaluate(Vector2D input) {
				//return valueAtStateK - input.y - input.x - Seller.F(input.y, costDistribution, costParameters)/Seller.f(input.y, costDistribution, costParameters);
				return valueAtStateK - 2*input.y - input.x;
			}
		};
		// Eq. (2.24) in Master Thesis
		Function2D maxFullInformation = new Function2D() {
			public Double evaluate(Vector2D input) {
				//return input.x-1/(1-gamma)*(Buyer.V(k-1)+gamma*rate*Seller.F(input.y, costDistribution, costParameters)*Seller.F(input.y, costDistribution, costParameters)/Seller.f(input.y, costDistribution, costParameters));
				return input.x-(Buyer.V(k-1)+gamma*rate*input.y*input.y)/(1-gamma);
			}
		};

		Constraint2D paymentsIntoSupport = null;

		if (costDistribution == "uniform") {
			double upperBound = costParameters[1];
			paymentsIntoSupport = new Constraint2D() {
				public Boolean evaluate(Vector2D input) {
					return (input.y >= 0 && input.y <= upperBound);
				}
			};
		} else if (costDistribution == "exponential") {
			paymentsIntoSupport = new Constraint2D() {
				public Boolean evaluate(Vector2D input) {
					return (input.y >= 0);
				}
			};
		};
		
		// solve system with Newton-Raphson method
		Vector2D solution = new NewtonRaphson2D(focFullInformation, maxFullInformation, paymentsIntoSupport)
				.accuracy(1e-6).iterationsPerTry(1000).iterationsTotal(1000000).solve();

		return solution;
	};

	public static double[] computeFutureTotalPayments(double rate, int N, double gamma, double delta, String distribution, double[] parameters, double[] fullInformationPayments) {
	
		return computeFutureTotalPaymentsUpToStatek(rate, N, gamma, delta, distribution, parameters, fullInformationPayments, 0);
	}
	
	public static double[] computeFutureTotalPaymentsUpToStatek(double rate, int N, double gamma, double delta, String distribution, double[] parameters, double[] fullInformationPayments, int minimumK) {

		double[] futureTotalPayments = new double[N];
		futureTotalPayments[N-1] = 0;
		for (int j=N-2; j>=minimumK; j--) {
			futureTotalPayments[j] = computeOneStepFutureTotalPayments(rate, gamma, delta, distribution, parameters, fullInformationPayments[j], futureTotalPayments[j+1]);			
		}
		
		return futureTotalPayments;

	}

	public static double computeOneStepFutureTotalPayments(double rate, double gamma, double delta, String distribution, double[] parameters, double futurePrice, double futureFTP) {

		double F = Seller.F(futurePrice, distribution, parameters);
		return delta*rate*F*(futurePrice+futureFTP)/(1-delta*(1-rate*F));
	}
	
}
