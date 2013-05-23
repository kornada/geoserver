/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs2_0.eo.response;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.opengis.wcs20.DescribeEOCoverageSetType;
import net.opengis.wcs20.Section;

import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.wcs.WCSInfo;
import org.geoserver.wcs2_0.GetCoverage;
import org.geoserver.wcs2_0.eo.EOCoverageResourceCodec;
import org.geoserver.wcs2_0.eo.WCSEOMetadata;
import org.geoserver.wcs2_0.exception.WCS20Exception;
import org.geoserver.wcs2_0.response.WCS20DescribeCoverageTransformer;
import org.geoserver.wcs2_0.response.WCS20DescribeCoverageTransformer.WCS20DescribeCoverageTranslator;
import org.geoserver.wcs2_0.response.WCSTimeDimensionHelper;
import org.geoserver.wcs2_0.util.EnvelopeAxesLabelsMapper;
import org.geotools.coverage.grid.io.GranuleSource;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.coverage.grid.io.StructuredGridCoverage2DReader;
import org.geotools.data.DataUtilities;
import org.geotools.data.Query;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.store.MaxFeaturesFeatureCollection;
import org.geotools.geometry.GeneralEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.referencing.CRS.AxisOrder;
import org.geotools.util.logging.Logging;
import org.geotools.xml.transform.TransformerBase;
import org.geotools.xml.transform.Translator;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.helpers.AttributesImpl;

/**
 * Encodes a DescribeEOCoverageSet response
 * 
 * @author Andrea Aime - GeoSolutions
 */
public class DescribeEOCoverageSetTransformer extends TransformerBase {

    static Logger LOGGER = Logging.getLogger(WCS20DescribeEOCoverageSetTranslator.class);
    
    EOCoverageResourceCodec codec;

    WCS20DescribeCoverageTransformer dcTransformer;

    EnvelopeAxesLabelsMapper envelopeAxisMapper;

    WCSInfo wcs;

    public DescribeEOCoverageSetTransformer(WCSInfo wcs, EOCoverageResourceCodec codec,
            EnvelopeAxesLabelsMapper envelopeAxesMapper,
            WCS20DescribeCoverageTransformer dcTransformer) {
        this.wcs = wcs;
        this.codec = codec;
        this.envelopeAxisMapper = envelopeAxesMapper;
        this.dcTransformer = dcTransformer;
        setIndentation(2);
    }

    @Override
    public Translator createTranslator(ContentHandler handler) {
        WCS20DescribeCoverageTranslator dcTranslator = dcTransformer.createTranslator(handler);
        return new WCS20DescribeEOCoverageSetTranslator(handler, dcTranslator);
    }

    public class WCS20DescribeEOCoverageSetTranslator extends TranslatorSupport {

        private WCS20DescribeCoverageTranslator dcTranslator;

        public WCS20DescribeEOCoverageSetTranslator(ContentHandler handler,
                WCS20DescribeCoverageTranslator dcTranslator) {
            super(handler, null, null);
            this.dcTranslator = dcTranslator;
        }

