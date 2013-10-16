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
import org.geoserver.rest.format.DataFormat;
import org.geoserver.rest.format.ReflectiveXMLFormat;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.factory.GeoTools;
import org.opengis.coverage.grid.GridCoverageReader;
import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;

import com.thoughtworks.xstream.XStream;

/**
 * A {@link CatalogResource} 
 * 
 * @author Daniele Romagnoli, GeoSolutions SAS
 *
 */
public class HarvestedCoverageResource extends AbstractCatalogResource {

    public HarvestedCoverageResource(Context context, Request request,
            Response response, Catalog catalog) {
        super(context, request, response, CoverageInfo.class, catalog);
        
    }

    @Override
    protected DataFormat createHTMLFormat(Request request, Response response) {
        return new ResourceHTMLFormat(CoverageInfo.class, request, response, this);
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

//    
//    
//    @Override
//    protected Object handleObjectGet() throws Exception {
//        String workspace = getAttribute( "workspace");
//        String coveragestore = getAttribute( "coveragestore");
//        String coverage = getAttribute( "coverage" );
//
//        LOGGER.fine( "GET coverage " + coveragestore + "," + coverage );
//        final CoverageStoreInfo cs = catalog.getCoverageStoreByName(workspace, coveragestore);
//        final GridCoverageReader reader = cs.getGridCoverageReader(null, null);
//        final String[] coverageNames = reader.getGridCoverageNames();
//        final List<CoverageInfo> infos = new ArrayList<CoverageInfo>();
//        for (String name: coverageNames) {
//            CoverageInfoImpl cInfo = new CoverageInfoImpl(catalog, name);
//            cInfo.setName(name);
//            NamespaceInfo namespace = catalog.getNamespaceByPrefix(workspace);
//            if (namespace == null) {
//                namespace = catalog.getDefaultNamespace();
//            }
//            cInfo.setNamespace(namespace);
//
//            infos.add(cInfo);
//        }
//        return infos;
//    }

    @Override
    public boolean allowPost() {
        return true;//getAttribute("coveragestores") == null;
    }
    
    @Override
    protected String handleObjectPost(Object object) throws Exception {
        String workspace = getAttribute( "workspace");
        String coveragestore = getAttribute( "coveragestore");

        CoverageInfo coverage = (CoverageInfo) object;
        if ( coverage.getStore() == null ) {
            //get from requests
            CoverageStoreInfo ds = catalog.getCoverageStoreByName( workspace, coveragestore );
            coverage.setStore( ds );
        }
        String name = coverage.getName();
        CatalogBuilder builder = new CatalogBuilder(catalog);
        CoverageStoreInfo store = coverage.getStore();
        builder.setStore(store);

        GridCoverage2DReader reader = (GridCoverage2DReader) catalog
                .getResourcePool().getGridCoverageReader(store, GeoTools.getDefaultHints());
        coverage = builder.buildCoverage(reader, name, null);
        catalog.add( coverage );

        //create a layer for the coverage
        catalog.add(builder.buildLayer(coverage));

        LOGGER.info( "POST coverage " + coveragestore + "," + coverage.getName() );
        return name;
    }

    @Override
    public boolean allowPut() {
        return false;
    }

    void clear(CoverageInfo info) {
        catalog.getResourcePool().clear(info.getStore());
    }

}
