package io.github.luzzu.qualitymetrics.accessibility.interlinking;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.RedirectLocations;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.jena.atlas.web.HttpException;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.sparql.core.Quad;
import org.apache.jena.vocabulary.RDF;
import org.mapdb.DB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.luzzu.qualitymetrics.utilities.AbstractQualityMetric;
import io.github.luzzu.qualitymetrics.utilities.mapdb.MapDbFactory;
import io.github.luzzu.qualitymetrics.utilities.techniques.probabilistic.ResourceBaseURIOracle;
import io.github.luzzu.qualitymetrics.utilities.vocabularies.DQM;
import io.github.luzzu.qualityproblems.ProblemCollection;

/**
 * @author Jeremy Debattista
 * 
 * In this metric we identify the total number of external linked used in the dataset. An external link
 * is identified if the subject URI is from one data source and an object URI from ￼another data source.
 * The data source should return RDF data to be considered as 'linked'.
 * In this metric rdf:type triples are skipped since these are not normally considered as part of the
 * Data Level Constant (or Data Level Position).
 * The value returned by this metric is the number of valid external links a dataset has (i.e. the number
 * of resource links not the number of links to datasets)
 * 
 * Based on: [1] Hogan Aidan, Umbrich Jürgen. An empirical survey of Linked Data conformance. Section 5.2, 
 * Linking, Issue VI: Use External URIs (page 20).
 */
public class LinkExternalDataProviders extends AbstractQualityMetric {
	
	/**
	 * MapDB database, used to persist the Map containing the instances found to be declared in the dataset
	 */
	private DB mapDB = MapDbFactory.createAsyncFilesystemDB();
	
	/**
	 * A set that holds all unique resources
	 */
	private Set<String> setResources = MapDbFactory.createHashSet(mapDB, UUID.randomUUID().toString());

	
	/**
	 * A set that holds all unique PLDs that return RDF data
	 */
	private Set<String> setPLDsRDF = new HashSet<String>();
	
	final static Logger logger = LoggerFactory.getLogger(LinkExternalDataProviders.class);


	private final Resource METRIC_URI = DQM.LinksToExternalDataProvidersMetric;
	
	private List<Quad> _problemList = new ArrayList<Quad>();

	private boolean computed = false;
	
	private String localPLD = null;

	private Map<String,String> resolver = new HashMap<String,String>();

	private Set<String> ns404 = new HashSet<String>();

	
	@Override
	public void compute(Quad quad) {
		logger.debug("Computing : {} ", quad.asTriple().toString());
		
		if (localPLD == null)
			localPLD = ResourceBaseURIOracle.extractPayLevelDomainURI(this.getDatasetURI());
		
		if (!(quad.getPredicate().getURI().equals(RDF.type.getURI()))){
			if ((quad.getObject().isURI()) && (!(ResourceBaseURIOracle.extractPayLevelDomainURI(quad.getObject().getURI()).equals(localPLD)))){
			}
		}
		
		if (!(quad.getPredicate().getURI().equals(RDF.type.getURI()))){
			if ((quad.getObject().isURI()) && (!(ResourceBaseURIOracle.extractPayLevelDomainURI(quad.getObject().getURI()).equals(localPLD)))){
				if ((quad.getObject().getURI().startsWith("http")) || (quad.getObject().getURI().startsWith("https"))){
					if ((ResourceBaseURIOracle.extractPayLevelDomainURI(quad.getObject().getURI()).equals("purl.org"))
							|| (ResourceBaseURIOracle.extractPayLevelDomainURI(quad.getObject().getURI()).equals("w3id.org"))){
						String ns = quad.getObject().getNameSpace();
						String ext = null;
						if (resolver.containsKey(ns)) ext = resolver.get(ns);
						else {
							ext = this.getRedirection(quad.getObject().getURI());
							if (ext != null) 
								resolver.put(ns, ext);	
						}
	//					if (ext == null) this.addUriToSampler(quad.getObject().toString()); // do not put purl.org uris 
						if ((ext != null) && (!(ResourceBaseURIOracle.extractPayLevelDomainURI(ext).equals(localPLD)))) setResources.add(ext);
					}
					else
						setResources.add(quad.getObject().toString());
				}
			}
		}
	}
	@Override
	public Long metricValue() {
		if (!computed){
			this.checkForRDFLinks();
			computed = true;
		}
		
		statsLogger.info("LinkExternalDataProviders. Dataset: {} - # Top Level Domains : {};", 
				this.getDatasetURI(), setPLDsRDF.size());
		
		return (long)setPLDsRDF.size();
	}

	@Override
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
	
	private void checkForRDFLinks() {
		ExecutorService service = Executors.newCachedThreadPool();
		for (String s : setResources){
			if (setPLDsRDF.contains(ResourceBaseURIOracle.extractPayLevelDomainURI(s))) 
				continue;

			Future<Boolean> future = service.submit(new ParsableContentChecker(s));
			try {
				boolean isParsable = future.get(3, TimeUnit.SECONDS);
				if (isParsable) {
					setPLDsRDF.add(ResourceBaseURIOracle.extractPayLevelDomainURI(s));
					break; // we have a dereferenceable Linked Data source.
				} 
			} catch (InterruptedException | ExecutionException
					| TimeoutException e) {
			}
		}
 		
//		for (String s : setResources){
//			if (setPLDsRDF.contains(ResourceBaseURIOracle.extractPayLevelDomainURI(s))) 
//				continue;
//			if (isParsableContent(s)) {
//				setPLDsRDF.add(ResourceBaseURIOracle.extractPayLevelDomainURI(s));
//			}
//		}
	}
	
	private String getRedirection(String resource){
		HttpHead head = new HttpHead(resource);
		
		RequestConfig requestConfig = RequestConfig.custom()
				.setSocketTimeout(1000)
				.setConnectTimeout(1000)
				.build();

		CloseableHttpClient httpClient = HttpClientBuilder
									.create()
									.setDefaultRequestConfig(requestConfig)
									.build();
		
        HttpContext context = new BasicHttpContext(); 

		try {
			httpClient.execute(head,context);
			RedirectLocations locations = (RedirectLocations) context.getAttribute(HttpClientContext.REDIRECT_LOCATIONS);
			if (locations.size() == 1) return locations.get(0).toString();
			for(URI loc : locations.getAll()){
				if ((loc.toString().contains("purl.org")) || (loc.toString().contains("w3id.org"))) continue;
				else return loc.toString();
			}
		} catch (Exception e) {
		}		
		return null;
	}
	
//	private boolean isParsableContent(String uri){
//		String ns = ModelFactory.createDefaultModel().createResource(uri).getNameSpace();
//		if (this.ns404.contains(ns)) return false;
//		try{
//			return (RDFDataMgr.loadModel(uri).size() > 0);
//		} catch (HttpException httpE){
//			if (httpE.getResponseCode() == 404) this.ns404.add(ns);
//			return false;
//		} catch (Exception e){
//			return false;
//		}
//	}
//	
	class ParsableContentChecker implements Callable<Boolean>{
		
		String uri = "";
		
		public ParsableContentChecker(String uri){
			this.uri = uri;
		}

		@Override
		public Boolean call() throws Exception {
			final String ns = org.apache.jena.rdf.model.ModelFactory.createDefaultModel().createResource(uri).getNameSpace();
			if (ns404.contains(ns)) return false;
			
			try{
				return (RDFDataMgr.loadModel(uri).size() > 0);
			} catch (HttpException httpE){
				if (httpE.getResponseCode() == 404) ns404.add(ns);
				return false;
			} catch (Exception e){
				return false;
			}
		}
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
