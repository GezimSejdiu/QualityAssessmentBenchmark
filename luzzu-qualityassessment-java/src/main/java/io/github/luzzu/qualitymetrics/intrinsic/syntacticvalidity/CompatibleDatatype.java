package io.github.luzzu.qualitymetrics.intrinsic.syntacticvalidity;

import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.graph.Node;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.sparql.core.Quad;

import io.github.luzzu.qualitymetrics.utilities.AbstractQualityMetric;
import io.github.luzzu.qualitymetrics.utilities.vocabularies.DQM;
import io.github.luzzu.qualityproblems.ProblemCollection;

/**
 * This metric checks the compatability of the literal datatype against the
 * lexical form of the said literal. This metric only catches literals with a
 * datatype whilst untyped literals are not checked in this metric as their
 * lexical form cannot be validated.
 * 
 * Therefore, in order to check for untyped literals, the metric UntypedLiterals
 * in the same dimension checks for such quality problems.
 * 
 * @author Jeremy Debattista
 * 
 */
public class CompatibleDatatype extends AbstractQualityMetric {

	// private static Logger logger =
	// LoggerFactory.getLogger(CompatibleDatatype.class);

	private Model problemModel = ModelFactory.createDefaultModel();


	private int numberCorrectLiterals = 0;
	private int numberIncorrectLiterals = 0;

	@Override
	public void compute(Quad quad) {
		Node obj = quad.getObject();

		if (obj.isLiteral()) {
			if (obj.getLiteralDatatype() != null) {
				// unknown datatypes cannot be checked for their correctness,
				// but in the UsageOfIncorrectDomainOrRangeDatatypes metric
				// we check if these literals are used correctly against their
				// defined property. We also check for untyped literals in another metric
				if (this.compatibleDatatype(obj))
					numberCorrectLiterals++;
				else {
					numberIncorrectLiterals++;
				}
			}
		}
	}

	@Override
	public Long metricValue() {
		long metricValue = (((long) numberIncorrectLiterals + (long) numberCorrectLiterals) > 0)
				? (long) numberCorrectLiterals / ((long) numberIncorrectLiterals + (long) numberCorrectLiterals)
				: 1;
		statsLogger.info(
				"CompatibleDatatype. Dataset: {} - Total # Correct Literals : {}; # Incorrect Literals : {}; # Metric Value: {}",
				this.getDatasetURI(), numberCorrectLiterals, numberIncorrectLiterals, metricValue);

	/*	if (((Double) metricValue).isNaN())
			metricValue = 1.0d;*/

		return metricValue;
	}

	@Override
	public Resource getMetricURI() {
		return DQM.CompatibleDatatype;
	}

	
	@Override
	public boolean isEstimate() {
		return false;
	}

	@Override
	public Resource getAgentURI() {
		return DQM.LuzzuProvenanceAgent;
	}

	private boolean compatibleDatatype(Node lit_obj) {
		RDFNode n = problemModel.asRDFNode(lit_obj);
		Literal lt = (Literal) n;
		RDFDatatype dt = lt.getDatatype();
		String stringValue = lt.getLexicalForm();

		return dt.isValid(stringValue);
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
