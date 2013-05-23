/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs2_0.eo.response;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Set;

import javax.media.jai.ImageLayout;

import org.geoserver.util.ISO8601Formatter;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.io.DimensionDescriptor;
import org.geotools.coverage.grid.io.GranuleSource;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.coverage.grid.io.HarvestedFile;
import org.geotools.coverage.grid.io.OverviewPolicy;
import org.geotools.coverage.grid.io.StructuredGridCoverage2DReader;
import org.geotools.factory.Hints;
import org.geotools.geometry.GeneralEnvelope;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.coverage.grid.Format;
import org.opengis.coverage.grid.GridEnvelope;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.datum.PixelInCell;
import org.opengis.referencing.operation.MathTransform;

import com.vividsolutions.jts.geom.Geometry;

/**
 * Provides a view of a single granule to the DescribeCoverage encoder (to be used in 
 * DescribeOECoverageSet response)
 * 
 * @author Andrea Aime - GeoSolutions
 */
public class SingleGranuleGridCoverageReader implements StructuredGridCoverage2DReader {

    private StructuredGridCoverage2DReader reader;

    private SimpleFeature feature;

    String featureTime;

    GeneralEnvelope granuleEnvelope;

    ISO8601Formatter formatter = new ISO8601Formatter();

    public SingleGranuleGridCoverageReader(StructuredGridCoverage2DReader reader,
            SimpleFeature feature) {
        this.reader = reader;
        this.feature = feature;
        this.featureTime = formatter.format(lookupFeatureTime());
        Geometry featureGeometry = lookupFeatureGeometry();
        ReferencedEnvelope re = new ReferencedEnvelope(featureGeometry.getEnvelopeInternal(),
                reader.getCoordinateReferenceSystem());
        this.granuleEnvelope = new GeneralEnvelope(re);
    }

    private Geometry lookupFeatureGeometry() {
        return (Geometry) feature.getDefaultGeometry();
    }

    private Date lookupFeatureTime() {
        for (Object value : feature.getAttributes()) {
            if (value instanceof Date) {
                return (Date) value;
            }
        }

        throw new IllegalStateException("The feature does not have a date!");
    }

    public Format getFormat() {
        return reader.getFormat();
    }

    public Object getSource() {
        return reader.getSource();
    }

    public String[] getMetadataNames() throws IOException {
        return reader.getMetadataNames();
    }

    public GranuleSource getGranules(String coverageName, boolean readOnly) throws IOException,
            UnsupportedOperationException {
        return reader.getGranules(coverageName, readOnly);
    }

    public String[] getMetadataNames(String coverageName) throws IOException {
        return reader.getMetadataNames(coverageName);
    }

    public boolean isReadOnly() {
        return reader.isReadOnly();
    }

    public void createCoverage(String coverageName, SimpleFeatureType schema) throws IOException,
            UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    public boolean removeCoverage(String coverageName) throws IOException,
            UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    public String getMetadataValue(String name) throws IOException {
        if (GridCoverage2DReader.TIME_DOMAIN.equals(name)
                || GridCoverage2DReader.TIME_DOMAIN_MAXIMUM.equals(name)
                || GridCoverage2DReader.TIME_DOMAIN_MINIMUM.equals(name)) {
            return featureTime;
        }
        return reader.getMetadataValue(name);
    }

    public List<HarvestedFile> harvest(String defaultTargetCoverage, File source, Hints hints)
            throws IOException, UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    public String getMetadataValue(String coverageName, String name) throws IOException {
        throw new UnsupportedOperationException();
    }

    public String[] listSubNames() throws IOException {
        return reader.listSubNames();
    }

    public GeneralEnvelope getOriginalEnvelope() {
        return granuleEnvelope;
    }

    public GeneralEnvelope getOriginalEnvelope(String coverageName) {
        throw new UnsupportedOperationException();
    }

    public String[] getGridCoverageNames() throws IOException {
        return reader.getGridCoverageNames();
    }

    public CoordinateReferenceSystem getCoordinateReferenceSystem() {
        return reader.getCoordinateReferenceSystem();
    }

    public int getGridCoverageCount() throws IOException {
        return reader.getGridCoverageCount();
    }

    public CoordinateReferenceSystem getCoordinateReferenceSystem(String coverageName) {
        return reader.getCoordinateReferenceSystem(coverageName);
    }

    public String getCurrentSubname() throws IOException {
        return reader.getCurrentSubname();
    }

    public GridEnvelope getOriginalGridRange() {
        return reader.getOriginalGridRange();
    }

    public boolean hasMoreGridCoverages() throws IOException {
        return reader.hasMoreGridCoverages();
    }

    public GridEnvelope getOriginalGridRange(String coverageName) {
        return reader.getOriginalGridRange(coverageName);
    }

    public MathTransform getOriginalGridToWorld(PixelInCell pixInCell) {
        return reader.getOriginalGridToWorld(pixInCell);
    }

    public MathTransform getOriginalGridToWorld(String coverageName, PixelInCell pixInCell) {
        return reader.getOriginalGridToWorld(coverageName, pixInCell);
    }

    public GridCoverage2D read(GeneralParameterValue[] parameters) throws IOException {
        return reader.read(parameters);
    }

    public GridCoverage2D read(String coverageName, GeneralParameterValue[] parameters)
            throws IOException {
        return reader.read(coverageName, parameters);
    }

    public void skip() throws IOException {
        reader.skip();
    }

    public void dispose() throws IOException {
        reader.dispose();
    }

    public Set<ParameterDescriptor<List>> getDynamicParameters() throws IOException {
        return reader.getDynamicParameters();
    }

    public Set<ParameterDescriptor<List>> getDynamicParameters(String coverageName)
            throws IOException {
        return reader.getDynamicParameters(coverageName);
    }

    public double[] getReadingResolutions(OverviewPolicy policy, double[] requestedResolution)
            throws IOException {
        return reader.getReadingResolutions(policy, requestedResolution);
    }

    public double[] getReadingResolutions(String coverageName, OverviewPolicy policy,
            double[] requestedResolution) throws IOException {
        return reader.getReadingResolutions(coverageName, policy, requestedResolution);
    }

    public int getNumOverviews() {
        return reader.getNumOverviews();
    }

    public int getNumOverviews(String coverageName) {
        return reader.getNumOverviews(coverageName);
    }

    public ImageLayout getImageLayout() throws IOException {
        throw new UnsupportedOperationException();
    }

    public ImageLayout getImageLayout(String coverageName) throws IOException {
        throw new UnsupportedOperationException();
    }

    public double[][] getResolutionLevels() throws IOException {
        return reader.getResolutionLevels();
    }

    public double[][] getResolutionLevels(String coverageName) throws IOException {
        return reader.getResolutionLevels(coverageName);
    }

    @Override
    public List<DimensionDescriptor> getDimensionDescriptors(String coverageName) {
        return reader.getDimensionDescriptors(coverageName);
    }

}
