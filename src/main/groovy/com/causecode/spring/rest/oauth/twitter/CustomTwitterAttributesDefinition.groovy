/*
 * Copyright (c) 2016, CauseCode Technologies Pvt Ltd, India.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or
 * without modification, are not permitted.
 */
package com.causecode.spring.rest.oauth.twitter

import org.pac4j.core.profile.converter.Converters
import org.pac4j.oauth.profile.twitter.TwitterAttributesDefinition

/**
 * An extension to TwitterAttributesDefinition class for handling email field in the response from the Twitter Api.
 *
 * @author Nikhil Sharma
 * @since 0.0.1
 */
class CustomTwitterAttributesDefinition extends TwitterAttributesDefinition {

    public static final String EMAIL = 'email'

    CustomTwitterAttributesDefinition() {
        super()
        addAttribute(EMAIL, Converters.stringConverter)
    }
}
