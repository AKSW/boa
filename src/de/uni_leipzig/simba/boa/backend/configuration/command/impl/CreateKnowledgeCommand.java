package de.uni_leipzig.simba.boa.backend.configuration.command.impl;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.lucene.queryParser.ParseException;

import de.uni_leipzig.simba.boa.backend.Constants;
import de.uni_leipzig.simba.boa.backend.configuration.NLPediaSettings;
import de.uni_leipzig.simba.boa.backend.configuration.NLPediaSetup;
import de.uni_leipzig.simba.boa.backend.configuration.command.Command;
import de.uni_leipzig.simba.boa.backend.dao.DaoFactory;
import de.uni_leipzig.simba.boa.backend.dao.pattern.PatternMappingDao;
import de.uni_leipzig.simba.boa.backend.dao.rdf.ResourceDao;
import de.uni_leipzig.simba.boa.backend.dao.rdf.TripleDao;
import de.uni_leipzig.simba.boa.backend.entity.context.Context;
import de.uni_leipzig.simba.boa.backend.entity.context.LeftContext;
import de.uni_leipzig.simba.boa.backend.entity.context.RightContext;
import de.uni_leipzig.simba.boa.backend.entity.pattern.Pattern;
import de.uni_leipzig.simba.boa.backend.entity.pattern.PatternMapping;
import de.uni_leipzig.simba.boa.backend.entity.pattern.feature.PatternScoreThread;
import de.uni_leipzig.simba.boa.backend.logging.NLPediaLogger;
import de.uni_leipzig.simba.boa.backend.nlp.NamedEntityRecognizer;
import de.uni_leipzig.simba.boa.backend.rdf.entity.Property;
import de.uni_leipzig.simba.boa.backend.rdf.entity.Resource;
import de.uni_leipzig.simba.boa.backend.rdf.entity.Triple;
import de.uni_leipzig.simba.boa.backend.rdf.uri.UriRetrieval;
import de.uni_leipzig.simba.boa.backend.rdf.uri.impl.DbpediaUriRetrieval;
import de.uni_leipzig.simba.boa.backend.search.PatternSearcher;
import de.uni_leipzig.simba.boa.backend.search.concurrent.CreateKnowledgeCallable;
import de.uni_leipzig.simba.boa.backend.search.concurrent.CreateKnowledgeThread;
import de.uni_leipzig.simba.boa.backend.search.concurrent.PrintProgressTask;
import de.uni_leipzig.simba.boa.backend.util.ListUtil;
import de.uni_leipzig.simba.boa.backend.util.PatternUtil;
import de.uni_leipzig.simba.boa.backend.util.PatternUtil.PatternSelectionStrategy;

/**
 * 
 * @author Daniel Gerber
 */
public class CreateKnowledgeCommand implements Command {

	private final NLPediaLogger logger = new NLPediaLogger(CreateKnowledgeCommand.class);
	
	private final PatternMappingDao patternMappingDao			= (PatternMappingDao) DaoFactory.getInstance().createDAO(PatternMappingDao.class);
	private final TripleDao tripleDao							= (TripleDao) DaoFactory.getInstance().createDAO(TripleDao.class);
	
	private final Integer NUMBER_OF_CREATE_KNOWLEDGE_THREADS	= Integer.valueOf(NLPediaSettings.getInstance().getSetting("number.of.create.knowledge.threads"));
	private final String NEW_TRIPLES_FILE							=	NLPediaSettings.getInstance().getSetting("ntriples.file.path") +  
																	"new_triples_score" + 
																	NLPediaSettings.getInstance().getSetting("score.threshold.create.knowledge") +
																	"_topn" + 
																	NLPediaSettings.getInstance().getSetting("top.n.pattern") +
																	".nt";
																	// something like: /path/to/file/triples_score0.7_topn20.nt
	private final String KNOWN_TRIPLES_FILE							=	NLPediaSettings.getInstance().getSetting("ntriples.file.path") +  
																	"known_triples_score" + 
																	NLPediaSettings.getInstance().getSetting("score.threshold.create.knowledge") +
																	"_topn" + 
																	NLPediaSettings.getInstance().getSetting("top.n.pattern") +
																	".nt";
																	// something like: /path/to/file/triples_score0.7_topn20.nt
	
	private static final String BACKGROUND_KNOWLEDGE = NLPediaSettings.getInstance().getSetting("bk.out.file");
	private final List<PatternMapping> patternMappingList;
	public static Map<Integer,Triple> tripleMap = null;

	public CreateKnowledgeCommand(List<PatternMapping> mappings) {

		if ( mappings != null ) this.patternMappingList = mappings;
		else this.patternMappingList = patternMappingDao.findAllPatternMappings();
		this.buildTripleMap();
	}
	
