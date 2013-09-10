package fi.nls.oskari.control.view;

import fi.mml.portti.service.db.permissions.PermissionsService;
import fi.mml.portti.service.db.permissions.PermissionsServiceIbatisImpl;
import fi.nls.oskari.control.ActionParameters;
import fi.nls.oskari.control.ontology.SearchKeywordsHandler;
import fi.nls.oskari.domain.User;
import fi.nls.oskari.ontology.domain.Keyword;
import fi.nls.oskari.ontology.service.KeywordService;
import fi.nls.oskari.ontology.service.KeywordServiceIbatisImpl;
import fi.nls.test.control.JSONActionRouteTest;
import fi.nls.test.util.ResourceHelper;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

/**
 * @author TMIKKOLAINEN
 */
public class SearchKeywordsHandlerTest extends JSONActionRouteTest {

    final private SearchKeywordsHandler handler = new SearchKeywordsHandler();

    private KeywordService keywordService = null;
    private PermissionsService permissionsService = null;

    @Before
    public void setUp() throws Exception {
        keywordService = mock(KeywordServiceIbatisImpl.class);
        permissionsService = mock(PermissionsServiceIbatisImpl.class);

        handler.setService(keywordService);
        handler.setPermissionsService(permissionsService);
        handler.init();
    }

    private static Keyword createKeyword(Long id, String value, Long... layers) {
        Keyword keyword = new Keyword();
        keyword.setId(id);
        keyword.setValue(value);
        List<Long> layerIds = new ArrayList<Long>();
        for(Long layer : layers) {
            layerIds.add(layer);
        }
        keyword.setLayerIds(layerIds);
        return keyword;
    }

    private static List<Map<String, Object>> getPermissionsList(Long... layerIDs) {
        List<Map<String, Object>> permissions = new ArrayList<Map<String, Object>>();
        for (Long layerID : layerIDs) {
            Map<String, Object> permission = new HashMap<String, Object>();
            permission.put("id", layerID);
            permissions.add(permission);
        }
        return permissions;
    }

    @Test
    public void testEmptyResponse() throws Exception {
        // Empty result should still give us a fake exact hit
        doReturn(null).when(keywordService).findExactKeyword(anyString(), anyString());
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put("keyword", "nothing");
        parameters.put("lang", "fi");
        final ActionParameters actionParameters = createActionParams(parameters);
        verifyResponseNotWritten(actionParameters);
        handler.handleAction(actionParameters);
        verifyResponseWritten(actionParameters);
        verifyResponseContent(ResourceHelper.readJSONArrayResource("SearchKeywordsHandlerTest-empty-result.json", this));
    }

    @Test
    public void testNonPermittedLayerResponse() throws Exception {
        // test a response that doesn't have a layer because the user doesn't have permissions for it.
        // mock response from services
        doReturn(createKeyword(1l, "test", 1l, 2l)).when(keywordService).findExactKeyword(anyString(), anyString());
        doReturn(getPermissionsList(1l)).when(permissionsService).getListOfMaplayerIdsForViewPermissionByUser(any(User.class));
        // do unmocked calls simply return a new<Whatever>?
        // check that the response doesn't have layers
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put("keyword", "test");
        parameters.put("lang", "fi");
        final ActionParameters actionParameters = createActionParams(parameters);
        verifyResponseNotWritten(actionParameters);
        handler.handleAction(actionParameters);
        verifyResponseWritten(actionParameters);
        verifyResponseContent(ResourceHelper.readJSONArrayResource("SearchKeywordsHandlerTest-one-layer.json", this));
    }

    @Test
    public void testFullResponse() throws Exception {
        // test a response that carries all possible response/relation types.
        // shows that we get all the relation types and that they are in the right order

        // exact match kw with no layers
        doReturn(createKeyword(1l, "test")).when(keywordService).findExactKeyword(anyString(), anyString());
        // synonym with no layers
        List<Keyword> synonyms = new ArrayList<Keyword>();
        synonyms.add(createKeyword(2l, "trial"));
        doReturn(synonyms).when(keywordService).findSynonyms(anyLong(), anyString());
        // near match with no layers
        List<Keyword> nearMatches = new ArrayList<Keyword>();
        nearMatches.add(createKeyword(3l, "testarossa"));
        doReturn(nearMatches).when(keywordService).findKeywordsMatching(anyString(), anyString());
        // parent with a layer
        List<Keyword> parents = new ArrayList<Keyword>();
        parents.add(createKeyword(4l, "parent", 1l));
        doReturn(parents).when(keywordService).findParents(anyLong(), anyString());
        // sibling with a not-permitted layer
        List<Keyword> siblings = new ArrayList<Keyword>();
        siblings.add(createKeyword(5l, "sibling", 2l));
        doReturn(siblings).when(keywordService).findSiblings(anyLong(), anyString());

        doReturn(getPermissionsList(1l)).when(permissionsService).getListOfMaplayerIdsForViewPermissionByUser(any(User.class));

        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put("keyword", "test");
        parameters.put("lang", "fi");
        final ActionParameters actionParameters = createActionParams(parameters);
        verifyResponseNotWritten(actionParameters);
        handler.handleAction(actionParameters);
        verifyResponseWritten(actionParameters);
        verifyResponseContent(ResourceHelper.readJSONArrayResource("SearchKeywordsHandlerTest-all-relation-types.json", this));

    }

    @Test
    public void testNonPermittedLayerDoesNotBlockParentInclusion() throws Exception {
        doReturn(createKeyword(1l, "test", 1l)).when(keywordService).findExactKeyword(anyString(), anyString());
        doReturn(getPermissionsList(2l)).when(permissionsService).getListOfMaplayerIdsForViewPermissionByUser(any(User.class));
        List<Keyword> parents = new ArrayList<Keyword>();
        parents.add(createKeyword(4l, "parent", 2l));
        doReturn(parents).when(keywordService).findParents(anyLong(), anyString());

        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put("keyword", "test");
        parameters.put("lang", "fi");
        final ActionParameters actionParameters = createActionParams(parameters);
        verifyResponseNotWritten(actionParameters);
        handler.handleAction(actionParameters);
        verifyResponseWritten(actionParameters);
        verifyResponseContent(ResourceHelper.readJSONArrayResource("SearchKeywordsHandlerTest-non-permitted-layer-on-exact-match.json", this));

    }
}