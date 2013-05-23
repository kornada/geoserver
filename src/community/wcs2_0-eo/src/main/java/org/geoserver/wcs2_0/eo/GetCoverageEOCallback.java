/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs2_0.eo;

import net.opengis.wcs20.GetCoverageType;

import org.geoserver.catalog.CoverageInfo;
import org.geoserver.ows.AbstractDispatcherCallback;
import org.geoserver.ows.Request;
import org.geoserver.platform.Operation;
import org.geoserver.wcs2_0.util.NCNameResourceCodec;
import org.geotools.factory.CommonFactoryFinder;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;

/**
 * Plugs into the GetCoverage request cycle and transforms a request for a single EO granule
 * to one against the coverage, but with the filter to limit it to the specified granule 
 * 
 * @author Andrea Aime - GeoSolutions
 */
public class GetCoverageEOCallback extends AbstractDispatcherCallback {
    
    private static FilterFactory FF = CommonFactoryFinder.getFilterFactory2();
    
    private EOCoverageResourceCodec codec;

    public GetCoverageEOCallback(EOCoverageResourceCodec codec) {
        this.codec = codec;
    }

    @Override
    public Operation operationDispatched(Request request, Operation operation) {
        Object[] parameters = operation.getParameters();
        if(parameters != null && parameters.length > 0 && parameters[0] instanceof GetCoverageType) {
            // check we are going against a granule
            GetCoverageType gc = (GetCoverageType) parameters[0];
            String coverageId = gc.getCoverageId();
            CoverageInfo coverage = codec.getGranuleCoverage(coverageId);
            if(coverage != null) {
                // set the actual coverage name
                String actualCoverageId = NCNameResourceCodec.encode(coverage);
                gc.setCoverageId(actualCoverageId);
                
                // extract the granule filter
                Filter granuleFilter = codec.getGranuleFilter(coverageId);
                Filter previous = gc.getFilter();
                if(previous == null || previous == Filter.INCLUDE) {
                    gc.setFilter(granuleFilter);
                } else {
                    gc.setFilter(FF.and(previous, granuleFilter));
                }
            }
        } 
        
        return operation;
    }
}
