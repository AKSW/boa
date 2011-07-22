package de.uni_leipzig.simba.boa.backend.nlp.learn;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import org.apache.lucene.analysis.SimpleAnalyzer;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.util.FileManager;

import de.uni_leipzig.simba.boa.backend.Constants;
import de.uni_leipzig.simba.boa.backend.configuration.NLPediaSettings;
import de.uni_leipzig.simba.boa.backend.logging.NLPediaLogger;
import de.uni_leipzig.simba.boa.backend.util.SerializationUtil;
import de.uni_leipzig.simba.boa.backend.util.rdf.ClassIndexer;

/**
 * 
 * @author Daniel Gerber
 */
public class NamedEntityRecognizerLearner {

	private final NLPediaLogger logger = new NLPediaLogger(NamedEntityRecognizerLearner.class);

	private Map<Integer, String> sentences = new HashMap<Integer, String>();
	private QueryParser exactMatchParser = new QueryParser(Version.LUCENE_30, "sentence", new SimpleAnalyzer());
	private Directory index = null;
	private IndexSearcher indexSearcher = null;
	private ClassIndexer indexer = null;

	private static String PREFIX = NLPediaSettings.getInstance().getSetting("learnPrefix");

	private String pathToTrainedSentenceFile = PREFIX + "trained_sentences.txt";
	private String pathToLabelsFile = PREFIX + "labels_en.nt";
	private String pathToTypesFile = PREFIX + "instance_types_en.nt";
	private String pathToDBpediaOntology = PREFIX + "dbpedia_3.6.owl";
	private String pathToSerializedFile = PREFIX + "knowledge.ser";

	private int maxNumberOfDocuments = Integer.valueOf(NLPediaSettings.getInstance().getSetting("maxNumberOfLearnSentences"));

	/**
	 * @param args
	 */
	public void learn() {

		long start = new Date().getTime();
		System.out.print("Reading ontology ... ");
		OntModel ontModel = ModelFactory.createOntologyModel();
		InputStream in = FileManager.get().open(pathToDBpediaOntology);
		ontModel.read(in, "");
		indexer = new ClassIndexer();
		indexer.index(ontModel);
		System.out.print("DONE in " + (new Date().getTime() - start) + "ms!\n");

		try {

			index = FSDirectory.open(new File(NLPediaSettings.getInstance().getSetting("sentenceIndexDirectory")));
			indexSearcher = new IndexSearcher(index, true);
		}
		catch (CorruptIndexException cie) {

			cie.printStackTrace();
		}
		catch (IOException ioe) {

			ioe.printStackTrace();
		}
		
		SerializationUtil sUtil = new SerializationUtil();
		
		BackgroundKnowledge knowledge = null;
		if ( sUtil.isDeserializeable(pathToSerializedFile) ) {
			
			start = new Date().getTime();
			System.out.print("Reading labels ... ");
			knowledge = sUtil.deserializeObject(new BackgroundKnowledge(), pathToSerializedFile);
			System.out.print("DONE in " + (new Date().getTime() - start) + "ms!\n");
		}
		else {
			
			knowledge = new BackgroundKnowledge(pathToTypesFile, pathToLabelsFile);
			sUtil.serializeObject(knowledge, pathToSerializedFile);
		}
		
		System.out.println("There are " + knowledge.getLabels().size() + " labels to search.");
		System.out.println("They maximum number of sentences is " + this.maxNumberOfDocuments);

		int n = 0;
		
		// go through each label
		for (Entry<String, String> entry : knowledge.getLabels().entrySet()) {

			try {
				
				if (n++ % 100 == 0 ) {
					
					this.printProgBar((n/knowledge.getLabels().size()) * 100);
					this.logger.info("Iteration n: \t" + n + " \t" +((n/knowledge.getLabels().size()) * 100) + " %");
				}

				String uri = entry.getKey();
				String label = entry.getValue().trim();
				
				System.out.println(uri + ": \"" + label + "\"");

				if (uri != null & label != null) {

					// find all sentences containing the label
					Map<Integer, String> sentencesContainingLabels = getSentencesContainingLabel(label);

					// get the most precise type definition for an URI
					Set<String> typesForUri = knowledge.getTypes().get(uri);

					// for some uris we have labels but no types
					if (typesForUri == null) {

						this.logger.info("No type statements found for: " + uri);
					}
					else {

						String typeReplacement = getTypeForUri(uri, typesForUri);
						typeReplacement = typeReplacement.replace("http://dbpedia.org/ontology/", "");

						String[] tokensOfLabel = label.split(" ");

						for (Entry<Integer, String> sent : sentencesContainingLabels.entrySet()) {

							int indexId = sent.getKey();
							// if the sentence was already found in the sentence
							// list, take it from there else use it untagged
							// from the index
							String sentence = sent.getValue();
							if (sentences.containsKey(indexId)) sentences.get(indexId);

							String replacement = "";
							for (int i = 0; i < tokensOfLabel.length; i++) {
								
								replacement += tokensOfLabel[i] + "___" + typeReplacement + " ";
							}
							// replace the whole label with the complete replacement
							while (sentence.contains(label)) {
								
								sentence = sentence.replace(label, replacement);
							}
							logger.debug("##################################################");
							logger.debug(label);
							logger.debug(replacement);
							logger.debug(sentence);
							sentences.put(indexId, sentence);
						}
					}
				}
			}
			catch (Exception e) {

				logger.error("Something went wrong during creation of NER training data", e);
			}
		}
		writeTrainedModelToFile();
	}

