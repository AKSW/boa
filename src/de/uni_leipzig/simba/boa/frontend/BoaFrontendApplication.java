package de.uni_leipzig.simba.boa.frontend;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.lucene.queryParser.ParseException;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QueryParseException;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.ResultSetFormatter;
import com.hp.hpl.jena.query.Syntax;
import com.vaadin.Application;
import com.vaadin.data.Property;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.event.Action;
import com.vaadin.event.FieldEvents.TextChangeEvent;
import com.vaadin.event.FieldEvents.TextChangeListener;
import com.vaadin.event.ItemClickEvent;
import com.vaadin.event.ItemClickEvent.ItemClickListener;
import com.vaadin.terminal.ExternalResource;
import com.vaadin.terminal.ThemeResource;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Form;
import com.vaadin.ui.GridLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.HorizontalSplitPanel;
import com.vaadin.ui.Label;
import com.vaadin.ui.Layout;
import com.vaadin.ui.Link;
import com.vaadin.ui.Panel;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.Tree.ExpandEvent;
import com.vaadin.ui.Tree.ExpandListener;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.VerticalSplitPanel;
import com.vaadin.ui.Window;

import de.uni_leipzig.simba.boa.backend.configuration.NLPediaSettings;
import de.uni_leipzig.simba.boa.backend.configuration.NLPediaSetup;
import de.uni_leipzig.simba.boa.backend.dao.DaoFactory;
import de.uni_leipzig.simba.boa.backend.dao.pattern.PatternMappingDao;
import de.uni_leipzig.simba.boa.backend.entity.pattern.Pattern;
import de.uni_leipzig.simba.boa.backend.entity.pattern.PatternMapping;
import de.uni_leipzig.simba.boa.backend.logging.NLPediaLogger;
import de.uni_leipzig.simba.boa.backend.persistance.hibernate.HibernateFactory;
import de.uni_leipzig.simba.boa.backend.rdf.Model;
import de.uni_leipzig.simba.boa.backend.rdf.store.Store;
import de.uni_leipzig.simba.boa.backend.search.PatternSearcher;
import de.uni_leipzig.simba.boa.frontend.data.PatternContainer;
import de.uni_leipzig.simba.boa.frontend.ui.PatternTable;
import de.uni_leipzig.simba.boa.frontend.ui.DatabaseNavigationTree;
import de.uni_leipzig.simba.boa.frontend.ui.RdfModelTree;
import de.uni_leipzig.simba.boa.frontend.ui.StatementForm;

@SuppressWarnings("serial")
public class BoaFrontendApplication extends Application implements ItemClickListener, Action.Handler, Button.ClickListener, TextChangeListener {

	private NLPediaSetup setup = new NLPediaSetup(false);
	private NLPediaLogger logger = new NLPediaLogger(BoaFrontendApplication.class);

	private Button evaluationButton = new Button("Evaluation");
	private Button databasesButton = new Button("Corpora");
	private Button sparqlButton = new Button("SPARQL");
	private Button startQuery = new Button("Query"); 
	
	private DatabaseNavigationTree tree;
	private RdfModelTree rdfTree;
	private RdfModelTree rdfSparqlTree;
	private PatternTable patternTable;
	
	private TextArea sparqlQueryString;
	private String sparqlQueryModel = "";
	
	private HorizontalSplitPanel horizontalSplitPanel = new HorizontalSplitPanel();
	private String currentDatabase;

	@Override
	public void init() {
		
		buildMainLayout();
	}
	
