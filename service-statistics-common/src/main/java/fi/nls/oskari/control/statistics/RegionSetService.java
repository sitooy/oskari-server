package fi.nls.oskari.control.statistics;

import fi.nls.oskari.control.statistics.db.RegionSet;
import fi.nls.oskari.control.statistics.xml.Region;
import fi.nls.oskari.control.statistics.xml.WfsXmlParser;
import fi.nls.oskari.service.OskariComponent;
import fi.nls.oskari.service.ServiceException;
import fi.nls.oskari.util.IOHelper;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.List;

/**
 * Created by SMAKINEN on 27.4.2016.
 */
public abstract class RegionSetService extends OskariComponent {

    public abstract List<RegionSet> getRegionSets();
    public abstract RegionSet getRegionSet(long id);

    public List<Region> getRegions(RegionSet regionset) throws IOException, ServiceException {
        final String propId = regionset.getIdProperty();
        final String propName = regionset.getNameProperty();

        // For example: http://localhost:8080/geoserver/wfs?service=wfs&version=1.1.0&request=GetFeature&typeNames=oskari:kunnat2013
        //&propertyName=kuntakoodi,kuntanimi,geom
        final String url = regionset.getFeaturesUrl() + "?service=wfs&version=1.1.0&request=GetFeature&typeNames=" + regionset.getOskariLayerName();
                //"&propertyName=" + propId + "," + propName;

        final HttpURLConnection connection = IOHelper.getConnection(url);
        return WfsXmlParser.parse(connection.getInputStream(), propId, propName);
    }
}
