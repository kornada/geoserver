package org.geoserver.wcs2_0.response;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import net.opengis.wcs20.GetCoverageType;

import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.util.ReaderDimensionsAccessor;
import org.geoserver.wcs2_0.GridCoverageRequest;
import org.geotools.coverage.grid.io.DimensionDescriptor;
import org.geotools.coverage.grid.io.GranuleSource;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.coverage.grid.io.StructuredGridCoverage2DReader;
import org.geotools.data.Query;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.filter.SortByImpl;
import org.geotools.geometry.GeneralEnvelope;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.resources.coverage.FeatureUtilities;
import org.geotools.util.DateRange;
import org.geotools.util.NumberRange;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.expression.Literal;
import org.opengis.filter.expression.PropertyName;
import org.opengis.filter.sort.SortBy;
import org.opengis.filter.sort.SortOrder;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Polygon;

/**
 * A class which takes care of handling default values for unspecified dimensions (if needed).
 * 
 * @author Daniele Romagnoli, GeoSolutions SAS
 *
 */
public class WCSDefaultValuesHelper {

    FilterFactory2 FF = CommonFactoryFinder.getFilterFactory2();

    private String coverageName;

    private GridCoverage2DReader reader;

    private GetCoverageType request;

    private ReaderDimensionsAccessor accessor;

    public WCSDefaultValuesHelper(GridCoverage2DReader reader, ReaderDimensionsAccessor accessor, GetCoverageType request, String coverageName) throws IOException {
        super();
        this.accessor = accessor == null ? new ReaderDimensionsAccessor(reader) : accessor; // Force the creation of an accessor
        this.reader = reader;
        this.request = request;
        this.coverageName = coverageName;
    }

    /**
     * Check the current request and update default values if needed.
     * Return the updated {@link GridCoverageRequest} 
     * 
     * 
     * @param subsettingRequest
     * @return
     * @throws IOException
     */
    public void setDefaults(GridCoverageRequest subsettingRequest) throws IOException {
        // Deal with default values
        final String format = request.getFormat();
        if (format != null && !format.equalsIgnoreCase("NETCDFOutputFormat")) {
            // TODO: Revisit this code and change that Format String. 
            // NetCDFOutputFormat should support multidimensional output format
            // Therefore, no need to set default values

            // For 2D output format, we can setup default values to reduce the number of results
            if (! (reader instanceof StructuredGridCoverage2DReader)) {
                setStandardReaderDefaults(subsettingRequest);
            } else {
               setDefaultsFromStructuredReader(subsettingRequest);
            }
        } 
    }

    /**
     * Set default values by querying a {@link GranuleSource} from {@link StructuredGridCoverage2DReader} in
     * order to update unspecified dimensions values from attributes values obtained from the query.
     * 
     * @param subsettingRequest
     * @return
     * @throws IOException
     */
    private GridCoverageRequest setDefaultsFromStructuredReader(GridCoverageRequest subsettingRequest) throws IOException {

        // Get subsetting request
        DateRange temporalSubset = subsettingRequest.getTemporalSubset();
        NumberRange<?> elevationSubset = subsettingRequest.getElevationSubset();
        Map<String, List> dimensionSubset = subsettingRequest.getDimensionsSubset();
        GeneralEnvelope envelope = subsettingRequest.getSpatialSubset();
        
//        final int specifiedDimensionsSubset = dimensionSubset != null ? dimensionSubset.size() : 0;

        // Casting to StructuredGridCoverage2DReader
        StructuredGridCoverage2DReader structuredReader = (StructuredGridCoverage2DReader) reader;

        // Getting dimension descriptors
        List<DimensionDescriptor> dimensionDescriptors = structuredReader.getDimensionDescriptors(coverageName);

        DimensionDescriptor timeDimension = null;
        DimensionDescriptor elevationDimension = null;
//        List<DimensionDescriptor> additionalDimensions = new ArrayList<DimensionDescriptor>();
//        int dimensions = 0;

        //TODO: Add custom dimension defaults
        // Collect dimension Descriptor info
        for (DimensionDescriptor dimensionDescriptor: dimensionDescriptors) {
            if (dimensionDescriptor.getName().equalsIgnoreCase(ResourceInfo.TIME)) {
                timeDimension = dimensionDescriptor;
            } else if (dimensionDescriptor.getName().equalsIgnoreCase(ResourceInfo.ELEVATION)) {
                elevationDimension = dimensionDescriptor;
            } else {
//                additionalDimensions.add(dimensionDescriptor);
//                dimensions++;
            }
        }
        
        final boolean defaultTimeNeeded = temporalSubset == null && timeDimension != null;
        final boolean defaultElevationNeeded = elevationSubset == null && elevationDimension != null;
//        final boolean defaultCustomDimensionsNeeded = dimensions != specifiedDimensionsSubset;

        // Note that only Slicing is currently supported;
        if (defaultTimeNeeded || defaultElevationNeeded) {

            // Get granules source
            GranuleSource source = structuredReader.getGranules(coverageName, true);

            // Set filtering query matching the specified subsets. 
            Filter finalFilter = setFilters(temporalSubset, elevationSubset, envelope, structuredReader, timeDimension, elevationDimension);
            Query query = new Query();

            // Set sorting order (default Policy is using Max... therefore Descending order)
            sortBy(query, timeDimension, elevationDimension);
            query.setFilter(finalFilter);

            // Get granules from query
            SimpleFeatureCollection granulesCollection = source.getGranules(query);
            SimpleFeatureIterator features = granulesCollection.features();
            try {
                if (features.hasNext()) {
                    SimpleFeature f = features.next();

                    // Setting defaults from the resulting query
                    if (defaultTimeNeeded && timeDimension != null) {
                        temporalSubset = setDefaultTemporalSubset(timeDimension, f);
                        subsettingRequest.setTemporalSubset(temporalSubset);
                    }
                    if (defaultElevationNeeded && elevationDimension != null) {
                        elevationSubset = setDefaultElevationSubset(elevationDimension, f);
                        subsettingRequest.setElevationSubset(elevationSubset);
                    }
                }
            } finally {
                if (features != null) {
                    features.close();
                }
            }
        }
        
        return subsettingRequest;
    }