	public void execute(){
		
		// split the mappings into numberOfThreads lists for numberOfThreads threads
		List<List<PatternMapping>> patternMappingSubLists	= ListUtil.split(patternMappingList, (patternMappingList.size() / NUMBER_OF_CREATE_KNOWLEDGE_THREADS) + 1 );
		List<Thread> threadList = new ArrayList<Thread>();
		
		// start all threads
		for (int i = 0 ; i < NUMBER_OF_CREATE_KNOWLEDGE_THREADS ; i++ ) {
			
			Thread t = new CreateKnowledgeThread(patternMappingSubLists.get(i));
			t.setName("PatternScoreThread-" + (i + 1) + "-" + patternMappingSubLists.get(i).size());
			threadList.add(i, t);
			t.start();
			System.out.println(t.getName() + " started!");
			this.logger.info(t.getName() + " started!");
		}
		
		// print the progress
		Timer timer = new Timer();
		timer.schedule(new PrintProgressTask(threadList), 0, 30000);
		
		// wait for all to finish
		for ( Thread t : threadList ) {
			
			try {
				t.join();	
			}
			catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		timer.cancel();
		
		for ( Thread t: threadList ) {
			
			writeNewTriplesFile(((CreateKnowledgeThread)t).getNewTripleMap().values());
			writeKnownTriplesFile(((CreateKnowledgeThread)t).getKnownTripleMap().values());
		}
	}

	/**
	 * @param filename 
	 * 
	 */
//	@Override
//	public void execute() {
//
//		// one thread per pattern mapping but only n threads get executed at the same time
//		for (PatternMapping mapping : this.patternMappingList ) {
//			
//			this.writeNTriplesFile(new CreateKnowledgeCallable(mapping).call());
//			this.logger.info("Added worker for mapping: " + mapping.getProperty().getUri());
//		}
//		
//		try {
//			
//			// create a thread pool and service for n threads/callable
//			ExecutorService executorService = Executors.newFixedThreadPool(NUMBER_OF_CREATE_KNOWLEDGE_THREADS);
//			this.logger.info("Created executorservice for knowledge creation of " + NUMBER_OF_CREATE_KNOWLEDGE_THREADS + " threads.");
//			
//			List<Callable<Collection<Triple>>> todo = new ArrayList<Callable<Collection<Triple>>>(this.patternMappingList.size());
//
//			// one thread per pattern mapping but only n threads get executed at the same time
//			for (PatternMapping mapping : this.patternMappingList ) {
//				
//				todo.add(new CreateKnowledgeCallable(mapping));
//				this.logger.info("Added worker for mapping: " + mapping.getProperty().getUri());
//			}
//			
//			// invoke all waits until all threads are finished
//			List<Future<Collection<Triple>>> answers = executorService.invokeAll(todo);
//			
//			for (Future<Collection<Triple>> future : answers) {
//			
//				Collection<Triple> triples = future.get();
//				this.logger.info("Calling write to file method with " + triples.size() + " triples.");
//				this.writeNTriplesFile(triples);
//			}
//			
//			// shut down the service and all threads
//			executorService.shutdown();
//		}
//		catch (ExecutionException e) {
//			
//			this.logger.error("Execption", e);
//			e.printStackTrace();
//		}
//		catch (InterruptedException e) {
//			
//			this.logger.error("Execption", e);
//			e.printStackTrace();
//		}
//		catch (Exception e) {
//			
//			this.logger.error("Execption", e);
//			e.printStackTrace();
//		}
//	}

	private void writeFile(String filename, Collection<Triple> resultList) {

		try {
			
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filename, true), "UTF-8"));

			for (Triple t : resultList) {
				
				if ( t.getObject().getUri().startsWith("http://")) {
					
					writer.write("<" + t.getSubject().getUri() + "> " +
								 "<"+ t.getProperty().getUri() + "> " +
								 "<" + t.getObject().getUri() +"> . " + Constants.NEW_LINE_SEPARATOR);
				}
				else {
					
					writer.write("<" + t.getSubject().getUri() + "> " +
								 "<"+ t.getProperty().getUri() + "> " +
								 "\"" + t.getObject().getLabel() +"\" . " + Constants.NEW_LINE_SEPARATOR);
				}
			}
			tripleDao.batchSaveOrUpdate(new ArrayList<Triple>(resultList));
			writer.close();
		}
		catch (UnsupportedEncodingException e1) {
			
			this.logger.error("UnsupportedEncodingException", e1);
		}
		catch (FileNotFoundException e1) {
			
			this.logger.error("UnsupportedEncodingException", e1);
		}
		catch (IOException e) {
			
			this.logger.error("UnsupportedEncodingException", e);
		}
	}
	
	private void writeKnownTriplesFile(Collection<Triple> resultList) {

		this.writeFile(KNOWN_TRIPLES_FILE, resultList);
	}
	
	private void writeNewTriplesFile(Collection<Triple> resultList) {

		this.writeFile(NEW_TRIPLES_FILE, resultList);
	}

	private void buildTripleMap() {

		if ( tripleMap == null ) {
			
			if ( !(new File(BACKGROUND_KNOWLEDGE)).exists() ) {
				
				tripleMap = new HashMap<Integer,Triple>();
				for (Triple t : tripleDao.findAllTriples()) {
					
					tripleMap.put(t.hashCode(), t);
				}
				try {
					
					ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(new File(BACKGROUND_KNOWLEDGE)));
					oos.writeObject(tripleMap);
					oos.close();
				}
				catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			else {
				
				try {
					
					ObjectInputStream ois = new ObjectInputStream(new FileInputStream(new File(BACKGROUND_KNOWLEDGE)));
					tripleMap = (HashMap<Integer,Triple>) ois.readObject();
					ois.close();
				}
				catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				catch (ClassNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
}
