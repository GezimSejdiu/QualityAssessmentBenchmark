package io.github.luzzu.qualitymetrics.accessibility.licensing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.jena.graph.Node;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.NodeIterator;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.sparql.core.Quad;
import org.apache.jena.vocabulary.OWL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.luzzu.qualitymetrics.utilities.AbstractQualityMetric;
import io.github.luzzu.qualitymetrics.utilities.vocabularies.DQM;
import io.github.luzzu.qualitymetrics.utilities.vocabularies.DQMPROB;
import io.github.luzzu.qualityproblems.ProblemCollection;
import io.github.luzzu.semantics.vocabularies.QPRO;


/**
 * @author Santiago Londono
 * Verifies whether consumers of the dataset are explicitly granted permission to re-use it, under defined 
 * conditions, by annotating the resource with a machine-readable indication (e.g. a VoID description) of the license.
 *  
 */
public class MachineReadableLicense extends AbstractQualityMetric {
	
	private final Resource METRIC_URI = DQM.MachineReadableLicenseMetric;
	
	private static Logger logger = LoggerFactory.getLogger(MachineReadableLicense.class);
	
	//TODO: Add in DQMPROB
    private static final Resource NotMachineReadableLicense = ModelFactory.createDefaultModel().createResource( "http://www.diachron-fp7.eu/dqm-prob#NotMachineReadableLicence" );

	
	/**
	 * Map containing all the resources for which an annotation about their license has been found in the quads.
	 * The key of the map corresponds to the URI of the resource (i.e. subject in the quads) and the value contains the 
	 * object node containing the information about the license
	 */
//	private Map<String, Node> mapLicensedResources = new HashMap<String, Node>();
	
	
	/**
	 * Allows to determine if a predicate states what is the licensing schema of a resource
	 */
	private LicensingModelClassifier licenseClassifier = new LicensingModelClassifier();
	
	/**
	 * Mapping all licenses that are attached to a void:Dataset
	 */
//	private Map<String, Node> mapLicensedDatasets = new HashMap<String, Node>();
	
	
	/**
	 * Holds the local resource URIs seen
	 */
	private List<Quad> _problemList = new ArrayList<Quad>();
	
	
	private Map<String,Boolean> unconventionalLicenses = new HashMap<String,Boolean>();
	
//	private double validLicenses = 0.0d;
	private boolean hasValidLicense = false;
	private double totalPossibleLicenses = 0.0d;
	private double nonMachineReadableLicenses = 0.0d;
	private double nonRecommended = 0.0d;

	
	
	/**
	 * Processes a single quad being part of the dataset. Firstly, tries to figure out the URI of the dataset whence the quads come. 
	 * If so, the URI is extracted from the corresponding subject and stored to be used in the calculation of the metric. Otherwise, verifies 
	 * whether the quad contains licensing information (by checking if the property is part of those known to be about licensing) and if so, stores 
	 * the URL of the subject in the map of resources confirmed to have licensing information
	 * @param quad Quad to be processed and examined to try to extract the dataset's URI
	 */
	public void compute(Quad quad) {
		logger.debug("Computing : {} ", quad.asTriple().toString());

		// Extract the predicate (property) of the statement, the described resource (subject) and the value set (object)
		Node subject = quad.getSubject();
		Node predicate = quad.getPredicate();
		Node object = quad.getObject();
		
		if ((subject.isURI()) && (subject.getURI().startsWith(this.getDatasetURI()))){
			if (licenseClassifier.isLicensingPredicate(predicate)) {
				totalPossibleLicenses++;
				
				
				if (object.isURI()){
					boolean isValidLicense = false;
					
					if (licenseClassifier.isCopyLeftLicenseURI(object)){
						isValidLicense = true;
					} else if (licenseClassifier.isNotRecommendedCopyLeftLicenseURI(object)){
						Quad q = new Quad(null, object, QPRO.exceptionDescription.asNode(), DQMPROB.NotRecommendedLicenceInDataset.asNode());
						this._problemList.add(q);
						isValidLicense = true;
					} else {
						String theLicense = "";
						if (object.isURI()) theLicense = object.getURI();
						else object.toString();
						
						if (unconventionalLicenses.containsKey(theLicense)){
							isValidLicense = unconventionalLicenses.get(theLicense);
						} else {
							Model licenseModel = ModelFactory.createDefaultModel();
							try{
								licenseModel = RDFDataMgr.loadModel(theLicense);
							} catch (Exception e) {
								Quad q = new Quad(null, object, QPRO.exceptionDescription.asNode(), NotMachineReadableLicense.asNode());
								this._problemList.add(q);
								nonMachineReadableLicenses++;
							}
							if (licenseModel.size() > 0){
								// is there an owl:sameAs
								NodeIterator itr = licenseModel.listObjectsOfProperty(licenseModel.asRDFNode(object).asResource(), OWL.sameAs);
								while(itr.hasNext()){
									RDFNode possLicense = itr.next();
									
									if (licenseClassifier.isCopyLeftLicenseURI(possLicense.asNode())){
										isValidLicense = true;
										break;
									}
									
									if (licenseClassifier.isNotRecommendedCopyLeftLicenseURI(possLicense.asNode())){
										Quad q = new Quad(null, object, QPRO.exceptionDescription.asNode(), DQMPROB.NotRecommendedLicenceInDataset.asNode());
										this._problemList.add(q);
										isValidLicense = true;
										nonRecommended++;
										break;
									}
								}
							}
						}
						unconventionalLicenses.put(theLicense, isValidLicense);
					}
					if (isValidLicense) 
						hasValidLicense = true;
				} else {
					Quad q = new Quad(null, object, QPRO.exceptionDescription.asNode(), NotMachineReadableLicense.asNode());
					this._problemList.add(q);
					nonMachineReadableLicenses++;
				}
			}
		}
	}

	/**
	 * Returns the current value of the Machine-readable indication of a license metric, the value of the metric will be 1, 
	 * if the dataset containing the processed quads contains an annotation providing information about its license. 0 otherwise.
	 * @return Current value of the Machine-readable indication of a license metric, measured for the whole dataset. [Range: 0 or 1. Error: -1]
	 */
	public Long metricValue() {
		long metValue = (this.hasValidLicense) ? 1 : 0;
		
//		if ((totalPossibleLicenses == 0) || (validLicenses == 0)) metValue = 0.0d;
//		else metValue = (double)validLicenses / ((double)totalPossibleLicenses);

		
		statsLogger.info("MachineReadableLicense. Dataset: {} - Total # Licenses in Dataset : {}; Total Non-Recommended Licenses : {}; Total Non-Machine Readable Licenses: {}", 
				this.getDatasetURI(), totalPossibleLicenses, this.nonRecommended, this.nonMachineReadableLicenses);
		
		return metValue;
	}
	
	public Resource getMetricURI() {
		return METRIC_URI;
	}

	
	@Override
	public boolean isEstimate() {
		return false;
	}

	@Override
	public Resource getAgentURI() {
		return 	DQM.LuzzuProvenanceAgent;
	}

	@Override
	public ProblemCollection<?> getProblemCollection() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Model getObservationActivity() {
		// TODO Auto-generated method stub
		return null;
	}
	
}
