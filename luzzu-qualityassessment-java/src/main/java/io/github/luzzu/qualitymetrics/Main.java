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
		String method = args[1].toString();
		loader.loadDataSet(input);
		long startTime = 0;

		switch (method) {
		case "single":
			startTime = System.currentTimeMillis();
			assessQualitySingle();
			System.out.println("assessQualitySingle: " + (System.currentTimeMillis() - startTime) + "ms.");
			break;
		case "joint":
			startTime = System.currentTimeMillis();
			assessQualityJoint();
			System.out.println("assessQualityJoint: " + (System.currentTimeMillis() - startTime) + "ms.");
			break;
		default:
			throw new IllegalArgumentException("Invalid method: " + method);
		}

		System.exit(0);

	}

	public static void assessQualitySingle() throws MetricProcessingException {
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

		for (Quad quad : streamingQuads) {
			metric.compute(quad);
		}

		Long l = metric.metricValue();
		System.out.println(metric.getMetricURI().getLocalName() + " :" + l);

	}

	public static void assessQualityJoint() throws MetricProcessingException {
		// Load quads...
		List<Quad> streamingQuads = loader.getStreamingQuads();
		AbstractQualityMetric metric_HumanReadableLicense = new HumanReadableLicense();
		AbstractQualityMetric metric_MachineReadableLicense = new MachineReadableLicense();
		AbstractQualityMetric metric_LinkExternalDataProviders = new LinkExternalDataProviders();
		AbstractQualityMetric metric_HumanReadableLabelling = new HumanReadableLabelling();
		AbstractQualityMetric metric_ShortURIs = new ShortURIs();
		AbstractQualityMetric metric_CompatibleDatatype = new CompatibleDatatype();
		AbstractQualityMetric metric_ExtensionalConciseness = new ExtensionalConciseness();

		for (Quad quad : streamingQuads) {
			metric_HumanReadableLicense.compute(quad);
			metric_MachineReadableLicense.compute(quad);
			metric_LinkExternalDataProviders.compute(quad);
			metric_HumanReadableLabelling.compute(quad);
			metric_ShortURIs.compute(quad);
			metric_CompatibleDatatype.compute(quad);
			metric_ExtensionalConciseness.compute(quad);

		}

		Long metric_HumanReadableLicenseValue = metric_HumanReadableLicense.metricValue();
		System.out.println(
				metric_HumanReadableLicense.getMetricURI().getLocalName() + " :" + metric_HumanReadableLicenseValue);
		Long metric_MachineReadableLicenseValue = metric_MachineReadableLicense.metricValue();
		System.out.println(metric_MachineReadableLicense.getMetricURI().getLocalName() + " :"
				+ metric_MachineReadableLicenseValue);
		Long metric_LinkExternalDataProvidersValue = metric_LinkExternalDataProviders.metricValue();
		System.out.println(metric_LinkExternalDataProviders.getMetricURI().getLocalName() + " :"
				+ metric_LinkExternalDataProvidersValue);
		Long metric_HumanReadableLabellingValue = metric_HumanReadableLabelling.metricValue();
		System.out.println(metric_HumanReadableLabelling.getMetricURI().getLocalName() + " :"
				+ metric_HumanReadableLabellingValue);
		Long metric_ShortURIsValue = metric_ShortURIs.metricValue();
		System.out.println(metric_ShortURIs.getMetricURI().getLocalName() + " :" + metric_ShortURIsValue);
		Long metric_CompatibleDatatypeValue = metric_CompatibleDatatype.metricValue();
		System.out.println(
				metric_CompatibleDatatype.getMetricURI().getLocalName() + " :" + metric_CompatibleDatatypeValue);
		Long metric_ExtensionalConcisenessValue = metric_ExtensionalConciseness.metricValue();
		System.out.println(metric_ExtensionalConciseness.getMetricURI().getLocalName() + " :"
				+ metric_ExtensionalConcisenessValue);

	}

}
