package de.uni_leipzig.simba.boa.backend.dao.pattern;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import de.uni_leipzig.simba.boa.backend.dao.AbstractDao;
import de.uni_leipzig.simba.boa.backend.entity.pattern.Pattern;
import de.uni_leipzig.simba.boa.backend.entity.pattern.PatternMapping;
import de.uni_leipzig.simba.boa.backend.persistance.Entity;
import de.uni_leipzig.simba.boa.backend.persistance.hibernate.HibernateFactory;
import de.uni_leipzig.simba.boa.backend.rdf.entity.Property;
import de.uni_leipzig.simba.boa.backend.rdf.entity.Resource;
import de.uni_leipzig.simba.boa.backend.rdf.entity.Triple;


/**
 * 
 * @author Daniel Gerber
 */
public class PatternMappingDao extends AbstractDao {

	public PatternMappingDao() {
        super();
    }

	/**
	 * 
	 * @param patternMapping
	 */
    public PatternMapping createAndSavePatternMapping(PatternMapping patternMapping) {
    	
        return (PatternMapping) super.saveOrUpdateEntity(patternMapping);
    }
    
    /**
     * 
     * @return
     */
    public PatternMapping createNewEntity(Entity p) {
    	
    	if ( p instanceof Property ) {
    		
    		return (PatternMapping) super.saveOrUpdateEntity(new PatternMapping((Property) p));
    	}
        throw new RuntimeException("Parameter passed to this method was not a Property but a: " + p.getClass() );
    }

    /**
     * 
     * @param patternMapping
     */
    public void deletePattern(PatternMapping patternMapping) {
    	
        super.deleteEntity(patternMapping);
    }

    /**
     * 
     * @param id
     * @return
     */
    public PatternMapping findPatternMapping(int id) {
    	
        return (PatternMapping) super.findEntityById(PatternMapping.class, id);
    }
    
    public PatternMapping findPatternMappingWithoutZeroConfidencePattern(PatternMapping pm) {
    	
    	Transaction tx = null;
		Session session = null;

		try {
			
			session = HibernateFactory.getSessionFactory().openSession();
			tx = session.beginTransaction();
			
			String queryString = "select p.id, p.naturalLanguageRepresentation, " +
									"p.confidence, p.globalConfidence, p.numberOfOccurrences, p.useForPatternEvaluation, p.luceneDocIds, " +
									"p.specificity, p.typicity, p.support " + 
								 "from pattern_mapping as pm, pattern as p, resource as r " +
								 "where r.uri='"+pm.getProperty().getUri().trim()+"' and pm.property_id = r.id and r.DTYPE = 'PROPERTY' and pm.id = p.patternMapping_id and (p.specificity > 0 or p.support > 0 or p.typicity > 0) " +
								 "order by r.uri;"; 
			
			Query query = session.createSQLQuery(queryString);
			
			List<Object[]> objs = query.list();
			for (Object[] obj : objs) {
				
				Pattern pattern = new Pattern();
				pattern.setId((Integer) obj[0]);
				pattern.setNaturalLanguageRepresentation((String) obj[1]);
				pattern.setConfidenceForIteration(1, ((Double) obj[2]));
				pattern.setGlobalConfidence((Double) obj[3]);
				pattern.setNumberOfOccurrences((Integer) obj[4]);
				pattern.setUseForPatternEvaluation((Boolean) obj[5]);
				pattern.setLuceneDocIds((String) obj[6]);
				pattern.setSpecificityForIteration(1, (Double) obj[7]);
				pattern.setTypicityForIteration(1, (Double) obj[8]);
				pattern.setSupportForIteration(1 , (Double) obj[9]);
				
				pm.addPattern(pattern);
			}
			tx.commit();
		}
		catch (HibernateException he) {
			
			HibernateFactory.rollback(tx);
			super.logger.error("Error...", he);
			he.printStackTrace();
		}
		finally {
			
			HibernateFactory.closeSession(session);
		}
		return pm;
    }

