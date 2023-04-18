package com.github.ruediste.digitalSmpsSim;

import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.stream.Collectors;

import org.knowm.xchart.XChartPanel;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYChartBuilder;
import org.knowm.xchart.XYSeries;
import org.knowm.xchart.internal.series.Series.DataType;
import org.knowm.xchart.style.Styler.LegendPosition;

public class DigitalSmpsSimApplication {

	public static void main(String[] args) {
		var simulations = new Simulations();
		simulations.run();

		var circuits = simulations.circuits.stream().map(circuit -> {

			var circuitPanel = new JPanel();
			circuitPanel.setLayout(new BoxLayout(circuitPanel, BoxLayout.PAGE_AXIS));

			circuitPanel.add(new JLabel(circuit.parameterValues.stream().map(x -> x.axis() + ":" + x.label())
					.collect(Collectors.joining(" ")) + " totalCost: %.3e".formatted(circuit.costCalculator.totalCost)
					+ " "
					+ circuit.control.parameterInfo()));

			for (var plot : circuit.plots) {
				// Create Chart
				final XYChart chart = new XYChartBuilder().width(1200).height(400).title(plot.title)
						.xAxisTitle("Time [" + plot.timePrefix.symbol + "s]")
						.build();
				// Customize Chart
				chart.getStyler().setLegendPosition(LegendPosition.InsideNE);
				chart.setCustomXAxisTickLabelsFormatter(t -> plot.timePrefix.toString(t, "s"));
				// chart.getStyler().setDefaultSeriesRenderStyle(XYSeriesRenderStyle.Area);

				for (var axis : plot.axes) {
					chart.setYAxisGroupTitle(axis.index, axis.unitSymbol);
				}

				for (int i = 0; i < plot.series.size(); i++) {
					var s = plot.series.get(i);
					double[] xData = new double[plot.values.size()];
					double[] yData = new double[plot.values.size()];
					for (int p = 0; p < plot.values.size(); p++) {
						var values = plot.values.get(p);
						xData[p] = values.time;
						yData[p] = values.values.get(i);
						if (Double.isInfinite(yData[p])) {
							yData[p] = 0;
						}
					}
					var series = new XYSeries(s.name, xData, yData, null, DataType.Number);
					series.setYAxisGroup(s.yAxisIndex);
					chart.getSeriesMap().put(s.name, series);
				}

				circuitPanel.add(new XChartPanel<XYChart>(chart));
			}
			return circuitPanel;
		}).toList();

		// Schedule a job for the event-dispatching thread:
		// creating and showing this application's GUI.
		javax.swing.SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {

				// Create and set up the window.
				JFrame frame = new JFrame("Advanced Example");
				frame.setLayout(new BorderLayout());
				frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

				// chart

				// var plotsPanel = new PlotsPanel();

				var plotsPanel = new JPanel(new GridLayout(0, 3));
				JScrollPane scrollPane = new JScrollPane(plotsPanel);
				scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
				scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

				frame.add(scrollPane, BorderLayout.CENTER);

				circuits.forEach(plotsPanel::add);

				// label
				JLabel label = new JLabel("Blah blah blah.", SwingConstants.CENTER);
				frame.add(label, BorderLayout.SOUTH);

				// Display the window.
				frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
				// frame.pack();
				frame.setVisible(true);
			}
		});
	}
}
