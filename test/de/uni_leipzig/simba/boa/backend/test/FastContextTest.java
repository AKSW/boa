package de.uni_leipzig.simba.boa.backend.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import junit.framework.JUnit4TestAdapter;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import de.uni_leipzig.simba.boa.backend.configuration.NLPediaSetup;
import de.uni_leipzig.simba.boa.backend.entity.context.FastLeftContext;
import de.uni_leipzig.simba.boa.backend.entity.context.FastRightContext;
import de.uni_leipzig.simba.boa.backend.logging.NLPediaLogger;


public class FastContextTest {

	// initialize logging and settings
	NLPediaSetup setup = null;
	NLPediaLogger logger = null;
	
	public static junit.framework.Test suite() {
		
		return new JUnit4TestAdapter(FastContextTest.class);
	}
	
	@Before
	public void setUp() {
		
		this.setup = new NLPediaSetup(true);
		this.logger = new NLPediaLogger(FastContextTest.class);
	}

	@After
	public void cleanUpStreams() {
	    
		this.setup.destroy();
	}
	
	@Test
	public void testFastLeftContext1() {
	
		String sentence		= "The Corinthian is a fictional character in Neil Gaiman 's comic book series `` The Sandman '' .";
		String nerTagged	= "The_O Corinthian_B-MISC is_O a_O fictional_O character_O in_O Neil_B-PER Gaiman_I-PER 's_O comic_O book_O series_O ``_O The_O Sandman_B-MISC ''_O ._O";
		String pattern		= "'s comic book series ``";
		
		FastLeftContext flc = new FastLeftContext(nerTagged, sentence, pattern);
		
		assertEquals(13, flc.getCleanWords().size());
		assertEquals("series", flc.getCleanWords().get(flc.getCleanWords().size() -1 ));
		assertEquals("series_O", flc.getTaggedWords().get(flc.getTaggedWords().size() -1 ));
		assertEquals("Neil Gaiman", flc.getSuitableEntity("http://dbpedia.org/ontology/Person"));
		assertEquals(0, flc.getSuitableEntityDistance("http://dbpedia.org/ontology/Person"));
		
		FastRightContext frc = new FastRightContext(nerTagged, sentence, pattern);
		
		assertEquals(8, frc.getCleanWords().size());
		assertEquals("comic", frc.getCleanWords().get(0));
		assertEquals("comic_O", frc.getTaggedWords().get(0));
		assertEquals("Sandman", frc.getSuitableEntity("http://dbpedia.org/ontology/Work"));
		assertEquals(1, frc.getSuitableEntityDistance("http://dbpedia.org/ontology/Work"));
	}
	
	@Test
	public void testFastLeftContext2() {
	
		String sentence		= "Uprock was created in Brooklyn , N.Y. and breaking was created in the Bronx .";
		String nerTagged	= "Uprock_O was_O created_O in_O Brooklyn_B-LOC ,_O N.Y._B-LOC and_O breaking_O was_O created_O in_O the_B-LOC Bronx_I-LOC ._O";
		String pattern		= "created in the";
		
		FastLeftContext flc = new FastLeftContext(nerTagged, sentence, pattern);
		
		assertEquals(12, flc.getCleanWords().size());
		assertEquals("in", flc.getCleanWords().get(flc.getCleanWords().size() -1 ));
		assertEquals("in_O", flc.getTaggedWords().get(flc.getTaggedWords().size() -1 ));
		assertEquals("N.Y.", flc.getSuitableEntity("http://dbpedia.org/ontology/Place"));
		assertEquals(3, flc.getSuitableEntityDistance("http://dbpedia.org/ontology/Place"));
		
		FastRightContext frc = new FastRightContext(nerTagged, sentence, pattern);
		
		assertEquals(4, frc.getCleanWords().size());
		assertEquals("in", frc.getCleanWords().get(0) );
		assertEquals("in_O", frc.getTaggedWords().get(0));
		assertEquals("the Bronx", frc.getSuitableEntity("http://dbpedia.org/ontology/Place"));
		assertEquals(0, frc.getSuitableEntityDistance("http://dbpedia.org/ontology/Place"));
	}
	
