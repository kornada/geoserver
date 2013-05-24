/* Copyright (c) 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs2_0.response;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.opengis.wcs20.DimensionSliceType;
import net.opengis.wcs20.DimensionSubsetType;
import net.opengis.wcs20.GetCoverageType;

import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.util.ReaderDimensionsAccessor;
import org.geoserver.wcs2_0.exception.WCS20Exception;
import org.geotools.util.NumberRange;
import org.geotools.util.logging.Logging;
import org.geotools.xml.impl.DatatypeConverterImpl;

/**
 * Provides support to deal with dimensions slicing, trimming and values conversions
 * 
 * @author Daniele Romagnoli - GeoSolutions
 */
public class WCSDimensionsSubsetHelper {

    private final static Logger LOGGER = Logging.getLogger(WCSDimensionsHelper.class);

    private final static DatatypeConverterImpl XML_CONVERTER = DatatypeConverterImpl.getInstance();

    private ReaderDimensionsAccessor accessor;

    private GetCoverageType request;

    private Map<String, DimensionInfo> dimensions;
    

    public WCSDimensionsSubsetHelper(ReaderDimensionsAccessor accessor, GetCoverageType request,
            Map<String, DimensionInfo> dimensions) {
        this.accessor = accessor;
        this.request = request;
        this.dimensions = dimensions;
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
