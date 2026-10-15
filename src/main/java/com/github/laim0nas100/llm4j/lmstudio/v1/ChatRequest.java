package com.github.laim0nas100.llm4j.lmstudio.v1;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import com.github.laim0nas100.commonslb.F;
import com.github.laim0nas100.commonslb.clone.Cloner;

/**
 *
 * @author Lemmin
 */
public class ChatRequest extends ChatSession {

    /* * 
    LM Studio accepts either: 
    * * "input": "Hello" * * or: * * 
    "input": [ * {"type": "text", "content": "Hello"}, {"type": "image", "data_url": "..."} * ] 
    Object allows Jackson to serialize both forms. */
    protected Object input;

    protected Boolean stream = false;

    @JsonProperty("previous_response_id")
    protected String previousResponseId;

    public ChatRequest() {
    }

    public ChatRequest(ChatSession session) {
        super(session);
    }

    public Object getInput() {
        return input;
    }

    public void setInput(Object input) {
        this.input = input;
    }

    public String getPreviousResponseId() {
        return previousResponseId;
    }

    public void setPreviousResponseId(String previousResponseId) {
        this.previousResponseId = previousResponseId;
    }

    public Boolean getStream() {
        return stream;
    }

    public void setStream(Boolean stream) {
        this.stream = stream;
    }

    @Override
    public ChatRequest clone(Cloner cloner) throws CloneNotSupportedException {
        return F.cast(super.clone(cloner));
    }

    @Override
    public ChatRequest clone() throws CloneNotSupportedException {
        return F.cast(super.clone());
    }

    @Override
    public ChatSession justConfig() {
        return new ChatSession(this);
    }
    
    

}
