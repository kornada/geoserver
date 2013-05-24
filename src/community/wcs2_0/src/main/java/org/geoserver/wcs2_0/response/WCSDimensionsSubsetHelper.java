/* Copyright (c) 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs2_0.response;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.opengis.wcs20.DimensionSliceType;
import net.opengis.wcs20.DimensionSubsetType;
import net.opengis.wcs20.DimensionTrimType;
import net.opengis.wcs20.GetCoverageType;

import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.util.ReaderDimensionsAccessor;
import org.geoserver.wcs2_0.exception.WCS20Exception;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.util.DateRange;
import org.geotools.util.NumberRange;
import org.geotools.util.logging.Logging;
import org.geotools.xml.impl.DatatypeConverterImpl;

/**
 * Provides support to deal with dimensions slicing, trimming and values conversions for additional dimensions
 * 
 * TODO: Port timeSubset and elevationSubset code here too
 * @author Daniele Romagnoli - GeoSolutions
 */
public class WCSDimensionsSubsetHelper {

    public static final Set<String> TIME_NAMES = new HashSet<String>();

    public static final Set<String> ELEVATION_NAMES = new HashSet<String>();

    private final static Logger LOGGER = Logging.getLogger(WCSDimensionsHelper.class);

    private final static DatatypeConverterImpl XML_CONVERTER = DatatypeConverterImpl.getInstance();

    private GetCoverageType request;

    private Map<String, DimensionInfo> dimensions;

    private ReaderDimensionsAccessor accessor;

    private DimensionInfo timeDimension;

    private DimensionInfo elevationDimension;

    static {
        TIME_NAMES.add("t");
        TIME_NAMES.add("time");
        TIME_NAMES.add("temporal");
        TIME_NAMES.add("phenomenontime");
        ELEVATION_NAMES.add("elevation"); 
    }

    public WCSDimensionsSubsetHelper(GridCoverage2DReader reader, GetCoverageType request,
            Map<String, DimensionInfo> dimensions) throws IOException {
        this.request = request;
        this.dimensions = dimensions;
        timeDimension = dimensions.get(ResourceInfo.TIME);
        elevationDimension = dimensions.get(ResourceInfo.ELEVATION);
        
        if (timeDimension != null || elevationDimension != null) {
            // Create accessor once and provide it to any method which uses accessor
            // Accessor creation requires metadata initialization. Creating it once, 
            // should be faster.
            accessor = new ReaderDimensionsAccessor(reader);
        }
        
    }

