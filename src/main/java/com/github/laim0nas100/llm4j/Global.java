package com.github.laim0nas100.llm4j;

import com.fasterxml.jackson.databind.json.JsonMapper;

/**
 *
 * Simple explicit dependency injection
 *
 * @author Lemmin
 */
public class Global {

    public static final JsonMapper jsonMapper = new JsonMapper();

}
