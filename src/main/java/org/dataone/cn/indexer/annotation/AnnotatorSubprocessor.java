package org.dataone.cn.indexer.annotation;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.xml.xpath.XPathExpressionException;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;

import org.apache.commons.codec.EncoderException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dataone.cn.indexer.parser.IDocumentSubprocessor;
import org.dataone.cn.indexer.parser.ISolrDataField;
import org.dataone.cn.indexer.solrhttp.HTTPService;
import org.dataone.cn.indexer.solrhttp.SolrDoc;
import org.dataone.cn.indexer.solrhttp.SolrElementField;
import org.springframework.beans.factory.annotation.Autowired;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.tdb.TDBFactory;

/**
 * The intent of this subprocessor is to fetch annotations about the given 
 * documents as housed in an external store and add them to the solr index.
 * As of November 2014, this is still experimental for the semantic goals of 
 * the project.
 * @author leinfelder
 *
 */
public class AnnotatorSubprocessor implements IDocumentSubprocessor {

    public static final String FIELD_ANNOTATION = "annotation_sm";
    public static final String FIELD_ANNOTATES = "annotates_sm";
    public static final String FIELD_ANNOTATED_BY = "annotated_by_sm";
    public static final String FIELD_COMMENT = "comment_sm";

    private static Log log = LogFactory.getLog(AnnotatorSubprocessor.class);
    
    @Autowired
    private HTTPService httpService = null;

    @Autowired
    private String solrQueryUri = null;

    private List<String> matchDocuments = null;
    
    private List<String> fieldsToMerge = new ArrayList<String>();

    private List<ISolrDataField> fieldList = new ArrayList<ISolrDataField>();

    public HTTPService getHttpService() {
        return httpService;
    }

    public void setHttpService(HTTPService httpService) {
        this.httpService = httpService;
    }

    public String getSolrQueryUri() {
        return solrQueryUri;
    }

    public void setSolrQueryUri(String solrQueryUri) {
        this.solrQueryUri = solrQueryUri;
    }

    public List<String> getMatchDocuments() {
        return matchDocuments;
    }

    public void setMatchDocuments(List<String> matchDocuments) {
        this.matchDocuments = matchDocuments;
    }

    public List<String> getFieldsToMerge() {
        return fieldsToMerge;
    }

    public void setFieldsToMerge(List<String> fieldsToMerge) {
        this.fieldsToMerge = fieldsToMerge;
    }

    public List<ISolrDataField> getFieldList() {
		return fieldList;
	}

	public void setFieldList(List<ISolrDataField> fieldList) {
		this.fieldList = fieldList;
	}

	/**
     * Returns true if subprocessor should be run against object
     * 
     * @param formatId the the document to be processed
     * @return true if this processor can parse the formatId
     */
    public boolean canProcess(String formatId) {
        return matchDocuments.contains(formatId);
    }

    @Override
    public Map<String, SolrDoc> processDocument(String annotationId, Map<String, SolrDoc> docs,
            InputStream is) throws Exception {

        // check for annotations, and add them if found
        SolrDoc annotations = parseAnnotation(is);
        if (annotations != null) {
            String referencedPid = annotations.getIdentifier();
            SolrDoc referencedDoc = docs.get(referencedPid);

            // make sure we have a reference for the document we annotating
            if (referencedDoc == null) {
                referencedDoc = new SolrDoc();
                docs.put(referencedPid, referencedDoc);
            }

            // make sure we say we annotate the object
            SolrDoc annotationDoc = docs.get(annotationId);
            if (annotationDoc != null) {
                annotationDoc.addField(new SolrElementField(FIELD_ANNOTATES, referencedPid));
            }

            // add the annotations to the referenced document
            Iterator<SolrElementField> annotationIter = annotations.getFieldList().iterator();
            while (annotationIter.hasNext()) {
                SolrElementField annotation = annotationIter.next();
                referencedDoc.addField(annotation);
            }
        }

        // return the collection that we have augmented
        return docs;
    }