        /**
         * Encode the object.
         */
        @Override
        public void encode(Object o) throws IllegalArgumentException {
            DescribeEOCoverageSetType dcs = (DescribeEOCoverageSetType) o;

            List<CoverageInfo> coverages = getCoverages(dcs);
            List<CoverageGranules> coverageGranules = getCoverageGranules(coverages);
            int granuleCount = getGranuleCount(coverageGranules);
            Integer maxCoverages = getMaxCoverages(dcs);
            int returned = granuleCount < maxCoverages ? granuleCount : maxCoverages;

            String eoSchemaLocation = ResponseUtils.buildSchemaURL(dcs.getBaseUrl(),
                    "wcseo/1.0/wcsEOAll.xsd");
            Attributes atts = atts(
                    "xmlns:eop",
                    "http://www.opengis.net/eop/2.0", //
                    "xmlns:gml",
                    "http://www.opengis.net/gml/3.2", //
                    "xmlns:wcsgs",
                    "http://www.geoserver.org/wcsgs/2.0", //
                    "xmlns:gmlcov", "http://www.opengis.net/gmlcov/1.0", "xmlns:om",
                    "http://www.opengis.net/om/2.0", "xmlns:swe", "http://www.opengis.net/swe/2.0",
                    "xmlns:wcs", "http://www.opengis.net/wcs/2.0", "xmlns:wcseo",
                    "http://www.opengis.net/wcseo/1.0", "xmlns:xlink",
                    "http://www.w3.org/1999/xlink", "xmlns:xsi",
                    "http://www.w3.org/2001/XMLSchema-instance", "numberMatched",
                    String.valueOf(granuleCount), "numberReturned", String.valueOf(returned),
                    "xsi:schemaLocation", "http://www.opengis.net/wcseo/1.0 " + eoSchemaLocation);

            start("wcseo:EOCoverageSetDescription", atts);

            
            List<CoverageGranules> reducedGranules = applyMaxCoverages(coverageGranules, maxCoverages);
            
            boolean allSections = dcs.getSections() == null || dcs.getSections().getSection() == null ||
                    dcs.getSections().getSection().contains(Section.ALL);
            if(allSections || dcs.getSections().getSection().contains(Section.COVERAGEDESCRIPTIONS)) {
                handleCoverageDescriptions(reducedGranules);
            }
            if(allSections || dcs.getSections().getSection().contains(Section.DATASETSERIESDESCRIPTIONS)) {
                handleDatasetSeriesDescriptions(coverages);
            }

            end("wcseo:EOCoverageSetDescription");
        }

        /**
         * Returns the max number of coverages to return, if any (null otherwise) 
         * @param dcs
         * @return
         */
        private Integer getMaxCoverages(DescribeEOCoverageSetType dcs) {
            if(dcs.getCount() > 0) {
                return dcs.getCount();
            } 
            
            // fall back on the the default value, it's ok if it's null
            return wcs.getMetadata().get(WCSEOMetadata.COUNT_DEFAULT.key, Integer.class);
        }

        private List<CoverageGranules> applyMaxCoverages(List<CoverageGranules> coverageGranules,
                Integer maxCoverages) {
            List<CoverageGranules> result = new ArrayList<DescribeEOCoverageSetTransformer.CoverageGranules>();
            for (CoverageGranules cg : coverageGranules) {
                int size = cg.granules.size();
                if(size > maxCoverages) {
                    cg.granules = DataUtilities.simple(new MaxFeaturesFeatureCollection<SimpleFeatureType, SimpleFeature>(cg.granules, maxCoverages));
                }
                result.add(cg);
                maxCoverages -= size;
                if(maxCoverages <= 0) {
                    break;
                }
            }
            
            return result;
        }

        private void handleDatasetSeriesDescriptions(List<CoverageInfo> coverages) {
            start("wcseo:DatasetSeriesDescriptions");
            for (CoverageInfo ci : coverages) {
                String datasetId = codec.getDatasetName(ci);
                start("wcseo:DatasetSeriesDescription", atts("gml:id", datasetId));

                try {
                    GridCoverage2DReader reader = (GridCoverage2DReader) ci.getGridCoverageReader(
                            null, null);

                    // encode the bbox
                    encodeDatasetBounds(ci, reader);
                    
                    // the dataset series id
                    element("wcseo:DatasetSeriesId", datasetId);
                    
                    // encode the time
                    DimensionInfo time = ci.getMetadata().get(ResourceInfo.TIME, DimensionInfo.class);
                    WCSTimeDimensionHelper timeHelper = new WCSTimeDimensionHelper(time, reader, datasetId);
                    dcTranslator.encodeTimePeriod(timeHelper.getBeginPosition(), timeHelper.getEndPosition(), datasetId + "_timeperiod", null, null);

                    end("wcseo:DatasetSeriesDescription");
                } catch (IOException e) {
                    throw new WCS20Exception("Failed to build the description for dataset series "
                            + codec.getDatasetName(ci), e);
                }
            }
            end("wcseo:DatasetSeriesDescriptions");
        }

