/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs2_0.eo.kvp;

import net.opengis.wcs20.DescribeEOCoverageSetType;
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
}
