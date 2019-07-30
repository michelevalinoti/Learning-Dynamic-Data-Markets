package learningdynamicdatamarkets;


import java.awt.BasicStroke;
import java.io.File;
import java.io.IOException;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

public class Plot {

	public static XYSeries arrayToSeries(double[] somePolicy, String policy, int M) {
		
		XYSeries policySeries = new XYSeries(policy);
		
		double belief = 0;
		
		for(int m=0; m<M; m++) {
			
			policySeries.add(belief, somePolicy[m]);
			belief += 1.0/M;
		}
		
		return policySeries;
	}
	
	public static void plotTwoPolicies(XYSeries policyOne, XYSeries policyTwo) {
		
		XYSeriesCollection policies = new XYSeriesCollection(policyOne);
		policies.addSeries(policyTwo);
		
		JFreeChart graph = ChartFactory.createXYLineChart("Payment",
                										  "Belief",
                										  "Payment",
                										   policies,
                										   PlotOrientation.VERTICAL,
                										   true,
                										   false,
                										   false
                										  );
		
		XYPlot plot = graph.getXYPlot();
		/*ValueAxis valueAxis = plot.getRangeAxis();
		valueAxis.setRange(0.0645, 0.0675);
		plot.setRangeAxis(valueAxis);*/
		XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
		renderer.setBaseShapesVisible(false);
		renderer.setBaseStroke(new BasicStroke(0.1f));
		plot.setRenderer(renderer);

		
		//renderer.setSeriesStroke(0, new BasicStroke(3));
		//renderer.setSeriesShapesVisible(0,false);
		
		File imageFile = new File("payment.png");
	    int width = 640;
	    int height = 480;

	    try {
	        ChartUtilities.saveChartAsPNG(imageFile, graph, width, height);
	    } catch (IOException ex) {
	    System.err.println(ex);
	    }
	    
	    }

	
	}