    /**
     * Set default elevation value from the provided feature
     * @param elevationDimension
     * @param f
     * @return
     */
    private NumberRange<?> setDefaultElevationSubset(DimensionDescriptor elevationDimension, SimpleFeature f) {
        final String start = elevationDimension.getStartAttribute();
        final String end = elevationDimension.getEndAttribute();
        Number startTime = (Number) f.getAttribute(start);
        Number endTime = startTime;
        if (end != null) {
            endTime = (Number) f.getAttribute(end);
        }
        return new NumberRange(startTime.getClass(), startTime, endTime);
    }

    /**
     * Set default time value from the provided feature
     * @param timeDimension
     * @param f
     * @return
     */
    private DateRange setDefaultTemporalSubset(DimensionDescriptor timeDimension, SimpleFeature f) {
        final String start = timeDimension.getStartAttribute();
        final String end = timeDimension.getEndAttribute();
        Date startTime = (Date) f.getAttribute(start);
        Date endTime = startTime;
        if (end != null) {
            endTime = (Date) f.getAttribute(end);
        }
        return new DateRange(startTime, endTime);
    }

    /**
     * Current policy is to use the max value as default. Setting DESCENDING order to get the max value of attributes
     * @param query
     * @param timeDimension
     * @param elevationDimension
     */
    private void sortBy(Query query, DimensionDescriptor timeDimension, DimensionDescriptor elevationDimension) {
        final List<SortBy> clauses = new ArrayList<SortBy>();
        // TODO: Check sortBy clause is supported
        if (timeDimension != null) {
            clauses.add(new SortByImpl(FeatureUtilities.DEFAULT_FILTER_FACTORY.property(timeDimension.getStartAttribute()),
                    SortOrder.DESCENDING));
        }
        if (elevationDimension != null) {
            clauses.add(new SortByImpl(FeatureUtilities.DEFAULT_FILTER_FACTORY.property(elevationDimension.getStartAttribute()),
                    SortOrder.DESCENDING));
        }
        final SortBy[] sb = clauses.toArray(new SortBy[] {});
        query.setSortBy(sb);
        
    }

