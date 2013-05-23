package org.geoserver.wcs2_0.eo;

import static org.junit.Assert.*;

import org.junit.Test;
import org.w3c.dom.Document;

public class DescribeOECoverageSetTest extends WCSEOTestSupport {

    @Test
    public void testBasicTimeRanges() throws Exception {
        Document dom = getAsDOM("wcs?request=DescribeEOCoverageSet&version=2.0.1&service=WCS&eoid=sf__timeranges_dss");
        print(dom);
        
        assertEquals("1", xpath.evaluate("count(//wcseo:EOCoverageSetDescription)", dom));

    }
    
    @Test
    public void testBasic2() throws Exception {
        Document dom = getAsDOM("wcs?request=DescribeEOCoverageSet&version=2.0.1&service=WCS&eoid=sf__watertemp_dss");
        print(dom);
        
        assertEquals("1", xpath.evaluate("count(//wcseo:EOCoverageSetDescription)", dom));
    }
        

}
