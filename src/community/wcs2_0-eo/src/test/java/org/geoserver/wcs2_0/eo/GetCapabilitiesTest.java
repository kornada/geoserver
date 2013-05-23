package org.geoserver.wcs2_0.eo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import javax.xml.namespace.QName;

import org.geoserver.data.test.MockData;
import org.geoserver.wcs.WCSInfo;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;

public class GetCapabilitiesTest extends WCSEOTestSupport {

    protected static QName WATTEMP = new QName(MockData.SF_URI, "watertemp", MockData.SF_PREFIX);

    @Before
    public void enableWCSEO() {
        WCSInfo wcs = getGeoServer().getService(WCSInfo.class);
        wcs.getMetadata().put(WCSEOMetadata.ENABLED.key, true);
        wcs.getMetadata().put(WCSEOMetadata.COUNT_DEFAULT.key, String.valueOf(20));
        wcs.getSRS().clear();
        wcs.getSRS().add("4326");
        wcs.getSRS().add("3857");
        getGeoServer().save(wcs);
        
        wcs = getGeoServer().getService(WCSInfo.class);
        assertTrue(wcs.getMetadata().get(WCSEOMetadata.ENABLED.key, Boolean.class));
    }
    
    @Before
    public void enableEODatasets() {
        enableEODataset(getLayerId(WATTEMP));
        enableEODataset(getLayerId(TIMERANGES));
    }

    @Test
    public void testEOExtensions() throws Exception {
        Document dom = getAsDOM("wcs?request=GetCapabilities&version=2.0.1&service=WCS");
        // print(dom);
        
        // operations metadata checks
        assertEquals("1", xpath.evaluate("count(//ows:Operation[@name='DescribeEOCoverageSet'])", dom));
        assertEquals("1", xpath.evaluate("count(//ows:Constraint[@name='CountDefault'])", dom));
        assertEquals("20", xpath.evaluate("//ows:Constraint[@name='CountDefault']/ows:DefaultValue", dom));
        
        // dataset series checks
        assertEquals("2", xpath.evaluate("count(//wcs:Extension/wcseo:DatasetSeriesSummary)", dom));
        assertEquals("1", xpath.evaluate("count(//wcs:Extension/wcseo:DatasetSeriesSummary[wcseo:DatasetSeriesId='sf__timeranges_dss'])", dom));
        assertEquals("1", xpath.evaluate("count(//wcs:Extension/wcseo:DatasetSeriesSummary[wcseo:DatasetSeriesId='sf__timeranges_dss']/ows:WGS84BoundingBox)", dom));
        assertEquals("2008-10-31T00:00:00.000Z", xpath.evaluate("//wcs:Extension/wcseo:DatasetSeriesSummary[wcseo:DatasetSeriesId='sf__timeranges_dss']/gml:TimePeriod/gml:beginPosition", dom));
        assertEquals("2008-11-07T00:00:00.000Z", xpath.evaluate("//wcs:Extension/wcseo:DatasetSeriesSummary[wcseo:DatasetSeriesId='sf__timeranges_dss']/gml:TimePeriod/gml:endPosition", dom));
        assertEquals("1", xpath.evaluate("count(//wcs:Extension/wcseo:DatasetSeriesSummary[wcseo:DatasetSeriesId='sf__watertemp_dss'])", dom));
        assertEquals("1", xpath.evaluate("count(//wcs:Extension/wcseo:DatasetSeriesSummary[wcseo:DatasetSeriesId='sf__watertemp_dss'])", dom));
        assertEquals("1", xpath.evaluate("count(//wcs:Extension/wcseo:DatasetSeriesSummary[wcseo:DatasetSeriesId='sf__watertemp_dss']/ows:WGS84BoundingBox)", dom));
        assertEquals("2008-10-31T00:00:00.000Z", xpath.evaluate("//wcs:Extension/wcseo:DatasetSeriesSummary[wcseo:DatasetSeriesId='sf__watertemp_dss']/gml:TimePeriod/gml:beginPosition", dom));
        assertEquals("2008-11-01T00:00:00.000Z", xpath.evaluate("//wcs:Extension/wcseo:DatasetSeriesSummary[wcseo:DatasetSeriesId='sf__watertemp_dss']/gml:TimePeriod/gml:endPosition", dom));
    }
    
    @Test
    public void testDisableEOExtensions() throws Exception {
        // disable EO extensions
        WCSInfo wcs = getGeoServer().getService(WCSInfo.class);
        wcs.getMetadata().put(WCSEOMetadata.ENABLED.key, false);
        getGeoServer().save(wcs);

        Document dom = getAsDOM("wcs?request=GetCapabilities&version=2.0.1&service=WCS");
        
        assertEquals("0", xpath.evaluate("count(//ows:Operation[@name='DescribeEOCoverageSet'])", dom));
        assertEquals("0", xpath.evaluate("count(//ows:Constraint[@name='CountDefault'])", dom));
        assertEquals("0", xpath.evaluate("count(//wcs:Extension/wcseo:DatasetSeriesSummary)", dom));
    }

}
