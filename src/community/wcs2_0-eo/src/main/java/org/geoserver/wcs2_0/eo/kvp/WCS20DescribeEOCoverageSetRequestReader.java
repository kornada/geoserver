/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs2_0.eo.kvp;

import java.util.Map;

import net.opengis.ows11.SectionsType;
import net.opengis.wcs20.DescribeEOCoverageSetType;
import net.opengis.wcs20.Section;
import net.opengis.wcs20.Sections;
import net.opengis.wcs20.Wcs20Factory;

import org.geoserver.ows.kvp.EMFKvpRequestReader;

/**
 * Parses a DescribeEOCoverageSet request for WCS EO into the correspondent model object
 * @author Andrea Aime - GeoSolutions
 */
public class WCS20DescribeEOCoverageSetRequestReader extends EMFKvpRequestReader {

    public WCS20DescribeEOCoverageSetRequestReader() {
        super(DescribeEOCoverageSetType.class, Wcs20Factory.eINSTANCE);
    }
    
    @Override
    public Object read(Object request, Map kvp, Map rawKvp) throws Exception {
        SectionsType owsSections = (SectionsType) kvp.get("sections");
        if(owsSections != null) {
            Sections sections = Wcs20Factory.eINSTANCE.createSections();
            for(Object o : owsSections.getSection()) {
                String sectionName = (String) o;
                sections.getSection().add(Section.get(sectionName));
            }
            kvp.put("sections", sections);
        }
        return super.read(request, kvp, rawKvp);
    }
}
