package com.github.laim0nas100.llm4j.store;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.laim0nas100.llm4j.Global;
import com.github.laim0nas100.llm4j.lmstudio.v1.ImageInput;
import com.github.laim0nas100.llm4j.lmstudio.v1.TextInput;
import com.github.laim0nas100.llm4j.store.Conversation.MessageRole;
import com.github.laim0nas100.llm4j.store.Conversation.MessageStatus;
import com.github.laim0nas100.llm4j.store.MessageStep.*;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Lemmin
 */
public class Helpers {

    public static class MessageProxyHold {

        public String proxy;
        public String proxy_json;

        public String getProxy() {
            return proxy;
        }

        public void setProxy(String proxy) {
            this.proxy = proxy;
        }

        public String getProxy_json() {
            return proxy_json;
        }

        public void setProxy_json(String proxy_json) {
            this.proxy_json = proxy_json;
        }
    }

    public static class MessageIdHold {

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

    public static class ChatContentHold {

        public List<String> attachments = new ArrayList<>();
        public String content_json;
        public String provider;
        public String provder_json;
        public MessageRole role;
        public MessageStatus status = Conversation.MessageStatus.COMPLETE;

        public List<String> getAttachments() {
            return attachments;
        }

        public void setAttachments(List<String> attachments) {
            this.attachments = attachments;
        }

        public String getContent_json() {
            return content_json;
        }

        public void setContent_json(String content_json) {
            this.content_json = content_json;
        }

        public String getProvider() {
            return provider;
        }

        public void setProvider(String provider) {
            this.provider = provider;
        }

        public String getProvder_json() {
            return provder_json;
        }

        public void setProvder_json(String provder_json) {
            this.provder_json = provder_json;
        }

        public MessageRole getRole() {
            return role;
        }

        public void setRole(MessageRole role) {
            this.role = role;
        }

        public MessageStatus getStatus() {
            return status;
        }

        public void setStatus(MessageStatus status) {
            this.status = status;
        }

    }

    public static class ChatBranchHold {

        public static final String DEFAULT_CHAT_BRANCH_TITLE = "New Chat";
        public String title = DEFAULT_CHAT_BRANCH_TITLE;
        public String conversationId;
        public String headMessageId;
        public String forkedFromId;

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getConversationId() {
            return conversationId;
        }

        public void setConversationId(String conversationId) {
            this.conversationId = conversationId;
        }

        public String getHeadMessageId() {
            return headMessageId;
        }

        public void setHeadMessageId(String headMessageId) {
            this.headMessageId = headMessageId;
        }

        public String getForkedFromId() {
            return forkedFromId;
        }

        public void setForkedFromId(String forkedFromId) {
            this.forkedFromId = forkedFromId;
        }
    }
    
    public static class ChatTemplateHold{
                public static final String DEFAULT_CHAT_TEMPLATE_NAME = "DEFAULT_TEMPLATE";

        public String templateName;
        public String config_json;

        public String getTemplateName() {
            return templateName;
        }

        public void setTemplateName(String templateName) {
            this.templateName = templateName;
        }

        public String getConfig_json() {
            return config_json;
        }

        public void setConfig_json(String config_json) {
            this.config_json = config_json;
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
