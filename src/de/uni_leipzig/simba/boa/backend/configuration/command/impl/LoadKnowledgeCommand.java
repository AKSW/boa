package de.uni_leipzig.simba.boa.backend.configuration.command.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import de.uni_leipzig.simba.boa.backend.configuration.NLPediaSettings;
import de.uni_leipzig.simba.boa.backend.configuration.NLPediaSetup;
import de.uni_leipzig.simba.boa.backend.configuration.command.Command;
import de.uni_leipzig.simba.boa.backend.crawl.RelationFinder;
import de.uni_leipzig.simba.boa.backend.dao.DaoFactory;
import de.uni_leipzig.simba.boa.backend.dao.rdf.TripleDao;
import de.uni_leipzig.simba.boa.backend.rdf.entity.Property;
import de.uni_leipzig.simba.boa.backend.rdf.entity.Resource;
import de.uni_leipzig.simba.boa.backend.rdf.entity.Triple;
import de.uni_leipzig.simba.boa.backend.wordnet.query.WordnetQuery;


public class LoadKnowledgeCommand implements Command {

	private List<Triple> tripleList = new ArrayList<Triple>();
//	static {
//		
//		allowedProperties = Arrays.asList("http://dbpedia.org/ontology/crosses","http://dbpedia.org/ontology/locatedInArea","http://dbpedia.org/ontology/manager",
//				"http://dbpedia.org/ontology/creator","http://dbpedia.org/ontology/operator","http://dbpedia.org/ontology/unitaryAuthority",
//				"http://dbpedia.org/ontology/notableCommander","http://dbpedia.org/ontology/capital","http://dbpedia.org/ontology/ceremonialCounty",
//				"http://dbpedia.org/ontology/place","http://dbpedia.org/ontology/foundationPerson","http://dbpedia.org/ontology/hubAirport",
//				"http://dbpedia.org/ontology/child","http://dbpedia.org/ontology/cinematography","http://dbpedia.org/ontology/architect",
//				"http://dbpedia.org/ontology/influencedBy","http://dbpedia.org/ontology/regionServed","http://dbpedia.org/ontology/garrison",
//				"http://dbpedia.org/ontology/commandStructure","http://dbpedia.org/ontology/leftTributary","http://dbpedia.org/ontology/rightTributary",
//				"http://dbpedia.org/ontology/mother","http://dbpedia.org/ontology/militaryUnit","http://dbpedia.org/ontology/recordPlace",
//				"http://dbpedia.org/ontology/father","http://dbpedia.org/ontology/tenant","http://dbpedia.org/ontology/musicComposer",
//				"http://dbpedia.org/ontology/network","http://dbpedia.org/ontology/sisterStation","http://dbpedia.org/ontology/guest",
//				"http://dbpedia.org/ontology/managerClub","http://dbpedia.org/ontology/leaderName","http://dbpedia.org/ontology/nearestCity",
//				"http://dbpedia.org/ontology/publisher","http://dbpedia.org/ontology/author","http://dbpedia.org/ontology/coachedTeam",
//				"http://dbpedia.org/ontology/spouse","http://dbpedia.org/ontology/affiliation","http://dbpedia.org/ontology/ground",
//				"http://dbpedia.org/ontology/riverMouth","http://dbpedia.org/ontology/musicalArtist","http://dbpedia.org/ontology/musicalBand",
//				"http://dbpedia.org/ontology/award","http://dbpedia.org/ontology/writer","http://dbpedia.org/ontology/almaMater",
//				"http://dbpedia.org/ontology/occupation","http://dbpedia.org/ontology/formerTeam","http://dbpedia.org/ontology/deathPlace",
//				"http://dbpedia.org/ontology/birthPlace","http://dbpedia.org/ontology/trainer");
//	}
	
	public static void main(String[] args) {

//		NLPediaSetup s = new NLPediaSetup(false);
//		LoadKnowledgeCommand c = new LoadKnowledgeCommand();
//		c.execute();
	}
	
