import java.util.HashMap;
import java.util.Map;

public class MyopicBayesianPolicyPayment extends Payment {

	public static double computeMLPPayment(int k, double belief, double thetaHigh, double thetaLow, int N, double gamma, double delta, String distribution,
			double[] parameters){
	
		double MLPPayment = computeFullInformationValuesAndPayments(UncertainRatePlatform.theta(belief, thetaHigh, thetaLow), N, gamma, distribution, parameters).get("fullInformationPayments")[k];
		return MLPPayment;
	}
	
	public static Map<String, double[][]> computeMLPValuesAndPayments(int M, double thetaHigh, double thetaLow, int N, double gamma, double delta, String distribution,
			double[] parameters){

		Map<String, double[][]> MLPValuesAndPayments = new HashMap<String, double[][]>();
		double[][] MLPValues = new double[N+1][M];
		double[][] MLPPayments = new double[N+1][M];

		double b=0;
		for(int i=0; i<M; i++) {
			Map<String, double[]> MLPValuesAndPaymentsPerBelief = computeFullInformationValuesAndPayments(UncertainRatePlatform.theta(b, thetaHigh, thetaLow), N, gamma,distribution, parameters);
			for(int j=0; j<N+1; j++) {
				MLPValues[j][i] = MLPValuesAndPaymentsPerBelief.get("fullInformationValues")[j];
				MLPPayments[j][i] = MLPValuesAndPaymentsPerBelief.get("fullInformationPayments")[j];
			}
			b += 1/M;
		}

		MLPValuesAndPayments.put("values", MLPValues);
		MLPValuesAndPayments.put("payments", MLPPayments);

		return MLPValuesAndPayments;
	}

}
