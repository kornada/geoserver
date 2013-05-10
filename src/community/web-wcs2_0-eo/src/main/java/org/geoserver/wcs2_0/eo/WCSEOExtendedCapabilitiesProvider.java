/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs2_0.eo;

import static org.geoserver.ows.util.ResponseUtils.appendQueryString;
import static org.geoserver.ows.util.ResponseUtils.buildURL;

import java.io.IOException;
import java.util.List;

import net.opengis.wcs20.GetCapabilitiesType;

import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.ows.URLMangler;
import org.geoserver.ows.URLMangler.URLType;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.wcs.WCSInfo;
import org.geoserver.wcs2_0.response.WCSExtendedCapabilitiesProvider;
import org.geoserver.wcs2_0.response.WCSTimeDimensionHelper;
import org.geoserver.wcs2_0.util.NCNameResourceCodec;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.NamespaceSupport;

/**
 * Encodes the extensions to the WCS capabilities document
 * 
 * @author Andrea Aime - GeoSolutions
 */
public class WCSEOExtendedCapabilitiesProvider extends WCSExtendedCapabilitiesProvider {
    
    public static final String NAMESPACE = "http://www.opengis.net/wcseo/1.0";

    /**
     * IGN : Do we still need to host this xsd ?
     */
    public String[] getSchemaLocations(String schemaBaseURL) {
        String schemaLocation = ResponseUtils.buildURL(schemaBaseURL, "schemas/wcseo/1.0/wcsEOGetCapabilites.xsd",
                null, URLType.RESOURCE);
        return new String[] { NAMESPACE, schemaLocation };
    }

    @Override
    public void registerNamespaces(NamespaceSupport namespaces) {
        namespaces.declarePrefix("wcseo", NAMESPACE);
    }

    @Override
    public void encodeExtendedOperations(org.geoserver.ExtendedCapabilitiesProvider.Translator tx,
            WCSInfo wcs, GetCapabilitiesType request) throws IOException {
        AttributesImpl attributes = new AttributesImpl();
        attributes.addAttribute(null, "name", "name", null, "DescribeEOCoverageSet");
        tx.start("ows:Operation", attributes);

        final String url = appendQueryString(buildURL(request.getBaseUrl(), "wcs", null, URLMangler.URLType.SERVICE), "");

        tx.start("ows:DCP");
        tx.start("ows:HTTP");
        attributes = new AttributesImpl();
        attributes.addAttribute("", "xlink:href", "xlink:href", "", url);
        element(tx, "ows:Get", null, attributes);
        tx.end("ows:HTTP");
        tx.end("ows:DCP");

        attributes = new AttributesImpl();
        attributes.addAttribute("", "xlink:href", "xlink:href", "", url);
        tx.start("ows:DCP");
        tx.start("ows:HTTP");
        element(tx, "ows:Post", null, attributes);
        tx.end("ows:HTTP");
        tx.end("ows:DCP");
        
        tx.end("ows:Operation");
    }

    @Override
    public void encodeExtendedContents(org.geoserver.ExtendedCapabilitiesProvider.Translator tx,
            WCSInfo wfs, List<CoverageInfo> coverages, GetCapabilitiesType request) throws IOException {
        tx.start("wcseo:DatasetSeriesSummary");
        for (CoverageInfo ci : coverages) {
            Boolean dataset = ci.getMetadata().get(WCSEOMetadata.DATASET.key, Boolean.class);
            DimensionInfo time = ci.getMetadata().get(ResourceInfo.TIME, DimensionInfo.class);
            if(dataset != null && dataset && time != null & time.isEnabled()) {
                ReferencedEnvelope bbox = ci.getLatLonBoundingBox();
                tx.start("ows:WGS84BoundingBox");
                element(tx, "ows:LowerCorner", bbox.getMinX() + " " + bbox.getMinY(), null);
                element(tx, "ows:UpperCorner", bbox.getMaxX() + " " + bbox.getMaxY(), null);
                tx.end("ows:WGS84BoundingBox");
                String datasetId = NCNameResourceCodec.encode(ci) + "__dss";
                element(tx, "wcseo:DatasetSeriesId", datasetId, null);
                
                GridCoverage2DReader reader = (GridCoverage2DReader) ci.getGridCoverageReader(null, null);
                WCSTimeDimensionHelper timeHelper = new WCSTimeDimensionHelper(time, reader, null);
                tx.start("gml:TimePeriod", atts("gml:id", datasetId + "__timeperiod"));
                element(tx, "gml:beginPosition", timeHelper.getBeginPosition(), null);
                element(tx, "gml:endPosition", timeHelper.getEndPosition(), null);
                tx.end("gml:TimePeriod");
            }
        }
        tx.end("wcseo:DatasetSeriesSummary");
    }
    
    private void element(org.geoserver.ExtendedCapabilitiesProvider.Translator tx, String element,
            String content, AttributesImpl attributes) {
        tx.start(element, attributes);
        if(content != null) {
            tx.chars(content);
        }
        tx.end(element);
    }
    
    Attributes atts(String... atts) {
        AttributesImpl attributes = new AttributesImpl();
        for (int i = 0; i < atts.length; i += 2) {
            attributes.addAttribute(null, atts[i], atts[i], null, atts[i + 1]);
        }
        return attributes;
    }

}
