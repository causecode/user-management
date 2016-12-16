/*
 * Copyright (c) 2016, CauseCode Technologies Pvt Ltd, India.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or
 * without modification, are not permitted.
 */
package com.causecode

import grails.plugin.json.view.JsonViewTemplateEngine
import org.springframework.http.HttpStatus

/**
 * A Restful controller for the app that extends the Grails default RestfulController and overrides the index and
 * delete action. The index action first checks for the domain specific template at
 * 'grails-app/views/domainName/index.gson' and if available renders that, else render the default app wide template
 * for index action at `grails-app/views/index/index.gson`.
 *
 * @author Nikhil Sharma
 * @since 0.0.1
 */
class RestfulController extends grails.rest.RestfulController implements BaseController {

    RestfulController(Class resource) {
        super(resource)
    }

    @Override
    def index() {
        JsonViewTemplateEngine jsonTemplateEngine = new JsonViewTemplateEngine()

        params.offset = params.offset ?: 0
        params.max = Math.min(params.max ?: 10, 100)
        params.sort = params.sort ?: 'lastUpdated'
        params.order = params.order ?: 'desc'

        String controllerName = this.controllerName

        // The overridden index view path.
        String view = "/$controllerName/index"

        /*
         * If the controller has specific index view defined at 'grails-app/views/controllerName/index.gson', then
         * render that, else use the default index view located at 'grails-app/views/index.gson'.
         */
        if (!jsonTemplateEngine.resolveTemplate(view)) {
            // Change the view to default index view if the overridden view is not found
            view = '/index/index'
        }

        Map model = [instanceList: listAllResources(params)]

        if (jsonTemplateEngine.resolveTemplate("/$controllerName/_$controllerName")) {
            model.domainName = controllerName
        }

        render(model: model, view: view)
    }

    @Override
    def delete() {
        response.setStatus(HttpStatus.METHOD_NOT_ALLOWED.value)

        respond([message: 'Method not allowed'])
    }
}