    /**
     * Parse a String as a Date or return null if impossible.
     * @param text
     * @return
     */
    public static Date parseAsDate(String text) {
        try {
            final Date slicePoint = XML_CONVERTER.parseDateTime(text).getTime();
            if (slicePoint != null) {
                return slicePoint;
            }
        } catch (IllegalArgumentException iae) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.fine(text + " can't be parsed as a time");
            }
        }
        return null;
    }
    
    /**
     * Extracts the simplified dimension name, throws exception if the dimension name is empty
     * @param dim
     * @return
     */
    public static String getDimensionName(DimensionSubsetType dim) {
        // get basic information
        String dimension = dim.getDimension(); 

        // remove common prefixes
        if (dimension.startsWith("http://www.opengis.net/def/axis/OGC/0/")) {
            dimension = dimension.substring("http://www.opengis.net/def/axis/OGC/0/".length());
        } else if (dimension.startsWith("http://opengis.net/def/axis/OGC/0/")) {
            dimension = dimension.substring("http://opengis.net/def/axis/OGC/0/".length());
        } else if (dimension.startsWith("http://opengis.net/def/crs/ISO/2004/")) {
            dimension = dimension.substring("http://opengis.net/def/crs/ISO/2004/".length());
        }

        // checks 
        // TODO synonyms on axes labels
        if (dimension == null || dimension.length() <= 0) { 
            throw new WCS20Exception("Empty/invalid axis label provided: " + dim.getDimension(),
                    WCS20Exception.WCS20ExceptionCode.InvalidAxisLabel, "subset");
        }
        return dimension;
    }
    
    /**
     * Parses a date range out of the dimension subsetting directives
     * @param accessor 
     * @param request
     * @param timeDimension
     * @return
     * @throws IOException 
     */
    public DateRange extractTimeSubset() throws IOException {
        DateRange timeSubset = null;
        if (timeDimension != null) {
            for (DimensionSubsetType dim : request.getDimensionSubset()) {
                String dimension = WCSDimensionsSubsetHelper.getDimensionName(dim);
                
                // only care for time dimensions
                if (!TIME_NAMES.contains(dimension.toLowerCase())) {
                    continue;
                }
                
                // did we parse the range already?
                if(timeSubset != null) {
                    throw new WCS20Exception("Time dimension trimming/slicing specified twice in the request",
                            WCS20Exception.WCS20ExceptionCode.InvalidSubsetting, "subset");
                }
    
                // now decide what to do
                if (dim instanceof DimensionTrimType) {
    
                    // TRIMMING
                    final DimensionTrimType trim = (DimensionTrimType) dim;
                    final Date low = XML_CONVERTER.parseDateTime(trim.getTrimLow()).getTime();
                    final Date high = XML_CONVERTER.parseDateTime(trim.getTrimHigh()).getTime();
    
                    // low > high???
                    if (low.compareTo(high) > 0) {
                        throw new WCS20Exception("Low greater than High: " + trim.getTrimLow() 
                                + ", " + trim.getTrimHigh(),
                                WCS20Exception.WCS20ExceptionCode.InvalidSubsetting, "subset");
                    }
    
                    timeSubset = new DateRange(low, high);
                } else if (dim instanceof DimensionSliceType) {
    
                    // SLICING
                    final DimensionSliceType slicing = (DimensionSliceType) dim;
                    final String slicePointS = slicing.getSlicePoint();
                    final Date slicePoint = XML_CONVERTER.parseDateTime(slicePointS).getTime();
    
                    timeSubset = new DateRange(slicePoint, slicePoint);
                } else {
                    throw new WCS20Exception(
                            "Invalid element found while attempting to parse dimension subsetting request: " + dim.getClass()
                            .toString(), WCS20Exception.WCS20ExceptionCode.InvalidSubsetting, "subset");
                }
            }
            
            // right now we don't support trimming
            // TODO: revisit when we have some multidimensional output support
            if(timeSubset != null && !timeSubset.getMinValue().equals(timeSubset.getMaxValue())) {
                throw new WCS20Exception("Trimming on time is not supported at the moment, only slicing is");
            }
            
            // apply nearest neighbor matching on time
            if (timeSubset != null) {
               timeSubset = interpolateTime(timeSubset, accessor);
            }

            if (timeSubset == null) {
                // use "current" as the default
                Date maxTime = accessor.getMaxTime();
                if (maxTime != null) {
                    timeSubset = new DateRange(maxTime, maxTime);
                }
            }
        }
        return timeSubset;
    }

    /**
     * Nearest interpolation against time
     * @param timeSubset
     * @param accessor
     * @return
     * @throws IOException
     */
    private DateRange interpolateTime(DateRange timeSubset, ReaderDimensionsAccessor accessor) throws IOException {
        TreeSet<Object> domain = accessor.getTimeDomain();
        Date slicePoint = timeSubset.getMinValue();
        if(!domainContainsPoint(slicePoint, domain)) {
            // look for the closest time
            Date previous = null;
            Date newSlicePoint = null;
            // for NN matching we don't need the ranges, NN against their extrema will be fine
            TreeSet<Date> domainDates = getDomainDates(domain);
            for (Date curr : domainDates) {
                if(curr.compareTo(slicePoint) > 0) {
                    if(previous == null) {
                        newSlicePoint = curr;
                        break;
                    } else {
                        long diffPrevious = slicePoint.getTime() - previous.getTime();
                        long diffCurr = curr.getTime() - slicePoint.getTime();
                        if(diffCurr > diffPrevious) {
                            newSlicePoint = curr;
                            break;
                        } else {
                            newSlicePoint = previous;
                            break;
                        }
                    }
                } else {
                    previous = curr;
                }
            }
            
            if(newSlicePoint == null) {
                newSlicePoint = previous;
            }
            timeSubset = new DateRange(newSlicePoint, newSlicePoint);
        }
        return timeSubset;
    }
    
    /**
     * Get the domain set as a set of dates.
     * @param domain
     * @return
     */
    private TreeSet<Date> getDomainDates(TreeSet<Object> domain) {
        TreeSet<Date> results = new TreeSet<Date>();
        for (Object item : domain) {
            if(item instanceof Date) {
                Date date = (Date) item;
                results.add(date);
            } else if(item instanceof DateRange) {
                DateRange range = (DateRange) item;
                results.add(range.getMinValue());
                results.add(range.getMaxValue());
            }
        }
        return results;
    }

    /**
     * Check whether the provided domain contains the specified slicePoint.
     * 
     * @param slicePoint the point to be checked (a Date or a Number)
     * @param domain the domain to be scan for containment.
     * @return
     */
    private boolean domainContainsPoint(final Object slicePoint, final TreeSet<Object> domain) {
        // cannot use this...
        //        if(domain.contains(slicePoint)) {
        //            return true;
        //        }
        
        // check date ranges for containment
        if (slicePoint instanceof Date) {
            Date sliceDate = (Date) slicePoint;
            for (Object curr : domain) {
                if(curr instanceof Date) {
                    Date date = (Date) curr;
                    int result = date.compareTo(sliceDate);
                    if(result > 0) {
                        return false;
                    } else if(result == 0) {
                        return true;
                    }
                } else if(curr instanceof DateRange) {
                    DateRange range = (DateRange) curr;
                    if(range.contains(sliceDate)) {
                        return true;
                    } else if(range.getMaxValue().compareTo(sliceDate) < 0) {
                        return false;
                    }
                }
            }
        } else if (slicePoint instanceof Number) {
            //TODO: Should we check for other data types?
            Number sliceNumber = (Number) slicePoint;
            for (Object curr : domain) {
                if(curr instanceof Number) {
                    Double num = (Double) curr;
                    int result = num.compareTo((Double)sliceNumber);
                    if( result > 0) {
                        return false;
                    } else if(result == 0) {
                        return true;
                    }
                } else if(curr instanceof NumberRange) {
                    NumberRange range = (NumberRange) curr;
                    if(range.contains(sliceNumber)) {
                        return true;
                    } else if(range.getMaxValue().compareTo(sliceNumber) < 0) {
                        return false;
                    }
                }
            }
        }
        
        return false;
    }
    /**
     * Parses a number range out of the dimension subsetting directives
     * @param accessor
     * @param request
     * @param elevationDimension
     * @return
     * @throws IOException 
     */
    public NumberRange extractElevationSubset() throws IOException {
        NumberRange elevationSubset = null;
        if (elevationDimension != null) {
            for (DimensionSubsetType dim : request.getDimensionSubset()) {
                String dimension = WCSDimensionsSubsetHelper.getDimensionName(dim);
                
                // only care for elevation dimensions
                if (!WCSDimensionsSubsetHelper.ELEVATION_NAMES.contains(dimension.toLowerCase())) {
                    continue;
                }
                
                // did we parse the range already?
                if (elevationSubset != null) {
                    throw new WCS20Exception("Elevation dimension trimming/slicing specified twice in the request",
                            WCS20Exception.WCS20ExceptionCode.InvalidSubsetting, "subset");
                }
    
                // now decide what to do
                if (dim instanceof DimensionTrimType) {
    
                    // TRIMMING
                    final DimensionTrimType trim = (DimensionTrimType) dim;
                    final Double low = XML_CONVERTER.parseDouble(trim.getTrimLow());
                    final Double high = XML_CONVERTER.parseDouble(trim.getTrimHigh());

                    // low > high???
                    if (low > high) {
                        throw new WCS20Exception("Low greater than High: " + trim.getTrimLow() 
                                + ", " + trim.getTrimHigh(),
                                WCS20Exception.WCS20ExceptionCode.InvalidSubsetting, "subset");
                    }

                    elevationSubset = new NumberRange<Double>(Double.class, low, high);
                } else if (dim instanceof DimensionSliceType) {

                    // SLICING
                    final DimensionSliceType slicing = (DimensionSliceType) dim;
                    final String slicePointS = slicing.getSlicePoint();
                    final Double slicePoint = XML_CONVERTER.parseDouble(slicePointS);

                    elevationSubset = new NumberRange<Double>(Double.class, slicePoint, slicePoint);
                } else {
                    throw new WCS20Exception(
                            "Invalid element found while attempting to parse dimension subsetting request: " + dim.getClass()
                            .toString(), WCS20Exception.WCS20ExceptionCode.InvalidSubsetting, "subset");
                }
            }
            
            // right now we don't support trimming
            // TODO: revisit when we have some multidimensional output support
            if (elevationSubset != null && !elevationSubset.getMinValue().equals(elevationSubset.getMaxValue())) {
                throw new WCS20Exception("Trimming on elevation is not supported at the moment, only slicing is");
            }
            
            // apply nearest neighbor matching on elevation
            if (elevationSubset != null) {
                interpolateElevation (elevationSubset, accessor);
            }

            if (elevationSubset == null) {
                // use "current" as the default
                Number maxElevation = accessor.getMaxElevation();
                if (maxElevation != null) {
                    elevationSubset = new NumberRange(maxElevation.getClass(), maxElevation, maxElevation);
                }
            }
        }
        return elevationSubset;
    }

    /**
     * Nearest interpolation on elevation
     * @param elevationSubset
     * @param accessor
     * @return
     * @throws IOException
     */
    private NumberRange interpolateElevation(NumberRange elevationSubset, ReaderDimensionsAccessor accessor) throws IOException {
        TreeSet<Object> domain = accessor.getElevationDomain();
        Double slicePoint = elevationSubset.getMinimum();
        if (!domainContainsPoint(slicePoint, domain)) {
            // look for the closest elevation
            Double previous = null;
            Double newSlicePoint = null;
            // for NN matching we don't need the range, NN against their extrema will be fine
            TreeSet<Double> domainDates = getDomainNumber(domain);
            for (Double curr : domainDates) {
                if (curr.compareTo(slicePoint) > 0) {
                    if (previous == null) {
                        newSlicePoint = curr;
                        break;
                    } else {
                        double diffPrevious = slicePoint - previous;
                        double diffCurr = curr - slicePoint;
                        if (diffCurr > diffPrevious) {
                            newSlicePoint = curr;
                            break;
                        } else {
                            newSlicePoint = previous;
                            break;
                        }
                    }
                } else {
                    previous = curr;
                }
            }

            if (newSlicePoint == null) {
                newSlicePoint = previous;
            }
            elevationSubset = new NumberRange<Double>(Double.class, newSlicePoint, newSlicePoint);
        }
        return elevationSubset;
    }

    
    /**
     * Get the domain set as a set of number.
     * @param domain
     * @return
     */
    private TreeSet<Double> getDomainNumber(TreeSet<Object> domain) {
        TreeSet<Double> results = new TreeSet<Double>();
        for (Object item : domain) {
            if(item instanceof Number) {
                Double number = (Double) item;
                results.add(number);
            } else if(item instanceof NumberRange) {
                NumberRange range = (NumberRange) item;
                results.add(range.getMinimum());
                results.add(range.getMaximum());
            }
        }
        return results;
    }
    
    /**
     * Extract custom dimension subset from the current helper 
     * 
     * @param accessor 
     * @param request
     * @param dimensions 
     * @param timeDimension
     * @return
     * @throws IOException 
     */
    public Map<String, List> extractDimensionsSubset() throws IOException {
        Map<String, List> dimensionSubset = new HashMap<String, List>();

        if (dimensions != null && !dimensions.isEmpty()) {
            Set<String> dimensionKeys = dimensions.keySet();
            for (DimensionSubsetType dim : request.getDimensionSubset()) {
                String dimension = getDimensionName(dim);

                // only care for time dimensions
                if (dimensionKeys.contains(dimension)) {
                    List<Object> selectedValues = new ArrayList<Object>();
                    //
                    // // now decide what to do
                    // if (dim instanceof DimensionTrimType) {
                    //
                    // // TRIMMING
                    // final DimensionTrimType trim = (DimensionTrimType) dim;
                    // final Date low = xmlConverter.parseDateTime(trim.getTrimLow()).getTime();
                    // final Date high = xmlConverter.parseDateTime(trim.getTrimHigh()).getTime();
                    //
                    // // low > high???
                    // if (low.compareTo(high) > 0) {
                    // throw new WCS20Exception("Low greater than High: " + trim.getTrimLow()
                    // + ", " + trim.getTrimHigh(),
                    // WCS20Exception.WCS20ExceptionCode.InvalidSubsetting, "subset");
                    // }
                    //
                    // timeSubset = new DateRange(low, high);
                    // } else
                    if (dim instanceof DimensionSliceType) {

                        // SLICING
                        final DimensionSliceType slicing = (DimensionSliceType) dim;
                        setSubsetValue(slicing, selectedValues);

                    } else {
                        throw new WCS20Exception(
                                "Invalid element found while attempting to parse dimension subsetting request: "
                                        + dim.getClass().toString(),
                                WCS20Exception.WCS20ExceptionCode.InvalidSubsetting, "subset");
                    }

                    // // right now we don't support trimming
                    // TODO: revisit when we have some multidimensional output support
                    // TODO: need to deal with interpolation?
                    // TODO: Deal with default values
                    dimensionSubset.put(dimension, selectedValues);
                }
            }
        }
        return dimensionSubset;
    }

    private void setSubsetValue(DimensionSliceType slicing, List<Object> selectedValues) {
        final String slicePointS = slicing.getSlicePoint();
        boolean sliceSet = false;

        // Try setting the value as a time
        if (!sliceSet) {
            sliceSet = setAsDate(slicePointS, selectedValues);
        }

        // Try setting the value as an integer
        if (!sliceSet) {
            sliceSet = setAsInteger(slicePointS, selectedValues);
        }

        // Try setting the value as a double
        if (!sliceSet) {
            sliceSet = setAsDouble(slicePointS, selectedValues);
        }

        if (!sliceSet) {
            // Setting it as a String
            selectedValues.add(slicePointS);
        }
    }

    /**
     * Set the slicePoint string as an {@link Integer}. Return true in case of success
     * @param slicePointS
     * @param selectedValues
     * @return
     */
    private boolean setAsInteger(String slicePointS, List<Object> selectedValues) {
        final Integer slicePoint = parseAsInteger(slicePointS);
        if (slicePoint != null) {
            selectedValues.add(slicePoint);
            return true;
        }
        return false;
    }

    /**
     * Set the slicePoint string as an {@link Double}. Return true in case of success
     * @param slicePointS
     * @param selectedValues
     * @return
     */
    private boolean setAsDouble(String slicePointS, List<Object> selectedValues) {
        final Double slicePoint = parseAsDouble(slicePointS);
        if (slicePoint != null) {
            selectedValues.add(slicePoint);
            return true;
        }
        return false;
    }

    /**
     * Set the slicePoint string as an {@link Date}. Return true in case of success
     * @param slicePointS
     * @param selectedValues
     * @return
     */
    private boolean setAsDate(String slicePointS, List<Object> selectedValues) {
        final Date slicePoint = parseAsDate(slicePointS);
        if (slicePoint != null) {
            selectedValues.add(slicePoint);
            return true;
        }
        return false;
    }
    
    /**
     * Parse a String as a Double or return null if impossible.
     * @param text
     * @return
     */
    public static Double parseAsDouble(String text) {
        try {
            final Double slicePoint = XML_CONVERTER.parseDouble(text);
            return slicePoint;
        } catch (NumberFormatException nfe) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.fine(text + " can't be parsed as an Double.");
            }
        }
        return null;
    }
    
    /**
     * Parse a String as a Range of Double or return null if impossible.
     * @param text
     * @return
     */
    public static NumberRange<Double> parseAsDoubleRange(String text) {
        try {
            if (text.contains("/")) {
                String[] range = text.split("/");
                if (range.length == 2) {
                    String min = range[0];
                    String max = range[1];
                    final Double minValue = XML_CONVERTER.parseDouble(min);
                    final Double maxValue = XML_CONVERTER.parseDouble(max);
                    return new NumberRange<Double>(Double.class, minValue, maxValue);
                }
            }
        } catch (NumberFormatException nfe) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.fine(text + " can't be parsed as an Double.");
            }
        }
        return null;
    }
    
    /**
     * Parse a String as an Integer or return null if impossible.
     * @param text
     * @return
     */
    public static Integer parseAsInteger(String text) {
        try {
            final Integer slicePoint = XML_CONVERTER.parseInt(text);
            return slicePoint;
        } catch (NumberFormatException nfe) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.fine(text + " can't be parsed as an Integer.");
            }
        }
        return null;
    }

}