	@Override
	public Action[] getActions(Object target, Object sender) {

		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void handleAction(Action action, Object sender, Object target) {

		// TODO Auto-generated method stub
		
	}
	
	public void buttonClick(ClickEvent event) {
		
		final Button source = event.getButton();
		
		if (source == this.evaluationButton) {
			
			Panel p = new Panel();
			// setze linken teil auf die modelle
			horizontalSplitPanel.setFirstComponent(rdfTree);
			// in den rechten teil kommt die erkl�rung
			horizontalSplitPanel.setSecondComponent(p);
			horizontalSplitPanel.setSplitPosition(400, HorizontalSplitPanel.UNITS_PIXELS);
		}
		else if ( source == this.databasesButton ) {
			
			Panel p = new Panel();
			p.addComponent(new Label("Hier kann man sich die Patterns anschauen"));
			// setze linken teil auf die modelle
			horizontalSplitPanel.setFirstComponent(tree);			
			// in denrechten teil kommt die erkl�rung
			horizontalSplitPanel.setSecondComponent(p);
			horizontalSplitPanel.setSplitPosition(400, HorizontalSplitPanel.UNITS_PIXELS);
		}
		else if ( source == this.sparqlButton ) {
			
			Panel p = new Panel();
			p.addComponent(new Label("Hier kann man SPARQL Queries starten!"));
			// setze linken teil auf die modelle
			horizontalSplitPanel.setFirstComponent(rdfSparqlTree);			
			// in denrechten teil kommt die erkl�rung
			horizontalSplitPanel.setSecondComponent(p);
			horizontalSplitPanel.setSplitPosition(130, HorizontalSplitPanel.UNITS_PIXELS);
		}
		else if ( source == this.startQuery ) {
			
			String queryString = this.sparqlQueryString.getValue().toString();
			
			try {
				
				Query query = QueryFactory.create(queryString, Syntax.syntaxARQ);
				Store store = new Store();
				Model model = store.getModel(this.sparqlQueryModel);
				QueryExecution qexec = QueryExecutionFactory.create(query, model.getModel());
				String solution = ResultSetFormatter.asText(qexec.execSelect());
				qexec.close() ;
				
//				solution = solution.replaceAll("http://dbpedia.org/resource/", "dbr:");
//				solution = solution.replaceAll("http://nlpedia.de/", "boa:");
//				solution = solution.replaceAll("http://dbpedia.org/ontology/", "dbo");
//				solution = solution.replaceAll(">", "");
//				solution = solution.replaceAll("<", "");
				
				Layout p = new HorizontalLayout();
//				Panel p = new Panel();
				Label output = new Label(solution);
				output.setContentMode(Label.CONTENT_PREFORMATTED);
				p.addComponent(output);
				p.setMargin(true);
				p.setSizeFull();
				
				((VerticalSplitPanel) this.horizontalSplitPanel.getSecondComponent()).setSecondComponent(output);
			}
			catch (QueryParseException qpe) {
			
				this.getMainWindow().showNotification(qpe.getLocalizedMessage());
			}
		}
	}
	
	public void itemClick(ItemClickEvent event) {
		
		if (event.getSource() == tree) {
			
			String itemId = (String) event.getItemId();
			
			if (itemId != null) {
				
				this.currentDatabase 	= itemId.substring(0, itemId.indexOf(":"));
				String uri				= itemId.substring(itemId.indexOf(":") + 1);
				
				HibernateFactory.changeConnection(this.currentDatabase);
				
				PatternMappingDao pmDao = (PatternMappingDao) DaoFactory.getInstance().createDAO(PatternMappingDao.class);
				PatternMapping pm = pmDao.findPatternMappingsWithoutPattern(uri).get(0);
				pm = pmDao.findPatternMappingWithoutZeroConfidencePattern(pm);
				
				GridLayout gridLayout = new GridLayout(5,6);
				gridLayout.setSpacing(true);
				gridLayout.setMargin(false);
				gridLayout.setSizeFull();
				
				gridLayout.addComponent(new Label(""), 0, 0);
				
				Label rdfsDomainLabel = new Label("rdfs:domain (?D?)");
				gridLayout.addComponent(rdfsDomainLabel, 1, 0);
				gridLayout.setComponentAlignment(rdfsDomainLabel, Alignment.MIDDLE_LEFT);
				
				Link rdfsDomainLink = new Link(pm.getRdfsDomain(), new ExternalResource(pm.getRdfsDomain()));
				gridLayout.addComponent(rdfsDomainLink, 2, 0);
				gridLayout.setComponentAlignment(rdfsDomainLink, Alignment.MIDDLE_LEFT);
				
				Label rdfsRangeLabel = new Label("rdfs:range (?R?)");
				gridLayout.addComponent(rdfsRangeLabel, 3, 0);
				gridLayout.setComponentAlignment(rdfsRangeLabel, Alignment.MIDDLE_RIGHT);
				
				Link rdfsRangeLink = new Link(pm.getRdfsRange(), new ExternalResource(pm.getRdfsRange()));
				gridLayout.addComponent(rdfsRangeLink, 4, 0);
				gridLayout.setComponentAlignment(rdfsRangeLink, Alignment.MIDDLE_LEFT);
				
				try {
					
					this.patternTable = new PatternTable(this, new PatternContainer(pm));
					gridLayout.addComponent(patternTable, 0, 1, 4, 5);
				}
				catch (InstantiationException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				catch (IllegalAccessException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				this.horizontalSplitPanel.setSecondComponent(gridLayout);
			}
		}
		else if (event.getSource() == rdfSparqlTree) {
			
			this.sparqlQueryModel = (String) event.getItemId();
			
			VerticalSplitPanel verticalSplitPanel = new VerticalSplitPanel();

			VerticalLayout hLayout = new VerticalLayout();
			
			this.sparqlQueryString = new TextArea("SPARQL-Querie", "SELECT * \nWHERE {\n\t ?s ?p ?o .\n}");
	        sparqlQueryString.setRows(10);
	        sparqlQueryString.setColumns(50);
	        sparqlQueryString.addListener((TextChangeListener) this);
	        sparqlQueryString.setImmediate(true);
	        
	        startQuery.addListener(this); // react to clicks
	        
	        hLayout.addComponent(sparqlQueryString);
	        hLayout.addComponent(startQuery);
	        hLayout.setSpacing(true);
	        hLayout.setMargin(true);
	        
	        verticalSplitPanel.addComponent(hLayout);
	        verticalSplitPanel.setSplitPosition(230, VerticalSplitPanel.UNITS_PIXELS);
	        verticalSplitPanel.setMargin(true);
	        verticalSplitPanel.setSizeFull();
	        
	        this.horizontalSplitPanel.setSecondComponent(verticalSplitPanel);
		}
		else if (event.getSource() == rdfTree) {
			
			String itemId = (String) event.getItemId();
			
			if (itemId != null) {
				
				Panel p = new Panel();
				p.addComponent(new StatementForm(this, itemId));
				this.horizontalSplitPanel.setSecondComponent(p);
			}
		}
		else if (event.getSource() == this.patternTable ) {
			
			Pattern pattern = (Pattern) event.getItemId();
			
			try {
				
				String naturalLanguageRepresentation = pattern.getNaturalLanguageRepresentation().substring(0, pattern.getNaturalLanguageRepresentation().length() - 3).substring(3).trim();
				
				// something like en_wiki or en_news
				String temp = currentDatabase.substring(0,currentDatabase.lastIndexOf("_"));
				String indexDir = NLPediaSettings.getInstance().getSetting("sentenceIndexDirectory");
				// this only works only if the both indexes are accessable via the same path TODO
				if ( !indexDir.contains(temp) ) {
					
					if ( indexDir.contains("en_news") ) indexDir = indexDir.replace("en_news", "en_wiki");
					else if ( indexDir.contains("en_wiki") ) indexDir = indexDir.replace("en_wiki", "en_news");
				}
				
				PatternSearcher patternSearcher = new PatternSearcher(indexDir);
				TreeSet<String> results = (TreeSet<String>) patternSearcher.getExactMatchSentences(naturalLanguageRepresentation, 1000);
				
				StringBuilder builder = new StringBuilder();
				builder.append("<h2>Search for label \""+ naturalLanguageRepresentation+"\" in the index returned " + results.size() + " results.</h2>");
				
				Iterator<String> iter = results.iterator(); 
				int i = 0;
				while ( iter.hasNext() && i++ < 100) {

					String sentence = iter.next();
					sentence = sentence.replaceFirst(naturalLanguageRepresentation, "<span style=\"color: red;\">"+naturalLanguageRepresentation+"</span>");
					builder.append(i + ". " + sentence + "<br/>");
					if (iter.hasNext()) builder.append("<hr/>");
				}
				
				builder.append("<h2>Pattern learned from:</h2>");
				iter = patternSearcher.getSentences(new ArrayList<Integer>(pattern.retrieveLuceneDocIdsAsList())).iterator();
				i = 1;
				while (iter.hasNext()) {
					
					String sentence = iter.next();
					sentence = sentence.replaceFirst(naturalLanguageRepresentation, "<span style=\"color: red;\">"+naturalLanguageRepresentation+"</span>");
					builder.append(i++ + ". " + sentence + "<br/>");
					if (iter.hasNext()) builder.append("<hr/>");
				}
				
				Window subwindow = new Window("Details for pattern: \""+naturalLanguageRepresentation+"\"");
		        subwindow.setModal(true);
		        VerticalLayout layout = (VerticalLayout) subwindow.getContent();
		        layout.setMargin(true);
		        layout.setSpacing(true);
		        Label content = new Label(builder.toString(), Label.CONTENT_XHTML);
		        subwindow.addComponent(content);
		        subwindow.setWidth("1200px");
		        subwindow.setResizable(false);
		        this.getMainWindow().addWindow(subwindow);
			}
			catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	private void buildMainLayout() {
		
		this.setMainWindow(new Window("Boa Frontend"));
		this.setTheme("boa");
		
		this.tree = new DatabaseNavigationTree(this);
		this.rdfTree = new RdfModelTree(this);
		this.rdfSparqlTree = new RdfModelTree(this);
		
		VerticalLayout layout = new VerticalLayout();
		layout.setSizeFull();
		layout.addComponent(this.createToolbar());
		layout.addComponent(horizontalSplitPanel);
		layout.setExpandRatio(horizontalSplitPanel, 1);
		
		horizontalSplitPanel.setSplitPosition(400, HorizontalSplitPanel.UNITS_PIXELS);
		horizontalSplitPanel.setFirstComponent(tree);
		
		this.getMainWindow().setContent(layout);
	}
	
	private GridLayout createToolbar() {
		
		GridLayout lo = new GridLayout(30,1);
		lo.addComponent(databasesButton,0,0);
		lo.addComponent(evaluationButton,1,0);
		lo.addComponent(sparqlButton,2,0);
		
		evaluationButton.addListener((ClickListener) this);
		databasesButton.addListener((ClickListener) this);
		sparqlButton.addListener((ClickListener) this);
		
		evaluationButton.setIcon(new ThemeResource("icons/32/folder-add.png"));
		databasesButton.setIcon(new ThemeResource("icons/32/users.png"));
		sparqlButton.setIcon(new ThemeResource("icons/32/document-edit.png"));
		
		lo.setMargin(true);
		lo.setSpacing(false);
		lo.setStyleName("toolbar");
		lo.setWidth("100%");
		
		this.horizontalSplitPanel.setSizeFull();

//		Embedded em = new Embedded("", new ThemeResource("images/logo.png"));
//		lo.addComponent(em);
//		lo.setComponentAlignment(em, Alignment.MIDDLE_RIGHT);
//		lo.setExpandRatio(em, 1);

		return lo;
	}

	@Override
	public void textChange(TextChangeEvent event) {

		// TODO Auto-generated method stub
		
	}

}