        private void encodeDatasetBounds(CoverageInfo ci, GridCoverage2DReader reader)
                throws IOException {
            // get the crs and look for an EPSG code
            final CoordinateReferenceSystem crs = ci.getCRS();
            GeneralEnvelope envelope = reader.getOriginalEnvelope();
            List<String> axesNames = envelopeAxisMapper.getAxesNames(envelope, true);

            // lookup EPSG code
            Integer EPSGCode = null;
            try {
                EPSGCode = CRS.lookupEpsgCode(crs, false);
            } catch (FactoryException e) {
                throw new IllegalStateException("Unable to lookup epsg code for this CRS:"
                        + crs, e);
            }
            if (EPSGCode == null) {
                throw new IllegalStateException("Unable to lookup epsg code for this CRS:"
                        + crs);
            }
            final String srsName = GetCoverage.SRS_STARTER + EPSGCode;
            // handle axes swap for geographic crs
            final boolean axisSwap = CRS.getAxisOrder(crs).equals(AxisOrder.EAST_NORTH);

            final StringBuilder builder = new StringBuilder();
            for (String axisName : axesNames) {
                builder.append(axisName).append(" ");
            }
            String axesLabel = builder.substring(0, builder.length() - 1);
            dcTranslator.handleBoundedBy(envelope, axisSwap, srsName, axesLabel, null);
        }

        private void handleCoverageDescriptions(List<CoverageGranules> coverageGranules) {
            start("wcs:CoverageDescriptions");
            for (CoverageGranules cg : coverageGranules) {
                SimpleFeatureIterator features = cg.granules.features();
                try {
                    while(features.hasNext()) {
                        SimpleFeature f = features.next();
                        String granuleId = codec.getGranuleId(cg.coverage, f.getID());
                        dcTranslator.handleCoverageDescription(granuleId, new GranuleCoverageInfo(cg.coverage, f));
                    }
                } finally {
                    if(features != null) {
                        features.close();
                    }
                }
            }
            end("wcs:CoverageDescriptions");
        }


        private int getGranuleCount(List<CoverageGranules> granules) {
            int sum = 0;
            for (CoverageGranules cg : granules) {
                sum += cg.granules.size();
            }

            return sum;
        }

        private List<CoverageInfo> getCoverages(DescribeEOCoverageSetType dcs) {
            List<CoverageInfo> results = new ArrayList<CoverageInfo>();
            for (String id : dcs.getEoId()) {
                CoverageInfo ci = codec.getDatasetCoverage(id);
                if (ci == null) {
                    throw new IllegalArgumentException(
                            "The dataset id is invalid, should have been checked earlier?");
                }
                results.add(ci);
            }

            return results;
        }

        private List<CoverageGranules> getCoverageGranules(List<CoverageInfo> coverages) {
            List<CoverageGranules> results = new ArrayList<CoverageGranules>();
            for (CoverageInfo ci : coverages) {
                GranuleSource source = null;
                try {
                    StructuredGridCoverage2DReader reader = (StructuredGridCoverage2DReader) ci
                            .getGridCoverageReader(null, null);
                    String name = ci.getNativeCoverageName() != null ? ci.getNativeCoverageName() : reader.getGridCoverageNames()[0];
                    source = reader.getGranules(name, true);
                    // TODO : filter based on dimension trimming
                    SimpleFeatureCollection collection = source.getGranules(Query.ALL);

                    CoverageGranules granules = new CoverageGranules(ci, reader, collection);
                    results.add(granules);
                } catch (IOException e) {
                    throw new WCS20Exception("Failed to load the coverage granules for covearge "
                            + ci.prefixedName(), e);
                } finally {
                    try {
                        if(source != null) {
                            source.dispose();
                        }
                    } catch (IOException e) {
                        LOGGER.log(Level.FINE, "Failed to dispose granule source", e);
                    }
                }
            }

            return results;
        }

        Attributes atts(String... atts) {
            AttributesImpl attributes = new AttributesImpl();
            for (int i = 0; i < atts.length; i += 2) {
                attributes.addAttribute(null, atts[i], atts[i], null, atts[i + 1]);
            }
            return attributes;
        }
    }

    static class CoverageGranules {
        CoverageInfo coverage;

        StructuredGridCoverage2DReader reader;

        SimpleFeatureCollection granules;

        public CoverageGranules(CoverageInfo coverage, StructuredGridCoverage2DReader reader,
                SimpleFeatureCollection granules) {
            this.coverage = coverage;
            this.reader = reader;
            this.granules = granules;
        }

    }

}