	@Test
	public void testFastLeftContext3() {
	
		String sentence		= "Uprock was created in Brooklyn .";
		String nerTagged	= "Uprock_MISC was_O created_O in_O Brooklyn_B-LOC ._O";
		String pattern		= "was created in";
		
		FastLeftContext flc = new FastLeftContext(nerTagged, sentence, pattern);
		
		assertEquals(3, flc.getCleanWords().size());
		assertEquals("created", flc.getCleanWords().get(flc.getCleanWords().size() -1 ));
		assertEquals("created_O", flc.getTaggedWords().get(flc.getTaggedWords().size() -1 ));
		assertEquals("Uprock", flc.getSuitableEntity("http://dbpedia.org/ontology/MusicalWork"));
		assertEquals(0, flc.getSuitableEntityDistance("http://dbpedia.org/ontology/MusicalWork"));
		
		FastRightContext frc = new FastRightContext(nerTagged, sentence, pattern);
		
		assertEquals(4, frc.getCleanWords().size());
		assertEquals("created", frc.getCleanWords().get(0) );
		assertEquals("created_O", frc.getTaggedWords().get(0));
		assertEquals("Brooklyn", frc.getSuitableEntity("http://dbpedia.org/ontology/Place"));
		assertEquals(0, frc.getSuitableEntityDistance("http://dbpedia.org/ontology/Place"));
	}
	
	@Test
	public void testFastLeftContext4() {
	
		String sentence		= "Has a of Josephine of Daughter , who was born in Germany .";
		String nerTagged	= "Has_O a_O of_O Josephine_B-PER of_O Daughter_I-PER ,_O who_O was_O born_O in_O Germany_LOC ._O";
		String pattern		= ", who was born in";
		
		FastLeftContext flc = new FastLeftContext(nerTagged, sentence, pattern);
		
		assertEquals(10, flc.getCleanWords().size());
		assertEquals("born", flc.getCleanWords().get(flc.getCleanWords().size() -1 ));
		assertEquals("born_O", flc.getTaggedWords().get(flc.getTaggedWords().size() -1 ));
		assertEquals("Daughter", flc.getSuitableEntity("http://dbpedia.org/ontology/Person"));
		assertEquals(0, flc.getSuitableEntityDistance("http://dbpedia.org/ontology/Person"));
		
		FastRightContext frc = new FastRightContext(nerTagged, sentence, pattern);
		
		assertEquals(6, frc.getCleanWords().size());
		assertEquals("who", frc.getCleanWords().get(0) );
		assertEquals("who_O", frc.getTaggedWords().get(0));
		assertEquals("Germany", frc.getSuitableEntity("http://dbpedia.org/ontology/Country"));
		assertEquals(0, frc.getSuitableEntityDistance("http://dbpedia.org/ontology/Country"));
	}
	
	@Test
	public void testFastLeftContext5() {
	
		String sentence		= "The Chernaya River is on the outskirts of Sevastopol .";
		String nerTagged	= "The_O Chernaya_B-LOC River_I-LOC is_O on_O the_O outskirts_O of_O Sevastopol_B-LOC ._O";
		String pattern		= "is on the outskirts of";
		
		FastLeftContext flc = new FastLeftContext(nerTagged, sentence, pattern);
		
		assertEquals(7, flc.getCleanWords().size());
		assertEquals("outskirts", flc.getCleanWords().get(flc.getCleanWords().size() -1 ));
		assertEquals("outskirts_O", flc.getTaggedWords().get(flc.getTaggedWords().size() -1 ));
		assertEquals("Chernaya River", flc.getSuitableEntity("http://dbpedia.org/ontology/Place"));
		assertEquals(0, flc.getSuitableEntityDistance("http://dbpedia.org/ontology/Place"));
		
		FastRightContext frc = new FastRightContext(nerTagged, sentence, pattern);
		
		assertEquals(6, frc.getCleanWords().size());
		assertEquals("on", frc.getCleanWords().get(0) );
		assertEquals("on_O", frc.getTaggedWords().get(0));
		assertEquals("Sevastopol", frc.getSuitableEntity("http://dbpedia.org/ontology/Place"));
		assertEquals(0, frc.getSuitableEntityDistance("http://dbpedia.org/ontology/Place"));
	}
	
	@Test
	public void testFastLeftContext6() {
	
		String sentence		= "At stake were the 450 seats in the State Duma -LRB- Gosudarstvennaya Duma -RRB- , the lower house of the Federal Assembly of Russia -LRB- The legislature -RRB- .";
		String nerTagged	= "At_O stake_O were_O the_O 450_O seats_O in_O the_O State_B-ORG Duma_I-ORG -LRB-_O Gosudarstvennaya_B-PER Duma_I-PER -RRB-_O ,_O the_O lower_O house_O of_O the_O Federal_B-ORG Assembly_I-ORG of_O Russia_B-LOC -LRB-_O The_O legislature_O -RRB-_O ._O";
		String pattern		= "-LRB- Gosudarstvennaya Duma -RRB- , the lower house of the";
		
		FastLeftContext flc = new FastLeftContext(nerTagged, sentence, pattern);
		
		assertEquals(19, flc.getCleanWords().size());
		assertEquals("of", flc.getCleanWords().get(flc.getCleanWords().size() -1 ));
		assertEquals("of_O", flc.getTaggedWords().get(flc.getTaggedWords().size() -1 ));
		assertEquals("State Duma", flc.getSuitableEntity("http://dbpedia.org/ontology/Organisation"));
		assertEquals(0, flc.getSuitableEntityDistance("http://dbpedia.org/ontology/Organisation"));
		
		FastRightContext frc = new FastRightContext(nerTagged, sentence, pattern);
		
		assertEquals(18, frc.getCleanWords().size());
		assertEquals("Gosudarstvennaya", frc.getCleanWords().get(0) );
		assertEquals("Gosudarstvennaya_B-PER", frc.getTaggedWords().get(0));
		assertEquals("Federal Assembly", frc.getSuitableEntity("http://dbpedia.org/ontology/Organisation"));
		assertEquals(0, frc.getSuitableEntityDistance("http://dbpedia.org/ontology/Organisation"));
	}
	
