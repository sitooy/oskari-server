package fi.nls.oskari.view.modifier;

import fi.nls.oskari.annotation.OskariViewModifier;
import fi.nls.oskari.util.JSONHelper;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Base class for components modifying Oskari Map Frameworks view on runtime.
 */
public abstract class ViewModifier {

    public static final String KEY_CONF = "conf";
    public static final String KEY_STATE = "state";
    
    public static final String BUNDLE_MAPFULL = "mapfull";
    public static final String BUNDLE_STATSGRID = "statsgrid";
    public static final String BUNDLE_MYPLACES2 = "myplaces2";
    public static final String BUNDLE_INFOBOX = "infobox";
    public static final String BUNDLE_POSTPROCESSOR = "postprocessor";
    public static final String BUNDLE_ADMINLAYERSELECTOR = "admin-layerselector";
    public static final String BUNDLE_ADMINLAYERRIGHTS = "admin-layerrights";
    public static final String BUNDLE_PUBLISHEDGRID = "publishedgrid";

    public void init() {

    }

    public void teardown() {

    }

    /**
     * Returns @OskariViewModifier annotation value if any or defaults to class name
     * @return name of the modifier
     */
    public String getName () {
        if(getClass().isAnnotationPresent(OskariViewModifier.class)) {
            OskariViewModifier r = getClass().getAnnotation(OskariViewModifier.class);
            if(!r.value().isEmpty()) {
                return r.value();
            }
        }
        return getClass().getSimpleName();
    }

    private boolean isParamsValid(final JSONObject config, final String bundleid) {
        return (config != null && bundleid != null && !bundleid.trim().isEmpty());
    }

    /**
     * Returns configuration[bundleid] as JSONObject from the config creating it if it doesn't exist.
     * Returns null if either parameter is null or empty.
     * @param config
     * @param bundleid
     * @return
     */
    private JSONObject getBundle(final JSONObject config, final String bundleid) {

        if (!isParamsValid(config, bundleid)) {
            return null;
        }

        if(!config.has(bundleid)) {
            final JSONObject obj = new JSONObject();
            JSONHelper.putValue(config, bundleid, obj);
        }
        return config.optJSONObject(bundleid);
    }

    /**
     * Returns configuration[bundleid].conf as JSONObject from the config creating it if it doesn't exist.
     * Returns null if parameter is null. Uses getName() to determine bundleid.
     * @param config
     * @return
     */
    public JSONObject getBundleConfig(final JSONObject config) {
        return getBundleConfig(config, getName());
    }
    /**
     * Returns configuration[bundleid].conf as JSONObject from the config creating it if it doesn't exist.
     * Returns null if either parameter is null or empty.
     * @param config
     * @param bundleid
     * @return
     */
    public JSONObject getBundleConfig(final JSONObject config, final String bundleid) {
        final JSONObject bundle = getBundle(config, bundleid);
        if (!isParamsValid(config, bundleid) || bundle == null) {
            return null;
        }

        if(!bundle.has(KEY_CONF)) {
            final JSONObject obj = new JSONObject();
            JSONHelper.putValue(bundle, KEY_CONF, obj);
        }
        return bundle.optJSONObject(KEY_CONF);
    }
    /**
     * Returns configuration[bundleid].state as JSONObject from the config creating it if it doesn't exist.
     * Returns null if parameter is null. Uses getName() to determine bundleid.
     * @param config
     * @return
     */
    public JSONObject getBundleState(final JSONObject config) {
        return getBundleState(config, getName());
    }
    /**
     * Returns configuration[bundleid].state as JSONObject from the config creating it if it doesn't exist.
     * Returns null if either parameter is null or empty.
     * @param config
     * @param bundleid
     * @return
     */
    public JSONObject getBundleState(final JSONObject config, final String bundleid) {

        final JSONObject bundle = getBundle(config, bundleid);
        if (!isParamsValid(config, bundleid) || bundle == null) {
            return null;
        }

        if(!bundle.has(KEY_STATE)) {
            final JSONObject obj = new JSONObject();
            JSONHelper.putValue(bundle, KEY_STATE, obj);
        }
        return bundle.optJSONObject(KEY_STATE);
    }

    /**
     * Checks if a bundle with given id is part of the startupSequence
     * @param startupSeq apps startup sequence
     * @param bundleid bundle to check
     * @return true if found
     */
    public boolean isBundlePresent(final JSONArray startupSeq, final String bundleid) {
        if(startupSeq == null || bundleid == null || bundleid.trim().isEmpty()) {
            return false;
        }

        for (int i = 0; i < startupSeq.length(); i++) {
            final JSONObject bundle = (JSONObject) startupSeq.opt(i);
            final String startupBundleid = bundle.optString("bundlename");
            if(startupBundleid != null && startupBundleid.equalsIgnoreCase(bundleid)) {
                return true;
            }
        }
        return false;
    }
}