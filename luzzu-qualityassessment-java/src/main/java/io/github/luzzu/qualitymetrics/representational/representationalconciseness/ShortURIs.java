package io.github.luzzu.qualitymetrics.representational.representationalconciseness;

import java.util.concurrent.ConcurrentMap;

import org.apache.jena.graph.Node;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.sparql.core.Quad;
import org.apache.jena.vocabulary.RDF;

import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;

import io.github.luzzu.operations.properties.EnvironmentProperties;
import io.github.luzzu.qualitymetrics.utilities.AbstractQualityMetric;
import io.github.luzzu.qualitymetrics.utilities.vocabularies.DQM;
import io.github.luzzu.qualitymetrics.utilities.vocabularies.DQMPROB;
import io.github.luzzu.qualityproblems.ProblemCollection;
import io.github.luzzu.semantics.vocabularies.QPRO;

/**
 * @author Santiago Londono Detects long URIs or those that contains query
 *         parameters, thereby providing a measure of how compactly is
 *         information represented in the dataset
 * 
 *         The W3C best practices for URIs say that a URI (including scheme)
 *         should be at max 80 characters http://www.w3.org/TR/chips/#uri.
 *         Parameterised URIs are considered bad immediately.
 * 
 *         Value returns a ratio of the total number of short uris in relation
 *         to the number of local URIs of a dataset
 *
 */
public class ShortURIs extends AbstractQualityMetric {

	// private static Logger logger = LoggerFactory.getLogger(ShortURIs.class);

	private final Resource METRIC_URI = DQM.ShortURIsMetric;

	// private static DB mapDb = MapDbFactory.getMapDBAsyncTempFile();
	// private Set<String> seenSet = MapDbFactory.createHashSet(mapDb,
	// UUID.randomUUID().toString());
	private ConcurrentMap<String, Boolean> seenSet = new ConcurrentLinkedHashMap.Builder<String, Boolean>()
			.maximumWeightedCapacity(100000).build();

	/**
	 * Sum short uri's
	 */
	private long shortURICount = 0;

	/**
	 * Counts the total number of possible dereferenceable URIs defined in the
	 * dataset
	 */
	private long countLocalDefURIs = 0;

	public void compute(Quad quad) {
		// logger.debug("Assessing quad: " + quad.asTriple().toString());

		if (!(quad.getPredicate().hasURI(RDF.type.getURI()))) {
			Node subject = quad.getSubject();
			if ((!(subject.isBlank())) && (!(this.seenSet.containsKey(subject.getURI())))) {
				if (subject.isURI()) {
					if (possibleDereferenceableURI(subject.getURI())) {
						countLocalDefURIs++;

						String uri = subject.getURI();
						if (uri.contains("?")) {
						} else if (uri.length() > 95) {
						} else {
							shortURICount++;
						}
					}
				}
				this.seenSet.put(subject.getURI(), true);
			}

			Node object = quad.getObject();
			if (object.isURI()) {
				if ((!(object.isBlank())) && (!(this.seenSet.containsKey(object.getURI())))) {
					if (possibleDereferenceableURI(object.getURI())) {
						countLocalDefURIs++;

						String uri = object.getURI();
						if (uri.contains("?")) {
						} else if (uri.length() > 95) {
							shortURICount++;
						}
					}
				}
				this.seenSet.put(object.getURI(), true);
			}
		}
	}

	public Long metricValue() {

		statsLogger.info("Short URI. Dataset: {} - Short URI Count {}, Possible Local Deref URIs {}",
				EnvironmentProperties.getInstance().getDatasetPLD(), shortURICount, countLocalDefURIs);

		return ((long) shortURICount / (long) countLocalDefURIs);
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
		return DQM.LuzzuProvenanceAgent;
	}

	private boolean possibleDereferenceableURI(String uri) {
		if (uri.startsWith("http") || uri.startsWith("https"))
			return true;
		return false;
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