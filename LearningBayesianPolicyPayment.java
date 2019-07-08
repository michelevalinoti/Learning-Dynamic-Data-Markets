import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.math3.exception.MaxCountExceededException;
import org.apache.commons.math3.ode.FirstOrderDifferentialEquations;
import org.apache.commons.math3.ode.FirstOrderIntegrator;
import org.apache.commons.math3.ode.nonstiff.ClassicalRungeKuttaIntegrator;
import org.apache.commons.math3.ode.sampling.StepHandler;
import org.apache.commons.math3.ode.sampling.StepInterpolator;

public class LearningBayesianPolicyPayment extends Payment {

	public static Map<String, double[][]> computeLBPValuesAndPayments(int N, double gamma, double thetaHigh, double thetaLow, String costDistribution, double[] costParameters){
	
		Map<String, double[][]> LBPPolicies = new HashMap<String, double[][]>();
		LBPPolicies = computeLBPValuesAndPaymentsUpTok(N, gamma, thetaHigh, thetaLow, costDistribution, costParameters, 0);
		
		return LBPPolicies;
	}
	
	public static Map<String, double[][]> computeLBPValuesAndPaymentsUpTok(int N, double gamma, double thetaHigh, double thetaLow, String costDistribution, double[] costParameters, int minimumK){

		int M = 99801;
		double[][] LBPValues = new double[N+1][M];
		double[][] LBPPayments = new double[N+1][M];

		double[][] LBPDerivativesValue = new double[N+1][M];

		for(int m=0; m<M; m++) {
			LBPValues[N][m] = Buyer.V(N)/(1-gamma); 
			LBPDerivativesValue[N][m] = 0;
		}

		for(int k=N; k>minimumK; k--) {
			
			Map<String, ArrayList<Double>> policiesAtStatek = LBPsolver(N, gamma, thetaHigh, thetaLow, k, costDistribution, costParameters, LBPValues[k], LBPDerivativesValue[k]);

			for(int m=0; m<M; m++) {
				LBPValues[k-1][m] = policiesAtStatek.get("values").get(m);
				LBPPayments[k-1][m] = policiesAtStatek.get("payments").get(m);
				LBPDerivativesValue[k-1][m] = policiesAtStatek.get("derivativesValue").get(m);
			}
			
		}

		Map<String, double[][]> LBPPolicies = new HashMap<String, double[][]>();
		LBPPolicies.put("LBPValues", LBPValues);
		LBPPolicies.put("LBPPayments", LBPPayments);
		
		return LBPPolicies;
	
	}
	
	public static Map<String, ArrayList<Double>> LBPsolver(int N, double gamma, double thetaHigh, double thetaLow, int k, String costDistribution, double[] costParameters, double[] valuesAtStatek, double[] dValuesAtStatekdB){

		ArrayList<Double> beliefs = new ArrayList<Double>();
		ArrayList<Double> values = new ArrayList<Double>();
		ArrayList<Double> derivativesValue = new ArrayList<Double>();
		FirstOrderIntegrator CRKIntegrator = new ClassicalRungeKuttaIntegrator(1.0e-5);
		FirstOrderDifferentialEquations ode = new LBPEquation(gamma, thetaHigh, thetaLow, k, valuesAtStatek, dValuesAtStatekdB);
		StepHandler stepHandler = new StepHandler() {

			@Override
			public void handleStep(StepInterpolator interpolator, boolean isLast) throws MaxCountExceededException {
				double belief = interpolator.getCurrentTime();
				double[] value = interpolator.getInterpolatedState();
				double[] derivativeValue = interpolator.getInterpolatedDerivatives();
				beliefs.add(belief);
				values.add(value[0]);
				derivativesValue.add(derivativeValue[0]);
			}

			@Override
			public void init(double b0, double[] value0, double b) {
				// TODO Auto-generated method stub

			}

		};

		CRKIntegrator.addStepHandler(stepHandler);
		double[] initialValue = new double[] {computeFullInformationValuesAndPayments(thetaLow, N, gamma, costDistribution, costParameters).get("fullInformationValues")[k-1]};
		CRKIntegrator.integrate(ode, 1e-3, initialValue, 1.0-1e-3, initialValue);

		ArrayList<Double> payments = new ArrayList<Double>();

		for(int m=0; m<values.size(); m++) {
			double payment = computePriceFromValue(gamma, k, beliefs.get(m), thetaHigh, thetaLow, values.get(m));
			if(Double.isNaN(payment)) {
				payment = costParameters[1];
			}
			payments.add(payment);

		}

		Map<String, ArrayList<Double>> policiesAtStatek = new HashMap<String, ArrayList<Double>>();
		policiesAtStatek.put("beliefs", beliefs);
		policiesAtStatek.put("values", values);
		policiesAtStatek.put("derivativesValue", derivativesValue);
		policiesAtStatek.put("payments", payments);

		return policiesAtStatek;

	}

