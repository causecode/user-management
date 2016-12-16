/*
 * Copyright (c) 2016, CauseCode Technologies Pvt Ltd, India.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or
 * without modification, are not permitted.
 */
package com.causecode.spring.rest.oauth.twitter

import com.fasterxml.jackson.databind.JsonNode
import org.pac4j.oauth.client.TwitterClient
import org.pac4j.oauth.profile.JsonHelper
import org.pac4j.oauth.profile.twitter.TwitterProfile
import org.scribe.model.Token

/**
 * An extension to the default Twitter client provided by the Pac4j library to support email retrieval.
 *
 * @author Nikhil Sharma
 * @since 0.0.1
 */
class CustomTwitterClient extends TwitterClient {

    CustomTwitterClient() {
    }

    CustomTwitterClient(final String key, final String secret) {
        super(key, secret)
    }

    @Override
    protected TwitterProfile extractUserProfile(final String body) {
        final TwitterProfile profile = new TwitterProfile()
        final JsonNode json = JsonHelper.getFirstNode(body)
        if (!json) {
            profile.setId(JsonHelper.get(json, 'id'))
            for (final String attribute : new CustomTwitterAttributesDefinition().allAttributes) {
                profile.addAttribute(attribute, JsonHelper.get(json, attribute))
            }
        }
        return profile
    }

    @Override
    protected CustomTwitterClient newClient() {
        return new CustomTwitterClient()
    }

    @Override
    protected String getProfileUrl(final Token accessToken) {
        return 'https://api.twitter.com/1.1/account/verify_credentials.json?include_email=true'
    }
}
