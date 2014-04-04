package fi.nls.oskari.util;

import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.log.Logger;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.util.AXIOMUtil;

import javax.xml.stream.XMLStreamException;
import java.io.StringWriter;
import java.util.Iterator;

/**
 * Created with IntelliJ IDEA.
 * User: SMAKINEN
 * Date: 26.3.2014
 * Time: 15:15
 * To change this template use File | Settings | File Templates.
 */
public class XmlHelper {

    private static final Logger log = LogFactory.getLogger(XmlHelper.class);

/*
    <wfs:Transaction xmlns:wfs="http://www.opengis.net/wfs" service="WFS" version="1.0.0" xsi:schemaLocation="http://www.opengis.net/wfs http://schemas.opengis.net/wfs/1.0.0/WFS-transaction.xsd" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
    <wfs:Insert>
    <feature:my_places xmlns:feature="http://www.oskari.org">
    <feature:geometry>
    <gml:MultiPoint xmlns:gml="http://www.opengis.net/gml" srsName="EPSG:3067">
    <gml:pointMember>
    <gml:Point>
    <gml:coordinates decimal="." cs="," ts=" ">395540,6706198.1514938</gml:coordinates>
    </gml:Point>
    </gml:pointMember>
    </gml:MultiPoint>
    </feature:geometry>
    <feature:name>poster test</feature:name>
    <feature:place_desc></feature:place_desc>
    <feature:attention_text></feature:attention_text>
    <feature:link></feature:link>
    <feature:image_url></feature:image_url>
    <feature:category_id>11588</feature:category_id>
    <feature:uuid>aaaaa-bbbbb-cccc-dddd-eeee</feature:uuid>
    </feature:my_places>
    </wfs:Insert>
    </wfs:Transaction>
    */

    public static OMElement parseXML(final String xml) {
        try {
            return AXIOMUtil.stringToOM(xml);
        } catch (Exception e) {
            log.error("Couldnt't parse XML", log.getCauseMessages(e), xml);
        }
        return null;
    }

    public static String toString(final OMElement xml) {
        try {
            StringWriter writer = new StringWriter();
            xml.serialize(writer);
            return writer.toString();
        } catch (Exception e) {
            log.error("Couldnt't serialize XML to String", log.getCauseMessages(e), xml);
        }
        return null;
    }

    /**
     * Checks OMElement direct children. Returns true if each child tag has given local name.
     * @param root element to check
     * @param tag localname to check against
     * @return false if params are null or there is a direct children with another name
     */
    public static boolean containsOnlyDirectChildrenOfName(final OMElement root, final String tag) {
        if(root == null || tag == null) {
            return false;
        }

        final Iterator<OMElement> it = root.getChildElements();
        while(it.hasNext()) {
            OMElement child = it.next();
            if(!tag.equals(child.getLocalName())) {
                return false;
            }
        }
        return true;
    }
}