	public static class LBPEquation implements FirstOrderDifferentialEquations {

		public double gamma;
		public double thetaHigh;
		public double thetaLow;
		public int k;
		public double[] valuesAtStatek;
		public double[] dValuesAtStatekdB;

		public LBPEquation(double gamma, double thetaHigh, double thetaLow, int k, double[] valuesAtStatek, double[] dValuesAtStatekdB) {

			this.gamma = gamma;
			this.thetaHigh = thetaHigh;
			this.thetaLow = thetaLow;
			this.k = k;
			this.valuesAtStatek = valuesAtStatek;
			this.dValuesAtStatekdB = dValuesAtStatekdB;
		}

		public int getDimension() {

			return 1;
		}


		public double valueAtStakekFunction(double b) {

			return UncertainRatePlatform.interpolator(b, valuesAtStatek);
		}


		public double dValueAtStatekdBFunction(double b) {

			return UncertainRatePlatform.interpolator(b, dValuesAtStatekdB);
		}

		public void computeDerivatives(double b, double[] valueAtStatekMinus1, double[] dValueAtStatekMinus1dB) {

			dValueAtStatekMinus1dB[0] = (-valueAtStatekMinus1[0] - 2*Math.sqrt((1-gamma)/(gamma*UncertainRatePlatform.theta(b, thetaHigh, thetaLow))*valueAtStatekMinus1[0] - Buyer.V(k-1)/(gamma*UncertainRatePlatform.theta(b, thetaHigh, thetaLow)))+ valueAtStakekFunction(b))/UncertainRatePlatform.db1(b, thetaHigh, thetaLow) + dValueAtStatekdBFunction(b);
			
		}

	}

	public static double computePriceFromValue(double gamma, int k, double b, double thetaHigh, double thetaLow, double valueAtStatekMinus1) {

		double priceAtStatek = (1-gamma)*valueAtStatekMinus1 - Buyer.V(k-1);
		priceAtStatek /= gamma*UncertainRatePlatform.theta(b, thetaHigh, thetaLow);
		return priceAtStatek;

	}
	
	public static double[][] computeFutureTotalPayments(int N, double gamma, double delta, double thetaHigh, double thetaLow, String costDistribution, double[] costParameters, double[][] LBPPayments){
		
		return computeFTPsUpToStatek(N, gamma, delta, thetaHigh, thetaLow, 0, costDistribution, costParameters, LBPPayments);
		
	}
	
	public static double[][] computeFTPsUpToStatek(int N, double gamma, double delta, double thetaHigh, double thetaLow, int minimumK, String costDistribution, double[] costParameters, double[][] LBPPayments) {
		
		int M = 99801;
		double[][] FTPs = new double[N][M];
		double[][] dFTPs = new double[N][M];
		
		for(int m=0; m<M; m++) {
			FTPs[N-1][m] = 0;
			dFTPs[N-1][m] = 0;
		}

		for(int k=N-2; k>=minimumK; k--) {
			
			Map<String, ArrayList<Double>> FTPsAtStatek = FTPsolver(N, gamma, delta, thetaHigh, thetaLow, k, costDistribution, costParameters, FTPs[k+1], dFTPs[k+1], LBPPayments);

			for(int m=0; m<M; m++) {
				FTPs[k][m] = FTPsAtStatek.get("FTPs").get(m);
				dFTPs[k][m] = FTPsAtStatek.get("dFTPs").get(m);
				
			}
			
		}

		return FTPs;
		
	}
	
