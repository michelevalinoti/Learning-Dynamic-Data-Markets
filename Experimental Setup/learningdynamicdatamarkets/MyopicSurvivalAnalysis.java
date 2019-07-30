package learningdynamicdatamarkets;

import java.util.HashMap;
import java.util.Map;

import de.linearbits.newtonraphson.Constraint2D;
import de.linearbits.newtonraphson.Function2D;
import de.linearbits.newtonraphson.NewtonRaphson2D;
import de.linearbits.newtonraphson.Vector2D;

public class MyopicSurvivalAnalysis extends Payment {

	public static double computeSurvivalAnalysisPayment(double rate, int N, int k, double gamma, String costDistribution, double[] costParameters, double rho) {
		
		return computeSurvivalAnalysisValuesAndPaymentsUpToStateK(rate, N, k, gamma, costDistribution, costParameters, rho).get("fullInformationPayments")[k];
		
	}
	
	public static Map<String, double[]> computeSurvivalAnalysisValuesAndPaymentsUpToStateK(double rate, int N, int k, double gamma, String costDistribution, double[] costParameters, double rho) {

		Map<String, double[]> fullInformationValuesAndPayments = new HashMap<String, double[]>();
		double[] fullInformationValues = new double[N+1]; 
		double[] fullInformationPayments = new double[N+1]; 

		fullInformationValues[N] = StochasticBuyer.upsilon(N)/(1-gamma); // value in the terminal state
		fullInformationPayments[N] = 0;					 // future payment when it is reached state N is equal to 0
		/*
		 * loop over the states of number of databases for j=k, it is computed
		 * fullInformationValues[k-1] equal to value(k) fullInformationValues[k-1] equal
		 * to p(k)
		 */
		
		for (int j = N; j > k ; j--) {
			
			Vector2D jStepSolution = computeOneStepSurvivalAnalysisPayments(rate, gamma, costDistribution, costParameters, j, fullInformationValues[j], rho);
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

	public static Vector2D computeOneStepSurvivalAnalysisPayments(double rate, double gamma, String costDistribution, double[] costParameters, int k, double valueAtStateK, double rho) {
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
				return input.x-(rho*StochasticBuyer.upsilon(k-1)+gamma*rate*input.y*input.y)/(1-gamma);
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
				.accuracy(1e-4).iterationsPerTry(100).iterationsTotal(10000).solve();

		return solution;
	};
	
}
