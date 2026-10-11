package com.github.laim0nas100.lmstudio4j.store;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.laim0nas100.lmstudio4j.Global;
import com.github.laim0nas100.lmstudio4j.model.v1.ImageInput;
import com.github.laim0nas100.lmstudio4j.model.v1.TextInput;
import com.github.laim0nas100.lmstudio4j.store.MessageStep.*;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Lemmin
 */
public class Helpers {
    
    public static class MessageSource{
        public String sourceId;
        public String source;
        public String source_json;

        public String getSourceId() {
            return sourceId;
        }

        public void setSourceId(String sourceId) {
            this.sourceId = sourceId;
        }

        public String getSource() {
            return source;
        }

        public void setSource(String source) {
            this.source = source;
        }

        public String getSource_json() {
            return source_json;
        }

        public void setSource_json(String source_json) {
            this.source_json = source_json;
        }
    }
    
    public static class MessageIds{
        public String conversationId;
        public String branchId;
        public String templateId;
        public String parentMessageId;

        public String getConversationId() {
            return conversationId;
        }

        public void setConversationId(String conversationId) {
            this.conversationId = conversationId;
        }

        public String getBranchId() {
            return branchId;
        }

        public void setBranchId(String branchId) {
            this.branchId = branchId;
        }

        public String getTemplateId() {
            return templateId;
        }

        public void setTemplateId(String templateId) {
            this.templateId = templateId;
        }

        public String getParentMessageId() {
            return parentMessageId;
        }

        public void setParentMessageId(String parentMessageId) {
            this.parentMessageId = parentMessageId;
        }
        
    }

    public static List<MessageStep> convertUserInput(Object content) {
        if (content instanceof String str) {
            return List.of(new MessageStep.TextStep(str));
        } else if (content instanceof List list) {
            List<MessageStep> steps = new ArrayList<>();
            for (Object input : list) {
                if (input instanceof TextInput textInput) {
                    steps.add(new TextStep(textInput.getContent()));
                } else if (input instanceof ImageInput imageInput) {
                    steps.add(new ImageStep(imageInput.getDataUrl()));
                } else {
                    steps.addAll(convertUserInput(input));
                }
            }
            return steps;
        } else {
            return List.of();
        }
    }

    public static String userInputToJson(Object content) throws JsonProcessingException {
        return Global.jsonMapper.writeValueAsString(convertUserInput(content));
    }
}