    /**
     * Parse the annotation for fields
     * @see "http://docs.annotatorjs.org/en/latest/storage.html"
     * @param the stream of the [JSON] annotation
     * @return
     */
    private SolrDoc parseAnnotation(InputStream is) {

        try {

            String results = IOUtils.toString(is, "UTF-8");
            log.debug("RESULTS: " + results);
            JSONObject annotation = (JSONObject) JSONValue.parse(results);

            SolrDoc annotations = new SolrDoc();

            // use catch-all annotation field for the tags
            String tagKey = FIELD_ANNOTATION;

            // make sure we know which pid we are talking about
            String pidValue = annotation.get("pid").toString();
            if (!annotations.hasFieldWithValue(SolrElementField.FIELD_ID, pidValue)) {
                annotations.addField(new SolrElementField(SolrElementField.FIELD_ID, pidValue));
            }

            // and which object is doing the annotating
            String idValue = annotation.get("id").toString();
            if (!annotations.hasFieldWithValue(FIELD_ANNOTATED_BY, idValue)) {
                annotations.addField(new SolrElementField(FIELD_ANNOTATED_BY, idValue));
            }

            // do not index rejected annotations (clear them out)
            Object reject = annotation.get("reject");
            if (reject != null && Boolean.parseBoolean(reject.toString())) {

                // include empty index values to force removal
                if (!annotations.hasFieldWithValue(tagKey, "")) {
                    annotations.addField(new SolrElementField(tagKey, ""));
                }
                if (!annotations.hasFieldWithValue(FIELD_COMMENT, "")) {
                    annotations.addField(new SolrElementField(FIELD_COMMENT, ""));
                }

            } else {

                // index the (semantic) tags
                // if the annotation told us the target index field, then use it
                Object field = annotation.get("field");
                if (field != null) {
                    tagKey = field.toString();
                }

                Object obj = annotation.get("tags");
                if (obj instanceof JSONArray) {
                    JSONArray tags = (JSONArray) obj;
                    for (Object tag : tags) {
                        String value = tag.toString();
                        if (!annotations.hasFieldWithValue(tagKey, value)) {
                            annotations.addField(new SolrElementField(tagKey, value));
                        }
                    }
                } else {
                    String value = obj.toString();
                    if (!annotations.hasFieldWithValue(tagKey, value)) {
                        annotations.addField(new SolrElementField(tagKey, value));
                    }
                }

                // index the comments
                Object commentObj = annotation.get("text");
                if (commentObj != null) {
                    String value = commentObj.toString();
                    if (value != null && value.length() > 0) {
                        if (!annotations.hasFieldWithValue(FIELD_COMMENT, value)) {
                            annotations.addField(new SolrElementField(FIELD_COMMENT, value));
                        }
                    }
                }

                // expand the tags, adding expanded concepts to the existing fields
                for (String tag : annotations.getAllFieldValues(tagKey)) {
                    try {
                        // get the expanded tags
                        Map<String, Set<String>> expandedConcepts = this.expandConcepts(tag);
                        for (Map.Entry<String, Set<String>> entry : expandedConcepts.entrySet()) {
                            for (String value : entry.getValue()) {
                                String name = entry.getKey();
                                if (!annotations.hasFieldWithValue(name, value)) {
                                    annotations.addField(new SolrElementField(name, value));
                                }
                            }
                        }
                    } catch (Exception e) {
                        log.error("Problem exapnding concept: " + tag, e);
                    }
                }

            }

            // return them
            return annotations;

        } catch (Exception e) {
            log.error("Problem parsing annotation: " + e.getMessage(), e);
        }

        return null;
    }

    protected Map<String, Set<String>> expandConcepts(String uri) throws Exception {

        // return structure allows multi-valued fields
        Map<String, Set<String>> conceptFields = new HashMap<String, Set<String>>();

        if (uri == null || uri.length() < 1) {
            return conceptFields;
        }

        // get the triples tore dataset
        Dataset dataset = TripleStoreService.getInstance().getDataset();

        // load the ontology
        boolean loaded = dataset.containsNamedModel(uri);
        if (!loaded) {
            OntModel ontModel = ModelFactory.createOntologyModel();
            //InputStream sourceStream = new URI(uri).toURL().openStream();
            // TODO: look up physical source from bioportal
            ontModel.read(uri);
            dataset.addNamedModel(uri, ontModel);
        }

        // process each field query
        for (ISolrDataField field : fieldList) {
            String q = null;
            if (field instanceof SparqlField) {
                q = ((SparqlField) field).getQuery();
                q = q.replaceAll("\\$CONCEPT_URI", uri);
                q = q.replaceAll("\\$GRAPH_NAME", uri);
                Query query = QueryFactory.create(q);
                QueryExecution qexec = QueryExecutionFactory.create(query, dataset);
                ResultSet results = qexec.execSelect();

                // each field might have multiple solution values
                String name = field.getName();
                Set<String> values = new TreeSet<String>();

                while (results.hasNext()) {

                    QuerySolution solution = results.next();
                    log.debug(solution.toString());

                    // the value[s] for that field
                    if (solution.contains(name)) {
                        String value = solution.get(field.getName()).toString();
                        values.add(value);
                    }
                }
                conceptFields.put(name, values);

            }
        }

        // clean up the triple store
        TDBFactory.release(dataset);

        return conceptFields;

    }

    /**
     * Merge updates with existing solr documents
     * 
     * @param indexDocument
     * @return
     * @throws IOException
     * @throws EncoderException
     * @throws XPathExpressionException
     */
    public SolrDoc mergeWithIndexedDocument(SolrDoc indexDocument) throws IOException,
            EncoderException, XPathExpressionException {

        SolrDoc solrDoc = httpService.retrieveDocumentFromSolrServer(indexDocument.getIdentifier(),
                solrQueryUri);
        if (solrDoc != null) {
            for (SolrElementField field : solrDoc.getFieldList()) {
                if (fieldsToMerge.contains(field.getName())
                        && !indexDocument.hasFieldWithValue(field.getName(), field.getValue())) {
                    indexDocument.addField(field);
                }
            }
        }
        return indexDocument;
    }
}