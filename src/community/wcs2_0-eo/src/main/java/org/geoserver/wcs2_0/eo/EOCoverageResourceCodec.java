/* Copyright (c) 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs2_0.eo;

import java.io.IOException;
import java.util.logging.Logger;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.platform.ServiceException;
import org.geoserver.wcs2_0.util.NCNameResourceCodec;
import org.geotools.coverage.grid.io.StructuredGridCoverage2DReader;
import org.geotools.util.logging.Logging;

/**
 * Utility class that maps the coverage data sets and child coverage names
 * 
 * @author Andrea Aime - GeoSolutions
 * 
 */
public class EOCoverageResourceCodec {
    private static Logger LOGGER = Logging.getLogger(EOCoverageResourceCodec.class);

    private static final String DATASET_SUFFIX = "_dss";
    
    private static final String GRANULE_SEPARATOR = "_granule_";

    private Catalog catalog;

    public EOCoverageResourceCodec(Catalog catalog) {
        this.catalog = catalog;
    }

    public String getDatasetName(CoverageInfo ci) {
        if (!isValidDataset(ci)) {
            throw new IllegalArgumentException("Specified covearge " + ci.prefixedName()
                    + " is not a valid EO dataset");
        }

        return NCNameResourceCodec.encode(ci) + DATASET_SUFFIX;
    }

    /**
     * Checks if the specified coverage is a valid dataset, e.g., it has the dataset flag enabled
     * and time dimension, and has a structured grid coverage reader backing it
     * 
     * @param ci
     * @return
     */
    public boolean isValidDataset(CoverageInfo ci) {
        Boolean dataset = ci.getMetadata().get(WCSEOMetadata.DATASET.key, Boolean.class);
        DimensionInfo time = ci.getMetadata().get(ResourceInfo.TIME, DimensionInfo.class);
        try {
            boolean structured = ci.getGridCoverageReader(null, null) instanceof StructuredGridCoverage2DReader;
            return dataset != null && dataset && time != null & time.isEnabled()
                    && structured;
        } catch (IOException e) {
            throw new ServiceException("Failed to locate the grid coverage reader for coverage " + ci.prefixedName());
        }
    }

    /**
     * Returns the coverage backed by the provided datasetId
     * 
     * @param datasetId
     * @return the coverage, or null if not found, or if not a coverage
     */
    public CoverageInfo getDatasetCoverage(String datasetId) {
        if (!datasetId.endsWith(DATASET_SUFFIX)) {
            LOGGER.fine("Invalid dataset id " + datasetId + " it does not end with "
                    + DATASET_SUFFIX);
            return null;
        }

        String coverageName = datasetId.substring(0, datasetId.length() - DATASET_SUFFIX.length());
        LayerInfo layer = NCNameResourceCodec.getCoverage(catalog, coverageName);
        if (layer == null) {
            LOGGER.fine("Invalid dataset id " + datasetId + " does not match any published dataset");
            return null;
        }
        CoverageInfo ci = (CoverageInfo) layer.getResource();
        if (!isValidDataset(ci)) {
            LOGGER.fine("Invalid dataset id " + datasetId + " does not match any published dataset");
            return null;
        }

        return ci;
    }

    /**
     * Builds the identifier for a granule inside a coverage
     * @return
     */
    public String getGranuleId(CoverageInfo coverage, String featureId) {
        return NCNameResourceCodec.encode(coverage) + GRANULE_SEPARATOR + featureId;
    }
}