	private void writeTrainedModelToFile() {

		try {

			// write the trained model in utf8 to a file containing
			// "word \tab tag" per line
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(pathToTrainedSentenceFile)), "UTF-8"));

			System.out.println("\nWriting " + sentences.values().size() + " sentences to file!");

			for (String sentence : sentences.values()) {

				for (String token : sentence.split(" ")) {

					if (!token.contains("___")) {

						writer.write(token + "\t" + "O" + Constants.NEW_LINE_SEPARATOR);
					}
					else {

						String[] tokens = token.split("___");
						writer.write(tokens[0] + "\t" + tokens[1] + Constants.NEW_LINE_SEPARATOR);
					}
				}
			}
			writer.close();
		}
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private String getTypeForUri(String uri, Set<String> types) {

		Map<String, Integer> depth = new HashMap<String, Integer>();

		// calculate the depth for all types in the ontology
		for (String type : types) {

			depth.put(type, new Long(indexer.getHierarchyForClassURI(type).size()).intValue());
		}

		String currentUri = "";
		int biggestCount = 0;

		for (Entry<String, Integer> entry : depth.entrySet()) {

			if (entry.getValue() > biggestCount) {

				biggestCount = entry.getValue();
				currentUri = entry.getKey();
			}
			else
				if (entry.getValue() == biggestCount) {

					System.out.println("multiple deepest types were found for uri: " + uri);
				}
		}

		return currentUri;
	}

	private Map<Integer, String> getSentencesContainingLabel(String label) {

		Map<Integer, String> sentencesContainingLabel = new HashMap<Integer, String>();

		try {

			ScoreDoc[] hits = indexSearcher.search(exactMatchParser.parse("\"" + QueryParser.escape(label) + "\""), null, maxNumberOfDocuments).scoreDocs;

			for (int i = hits.length - 1; i >= 0; i--) {

				sentencesContainingLabel.put(hits[i].doc, indexSearcher.doc(hits[i].doc).get("sentence"));
			}
		}
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return sentencesContainingLabel;
	}

	public void setPathToTrainedSentenceFile(String pathToTrainedSentenceFile) {

		this.pathToTrainedSentenceFile = pathToTrainedSentenceFile;
	}

	public void setPathToDBpediaOntology(String pathToDBpediaOntology) {

		this.pathToDBpediaOntology = pathToDBpediaOntology;
	}

	public void setPathToLabelsFile(String pathToLabelsFile) {

		this.pathToLabelsFile = pathToLabelsFile;
	}

	public void setPathToTypesFile(String pathToTypesFile) {

		this.pathToTypesFile = pathToTypesFile;
	}
	
	public void printProgBar(int percent){
        StringBuilder bar = new StringBuilder("[");

        for(int i = 0; i < 50; i++){
            if( i < (percent/2)){
                bar.append("=");
            }else if( i == (percent/2)){
                bar.append(">");
            }else{
                bar.append(" ");
            }
        }

        bar.append("]   " + percent + "%     ");
        System.out.print("\r" + bar.toString());
    }
}
