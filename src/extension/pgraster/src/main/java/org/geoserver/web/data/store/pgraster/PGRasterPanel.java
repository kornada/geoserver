/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.store.pgraster;

import org.apache.wicket.behavior.SimpleAttributeModifier;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.ResourceModel;
import org.geoserver.web.data.store.panel.PasswordParamPanel;
import org.geoserver.web.data.store.panel.TextParamPanel;
import org.geoserver.web.util.MapModel;

/**
 * a Panel with PGRaster automatic configuration options
 * TODO:
 * 1) Add numeric validator for PORT
 * 2) change text description on the GUI (right now there is the name of the params)
 */
public class PGRasterPanel extends Panel {

    private static final long serialVersionUID = -8845475833628642890L;

    /**
     * temporary parameter name used to hold the raster table selected by the drop down into the
     * store's connectionParameters
     */
    public static final String TABLE_NAME = "tableName";

    private static final String RESOURCE_KEY_PREFIX = PGRasterPanel.class
            .getSimpleName();

    FormComponent server;

    FormComponent port;

    FormComponent user;

    FormComponent password;

    FormComponent database;

    FormComponent table;

    FormComponent schema;

    FormComponent fileext;

    FormComponent importopt;

    public PGRasterPanel(final String id, final IModel paramsModel, final Form storeEditForm) {

        super(id);
        server = addTextPanel(paramsModel, "server", true);
        
        //TODO: Add numeric validator for port
        port = addTextPanel(paramsModel, "port", true);
        user = addTextPanel(paramsModel, "user", "Postgis user", true);
        password = addPasswordPanel(paramsModel, "password");
        database = addTextPanel(paramsModel, "database", "Postgis Database", true);
        table = addTextPanel(paramsModel, "table", true);
        schema = addTextPanel(paramsModel, "schema", true);
        fileext = addTextPanel(paramsModel, "fileext", "tiles file extension filter", false);
        importopt = addTextPanel(paramsModel, "importopt", "raster2pgsql script import options",  false);


        server.setOutputMarkupId(true);
        port.setOutputMarkupId(true);
        user.setOutputMarkupId(true);
        password.setOutputMarkupId(true);
        database.setOutputMarkupId(true);
        table.setOutputMarkupId(true);
        schema.setOutputMarkupId(true);

        fileext.setOutputMarkupId(true);
        importopt.setOutputMarkupId(true);
    }

    private FormComponent addPasswordPanel(final IModel paramsModel, final String paramName) {

        final String resourceKey = RESOURCE_KEY_PREFIX + "." + paramName;

        final PasswordParamPanel pwdPanel = new PasswordParamPanel(paramName, new MapModel(
                paramsModel, paramName), new ResourceModel(resourceKey, paramName), true);
        add(pwdPanel);

        String defaultTitle = paramName; 

        ResourceModel titleModel = new ResourceModel(resourceKey + ".title", defaultTitle);
        String title = String.valueOf(titleModel.getObject());

        pwdPanel.add(new SimpleAttributeModifier("title", title));

        return pwdPanel.getFormComponent();
    }

    private FormComponent addTextPanel(final IModel paramsModel, final String paramName, final boolean required) {
        return addTextPanel(paramsModel, paramName, paramName, required);
    }
    
    private FormComponent addTextPanel(final IModel paramsModel, final String paramName, final String paramTitle, final boolean required) {
        final String resourceKey = getClass().getSimpleName() + "." + paramName;

        final TextParamPanel textParamPanel = new TextParamPanel(paramName, new MapModel(
                paramsModel, paramTitle), new ResourceModel(resourceKey, paramName), required);
        textParamPanel.getFormComponent().setType(String.class/*param.type*/);

        String defaultTitle = paramTitle;

        ResourceModel titleModel = new ResourceModel(resourceKey + ".title", defaultTitle);
        String title = String.valueOf(titleModel.getObject());

        textParamPanel.add(new SimpleAttributeModifier("title", title));

        add(textParamPanel);
        return textParamPanel.getFormComponent();
    }

    public FormComponent[] getDependentFormComponents() {
        return new FormComponent[]{server, port, user, password, database, schema, table, fileext, importopt};
    }

    /**
     * Setup a URL String composing all the required configuration options
     * @return
     */
    public String buildURL() {
        StringBuilder builder = new StringBuilder("pgraster://");
//        pgraster://USER:PASS@HOST:PORT:DATABASE.SCHEMA.TABLE:*.FILE_EXTENSION?OPTIONS#/PATH/TO/RASTER_TILES/"
        builder.append(user.getValue()).append(":").append(password.getValue()).append("@").append(server.getValue()).append(":")
        .append(port.getValue()).append(":").append(database.getValue()).append(".").append(schema.getValue()).append(".")
        .append(table.getValue()).append(":");
        final String fileExt = fileext.getValue();
        if (fileExt != null && fileExt.trim().length() > 0) {
            builder.append(fileExt);
        }
        final String options = importopt.getValue();
        if (options != null && options.trim().length() > 0) {
            builder.append("?").append(options);
        }
        builder.append("#");
        return builder.toString();
    }
}