    /**
     * 
     * @param patternMapping
     */
    public void updatePatternMapping(PatternMapping patternMapping) {
    	
        super.saveOrUpdateEntity(patternMapping);
    }

    /**
     * 
     * @return
     */
    public List<PatternMapping> findAllPatternMappings() {
    	
        return (List<PatternMapping>) super.findAllEntitiesByClass(PatternMapping.class);
    }

	/**
	 * 
	 * @return
	 */
	public List<PatternMapping> findPatternMappingsWithoutPattern(String uri) {

		List<PatternMapping> objects = new ArrayList<PatternMapping>();
		
		Transaction tx = null;
		Session session = null;
		
		try {
			
			session = HibernateFactory.getSessionFactory().openSession();
			tx = session.beginTransaction();
			
			String queryString = (uri == null) 
				? 		"select distinct(pm.id), r.uri, r.rdfsRange, r.rdfsDomain " +
						"from pattern_mapping as pm, pattern as p, resource as r " +
						"where pm.id = p.patternMapping_id and (p.specificity > 0 or p.support > 0 or p.typicity > 0) and pm.property_id = r.id and r.DTYPE = 'PROPERTY' " +
						"order by r.uri;"
				: "select pm.id, r.uri, r.rdfsRange, r.rdfsDomain from pattern_mapping as pm, resource as r where pm.property_id = r.id and r.DTYPE = 'PROPERTY' and r.uri='"+uri+"';"; 
			
			
			Query query = session.createSQLQuery(queryString);
			
			List<Object[]> objs = query.list();
			for (Object[] obj : objs) {
				
				PatternMapping pm = new PatternMapping((String) obj[1], "", (String) obj[3], (String) obj[2]);
				pm.setId((Integer) obj[0]);
				objects.add(pm);
			}
			
			tx.commit();
		}
		catch (HibernateException he) {
			
			HibernateFactory.rollback(tx);
			super.logger.error("Error...", he);
			he.printStackTrace();
		}
		finally {
			
			HibernateFactory.closeSession(session);
		}
		return objects;
	}

	@Override
	public Entity createNewEntity() {

		throw new RuntimeException("Do not use this method!");
	}

	public void deleteAllPatternMappings() {

		Transaction tx = null;
		Session session = null;
		
		try {
			
			session = HibernateFactory.getSessionFactory().openSession();
			tx = session.beginTransaction();
			
			String queryString = "delete from pattern_mapping;";			
			Query query = session.createSQLQuery(queryString);
			query.executeUpdate();
			
			queryString = "delete from pattern;";
			query = session.createSQLQuery(queryString);
			query.executeUpdate();
			
			queryString = "delete from pattern_learned_from;";
			query = session.createSQLQuery(queryString);
			query.executeUpdate();
			
			tx.commit();
		}
		catch (HibernateException he) {
			
			HibernateFactory.rollback(tx);
			super.logger.error("Error...", he);
			he.printStackTrace();
		}
		finally {
			
			HibernateFactory.closeSession(session);
		}// TODO Auto-generated method stub
		
	}

	public List<String> findPatternMappingsWithPatterns() {

		Session session = HibernateFactory.getSessionFactory().openSession();
    	String queryString = "select distinct(prop.uri) from pattern_mapping as pm, resource as prop ,pattern_mapping_pattern as pmp, pattern as p  where pm.id = pmp.pattern_mapping_id and pmp.pattern_id = p.id and pm.property_id = prop.id and p.confidence > 0 and p.numberOfOccurrences > 10;";
        List<String> results = (List<String>) session.createSQLQuery(queryString).list();
        HibernateFactory.closeSession(session);
        return results;
	}
	
	public PatternMapping findPatternMappingByUri(String uri) {

		Session session = HibernateFactory.getSessionFactory().openSession();
    	List<PatternMapping> resourceList = session.createCriteria(PatternMapping.class)
    							.createCriteria("property").add(Restrictions.eq("uri", uri))
    							.list();
    	
    	HibernateFactory.closeSession(session);
    	
        return this.findPatternMapping(resourceList.get(0).getId());
	}
}