	@Test
	public void testFastLeftContext7() { // TODO
	
		String sentence		= "Vygotsky was born in Orsha , in the Russian Empire -LRB- today in Belarus -RRB- into a nonreligious Jewish family .";
		String nerTagged	= "Vygotsky_B-PER was_O born_O in_O Orsha_B-LOC ,_O in_O the_O Russian_B-MISC Empire_I-MISC -LRB-_O today_O in_O Belarus_B-LOC -RRB-_O into_O a_O nonreligious_O Jewish_B-MISC family_O ._O";
		String pattern		= "in the Russian Empire -LRB- today in";
		
		FastLeftContext flc = new FastLeftContext(nerTagged, sentence, pattern);
		
		assertEquals(12, flc.getCleanWords().size());
		assertEquals("today", flc.getCleanWords().get(flc.getCleanWords().size() -1 ));
		assertEquals("today_O", flc.getTaggedWords().get(flc.getTaggedWords().size() -1 ));
		assertEquals("Orsha", flc.getSuitableEntity("http://dbpedia.org/ontology/Place"));
		assertEquals(1, flc.getSuitableEntityDistance("http://dbpedia.org/ontology/Place"));
		
		FastRightContext frc = new FastRightContext(nerTagged, sentence, pattern);
		
		assertEquals(14, frc.getCleanWords().size());
		assertEquals("the", frc.getCleanWords().get(0) );
		assertEquals("the_O", frc.getTaggedWords().get(0));
		assertEquals("Belarus", frc.getSuitableEntity("http://dbpedia.org/ontology/Place"));
		assertEquals(0, frc.getSuitableEntityDistance("http://dbpedia.org/ontology/Place"));
	}
	
	@Test
	public void testTimeForContextCreation() {
		
		long total = 0;
		long iterations = 10000;
		
		for ( int i = 0 ; i < iterations ; i++ ) {

			long start = System.currentTimeMillis();
			
			String sentence		= "Vygotsky was born in Orsha , in the Russian Empire -LRB- today in Belarus -RRB- into a nonreligious Jewish family .";
			String nerTagged	= "Vygotsky_B-PER was_O born_O in_O Orsha_B-LOC ,_O in_O the_O Russian_B-MISC Empire_I-MISC -LRB-_O today_O in_O Belarus_B-LOC -RRB-_O into_O a_O nonreligious_O Jewish_B-MISC family_O ._O";
			String pattern		= "in the Russian Empire -LRB- today in";
			
			FastLeftContext flc = new FastLeftContext(nerTagged, sentence, pattern);
			
			assertEquals(12, flc.getCleanWords().size());
			assertEquals("today", flc.getCleanWords().get(flc.getCleanWords().size() -1 ));
			assertEquals("today_O", flc.getTaggedWords().get(flc.getTaggedWords().size() -1 ));
			assertEquals("Orsha", flc.getSuitableEntity("http://dbpedia.org/ontology/Place"));
			assertEquals(1, flc.getSuitableEntityDistance("http://dbpedia.org/ontology/Place"));
			
			FastRightContext frc = new FastRightContext(nerTagged, sentence, pattern);
			
			assertEquals(14, frc.getCleanWords().size());
			assertEquals("the", frc.getCleanWords().get(0) );
			assertEquals("the_O", frc.getTaggedWords().get(0));
			assertEquals("Belarus", frc.getSuitableEntity("http://dbpedia.org/ontology/Place"));
			assertEquals(0, frc.getSuitableEntityDistance("http://dbpedia.org/ontology/Place"));
			
			long end = System.currentTimeMillis();
			
			total += (end - start);
		}
		assertTrue("One sentence in less then 0.5ms!", (double) 0.5 > (double) total / (double) iterations);
		assertTrue(iterations + " sentence in less then " + iterations + "ms!", (double) iterations > (double) total);
	}
}
