package io.github.luzzu.qualitymetrics;

import java.util.List;

import org.apache.jena.sparql.core.Quad;

import io.github.luzzu.exceptions.MetricProcessingException;
import io.github.luzzu.qualitymetrics.accessibility.interlinking.*;
import io.github.luzzu.qualitymetrics.accessibility.licensing.*;
import io.github.luzzu.qualitymetrics.contextual.understandability.*;
import io.github.luzzu.qualitymetrics.intrinsic.conciseness.ExtensionalConciseness;
import io.github.luzzu.qualitymetrics.intrinsic.syntacticvalidity.CompatibleDatatype;
import io.github.luzzu.qualitymetrics.representational.representationalconciseness.ShortURIs;
import io.github.luzzu.qualitymetrics.utilities.AbstractQualityMetric;
import io.github.luzzu.qualitymetrics.utilities.Loader;

public class Main {

	public static Loader loader = new Loader();

	public static void main(String[] args) throws MetricProcessingException {

		String input = args[0];
		System.out.println("input:"+input);
		loader.loadDataSet(input);
		AbstractQualityMetric metric_HumanReadableLicense = new HumanReadableLicense();
		assessQuality(metric_HumanReadableLicense);
		AbstractQualityMetric metric_MachineReadableLicense = new MachineReadableLicense();
		assessQuality(metric_MachineReadableLicense);

		AbstractQualityMetric metric_LinkExternalDataProviders = new LinkExternalDataProviders();
		assessQuality(metric_LinkExternalDataProviders);

		AbstractQualityMetric metric_HumanReadableLabelling = new HumanReadableLabelling();
		assessQuality(metric_HumanReadableLabelling);

		AbstractQualityMetric metric_ShortURIs = new ShortURIs();
		assessQuality(metric_ShortURIs);

		AbstractQualityMetric metric_CompatibleDatatype = new CompatibleDatatype();
		assessQuality(metric_CompatibleDatatype);

		AbstractQualityMetric metric_ExtensionalConciseness = new ExtensionalConciseness();
		assessQuality(metric_ExtensionalConciseness);

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
