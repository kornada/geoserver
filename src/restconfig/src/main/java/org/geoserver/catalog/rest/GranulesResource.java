/* Copyright (c) 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.catalog.rest;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.rest.RestletException;
import org.geoserver.rest.format.DataFormat;
import org.geoserver.rest.format.StreamDataFormat;
import org.geotools.GML;
import org.geotools.GML.Version;
import org.geotools.coverage.grid.io.GranuleSource;
import org.geotools.coverage.grid.io.GranuleStore;
import org.geotools.coverage.grid.io.StructuredGridCoverage2DReader;
import org.geotools.data.Query;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.filter.text.ecql.ECQL;
import org.geotools.geojson.feature.FeatureJSON;
import org.opengis.filter.Filter;
import org.restlet.Context;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;

/**
 * REturns a filterable/pageable view of the granules in a particular coverage of a structured grid
 * reader
 * 
 * @author Andrea Aime - GeoSolutions
 * 
 */
public class GranulesResource extends CatalogResourceBase {

    private CoverageInfo coverage;

    public GranulesResource(Context context, Request request, Response response, Catalog catalog,
            CoverageInfo coverage) {
        super(context, request, response, SimpleFeatureCollection.class, catalog);
        this.coverage = coverage;
    }
    
    @Override
    public boolean allowDelete() {
        try {
            StructuredGridCoverage2DReader reader = (StructuredGridCoverage2DReader) coverage
                    .getGridCoverageReader(null, null);
            return !reader.isReadOnly();
        } catch(IOException e) {
            throw new RestletException("Failed to determine if the reader index can be written to", 
                    Status.SERVER_ERROR_INTERNAL, e);
        }
    }
    
    @Override
    protected void handleObjectDelete() throws Exception {
        String nativeCoverageName = coverage.getNativeCoverageName();
        StructuredGridCoverage2DReader reader = (StructuredGridCoverage2DReader) coverage
                .getGridCoverageReader(null, null);

        // do we have a filter?
        Form form = getRequest().getResourceRef().getQueryAsForm();
        Filter filter = parseFilter(form);
        if(filter == null) {
            filter = Filter.INCLUDE;
        }
        
        // perform the delete
        GranuleStore store = (GranuleStore) reader.getGranules(nativeCoverageName, false);
        store.removeGranules(filter);
    }
    
    @Override
    protected Object handleObjectGet() throws Exception {
        String nativeCoverageName = coverage.getNativeCoverageName();
        StructuredGridCoverage2DReader reader = (StructuredGridCoverage2DReader) coverage
                .getGridCoverageReader(null, null);

        GranuleSource source = reader.getGranules(nativeCoverageName, true);

        // build the query
        Query q = new Query(Query.ALL);

        // ... filter
        Form form = getRequest().getResourceRef().getQueryAsForm();
        Filter filter = parseFilter(form);
        if(filter != null) {
            q.setFilter(filter);
        }

        // ... offset
        Integer offset = getNonNegativeVariable(form, "offset", true);
        q.setStartIndex(offset);

        // ... limit
        Integer limit = getNonNegativeVariable(form, "limit", true);
        if (limit != null) {
            q.setMaxFeatures(limit);
        }

        return source.getGranules(q);
    }

    private Filter parseFilter(Form form) {
        String cql = form.getFirstValue("filter");
        if (cql != null) {
            try {
                return ECQL.toFilter(cql);
            } catch (CQLException e) {
                throw new RestletException("Invalid cql syntax: " + e.getMessage(),
                        Status.CLIENT_ERROR_BAD_REQUEST);
            }
        }
        
        return null;
    }

    private Integer getNonNegativeVariable(Form form, String variable, boolean allowZero) {
        String offset = form.getFirstValue(variable);
        if (offset != null) {
            try {
                int value = Integer.parseInt(offset);
                if (value < 0 || (!allowZero && value == 0)) {
                    throw new RestletException("Invalid " + variable + " value, : " + value,
                            Status.CLIENT_ERROR_BAD_REQUEST);
                }
                return value;
            } catch (NumberFormatException e) {
                throw new RestletException("Invalid " + variable
                        + " value, must be a positive integer: " + e.getMessage(),
                        Status.CLIENT_ERROR_BAD_REQUEST);
            }
        }

        return null;
    }

    /**
     * Creates the list of formats used to serialize and de-serialize instances of the target
     * object.
     * <p>
     * Subclasses may override or extend this method to customize the supported formats. By default
     * this method supports html, xml, and json.
     * </p>
     * 
     * @see #createHTMLFormat()
     * @see #createXMLFormat()
     * @see #createJSONFormat()
     */
    protected List<DataFormat> createSupportedFormats(Request request, Response response) {
        List<DataFormat> formats = new ArrayList<DataFormat>();
        formats.add(new FeaturesJSONFormat());
        formats.add(new FeaturesGMLFormat());

        return formats;
    }

    /**
     * A format for JSON features
     * 
     * @author Andrea Aime - GeoSolutions
     * 
     */
    public class FeaturesJSONFormat extends StreamDataFormat {
        protected FeaturesJSONFormat() {
            super(MediaType.APPLICATION_JSON);
        }

        @Override
        protected Object read(InputStream in) throws IOException {
            throw new UnsupportedOperationException("Can't read JSON documents yet");
        }

        @Override
        protected void write(Object object, OutputStream out) throws IOException {
            SimpleFeatureCollection features = (SimpleFeatureCollection) object;
            final FeatureJSON json = new FeatureJSON();
            boolean geometryless = features.getSchema().getGeometryDescriptor() == null;
            json.setEncodeFeatureCollectionBounds(!geometryless);
            json.setEncodeFeatureCollectionCRS(!geometryless);
            json.writeFeatureCollection(features, out);
        }

    }

    /**
     * A format for GML2 features
     * 
     * @author Andrea Aime - GeoSolutions
     * 
     */
    public class FeaturesGMLFormat extends StreamDataFormat {
        protected FeaturesGMLFormat() {
            super(MediaType.TEXT_XML);
        }

        @Override
        protected Object read(InputStream in) throws IOException {
            throw new UnsupportedOperationException("Can't read GML documents yet");
        }

        @Override
        protected void write(Object object, OutputStream out) throws IOException {
            SimpleFeatureCollection features = (SimpleFeatureCollection) object;
            GML gml = new GML(Version.WFS1_0);
            gml.setNamespace("gsr", "http://www.geoserver.org/rest");
            gml.encode(out, features);
        }

    }
}
