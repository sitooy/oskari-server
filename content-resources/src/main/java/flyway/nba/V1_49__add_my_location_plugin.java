package flyway.nba;

import java.sql.Connection;

import org.flywaydb.core.api.migration.jdbc.JdbcMigration;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import fi.nls.oskari.domain.map.view.Bundle;
import fi.nls.oskari.domain.map.view.View;
import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.log.Logger;
import fi.nls.oskari.map.view.ViewService;
import fi.nls.oskari.map.view.ViewServiceIbatisImpl;
import fi.nls.oskari.util.JSONHelper;


public class V1_49__add_my_location_plugin implements JdbcMigration {
    private static final Logger LOG = LogFactory.getLogger(V1_49__add_my_location_plugin.class);
    private ViewService service = new ViewServiceIbatisImpl();
    private static final String PLUGIN_NAME = "Oskari.mapframework.bundle.mapmodule.plugin.MyLocationPlugin";

    public void migrate(Connection connection) throws Exception {
        updateView(7);
    }

    private void updateView(long viewId)
            throws Exception {

        View view = service.getViewWithConf(viewId);

        final Bundle mapfull = view.getBundleByName("mapfull");
        boolean addedPlugin = addPlugin(mapfull);
        if(addedPlugin) {
            service.updateBundleSettingsForView(view.getId(), mapfull);
        }
    }

    private boolean addPlugin(final Bundle mapfull) throws JSONException {
        final JSONObject config = mapfull.getConfigJSON();
        final JSONArray plugins = config.optJSONArray("plugins");
        if(plugins == null) {
            throw new RuntimeException("No plugins" + config.toString(2));
            //continue;
        }
        boolean found = false;
        for(int i = 0; i < plugins.length(); ++i) {
            JSONObject plugin = plugins.getJSONObject(i);
            if(PLUGIN_NAME.equals(plugin.optString("id"))) {
                found = true;
                break;
            }
        }
        // add plugin if not there yet
        if(!found) {
            plugins.put(JSONHelper.createJSONObject("id", PLUGIN_NAME));
        }
        return true;
    }

}