package org.geoserver.wcs2_0.eo;

import static org.junit.Assert.*;

import org.geoserver.wcs.WCSInfo;
import org.junit.Test;
import org.w3c.dom.Document;

import com.mockrunner.mock.web.MockHttpServletResponse;

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
    public void testSectionsAll() throws Exception {
        Document dom = getAsDOM("wcs?request=DescribeEOCoverageSet&version=2.0.1&service=WCS&eoid=sf__watertemp_dss&sections=All");
        
        // everything
        assertEquals("1", xpath.evaluate("count(/wcseo:EOCoverageSetDescription)", dom));
        assertEquals("1", xpath.evaluate("count(/wcseo:EOCoverageSetDescription/wcs:CoverageDescriptions)", dom));
        assertEquals("1", xpath.evaluate("count(/wcseo:EOCoverageSetDescription/wcseo:DatasetSeriesDescriptions)", dom));
    }
    
    
    @Test
    public void testSectionsCoverageDescriptions() throws Exception {
        Document dom = getAsDOM("wcs?request=DescribeEOCoverageSet&version=2.0.1&service=WCS&eoid=sf__watertemp_dss&sections=CoverageDescriptions");
        
        // only descriptions
        assertEquals("1", xpath.evaluate("count(/wcseo:EOCoverageSetDescription)", dom));
        assertEquals("1", xpath.evaluate("count(/wcseo:EOCoverageSetDescription/wcs:CoverageDescriptions)", dom));
        assertEquals("0", xpath.evaluate("count(/wcseo:EOCoverageSetDescription/wcseo:DatasetSeriesDescriptions)", dom));
    }
    
    @Test
    public void testSectionsDatasetSeries() throws Exception {
        Document dom = getAsDOM("wcs?request=DescribeEOCoverageSet&version=2.0.1&service=WCS&eoid=sf__watertemp_dss&sections=DatasetSeriesDescriptions");
        
        // only descriptions
        assertEquals("1", xpath.evaluate("count(/wcseo:EOCoverageSetDescription)", dom));
        assertEquals("0", xpath.evaluate("count(/wcseo:EOCoverageSetDescription/wcs:CoverageDescriptions)", dom));
        assertEquals("1", xpath.evaluate("count(/wcseo:EOCoverageSetDescription/wcseo:DatasetSeriesDescriptions)", dom));
    }
    
    @Test
    public void testSectionsInvalid() throws Exception {
        // invalid request
        MockHttpServletResponse response = getAsServletResponse("wcs?request=DescribeEOCoverageSet&version=2.0.1&service=WCS&eoid=sf__watertemp_dss&sections=sectionNotThere");
        checkOws20Exception(response, 400, "InvalidParameterValue", "sections");
    }
    
    @Test
    public void testCountLessThanMatched() throws Exception {
        Document dom = getAsDOM("wcs?request=DescribeEOCoverageSet&version=2.0.1&service=WCS&eoid=sf__watertemp_dss&count=2");
        // print(dom);
        
        // expected granules are there
        assertEquals("4", xpath.evaluate("/wcseo:EOCoverageSetDescription/@numberMatched", dom));
        assertEquals("2", xpath.evaluate("/wcseo:EOCoverageSetDescription/@numberReturned", dom));
        assertEquals("2", xpath.evaluate("count(//wcs:CoverageDescriptions/wcs:CoverageDescription)", dom));
    }
    
    @Test
    public void testDefaultCount() throws Exception {
        // set the default count to 1
        WCSInfo wcs = getGeoServer().getService(WCSInfo.class);
        wcs.getMetadata().put(WCSEOMetadata.COUNT_DEFAULT.key, 1);
        getGeoServer().save(wcs);
        
        Document dom = getAsDOM("wcs?request=DescribeEOCoverageSet&version=2.0.1&service=WCS&eoid=sf__watertemp_dss");
        // print(dom);
        
        // expected granules are there
        assertEquals("4", xpath.evaluate("/wcseo:EOCoverageSetDescription/@numberMatched", dom));
        assertEquals("1", xpath.evaluate("/wcseo:EOCoverageSetDescription/@numberReturned", dom));
        assertEquals("1", xpath.evaluate("count(//wcs:CoverageDescriptions/wcs:CoverageDescription)", dom));
    }
    
    @Test
    public void testSpatioTemporalDataset() throws Exception {
        Document dom = getAsDOM("wcs?request=DescribeEOCoverageSet&version=2.0.1&service=WCS&eoid=sf__spatio-temporal_dss");
        // print(dom);
        
        // this one has 16 granules
        assertEquals("16", xpath.evaluate("count(//wcs:CoverageDescriptions/wcs:CoverageDescription)", dom));
        
        // four of which start at one of these corners (2 times, 2 elevations) (check the bbox is actually the one of the granule, that is)
        String envelopeBase = "//wcs:CoverageDescriptions/wcs:CoverageDescription/gml:boundedBy/gml:EnvelopeWithTimePeriod";
        assertEquals("4", xpath.evaluate("count(" + envelopeBase + "[gml:lowerCorner='42.000641593750004 0.23722100000000002'])", dom));
        assertEquals("4", xpath.evaluate("count(" + envelopeBase + "[gml:lowerCorner='42.000641593750004 9.424764334960939'])", dom));
        assertEquals("4", xpath.evaluate("count(" + envelopeBase + "[gml:lowerCorner='40.56208080273438 9.424764334960939'])", dom));
        assertEquals("4", xpath.evaluate("count(" + envelopeBase + "[gml:lowerCorner='40.56208080273438 0.23722100000000002'])", dom));
        
        // check also by time, they should be 8 and 8
        assertEquals("8", xpath.evaluate("count(" + envelopeBase + "[gml:beginPosition='2008-10-31T00:00:00.000Z' and gml:endPosition='2008-10-31T00:00:00.000Z'])", dom));
        assertEquals("8", xpath.evaluate("count(" + envelopeBase + "[gml:beginPosition='2008-11-01T00:00:00.000Z' and gml:endPosition='2008-11-01T00:00:00.000Z'])", dom));
        
    }
    
    @Test
    public void testInvalidSubset() throws Exception {
        MockHttpServletResponse response = getAsServletResponse("wcs?request=DescribeEOCoverageSet&version=2.0.1&service=WCS&eoid=sf__spatio-temporal_dss&subset=abc(1,5)");
        checkOws20Exception(response, 400, "InvalidParameterValue", "subset");
    }
    
    @Test
    public void testLonTrimOverlap() throws Exception {
        // only the first part
        Document dom = getAsDOM("wcs?request=DescribeEOCoverageSet&version=2.0.1&service=WCS&eoid=sf__spatio-temporal_dss&subset=Long(1,5)");
        assertEquals("8", xpath.evaluate("count(//wcs:CoverageDescriptions/wcs:CoverageDescription)", dom));
        
        // overlaps with all
        dom = getAsDOM("wcs?request=DescribeEOCoverageSet&version=2.0.1&service=WCS&eoid=sf__spatio-temporal_dss&subset=Long(5,12)");
        assertEquals("16", xpath.evaluate("count(//wcs:CoverageDescriptions/wcs:CoverageDescription)", dom));
    }
    
    @Test
    public void testLonTrimContains() throws Exception {
        // overlaps but not contains
        Document dom = getAsDOM("wcs?request=DescribeEOCoverageSet&version=2.0.1&service=WCS&eoid=sf__spatio-temporal_dss&subset=Long(1,5)&containment=contains");
        assertEquals("0", xpath.evaluate("count(//wcs:CoverageDescriptions/wcs:CoverageDescription)", dom));
        assertEquals("0", xpath.evaluate("count(//wcseo:DatasetSeriesDescriptions)", dom));
        
        // overlaps with all, contains none
        dom = getAsDOM("wcs?request=DescribeEOCoverageSet&version=2.0.1&service=WCS&eoid=sf__spatio-temporal_dss&subset=Long(5,12)&containment=contains");
        assertEquals("0", xpath.evaluate("count(//wcs:CoverageDescriptions/wcs:CoverageDescription)", dom));
        assertEquals("0", xpath.evaluate("count(//wcseo:DatasetSeriesDescriptions)", dom));
        
        // contains only half
        dom = getAsDOM("wcs?request=DescribeEOCoverageSet&version=2.0.1&service=WCS&eoid=sf__spatio-temporal_dss&subset=Long(0,10)&containment=contains");
        assertEquals("8", xpath.evaluate("count(//wcs:CoverageDescriptions/wcs:CoverageDescription)", dom));
    }
    
    @Test
    public void testLatTrimOverlap() throws Exception {
        // only the first part
        Document dom = getAsDOM("wcs?request=DescribeEOCoverageSet&version=2.0.1&service=WCS&eoid=sf__spatio-temporal_dss&subset=Lat(40,41)");
        assertEquals("8", xpath.evaluate("count(//wcs:CoverageDescriptions/wcs:CoverageDescription)", dom));
        
        // overlaps with all
        dom = getAsDOM("wcs?request=DescribeEOCoverageSet&version=2.0.1&service=WCS&eoid=sf__spatio-temporal_dss&subset=Lat(41,43)");
        assertEquals("16", xpath.evaluate("count(//wcs:CoverageDescriptions/wcs:CoverageDescription)", dom));
    }
    
    @Test
    public void testLatTrimContains() throws Exception {
        // overlaps but not contains
        Document dom = getAsDOM("wcs?request=DescribeEOCoverageSet&version=2.0.1&service=WCS&eoid=sf__spatio-temporal_dss&subset=Lat(40,41)&containment=contains");
        assertEquals("0", xpath.evaluate("count(//wcs:CoverageDescriptions/wcs:CoverageDescription)", dom));
        assertEquals("0", xpath.evaluate("count(//wcseo:DatasetSeriesDescriptions)", dom));
        
        // overlaps with all, contains none
        dom = getAsDOM("wcs?request=DescribeEOCoverageSet&version=2.0.1&service=WCS&eoid=sf__spatio-temporal_dss&subset=Lat(41,43)&containment=contains");
        assertEquals("0", xpath.evaluate("count(//wcs:CoverageDescriptions/wcs:CoverageDescription)", dom));
        assertEquals("0", xpath.evaluate("count(//wcseo:DatasetSeriesDescriptions)", dom));
        
        // contains only half
        dom = getAsDOM("wcs?request=DescribeEOCoverageSet&version=2.0.1&service=WCS&eoid=sf__spatio-temporal_dss&subset=Lat(39,43)&containment=contains");
        assertEquals("8", xpath.evaluate("count(//wcs:CoverageDescriptions/wcs:CoverageDescription)", dom));
        assertEquals("1", xpath.evaluate("count(//wcseo:DatasetSeriesDescriptions)", dom));
    }
        
    @Test
    public void testTimeTrimOverlaps() throws Exception {
        // overlaps with first half
        Document dom = getAsDOM("wcs?request=DescribeEOCoverageSet&version=2.0.1&service=WCS&eoid=sf__spatio-temporal_dss" +
        		"&subset=phenomenonTime(\"2008-10-31T00:00:00.000Z\",\"2008-10-31T23:59:00.000Z\")");
        assertEquals("8", xpath.evaluate("count(//wcs:CoverageDescriptions/wcs:CoverageDescription)", dom));
        assertEquals("1", xpath.evaluate("count(//wcseo:DatasetSeriesDescriptions)", dom));
        
        
        // overlaps with second half
        dom = getAsDOM("wcs?request=DescribeEOCoverageSet&version=2.0.1&service=WCS&eoid=sf__spatio-temporal_dss" +
                "&subset=phenomenonTime(\"2008-11-01T00:00:00.000Z\",\"2008-11-01T01:00:00.000Z\")");
        assertEquals("8", xpath.evaluate("count(//wcs:CoverageDescriptions/wcs:CoverageDescription)", dom));
        assertEquals("1", xpath.evaluate("count(//wcseo:DatasetSeriesDescriptions)", dom));
        
        // overlaps with none
        dom = getAsDOM("wcs?request=DescribeEOCoverageSet&version=2.0.1&service=WCS&eoid=sf__spatio-temporal_dss" +
                "&subset=phenomenonTime(\"2008-11-02T01:00:00.000Z\",\"2008-11-02T02:00:00.000Z\")");
        assertEquals("0", xpath.evaluate("count(//wcs:CoverageDescriptions/wcs:CoverageDescription)", dom));
        assertEquals("0", xpath.evaluate("count(//wcseo:DatasetSeriesDescriptions)", dom));
    }
    
    @Test
    public void testTimeIntervalTrimContains() throws Exception {
        // overlaps with some, contains none
        Document dom = getAsDOM("wcs?request=DescribeEOCoverageSet&version=2.0.1&service=WCS&eoid=sf__timeranges_dss" + 
           "&subset=phenomenonTime(\"2008-10-31T00:00:00.000Z\",\"2008-10-31T23:59:00.000Z\")&containment=contains");
        assertEquals("0", xpath.evaluate("count(//wcs:CoverageDescriptions/wcs:CoverageDescription)", dom));
        assertEquals("0", xpath.evaluate("count(//wcseo:DatasetSeriesDescriptions)", dom));
        
        // contains a bunch
        dom = getAsDOM("wcs?request=DescribeEOCoverageSet&version=2.0.1&service=WCS&eoid=sf__timeranges_dss" +
                "&subset=phenomenonTime(\"2008-10-30T00:00:00.000Z\",\"2008-11-03T00:00:00.000Z\")&containment=contains");
        print(dom);
        assertEquals("4", xpath.evaluate("count(//wcs:CoverageDescriptions/wcs:CoverageDescription)", dom));
        assertEquals("1", xpath.evaluate("count(//wcseo:DatasetSeriesDescriptions)", dom));
    }
    
    @Test
    public void testMixedTrim() throws Exception {
        // 
        Document dom = getAsDOM("wcs?request=DescribeEOCoverageSet&version=2.0.1&service=WCS&eoid=sf__spatio-temporal_dss" +
        		"&subset=Long(1,5)&subset=Lat(40,41)&subset=phenomenonTime(\"2008-10-31T00:00:00.000Z\",\"2008-10-31T23:59:00.000Z\")");
        print(dom);
        assertEquals("2", xpath.evaluate("count(//wcs:CoverageDescriptions/wcs:CoverageDescription)", dom));
    }

}
