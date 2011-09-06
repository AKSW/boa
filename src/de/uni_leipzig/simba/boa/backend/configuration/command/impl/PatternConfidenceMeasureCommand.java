package de.uni_leipzig.simba.boa.backend.configuration.command.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Timer;

import de.uni_leipzig.simba.boa.backend.configuration.NLPediaSettings;
import de.uni_leipzig.simba.boa.backend.configuration.command.Command;
import de.uni_leipzig.simba.boa.backend.dao.DaoFactory;
import de.uni_leipzig.simba.boa.backend.dao.pattern.PatternMappingDao;
import de.uni_leipzig.simba.boa.backend.entity.pattern.Pattern;
import de.uni_leipzig.simba.boa.backend.entity.pattern.PatternMapping;
import de.uni_leipzig.simba.boa.backend.entity.pattern.confidence.ConfidenceMeasure;
import de.uni_leipzig.simba.boa.backend.entity.pattern.confidence.ConfidenceMeasureFactory;
import de.uni_leipzig.simba.boa.backend.entity.pattern.confidence.PatternConfidenceMeasureThread;
import de.uni_leipzig.simba.boa.backend.logging.NLPediaLogger;
import de.uni_leipzig.simba.boa.backend.search.concurrent.PrintProgressTask;
import de.uni_leipzig.simba.boa.backend.util.ListUtil;

/**
 * 
 * @author Daniel Gerber
 */
public class PatternConfidenceMeasureCommand implements Command {

	private final NLPediaLogger logger = new NLPediaLogger(PatternConfidenceMeasureCommand.class);
	
	private PatternMappingDao patternMappingDao = (PatternMappingDao) DaoFactory.getInstance().createDAO(PatternMappingDao.class);
	private List<PatternMapping> patternMappingList = null;
	
	public static double NUMBER_OF_PATTERN_MAPPINGS;

	public PatternConfidenceMeasureCommand(Map<Integer,PatternMapping> patternMappingList) {
		
		if ( patternMappingList != null ){
		
			this.patternMappingList = new ArrayList<PatternMapping>(patternMappingList.values());
		}
		else {
			
			this.patternMappingList = this.patternMappingDao.findAllPatternMappings();
		}
		PatternConfidenceMeasureCommand.NUMBER_OF_PATTERN_MAPPINGS = (double) this.patternMappingList.size();
	}
	
	@Override
	public void execute() {
		
		int numberOfConfidenceMeasureThreads = new Integer(NLPediaSettings.getInstance().getSetting("numberOfConfidenceMeasureThreads")).intValue();
		
		// split the mappings into several lists
		List<List<PatternMapping>> patternMappingSubLists	= ListUtil.split(patternMappingList, (patternMappingList.size() / numberOfConfidenceMeasureThreads));
		
		List<Thread> threadList = new ArrayList<Thread>();
		List<PatternMapping> results = new ArrayList<PatternMapping>();
		
		if ( numberOfConfidenceMeasureThreads != 1 ) {
			
			for (int i = 0 ; i < numberOfConfidenceMeasureThreads ; i++ ) {
				
				Thread t = new PatternConfidenceMeasureThread(patternMappingSubLists.get(i));
				t.setName("PatternConfidenceMeasureThread-" + (i + 1));
				threadList.add(i, t);
				t.start();
				System.out.println(t.getName() + " started!");
				this.logger.info(t.getName() + " started!");
			}
			
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
				
				results.addAll(((PatternConfidenceMeasureThread)t).getConfidenceMeasuredPatternMappings());
			}
		}
		else {
			
			Map<String,ConfidenceMeasure> confidenceMeasures = ConfidenceMeasureFactory.getInstance().getConfidenceMeasureMap();
			
			System.out.println("Measuring confidence for " + this.patternMappingList.size() + " pattern mappings with " + confidenceMeasures.size() + " confidence measures!");
			
			// go through all confidence measurements
			for ( ConfidenceMeasure confidenceMeasure : confidenceMeasures.values() ) {

				this.logger.info(confidenceMeasure.getClass().getSimpleName() + " started from " + confidenceMeasure.getClass().getSimpleName() +"!");
				System.out.println(confidenceMeasure.getClass().getSimpleName() + " started!");
				long start = new Date().getTime();
				
				// and check each pattern mapping
				for (PatternMapping patternMapping : this.patternMappingList ) {
				
					this.logger.debug("Calculation of confidence for mapping: " + patternMapping.getProperty().getUri());
					confidenceMeasure.measureConfidence(patternMapping);
				}
				this.logger.info(confidenceMeasure.getClass().getSimpleName() + " from " + confidenceMeasure.getClass().getSimpleName() + " finished in " + (new Date().getTime() - start) + "ms!");
				System.out.println(confidenceMeasure.getClass().getSimpleName() + " finished!");
			}
			results.addAll(patternMappingList);
		}

