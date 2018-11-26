package io.github.luzzu.qualitymetrics.utilities;

import java.util.ArrayList;
import java.util.List;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.sparql.core.Quad;

import io.github.luzzu.operations.properties.PropertyManager;

public class Loader {

	protected List<Quad> streamingQuads = new ArrayList<Quad>();

	/**
	 * 
	 * @param fileName
	 */
	public void loadDataSet(String fileName) {

		Model m = ModelFactory.createDefaultModel();
		m.read(fileName, null);

		StmtIterator si = m.listStatements();
		while (si.hasNext()) {
			this.streamingQuads.add(new Quad(null, si.next().asTriple()));
		}

		// Set the dataset URI into the datasetURI property, so that it's retrieved by
		// EnvironmentProperties
		PropertyManager.getInstance().addToEnvironmentVars("datasetURI", fileName);
		PropertyManager.getInstance().addToEnvironmentVars("dataset-pld", fileName);
	}

	/**
	 * Sets a specific URI as base of the dataset to be evaluated
	 */
	public void loadDataSet(String fileName, String baseURI) {
		// String filename =
		// this.getClass().getClassLoader().getResource(fileName).toExternalForm();

		Model m = ModelFactory.createDefaultModel();
		m.read(fileName, null);

		StmtIterator si = m.listStatements();
		while (si.hasNext()) {
			this.streamingQuads.add(new Quad(null, si.next().asTriple()));
		}

		// Set the dataset URI into the datasetURI property, so that it's retrieved by
		// EnvironmentProperties
		PropertyManager.getInstance().addToEnvironmentVars("datasetURI", fileName);
		// Set the dataset's base URI into the baseURI property, so that it's retrieved
		// by EnvironmentProperties
		PropertyManager.getInstance().addToEnvironmentVars("baseURI", baseURI);
	}

	/**
	 * Returns a list of triples from the loaded dataset. This can be used to
	 * simulate the streaming of triples
	 * 
	 * @return list of Triples
	 */
	public List<org.apache.jena.sparql.core.Quad> getStreamingQuads() {
		return this.streamingQuads;
	}
}
