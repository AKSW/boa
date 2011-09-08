package de.uni_leipzig.simba.boa.backend.entity.pattern.confidence.impl;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections.ListUtils;

import uk.ac.shef.wit.simmetrics.similaritymetrics.AbstractStringMetric;
import uk.ac.shef.wit.simmetrics.similaritymetrics.QGramsDistance;
import de.uni_leipzig.simba.boa.backend.entity.pattern.Pattern;
import de.uni_leipzig.simba.boa.backend.entity.pattern.PatternMapping;
import de.uni_leipzig.simba.boa.backend.entity.pattern.confidence.ConfidenceMeasure;
import de.uni_leipzig.simba.boa.backend.logging.NLPediaLogger;
import de.uni_leipzig.simba.boa.backend.search.PatternSearcher;
import de.uni_leipzig.simba.boa.backend.wordnet.similarity.SimilarityAssessor;
import de.uni_leipzig.simba.boa.backend.wordnet.similarity.WordNotFoundException;


public class StringSimilarityMeasure implements ConfidenceMeasure {

	private SimilarityAssessor similarityAssessor = new SimilarityAssessor();
	private NLPediaLogger logger = new NLPediaLogger(StringSimilarityMeasure.class);
	
	@Override
	public void measureConfidence(PatternMapping mapping) {

//		System.out.println("Mapping: " +mapping.getProperty().getUri());
		
		// we calculate the qgram distance between the NLR and the label of the property
		for ( Pattern pattern : mapping.getPatterns() ) {
			
			if ( !pattern.isUseForPatternEvaluation() ) continue;
			
//			System.out.println("\tPattern: " +pattern.getNaturalLanguageRepresentation());
			
			// get the NLR and remove all stopwords
			String naturalLanguageRepresentation = pattern.retrieveNaturalLanguageRepresentationWithoutVariables();
			Set<String> tokens = new HashSet<String>(Arrays.asList(naturalLanguageRepresentation.split(" ")));
			tokens.removeAll(PatternSearcher.STOP_WORDS);
			
			double similarity = 0D;
			
			List<String> wordsToCompare;
			
			if ( !mapping.getProperty().retrieveSynsetsForLabel().isEmpty() ) {
				
				wordsToCompare = mapping.getProperty().retrieveSynsetsForLabel();
			}
			else {
				
				wordsToCompare = Arrays.asList(mapping.getProperty().getLabel().split(" "));
			}
			
			// go through all words and synset combination and sum up the similarity
			for ( String token : tokens ) {
				
				for ( String wordFrom : wordsToCompare ) {
				
					try {
						
						similarity = Math.max(similarity, this.similarityAssessor.getSimilarity(wordFrom, token));
					}
					catch (WordNotFoundException e) {
						
						this.logger.error("Word not found: " + e);
					}
				}
			}
			pattern.setSimilarity(similarity);
		}
	}
}
