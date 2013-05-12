package org.geoserver.wcs2_0.eo.response;

import java.io.IOException;

import org.geoserver.catalog.CoverageInfo;
import org.geoserver.security.decorators.DecoratingCoverageInfo;
import org.geotools.coverage.grid.io.StructuredGridCoverage2DReader;
import org.geotools.factory.Hints;
import org.opengis.coverage.grid.GridCoverageReader;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.util.ProgressListener;

/**
 * Builds a view of the coverage that contains only the specified coverage
 * 
 * @author Andrea Aime - GeoSolutions
 * 
 */
class GranuleCoverageInfo extends DecoratingCoverageInfo {

    private SimpleFeature feature;

    public GranuleCoverageInfo(CoverageInfo delegate, SimpleFeature feature) {
        super(delegate);
        this.feature = feature;
    }

    @Override
    public GridCoverageReader getGridCoverageReader(ProgressListener listener, Hints hints)
            throws IOException {
        StructuredGridCoverage2DReader reader = (StructuredGridCoverage2DReader) super.getGridCoverageReader(listener, hints);
        return new SingleGranuleGridCoverageReader(reader, feature);
    }
}