	public static Map<String, ArrayList<Double>> FTPsolver(int N, double gamma, double delta, double thetaHigh, double thetaLow, int k, String costDistribution, double[] costParameters, double[] FTPsAtStatekPlus1, double[] dFTPsAtStatekPlus1dB, double[][] LBPPayments){

		ArrayList<Double> beliefs = new ArrayList<Double>();
		ArrayList<Double> FTPs = new ArrayList<Double>();
		ArrayList<Double> dFTPs = new ArrayList<Double>();
		FirstOrderIntegrator CRKIntegrator = new ClassicalRungeKuttaIntegrator(1.0e-5);
		FirstOrderDifferentialEquations ode = new FTPEquation(delta, thetaHigh, thetaLow, k, costDistribution, costParameters, FTPsAtStatekPlus1, dFTPsAtStatekPlus1dB, LBPPayments);
		StepHandler stepHandler = new StepHandler() {

			@Override
			public void handleStep(StepInterpolator interpolator, boolean isLast) throws MaxCountExceededException {
				double belief = interpolator.getCurrentTime();
				double[] FTP = interpolator.getInterpolatedState();
				double[] dFTP = interpolator.getInterpolatedDerivatives();
				beliefs.add(belief);
				FTPs.add(FTP[0]);
				dFTPs.add(dFTP[0]);
			}

			@Override
			public void init(double b0, double[] value0, double b) {
				// TODO Auto-generated method stub

			}

		};

		CRKIntegrator.addStepHandler(stepHandler);
		double[] paymentsAtStateThetaLow = computeFullInformationValuesAndPayments(thetaLow, N, gamma, costDistribution, costParameters).get("fullInformationPayments");
		double[] initialValue = new double[] {computeFutureTotalPayments(thetaLow, N, gamma, delta, costDistribution, costParameters, paymentsAtStateThetaLow)[k]};
		CRKIntegrator.integrate(ode, 1e-3, initialValue, 1.0-1e-3, initialValue);

		Map<String, ArrayList<Double>> FTPsAtStatek = new HashMap<String, ArrayList<Double>>();
		FTPsAtStatek.put("FTPs", FTPs);
		FTPsAtStatek.put("dFTPs", dFTPs);
		
		return FTPsAtStatek;

	}
	
	public static class FTPEquation implements FirstOrderDifferentialEquations {

		public double delta;
		public double thetaHigh;
		public double thetaLow;
		public int k;
		public String costDistribution;
		public double[] costParameters;
		public double[] FTPsAtStatekPlus1;
		public double[] dFTPsAtStatekPlus1dB;
		public double[][] LBPPayments;

		public FTPEquation(double delta, double thetaHigh, double thetaLow, int k, String costDistribution, double[] costParameters, double[] FTPsAtStatekPlus1, double[] dFTPsAtStatekPlus1dB, double[][] LBPPayments) {

			this.delta = delta;
			this.thetaHigh = thetaHigh;
			this.thetaLow = thetaLow;
			this.k = k;
			this.costDistribution = costDistribution;
			this.costParameters = costParameters;
			this.FTPsAtStatekPlus1 = FTPsAtStatekPlus1;
			this.dFTPsAtStatekPlus1dB = dFTPsAtStatekPlus1dB;
			this.LBPPayments = LBPPayments;
			
		}

		public int getDimension() {

			return 1;
		}


		public double FTPsAtStatekPlus1Function(double b) {

			return UncertainRatePlatform.interpolator(b, FTPsAtStatekPlus1);
		}


		public double dFTPsAtStatekPlus1dBFunction(double b) {

			return UncertainRatePlatform.interpolator(b, dFTPsAtStatekPlus1dB);
		}
		
		public double LBPPaymentAtStatekPlus1Function(double b) {
			
			return UncertainRatePlatform.interpolator(b, LBPPayments[k+1]);
		}

		public void computeDerivatives(double b, double[] FTPsAtStatek, double[] dFTPsAtStatekdB) {

			dFTPsAtStatekdB[0] = (1-delta*(1-UncertainRatePlatform.theta(b, thetaHigh, thetaLow)*Seller.F(LBPPaymentAtStatekPlus1Function(b), costDistribution, costParameters)))*FTPsAtStatek[0];
			dFTPsAtStatekdB[0] -= delta*UncertainRatePlatform.theta(b, thetaHigh, thetaLow)*Seller.F(LBPPaymentAtStatekPlus1Function(b), costDistribution, costParameters)*(LBPPaymentAtStatekPlus1Function(b)+FTPsAtStatekPlus1Function(b)+UncertainRatePlatform.db1(b, thetaHigh, thetaLow)*dFTPsAtStatekPlus1dBFunction(b));
			dFTPsAtStatekdB[0] /= delta*(1-UncertainRatePlatform.theta(b, thetaHigh, thetaLow)*Seller.F(LBPPaymentAtStatekPlus1Function(b), costDistribution, costParameters))*UncertainRatePlatform.db0(b, thetaHigh, thetaLow, LBPPaymentAtStatekPlus1Function(b), costDistribution, costParameters);
			
		}

	}
}