	@Override
	public void execute() {
		
		System.out.println("Starting to load background knowledge into database!");
		long start = new Date().getTime();
		
		TripleDao tripleDao		= (TripleDao) DaoFactory.getInstance().createDAO(TripleDao.class);

		List<String[]> labels =  RelationFinder.getRelationFromFile(NLPediaSettings.getInstance().getSetting("labelOutputFile"));
		Map<String,Resource> resourceMap = new HashMap<String, Resource>();
		
		Pattern pattern = Pattern.compile("\\(.+?\\)");
	    Matcher matcher;

		// 0_URI1 ||| 1_LABEL1 ||| 2_LABELS1 ||| 3_PROP ||| 4_URI2 ||| 5_LABEL2 ||| 6_LABELS2 ||| 7_RANGE ||| 8_DOMAIN ||| 9_isSubject/isObject
		for ( String[] line : labels ) {
			
			String subjectUri		= "";
			String subjectLabel		= "";
			String subjectLabels	= "";
			String subjectContext	= "";
			String subjectType		= "";
			String objectUri		= "";
			String objectLabel		= "";
			String objectLabels		= "";
			String objectContext	= "";
			String objectType		= "";
			String predicate		= line[3];
			String range			= line[7];
			String domain			= line[8];
			
			boolean isSubject		= line[9].equals("isSubject") ? true : false;
			
			if ( isSubject ) { 
				
				subjectUri 		= line[0];
			    matcher = pattern.matcher(line[1]);
			    while (matcher.find()) { subjectContext = matcher.group(); }
				subjectLabel	= line[1].replaceAll("\\(.+?\\)", "").trim();
				
				objectUri		= line[4];
			    matcher			= pattern.matcher(line[5]);
			    while (matcher.find()) { objectContext = matcher.group(); }
				objectLabel		= line[5].replaceAll("\\(.+?\\)", "").trim();
				
				subjectLabels	= line[2];
				objectLabels	= line[6];
				subjectType		= domain;
				objectType		= range;
			}
			else {
				
				subjectUri 		= line[4];
				subjectLabel	= line[5].replaceAll("\\(.+?\\)", "").trim();
				matcher			= pattern.matcher(line[5]);
			    while (matcher.find()) { subjectContext = matcher.group(); }
				
				objectUri		= line[0];
				objectLabel		= line[1].replaceAll("\\(.+?\\)", "").trim();
				matcher			= pattern.matcher(line[1]);
			    while (matcher.find()) { objectContext = matcher.group(); }

			    subjectType		= range;
			    objectType		= domain;
			    subjectLabels	= line[6];
			    objectLabels	= line[2];
			}
			
			// create the resource: subject if not found
			Resource sub = resourceMap.get(subjectUri);
			if ( sub == null ) {
				
				sub = new Resource();
				sub.setUri(subjectUri);
				sub.setLabel(subjectLabel);
				sub.setSurfaceForms(subjectLabels);
				sub.setType(subjectType);
				if ( subjectContext.length() > 0 ) {
					sub.setContext(subjectContext.substring(1, subjectContext.length()-1));	
				}
				resourceMap.put(subjectUri, sub);
			}
			
			// create the property if not found
			Property p = (Property) resourceMap.get(predicate); 
			if ( p == null ) {
				
				p = new Property();
				p.setUri(predicate);
				p.setRdfsDomain(domain);
				p.setRdfsRange(range);
				p.setLabel(StringUtils.join(predicate.replace("http://dbpedia.org/ontology/", "").split("(?=\\p{Upper})"), " ").toLowerCase());
				p.setSynsets(StringUtils.join(WordnetQuery.getSynsetsForAllSynsetTypes(p.getLabel()), ","));
				resourceMap.put(predicate, p);
			}
			
			// create the resource: object if not found
			Resource obj = resourceMap.get(objectUri);
			if ( obj == null ) {
				
				obj = new Resource();
				obj.setUri(objectUri);
				obj.setLabel(objectLabel);
				obj.setSurfaceForms(objectLabels);
				obj.setType(objectType);
				if ( objectContext.length() > 0 ) {
					obj.setContext(objectContext.substring(1, objectContext.length()-1));
				}
				resourceMap.put(objectUri, obj);
			}
			
			// create and save the triple
			Triple triple = new Triple();
			triple.setLearnedInIteration(0);
			triple.setCorrect(true);
			triple.setSubject(sub);
			triple.setProperty(p);
			triple.setObject(obj);
			
			tripleList.add(triple);
		}
		System.out.println("Starting to batch save triples to database!");
		tripleDao.batchSaveOrUpdate(tripleList);
		
		System.out.println("Loading background knowledge took " + (new Date().getTime() - start) + "ms.");
	}
	
	public List<Triple> getTriples(){
		
		return this.tripleList;
	}
}