    /** 
     * Setup filter query on top of specified subsets values to return only granules satisfying the specified conditions.
     * @param temporalSubset
     * @param elevationSubset
     * @param envelope 
     * @param reader 
     * @param timeDimension
     * @param elevationDimension
     * @return
     * @throws IOException 
     */
    private Filter setFilters(DateRange temporalSubset, NumberRange<?> elevationSubset,
            GeneralEnvelope envelope, StructuredGridCoverage2DReader reader, DimensionDescriptor timeDimension, DimensionDescriptor elevationDimension) 
                    throws IOException {
        List<Filter> filters = new ArrayList<Filter>();

        // Setting temporal filter
        Filter timeFilter = temporalSubset == null && timeDimension == null ? null
                : setTimeFilter(temporalSubset, timeDimension.getStartAttribute(),
                        timeDimension.getEndAttribute());

        // Setting elevation filter
        Filter elevationFilter = elevationSubset == null && elevationDimension == null ? null
                : setElevationFilter(elevationSubset,
                        elevationDimension.getStartAttribute(),
                        elevationDimension.getEndAttribute());

        Filter envelopeFilter = null;
        // setting envelope filter
        if (envelope != null) {
            Polygon polygon = JTS.toGeometry(new ReferencedEnvelope(envelope));
            GeometryDescriptor geom = reader.getGranules(coverageName, true).getSchema().getGeometryDescriptor();
            PropertyName geometryProperty = FF.property(geom.getLocalName());
            Geometry nativeCRSPolygon;
            try {
                nativeCRSPolygon = JTS.transform(polygon, CRS.findMathTransform(DefaultGeographicCRS.WGS84,
                                reader.getCoordinateReferenceSystem()));
                Literal polygonLiteral = FF.literal(nativeCRSPolygon);
                // TODO: Check that geom operation. Should I do intersection or containment check?
                // if(overlaps) {
                // envelopeFilter = FF.intersects(geometryProperty, polygonLiteral);
                // } else {
                envelopeFilter = FF.within(geometryProperty, polygonLiteral);
                // }
            } catch (Exception e) {
                throw new IOException(e);
            }
        }

        // Updating filters 
        if (elevationFilter != null) {
            filters.add(elevationFilter);
        }
        if (timeFilter != null) {
            filters.add(timeFilter);
        }
        if (envelopeFilter != null) {
            filters.add(envelopeFilter);
        }

        // Merging all filters
        Filter finalFilter = FF.and(filters);
        return finalFilter;
    }

    /**
     * Set a {@link Filter} based on the specified time subset, or null if missing.
     * @param timeRange
     * @param start
     * @param end
     * @return
     */
    private Filter setTimeFilter(DateRange timeRange, String start, String end) {
        if (timeRange != null) {
            if(end == null) {
                // single value time
                return betweenFilter(start, timeRange.getMinValue(), timeRange.getMaxValue());
            } else {
                return rangeFilter(start, end, timeRange.getMinValue(), timeRange.getMaxValue());
            }
        }
        return null;
    }

    /**
     * Set a {@link Filter} based on the specified elevation subset, or null if missing.
     * @param elevationSubset
     * @param start
     * @param end
     * @return
     */
    private Filter setElevationFilter(NumberRange elevationSubset, String start, String end) {
        if (elevationSubset != null) {
            if(end == null) {
                // single value elevation
                return betweenFilter(start, elevationSubset.getMinValue(), elevationSubset.getMaxValue());
            } else {
                return rangeFilter(start, end, elevationSubset.getMinValue(), elevationSubset.getMaxValue());
            }
        }
        return null;
    }

    /**
     * A simple filter making sure a property is contained between minValue and maxValue
     * @param start
     * @param minValue
     * @param maxValue
     * @return
     */
    private Filter betweenFilter(String start, Object minValue, Object maxValue) {
        return FF.between(FF.property(start), FF.literal(minValue), FF.literal(maxValue));
    }

    private Filter rangeFilter(String start, String end, Object minValue, Object maxValue) {
//      if(overlaps) {
//      Filter f1 = FF.lessOrEqual(FF.property(start), FF.literal(maxValue));
//      Filter f2 = FF.greaterOrEqual(FF.property(end), FF.literal(minValue));
//      timeFilter = FF.and(Arrays.asList(f1, f2));
//    }

        Filter f1 = FF.greaterOrEqual(FF.property(start), FF.literal(minValue));
        Filter f2 = FF.lessOrEqual(FF.property(end), FF.literal(maxValue));
        return FF.and(Arrays.asList(f1, f2));
    }

    /** 
     * Set default values for the standard reader case (no DimensionsDescriptor available) 
     * 
     * @param subsettingRequest
     * @return
     * @throws IOException
     */
    private GridCoverageRequest setStandardReaderDefaults(GridCoverageRequest subsettingRequest) throws IOException {
        DateRange temporalSubset = subsettingRequest.getTemporalSubset();
        NumberRange<?> elevationSubset = subsettingRequest.getElevationSubset();
        Map<String, List> dimensionSubset = subsettingRequest.getDimensionsSubset();
        
        // Reader is not a StructuredGridCoverage2DReader instance. Set max values as default ones.
        if (temporalSubset == null) {
            // use "current" as the default
            Date maxTime = accessor.getMaxTime();
            if (maxTime != null) {
                temporalSubset = new DateRange(maxTime, maxTime);
            }
        }

        if (elevationSubset == null) {
            // use "current" as the default
            Number maxElevation = accessor.getMaxElevation();
            if (maxElevation != null) {
                elevationSubset = new NumberRange(maxElevation.getClass(), maxElevation, maxElevation);
            }
        }
        
        //TODO: set dimensionSubset
        subsettingRequest.setTemporalSubset(temporalSubset);
        subsettingRequest.setElevationSubset(elevationSubset);
        return subsettingRequest;
    }
    

}