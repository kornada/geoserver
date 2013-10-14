/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.rest;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.rest.format.ReflectiveXMLFormat;
import org.opengis.coverage.grid.GridCoverageReader;
import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;

import com.thoughtworks.xstream.XStream;

public class HarvestedCoverageListResource extends AbstractCatalogResource {

    public HarvestedCoverageListResource(Context context, Request request,
            Response response, Catalog catalog) {
        super(context, request, response, CoverageInfo.class, catalog);
        
    }

    @Override
    protected Object handleObjectGet() throws Exception {
        String workspace = getAttribute( "workspace");
        String coveragestore = getAttribute( "coveragestore");
        String coverage = getAttribute( "coverage" );

        LOGGER.fine( "GET coverage " + coveragestore + "," + coverage );
        final CoverageStoreInfo cs = catalog.getCoverageStoreByName(workspace, coveragestore);
        final GridCoverageReader reader = cs.getGridCoverageReader(null, null);
        final String[] coverageNames = reader.getGridCoverageNames();
        final List<String> coverages = new ArrayList<String>();
        for (String name: coverageNames) {
            coverages.add(name);
        }
        return coverages;
    }

    @Override
    protected ReflectiveXMLFormat createXMLFormat(Request request, Response response) {
        return new ReflectiveXMLFormat() {
          
            @Override
            protected void write(Object data, OutputStream output)
                    throws IOException {
                XStream xstream = new XStream();
                xstream.alias( "coverageName", String.class);
                xstream.toXML( data, output );
            }
        };
    }

    @Override
    public boolean allowPost() {
        return false; //getAttribute("coverage") == null;
    }
    
    @Override
    protected String handleObjectPost(Object object) throws Exception {
//        String workspace = getAttribute( "workspace");
//        String coveragestore = getAttribute( "coveragestore");
//
//        CoverageInfo coverage = (CoverageInfo) object;
//        if ( coverage.getStore() == null ) {
//            //get from requests
//            CoverageStoreInfo ds = catalog.getCoverageStoreByName( workspace, coveragestore );
//            coverage.setStore( ds );
//        }
//
//        CatalogBuilder builder = new CatalogBuilder(catalog);
//        builder.setStore(coverage.getStore());
//        builder.initCoverage(coverage);
//
//        NamespaceInfo ns = coverage.getNamespace();
//        if ( ns != null && !ns.getPrefix().equals( workspace ) ) {
//            //TODO: change this once the two can be different and we untie namespace
//            // from workspace
//            LOGGER.warning( "Namespace: " + ns.getPrefix() + " does not match workspace: " + workspace + ", overriding." );
//            ns = null;
//        }
//        
//        if ( ns == null){
//            //infer from workspace
//            ns = catalog.getNamespaceByPrefix( workspace );
//            coverage.setNamespace( ns );
//        }
//        
//        coverage.setEnabled(true);
//        catalog.add( coverage );
//        
//        //create a layer for the coverage
//        catalog.add(builder.buildLayer(coverage));
//        
//        LOGGER.info( "POST coverage " + coveragestore + "," + coverage.getName() );
//        return coverage.getName();
        return null;
    }

    @Override
    public boolean allowPut() {
        return getAttribute("coverage") != null;
    }

    @Override
    protected void handleObjectPut(Object object) throws Exception {
        CoverageInfo c = (CoverageInfo) object;
        
        String workspace = getAttribute("workspace");
        String coveragestore = getAttribute("coveragestore");
        String coverage = getAttribute("coverage");
        
        CoverageStoreInfo cs = catalog.getCoverageStoreByName(workspace, coveragestore);
        CoverageInfo original = catalog.getCoverageByCoverageStore( cs,  coverage );
        new CatalogBuilder(catalog).updateCoverage(original,c);
        calculateOptionalFields(c, original);
        catalog.save( original );
        
        clear(original);
        
        LOGGER.info( "PUT coverage " + coveragestore + "," + coverage );
    }

    void clear(CoverageInfo info) {
        catalog.getResourcePool().clear(info.getStore());
    }
    
}
