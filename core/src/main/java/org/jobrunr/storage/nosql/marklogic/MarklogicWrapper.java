package org.jobrunr.storage.nosql.marklogic;

import com.marklogic.client.DatabaseClient;
import com.marklogic.client.ResourceNotFoundException;
import com.marklogic.client.Transaction;
import com.marklogic.client.admin.QueryOptionsManager;
import com.marklogic.client.document.JSONDocumentManager;
import com.marklogic.client.eval.ServerEvaluationCall;
import com.marklogic.client.io.Format;
import com.marklogic.client.io.SearchHandle;
import com.marklogic.client.io.StringHandle;
import com.marklogic.client.io.marker.DocumentPatchHandle;
import com.marklogic.client.query.MatchDocumentSummary;
import com.marklogic.client.query.QueryManager;
import com.marklogic.client.query.StructuredQueryBuilder;
import com.marklogic.client.query.StructuredQueryDefinition;
import org.jobrunr.storage.PageRequest;
import org.jobrunr.storage.nosql.marklogic.mapper.MarklogicPageRequestMapper;
import org.jobrunr.utils.mapper.JsonMapper;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MarklogicWrapper {

  private final DatabaseClient databaseClient;
  private final JSONDocumentManager jsonDocumentManager;
  private final StructuredQueryBuilder structuredQueryBuilder;
  private final QueryOptionsManager queryOptionsManager;
  private final QueryManager queryManager;
  private final JsonMapper jsonMapper;

  private static final String OPT_FULL_RESULT = "full-result";
  private static final String OPT_MINIMAL_RESULT = "minimal-result";
  private static final String OPT_NO_RESULT = "no-result";

  public MarklogicWrapper(DatabaseClient databaseClient, JsonMapper jsonMapper) {
    this.databaseClient = databaseClient;
    this.jsonDocumentManager = databaseClient.newJSONDocumentManager();
    this.queryOptionsManager = databaseClient.newServerConfigManager().newQueryOptionsManager();

    String fullResultOptions = "{\"options\":{\"return-results\": \"true\", \"transform-results\": { \"apply\": \"raw\" }}}";
    queryOptionsManager.writeOptions(OPT_FULL_RESULT, new StringHandle(fullResultOptions).withFormat(Format.JSON));

    String minimalResultOptions = "{\"options\":{\"return-results\": \"true\", \"transform-results\": {\"apply\": \"empty-snippet\"}}}";
    queryOptionsManager.writeOptions(OPT_MINIMAL_RESULT, new StringHandle(minimalResultOptions).withFormat(Format.JSON));

    String noResultOptions = "{\"options\":{\"return-results\": \"false\"}}";
    queryOptionsManager.writeOptions(OPT_NO_RESULT, new StringHandle(noResultOptions).withFormat(Format.JSON));

    this.structuredQueryBuilder = new StructuredQueryBuilder(OPT_FULL_RESULT);
    this.queryManager = databaseClient.newQueryManager();
    this.jsonMapper = jsonMapper;
  }

  private String computeURI(String directory, String id) {
    return "/" + directory + "/" + id + ".json";
  }

  public void putDocument(String directory, String id, Map<String, Object> document) {
    putDocument(directory, id, document, null);
  }

  public void putDocument(String directory, String id, Map<String, Object> document, Transaction transaction) {
    String serializedDocument = jsonMapper.serializeRaw(document);
    jsonDocumentManager.write(computeURI(directory, id), new StringHandle(serializedDocument), transaction);
  }

  public boolean existsDocument(String directory, String id) {
    return existsDocument(directory, id, null);
  }

  public boolean existsDocument(String directory, String id, Transaction transaction) {
    return existsDocumentByURI(computeURI(directory, id), transaction);
  }

  public boolean existsDocumentByURI(String uri) {
    return existsDocumentByURI(uri, null);
  }

  public boolean existsDocumentByURI(String uri, Transaction transaction) {
    return jsonDocumentManager.exists(uri, transaction) != null;
  }

  public <T> T loadDocumentIfPresent(String directory, String id, Class<T> clazz) {
    return loadDocumentIfPresent(directory, id, clazz, null);
  }

  public <T> T loadDocumentIfPresent(String directory, String id, Class<T> clazz, Transaction transaction) {
    return loadDocumentIfPresentByURI(computeURI(directory, id), clazz, transaction);
  }

  public <T> T loadDocumentIfPresentByURI(String uri, Class<T> clazz) {
    return loadDocumentIfPresentByURI(uri, clazz, null);
  }

  public <T> T loadDocumentIfPresentByURI(String uri, Class<T> clazz, Transaction transaction) {
    if (existsDocumentByURI(uri, transaction)) {
      try {
        StringHandle sh = jsonDocumentManager.read(uri, new StringHandle(), transaction);
        String str = sh.get();
        return jsonMapper.deserializeRaw(str, clazz);
      } catch (ResourceNotFoundException e) {
        return null;
      }
    } else {
      return null;
    }
  }

  public <T> List<T> loadAllDocumentsInDirectory(String directory, Class<T> clazz) {
    StructuredQueryDefinition structuredQueryDefinition = structuredQueryBuilder.directory(1, "/" + directory + "/");
    return queryDocuments(structuredQueryDefinition, clazz);
  }

  public long countAllDocumentsInDirectory(String directory) {
    StructuredQueryDefinition structuredQueryDefinition = structuredQueryBuilder.directory(1, "/" + directory + "/");
    return countDocuments(structuredQueryDefinition);
  }

  public long countDocuments(StructuredQueryDefinition structuredQueryDefinition) {
    structuredQueryDefinition.setOptionsName(OPT_NO_RESULT);
    SearchHandle results = queryManager.search(structuredQueryDefinition, new SearchHandle());
    return results.getTotalResults();
  }

  public boolean patchDocumentIfPresent(String directory, String id, DocumentPatchHandle patch) {
    return patchDocumentIfPresent(directory, id, patch, null);
  }

  public boolean patchDocumentIfPresent(String directory, String id, DocumentPatchHandle patch, Transaction transaction) {
    return patchDocumentIfPresentByURI(computeURI(directory, id), patch, transaction);
  }

  public boolean patchDocumentIfPresentByURI(String uri, DocumentPatchHandle patch) {
    return patchDocumentIfPresentByURI(uri, patch, null);
  }

  public boolean patchDocumentIfPresentByURI(String uri, DocumentPatchHandle patch, Transaction transaction) {
    if (existsDocumentByURI(uri, transaction)) {
      try {
        jsonDocumentManager.patch(uri, patch, transaction);
        return true;
      } catch (ResourceNotFoundException e) {
        if ("RESTAPI-NODOCUMENT".equals(e.getServerMessageCode()))
          return false;
        else throw e;
      }
    }
    else {
      return false;
    }
  }

  public boolean deleteDocument(String directory, String id) {
    return deleteDocumentByURI(computeURI(directory, id));
  }

  public boolean deleteDocumentByURI(String uri) {
    if (existsDocumentByURI(uri)) {
      jsonDocumentManager.delete(uri);
      return true;
    } else {
      return false;
    }
  }

  public <T> List<T> queryDocuments(StructuredQueryDefinition structuredQueryDefinition, Class<T> clazz) {
    structuredQueryDefinition.setOptionsName(OPT_FULL_RESULT);
    SearchHandle results = queryManager.search(structuredQueryDefinition, new SearchHandle());
    return Arrays
            .stream(results.getMatchResults())
            .map(MatchDocumentSummary::getFirstSnippetText)
            .map(m -> jsonMapper.deserializeRaw(m, clazz))
            .collect(Collectors.toList());
  }

  public <T> List<T> queryDocuments(StructuredQueryDefinition structuredQueryDefinition, PageRequest pageRequest, Class<T> clazz) {
    structuredQueryDefinition.setOptionsName(getJobrunrQueryOptions(pageRequest));
    SearchHandle results = queryManager.search(structuredQueryDefinition, new SearchHandle(), pageRequest.getOffset() + 1);
    return Arrays
            .stream(results.getMatchResults())
            .map(MatchDocumentSummary::getFirstSnippetText)
            .map(m -> jsonMapper.deserializeRaw(m, clazz))
            .collect(Collectors.toList());
  }

  public List<String> queryDocumentURIs(StructuredQueryDefinition structuredQueryDefinition) {
    return queryDocumentURIs(structuredQueryDefinition, null);
  }

  public List<String> queryDocumentURIs(StructuredQueryDefinition structuredQueryDefinition, Transaction transaction) {
    structuredQueryDefinition.setOptionsName(OPT_MINIMAL_RESULT);
    SearchHandle results = queryManager.search(structuredQueryDefinition, new SearchHandle(), transaction);
    return Arrays
            .stream(results.getMatchResults())
            .map(MatchDocumentSummary::getUri)
            .collect(Collectors.toList());
  }

  public String getJobrunrQueryOptions(PageRequest pageRequest) {
    String optionsId = "page-" + pageRequest.getOrder() + "-" + pageRequest.getLimit();
    Map<String, Object> options = new HashMap<>();
    options.put("sort-order", new MarklogicPageRequestMapper().map(pageRequest));
    options.put("return-results", true);
    Map<String, Object> transformResults = new HashMap<>();
    transformResults.put("apply", "raw");
    options.put("transform-results", transformResults);
    options.put("page-length", pageRequest.getLimit());
    Map<String, Object> ret = new HashMap<>();
    ret.put("options", options);
    queryOptionsManager.writeOptions(optionsId, new StringHandle(jsonMapper.serializeRaw(ret)).withFormat(Format.JSON));
    return optionsId;
  }

  /*
   * Maintenance
   */
  public boolean createDirectory(String directory) throws IOException {
    String request =
            "xquery version \"1.0-ml\";\n" +
            "try\n" +
            "{\n" +
            "  let $res := xdmp:directory-create(\"/"+ directory +"/\")\n" +
            "  return true()\n" +
            "}\n" +
            "catch ($err)\n" +
            "{\n" +
            "  if ($err/*:code/text() eq \"XDMP-DIREXISTS\")\n" +
            "  then false()\n" +
            "  else $err\n" +
            "}";
    final ServerEvaluationCall evalCall = databaseClient.newServerEval().xquery(request);
    final String resultString = evalCall.evalAs(String.class);
    return booleanReply(resultString);
  }

  public boolean createRangeIndex(String type, String field) throws IOException {
    String request =
            "xquery version \"1.0-ml\";\n" +
            "import module namespace admin = \"http://marklogic.com/xdmp/admin\" at \"/MarkLogic/admin.xqy\";\n" +
            "try\n" +
            "{  \n" +
            "  let $config := admin:get-configuration()\n" +
            "  let $dbid := xdmp:database()\n" +
            "  let $rangespec := admin:database-range-element-index(\"" + type +"\", \"\", \"" + field + "\", \"http://marklogic.com/collation/\", fn:false() )\n" +
            "  let $updated-config := admin:database-add-range-element-index($config, $dbid, $rangespec)\n" +
            "  let $ret := admin:save-configuration-without-restart($updated-config)\n" +
            "  return fn:true()\n" +
            "}\n" +
            "catch ($err) \n" +
            "{\n" +
            "  if ($err/$err/*:code/text() eq \"ADMIN-DUPLICATECONFIGITEM\")\n" +
            "  then fn:false()\n" +
            "  else $err\n" +
            "}";
    final ServerEvaluationCall evalCall = databaseClient.newServerEval().xquery(request);
    final String resultString = evalCall.evalAs(String.class);
    return booleanReply(resultString);
  }

  public void deleteDirectory(String directory) throws IOException {
    String request =
            "xquery version \"1.0-ml\";\n" +
            "try\n" +
            "{\n" +
            "  xdmp:directory-delete(\"/"+ directory +"/\")\n" +
            "}\n" +
            "catch ($err)\n" +
            "{\n" +
            "  $err\n" +
            "}";
    final ServerEvaluationCall evalCall = databaseClient.newServerEval().xquery(request);
    final String resultString = evalCall.evalAs(String.class);
    voidReply(resultString);
  }

  private boolean booleanReply(String resultString) throws IOException {
    if ("true".equals(resultString))
      return true;
    else if ("false".equals(resultString))
      return false;
    else
      throw new IOException(resultString);
  }

  private void voidReply(String resultString) throws IOException {
    if (resultString != null)
      throw new IOException(resultString);
  }
}
