package org.geoserver.wcs2_0.eo;

import static org.junit.Assert.*;

import org.junit.Test;
import org.w3c.dom.Document;

public class DescribeOECoverageSetTest extends WCSEOTestSupport {

    @Test
    public void testBasic() throws Exception {
        Document dom = getAsDOM("wcs?request=DescribeEOCoverageSet&version=2.0.1&service=WCS&eoid=sf__watertemp_dss");
        // print(dom);
        
        // main structure is there
        assertEquals("1", xpath.evaluate("count(/wcseo:EOCoverageSetDescription)", dom));
        assertEquals("1", xpath.evaluate("count(/wcseo:EOCoverageSetDescription/wcs:CoverageDescriptions)", dom));
        assertEquals("1", xpath.evaluate("count(/wcseo:EOCoverageSetDescription/wcseo:DatasetSeriesDescriptions)", dom));
        
        // expected granules are there
        assertEquals("4", xpath.evaluate("/wcseo:EOCoverageSetDescription/@numberMatched", dom));
        assertEquals("4", xpath.evaluate("/wcseo:EOCoverageSetDescription/@numberReturned", dom));
        assertEquals("4", xpath.evaluate("count(//wcs:CoverageDescriptions/wcs:CoverageDescription)", dom));
        
        // check one granule
        String base = "//wcs:CoverageDescriptions/wcs:CoverageDescription[@gml:id='sf__watertemp_granule_watertemp.4']";
        assertEquals("1", xpath.evaluate("count(" + base + ")", dom));
        // ... the time has been sliced to this single granule
        assertEquals("2008-10-31T00:00:00.000Z", xpath.evaluate(base + "/gml:boundedBy/gml:EnvelopeWithTimePeriod/gml:beginPosition", dom));
        assertEquals("2008-10-31T00:00:00.000Z", xpath.evaluate(base + "/gml:boundedBy/gml:EnvelopeWithTimePeriod/gml:endPosition", dom));
        assertEquals("2008-10-31T00:00:00.000Z", xpath.evaluate(base + "/gmlcov:metadata/gmlcov:Extension/wcseo:EOMetadata" +
        		"/eop:EarthObservation/om:phenomenonTime/gml:TimePeriod/gml:beginPosition", dom));
        
        // check the DatasetSeriesDescriptions
        assertEquals("1", xpath.evaluate("count(/wcseo:EOCoverageSetDescription/wcseo:DatasetSeriesDescriptions/wcseo:DatasetSeriesDescription)", dom));
        assertEquals("1", xpath.evaluate("count(//wcseo:DatasetSeriesDescription/gml:boundedBy/gml:Envelope)", dom));
        assertEquals("sf__watertemp_dss", xpath.evaluate("//wcseo:DatasetSeriesDescription/wcseo:DatasetSeriesId", dom));
        assertEquals("2008-10-31T00:00:00.000Z", xpath.evaluate("//wcseo:DatasetSeriesDescription/gml:TimePeriod/gml:beginPosition", dom));
        assertEquals("2008-11-01T00:00:00.000Z", xpath.evaluate("//wcseo:DatasetSeriesDescription/gml:TimePeriod/gml:endPosition", dom));
    }
    
    @Test
    public void testSections() throws Exception {
        Document dom = getAsDOM("wcs?request=DescribeEOCoverageSet&version=2.0.1&service=WCS&eoid=sf__watertemp_dss&sections=All");
        
        // everything
        assertEquals("1", xpath.evaluate("count(/wcseo:EOCoverageSetDescription)", dom));
        assertEquals("1", xpath.evaluate("count(/wcseo:EOCoverageSetDescription/wcs:CoverageDescriptions)", dom));
        assertEquals("1", xpath.evaluate("count(/wcseo:EOCoverageSetDescription/wcseo:DatasetSeriesDescriptions)", dom));
        
        dom = getAsDOM("wcs?request=DescribeEOCoverageSet&version=2.0.1&service=WCS&eoid=sf__watertemp_dss&sections=CoverageDescriptions");
        
        // only descriptions
        assertEquals("1", xpath.evaluate("count(/wcseo:EOCoverageSetDescription)", dom));
        assertEquals("1", xpath.evaluate("count(/wcseo:EOCoverageSetDescription/wcs:CoverageDescriptions)", dom));
        assertEquals("0", xpath.evaluate("count(/wcseo:EOCoverageSetDescription/wcseo:DatasetSeriesDescriptions)", dom));
        
        
        dom = getAsDOM("wcs?request=DescribeEOCoverageSet&version=2.0.1&service=WCS&eoid=sf__watertemp_dss&sections=DatasetSeriesDescriptions");
        
        // only descriptions
        assertEquals("1", xpath.evaluate("count(/wcseo:EOCoverageSetDescription)", dom));
        assertEquals("0", xpath.evaluate("count(/wcseo:EOCoverageSetDescription/wcs:CoverageDescriptions)", dom));
        assertEquals("1", xpath.evaluate("count(/wcseo:EOCoverageSetDescription/wcseo:DatasetSeriesDescriptions)", dom));
    }
    
    @Test
    public void testCount() throws Exception {
        Document dom = getAsDOM("wcs?request=DescribeEOCoverageSet&version=2.0.1&service=WCS&eoid=sf__watertemp_dss&count=2");
        // print(dom);
        
        // expected granules are there
        assertEquals("4", xpath.evaluate("/wcseo:EOCoverageSetDescription/@numberMatched", dom));
        assertEquals("2", xpath.evaluate("/wcseo:EOCoverageSetDescription/@numberReturned", dom));
        assertEquals("2", xpath.evaluate("count(//wcs:CoverageDescriptions/wcs:CoverageDescription)", dom));
    }
        

}
