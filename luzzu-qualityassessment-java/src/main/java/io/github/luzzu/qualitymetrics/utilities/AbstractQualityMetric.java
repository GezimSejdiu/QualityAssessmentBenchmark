package io.github.luzzu.qualitymetrics.utilities;

import io.github.luzzu.assessment.QualityMetric;

public abstract class AbstractQualityMetric implements QualityMetric<Long> {

	private String datasetURI = "";
	
	@Override
	public String getDatasetURI() {
		return this.datasetURI;
	}

	@Override
	public void setDatasetURI(String datasetURI) {
		this.datasetURI = datasetURI;
	}

}
