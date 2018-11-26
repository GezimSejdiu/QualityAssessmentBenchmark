package io.github.luzzu.qualitymetrics;

import java.util.List;

import org.apache.jena.sparql.core.Quad;

import io.github.luzzu.exceptions.MetricProcessingException;
import io.github.luzzu.qualitymetrics.accessibility.licensing.*;
import io.github.luzzu.qualitymetrics.utilities.AbstractQualityMetric;
import io.github.luzzu.qualitymetrics.utilities.Loader;

public class Main {

	public static Loader loader = new Loader();

	public static void main(String[] args) throws MetricProcessingException {

		System.out.println("Test here");

		// String input = args[0]
		loader.loadDataSet("src/main/resources/rdf.nt");
		AbstractQualityMetric metric_HumanReadableLicense = new HumanReadableLicense();
		AbstractQualityMetric metric_MachineReadableLicense = new MachineReadableLicense();
		assessQuality(metric_HumanReadableLicense);
		assessQuality(metric_MachineReadableLicense);
	}

	public static void assessQuality(AbstractQualityMetric metric) throws MetricProcessingException {
		// Load quads...
		List<Quad> streamingQuads = loader.getStreamingQuads();
		int counter = 0;

		for (Quad quad : streamingQuads) {
			metric.compute(quad);
			counter++;
		}
		System.out.println("Counter:" + counter);

		Long l = metric.metricValue();
		System.out.println(metric.getMetricURI().getLocalName() + " :" + l);

	}
}
