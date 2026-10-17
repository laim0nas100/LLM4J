package com.github.laim0nas100.llm4j.store;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.laim0nas100.llm4j.GlobalCTX.Globals;
import com.github.laim0nas100.llm4j.store.Conversation.MessageRole;
import com.github.laim0nas100.llm4j.store.Conversation.MessageStatus;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author Lemmin
 */
public class Helpers {

    public static class MessageProxyHold {
        
        public String proxy;
        public String proxy_json = "{}";

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
        
        public static MessageProxyHold noProxy(){
            var hold = new MessageProxyHold();
            hold.proxy = null;
            hold.proxy_json = "{}";
            return hold;
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
        
        public static MessageIdHold respondingTo(Conversation.ChatMessage msg){
            var hold = new MessageIdHold();
            hold.branchId = msg.branch_id();
            hold.conversationId = msg.conversation_id();
            hold.parentMessageId = msg.id();
            hold.templateId = msg.chat_template_id();
            return hold;
            
        }

    }

    public static class ChatContentHold implements Globals {

        public List<String> attachments = new ArrayList<>();
        public String content_json = "{}";
        public String provider;
        public String provider_json = "{}";
        public MessageRole role;
        public MessageStatus status;
        
        public String getAttachmentsJson() throws JsonProcessingException{
            return jsonMapper().writeValueAsString(attachments);
        }

        public List<String> getAttachments() {
            return attachments;
        }

        public void setAttachments(List<String> attachments) {
            this.attachments.clear();
            for(String s:attachments){
                if(!StringUtils.isBlank(s)){
                    this.attachments.add(s);
                }
            }
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

        public String getProvider_json() {
            return provider_json;
        }

        public void setProvider_json(String provider_json) {
            this.provider_json = provider_json;
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
        
        public static ChatContentHold userMessage(String provider, String content_json, List<String> attachments){
            var hold = new ChatContentHold();
            hold.setProvider(provider);
            hold.setContent_json(content_json);
            hold.setStatus(MessageStatus.COMPLETE);
            hold.setRole(MessageRole.USER);
            hold.setAttachments(attachments);
            return hold;
        }
        
        public static ChatContentHold startAssitantResponse(String provider){
            var hold = new ChatContentHold();
            hold.setProvider(provider);
            hold.setContent_json("{}");
            hold.setStatus(MessageStatus.STREAMING);
            hold.setRole(MessageRole.ASSISTANT);
            return hold;
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

        public static ChatBranchHold newRootBranch(String conversationId) {
            ChatBranchHold hold = new ChatBranchHold();
            hold.setConversationId(conversationId);
            return hold;
        }

    }

    public static class ChatTemplateHold {

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

}
