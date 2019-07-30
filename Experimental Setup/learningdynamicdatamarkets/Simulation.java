package learningdynamicdatamarkets;

import java.awt.BasicStroke;
import java.awt.Color;
import java.io.File;
import java.io.IOException;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.RectangleInsets;
import org.jfree.chart.renderer.xy.*;

public class Simulation {

	private double rate = 0.8;
	private int N = 100;
	private double gamma = 0.9;
	private double delta = 0.95;

	private String distribution = "uniform";
	private double[] uniformParams = new double[]{0.0, 1.0};

	private double thetalow = 0.4;
	private double thetahigh = 0.8;
	private double prior = 0.5;
	private String policy = "MBP";

	private double alpha = 3;
	private double beta = 1;

	private XYSeriesCollection beliefPaths = new XYSeriesCollection();
	public void plotBeliefConvergence() throws Exception {
		
		for(int j=0;j<5;j++) {
			generateOutcomes(j);
		}
		plotCollection(beliefPaths);
	}

	private void generateOutcomes(int i) throws Exception {

		Market market = new Market(thetahigh, thetalow, prior, rate, N, gamma, delta, distribution, uniformParams, policy);
		market.initializePolicies();
		XYSeries beliefPath = new XYSeries(Integer.toString(i));
		beliefPath.add((int) 0, prior);
		do {
			System.out.println("--- TIME " + market.time + " ---");
			market.onePeriodTrade();
			beliefPath.add(market.getDatabases(), market.platform.getBelief());
			//} while(getDatabases()<N);
		} while(market.time<100);
		beliefPaths.addSeries(beliefPath);

	}

	private void plotCollection(XYSeriesCollection collection) {

		JFreeChart chart = ChartFactory.createXYLineChart("Beliefs",
				"t",
				"b",
				collection,
				PlotOrientation.VERTICAL,
				true,
				false,
				false
				);
		
        chart.setBackgroundPaint(Color.white);
        
        // get a reference to the plot for further customisation...
        XYPlot plot = (XYPlot) chart.getPlot();
        plot.setBackgroundPaint(Color.lightGray);
        plot.setAxisOffset(new RectangleInsets(5.0, 5.0, 5.0, 5.0));
        plot.setDomainGridlinePaint(Color.white);
        plot.setRangeGridlinePaint(Color.white);
        
        DeviationRenderer renderer = new DeviationRenderer(true, false);
        renderer.setSeriesStroke(0, new BasicStroke(3.0f, BasicStroke.CAP_ROUND,
                BasicStroke.JOIN_ROUND));
        renderer.setSeriesStroke(0, new BasicStroke(3.0f));
        renderer.setSeriesStroke(1, new BasicStroke(3.0f));
        renderer.setSeriesFillPaint(0, new Color(200, 200, 255));
        renderer.setSeriesFillPaint(1, new Color(255, 200, 200));
        plot.setRenderer(renderer);

        // change the auto tick unit selection to integer units only...
        NumberAxis yAxis = (NumberAxis) plot.getRangeAxis();
        yAxis.setAutoRangeIncludesZero(false);
        yAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
                

		
		/*XYPlot plot = graph.getXYPlot();
		DeviationRenderer renderer = new DeviationRenderer();
		plot.setRenderer(renderer);

		renderer.setSeriesStroke(0, new BasicStroke(3));
		renderer.setSeriesShapesVisible(0,false);*/

		File imageFile = new File("beliefs.png");
		int width = 640;
		int height = 480;

		try {
			ChartUtilities.saveChartAsPNG(imageFile, chart, width, height);
		} catch (IOException ex) {
			System.err.println(ex);
		}

	}


}
