package com.github.ruediste.digitalSmpsSim;

import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.knowm.xchart.XChartPanel;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYChartBuilder;
import org.knowm.xchart.XYSeries;
import org.knowm.xchart.internal.series.Series.DataType;
import org.knowm.xchart.style.Styler.LegendPosition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.ruediste.digitalSmpsSim.Simulations.CircuitParameterAxis;
import com.github.ruediste.digitalSmpsSim.Simulations.CircuitParameterValue;
import com.github.ruediste.digitalSmpsSim.shared.PowerCircuitBase;

public class DigitalSmpsSimApplication {

	static Logger log = LoggerFactory.getLogger((DigitalSmpsSimApplication.class));
	private JPanel plotsPanel;
	CircuitFilterManager filterManager;
	private Map<PowerCircuitBase, JPanel> circuitPanels;

	public static void main(String[] args) {
		new DigitalSmpsSimApplication().main();
	}

	public void main() {
		var simulations = new Simulations();
		simulations.run();

		circuitPanels = simulations.circuits.stream().collect(Collectors.toMap(c -> c, circuit -> {

			var circuitPanel = new JPanel();
			circuitPanel.setLayout(new BoxLayout(circuitPanel, BoxLayout.PAGE_AXIS));

			circuitPanel.add(new JLabel(circuit.parameterValues.stream().map(x -> x.axis() + ":" + x.label())
					.collect(Collectors.joining(" ")) + " totalCost: %.3e".formatted(circuit.costCalculator.totalCost)
					+ " "
					+ circuit.control.parameterInfo()));

			for (var plot : circuit.plots) {
				// Create Chart
				final XYChart chart = new XYChartBuilder().height(400).title(plot.title)
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
		}));

		// Schedule a job for the event-dispatching thread:
		// creating and showing this application's GUI.
		javax.swing.SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {

				// Create and set up the window.
				JFrame frame = new JFrame("DIY DC-DC Simulator");
				frame.setLayout(new BorderLayout());
				frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

				plotsPanel = new JPanel();
				plotsPanel.setLayout(new BoxLayout(plotsPanel, BoxLayout.PAGE_AXIS));

				// scroll pane
				JScrollPane scrollPane = new JScrollPane(plotsPanel);
				scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
				scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
				scrollPane.getVerticalScrollBar().setUnitIncrement(50);
				scrollPane.getVerticalScrollBar().setBlockIncrement(500);
				frame.add(scrollPane, BorderLayout.CENTER);

				filterManager = new CircuitFilterManager(simulations.circuits);
				fillPlotsPanel();

				// Display the window.
				frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
				// frame.pack();
				frame.setVisible(true);
			}
		});
	}

	private void fillPlotsPanel() {
		plotsPanel.removeAll();

		// filters
		var filterPanel = new JPanel();
		filterPanel.setLayout(new BoxLayout(filterPanel, BoxLayout.LINE_AXIS));
		{
			var checkBox = new JCheckBox("Sort by Cost",
					filterManager.sortByTotalCost);
			filterPanel.add(checkBox);
			checkBox.addActionListener(e -> {
				filterManager.sortByTotalCost = !filterManager.sortByTotalCost;
				fillPlotsPanel();
			});
		}
		for (var axis : filterManager.axes) {
			var axisPanel = new JPanel();
			axisPanel.setLayout(new BoxLayout(axisPanel, BoxLayout.PAGE_AXIS));
			filterPanel.add(axisPanel);

			axisPanel.add(new JLabel(axis.toString()));
			filterManager.allValuesPerAxis.get(axis).forEach(value -> {
				var checkBox = new JCheckBox(value.label(),
						filterManager.selectedValuesPerAxis.get(axis).contains(value));
				axisPanel.add(checkBox);
				checkBox.addActionListener(e -> {
					if (!SwingUtilities.isEventDispatchThread()) {
						log.error("Not on event dispatch thread");
					}
					var set = filterManager.selectedValuesPerAxis.get(axis);
					if (checkBox.isSelected()) {
						set.add(value);
					} else {
						set.remove(value);
					}
					fillPlotsPanel();
				});
			});
		}
		plotsPanel.add(filterPanel);
		filterManager.filteredCircuits().forEach(c -> plotsPanel.add(circuitPanels.get(c)));
		plotsPanel.getParent().revalidate();
	}

	private static class CircuitFilterManager {
		private List<PowerCircuitBase> circuits;
		public List<CircuitParameterAxis> axes;
		Map<CircuitParameterAxis, Set<CircuitParameterValue>> allValuesPerAxis = new HashMap<>();
		Map<CircuitParameterAxis, Set<CircuitParameterValue>> selectedValuesPerAxis = new HashMap<>();
		public boolean sortByTotalCost;

		public CircuitFilterManager(List<PowerCircuitBase> circuits) {
			this.circuits = circuits;
			axes = allParameterValues().map(v -> v.axis())
					.distinct().sorted().toList();
			allParameterValues()
					.forEach(v -> allValuesPerAxis.computeIfAbsent(v.axis(), x -> new LinkedHashSet<>()).add(v));
			allValuesPerAxis.forEach((axis, values) -> selectedValuesPerAxis.put(axis, new HashSet<>(values)));
		}

		private Stream<CircuitParameterValue> allParameterValues() {
			return circuits.stream().flatMap(c -> c.parameterValues.stream());
		}

		public List<PowerCircuitBase> filteredCircuits() {
			var stream = circuits.stream()
					.filter(c -> c.parameterValues.stream()
							.allMatch(v -> selectedValuesPerAxis.get(v.axis()).contains(v)));
			if (sortByTotalCost) {
				stream = stream.sorted(Comparator.comparing(x -> -x.costCalculator.totalCost));
			}
			return stream.toList();
		}
	}
}