		System.out.println("All confidence measurement threads are finished. Start to calculate normalized confidence!");
		
		double maxConfidenceForAllPatternMappings = 0;
		
		for (PatternMapping pm : results ) {
			
			// calculate local and global maxima
			double maxConfidenceForPatternMapping = 0;
			
			for (Pattern pattern : pm.getPatterns() ) {
				
				if ( !pattern.isUseForPatternEvaluation() ) continue;
				
				double specificity = pattern.getSpecificityForIteration(IterationCommand.CURRENT_ITERATION_NUMBER);
				double typicity = pattern.getTypicityForIteration(IterationCommand.CURRENT_ITERATION_NUMBER);
				double support = pattern.getSupportForIteration(IterationCommand.CURRENT_ITERATION_NUMBER);
				
//				System.out.println(String.format("%s: %s, %s, %s", pattern.getNaturalLanguageRepresentation(), specificity, typicity, support));
				
				double confidence = 10 * typicity + 2 * support + 1 * specificity + 10 * pattern.getSimilarity() + 4 * pattern.getReverb();
				pattern.setConfidenceForIteration(IterationCommand.CURRENT_ITERATION_NUMBER, confidence);
				pattern.setConfidence(confidence);
				
				maxConfidenceForPatternMapping		= Math.max(maxConfidenceForPatternMapping, confidence);
				maxConfidenceForAllPatternMappings	= Math.max(maxConfidenceForAllPatternMappings, confidence);
			}
			
			// set local maximums
			for ( Pattern pattern : pm.getPatterns() ) {
				
				if ( !Double.isNaN(maxConfidenceForPatternMapping) && !Double.isInfinite(maxConfidenceForPatternMapping) && pattern.isUseForPatternEvaluation() ) {
				
					pattern.setConfidenceForIteration(IterationCommand.CURRENT_ITERATION_NUMBER, pattern.getConfidenceForIteration(IterationCommand.CURRENT_ITERATION_NUMBER) /  maxConfidenceForPatternMapping);
					pattern.setConfidence(pattern.getConfidence() / maxConfidenceForPatternMapping);
				}
				else {
					
					pattern.setConfidenceForIteration(IterationCommand.CURRENT_ITERATION_NUMBER, 0D);
					pattern.setConfidence(0D);
				}
			}
		}
		// begin updating the pattern mappings
		long start = new Date().getTime();
		
		// set global maxima and update pattern mappings and cascade
		for ( PatternMapping mapping : results ) {
			
			for ( Pattern pattern : mapping.getPatterns() ) {
				
				if ( !Double.isNaN(maxConfidenceForAllPatternMappings) && !Double.isInfinite(maxConfidenceForAllPatternMappings) && pattern.isUseForPatternEvaluation() ) {
					
					pattern.setGlobalConfidence(pattern.getConfidence() /  maxConfidenceForAllPatternMappings);
				}
				else 
					pattern.setGlobalConfidence(0d);
			}
			this.logger.info("Updating pattern mapping " + mapping.getProperty().getUri());
			this.patternMappingDao.updatePatternMapping(mapping);
		}
		System.out.println("Updating PatternMappings took " + (new Date().getTime() - start) + "ms.");
		
		// set this so that the next command does not need to query them from the database again
		this.patternMappingList = results;
	}

	
	/**
	 * @return the patternMappingList
	 */
	public List<PatternMapping> getPatternMappingList() {
	
		return patternMappingList;
	}

	
	/**
	 * @param patternMappingList the patternMappingList to set
	 */
	public void setPatternMappingList(List<PatternMapping> patternMappingList) {
	
		this.patternMappingList = patternMappingList;
	}
}
