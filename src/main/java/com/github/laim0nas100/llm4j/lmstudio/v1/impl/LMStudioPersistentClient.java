package com.github.laim0nas100.llm4j.lmstudio.v1.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.laim0nas100.llm4j.lmstudio.v1.ChatRequest;
import com.github.laim0nas100.llm4j.lmstudio.v1.ChatSession;
import com.github.laim0nas100.llm4j.lmstudio.v1.InputItem;
import com.github.laim0nas100.llm4j.lmstudio.v1.LLMEvent;
import com.github.laim0nas100.llm4j.lmstudio.v1.Stats;
import com.github.laim0nas100.llm4j.store.Conversation;
import com.github.laim0nas100.llm4j.store.Conversation.ChatMessage;
import com.github.laim0nas100.llm4j.store.Helpers;
import com.github.laim0nas100.llm4j.store.MessageStep;
import com.github.laim0nas100.llm4j.store.TableCreation;
import com.github.laim0nas100.uncheckedutils.SafeOpt;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 *
 * @author Lemmin
 */
public class LMStudioPersistentClient extends LMStudioChatStore {

    protected LMStudioChattingClient client;

    public LMStudioPersistentClient(TableCreation tableCreation, LMStudioChattingClient client) throws SQLException {
        super(tableCreation);
        this.client = Objects.requireNonNull(client);
    }

    public SafeOpt<LMStudioResponse> startNewChat(String templateName, Object input, List<String> attachments) {
        return getConfigByName(templateName).map(config -> {
            ChatRequest chatRequest = new ChatRequest(config);
            chatRequest.setInput(input);
            return safeHandle(handle -> {
                var conv = createConversation(templateName).throwAnyGet();
                var branchHold = Helpers.ChatBranchHold.newRootBranch(conv.id());
                var branch = createBranch(branchHold).throwAnyGet();
                var ids = new Helpers.MessageIdHold();
                ids.setBranchId(branch.id());
                ids.setConversationId(conv.id());
                ids.setParentMessageId(null);
                ids.setTemplateId(conv.chat_default_template_id());

                var content = Helpers.ChatContentHold.userMessage(PROVIDER, userInputToJson(input), attachments);
                var proxy = Helpers.MessageProxyHold.noProxy();
                var message = createMessage(ids, content, proxy).throwAnyGet();

                var accumulator = createLLMResponse(chatRequest, message).throwAnyGet();

                return new LMStudioResponse(
                        config, accumulator,
                        ids.getConversationId(),
                        ids.getBranchId(),
                        message.id(),
                        accumulator.getMessageId()
                );

            }).throwAnyGet();
        });
    }

    public SafeOpt<LMStudioResponse> continueChat(
            String conversationId,
            String branchId,
            Object input,
            List<String> attachments) {

        return safeHandle(handle -> {
            var conv = getConversation(conversationId).throwAnyGet();
            var branch = getBranch(branchId).throwAnyGet();

            if (!branch.conversation_id().equals(conv.id())) {
                throw new IllegalArgumentException(
                        "Branch does not belong to the conversation");
            }
            

            var config = getConfigById(conv.chat_default_template_id()).throwAnyGet();
            
            var chatRequest = new ChatRequest(config);
            chatRequest.setInput(input);

            // The branch head identifies the latest message in this branch.
            var headMessageId = branch.head_message_id();

            chatRequest.setPreviousResponseId(getLastResponseIdForBranch(branchId).throwAnyGet());

            // Persist the new user message as a child of the branch head.
            var ids = new Helpers.MessageIdHold();
            ids.setConversationId(conv.id());
            ids.setBranchId(branch.id());
            ids.setParentMessageId(headMessageId);
            ids.setTemplateId(conv.chat_default_template_id());

            var content = Helpers.ChatContentHold.userMessage(
                    PROVIDER,
                    userInputToJson(input),
                    attachments);

            var proxy = Helpers.MessageProxyHold.noProxy();
            var userMessage = createMessage(ids, content, proxy).throwAnyGet();

            var accumulator = createLLMResponse(
                    chatRequest,
                    userMessage).throwAnyGet();

            return new LMStudioResponse(
                    config,
                    accumulator,
                    conv.id(),
                    branch.id(),
                    userMessage.id(),
                    accumulator.getMessageId()
            );

        });
    }

    protected SafeOpt<LLMEventAccumulator> createLLMResponse(ChatRequest request, ChatMessage userMessage) {
        return safeHandle(handle -> {

            var ids = Helpers.MessageIdHold.respondingTo(userMessage);
            var proxy = Helpers.MessageProxyHold.noProxy();
            var content = Helpers.ChatContentHold.startAssitantResponse(PROVIDER);

            var startingMessage = createMessage(ids, content, proxy).throwAnyGet();
            Stream<LLMEvent> responseStream = client.sendAsyncStream(request);

            LLMEventAccumulator accumulator = new LLMEventAccumulator(startingMessage.id(), responseStream, acc -> {
                safeHandle(innerHandle -> {
                    var err = acc.error();
                    if (err == null) {
                        content.setStatus(Conversation.MessageStatus.COMPLETE);
                    } else {
                        content.setStatus(Conversation.MessageStatus.FAILED);
                    }
                    content.setContent_json(stepsToJson(acc.steps()));
                    var resp = acc.completedResponse();

                    String provider_json = "{}";
                    if (resp != null) {
                        provider_json = jsonMapper().writeValueAsString(new LMStudioResponseMeta(resp.getStats(), resp.getResponseId()));
                    }
                    ChatMessage updatedContent = startingMessage.updatedContent(content).updatedProviderJson(provider_json);
                    return tableFor(ChatMessage.class).upsert(updatedContent).peek(succes -> {
                        if (resp != null && resp.getResponseId() != null) {
                            branchLastResponseId.put(updatedContent.branch_id(), resp.getResponseId());
                        }
                    }).throwAnyGet();
                }, acc).throwAnyGet();
            });
            return accumulator;
        }, request, userMessage);
    }

    protected List<MessageStep> convertUserInput(Object content) {
        if (content instanceof String str) {
            return List.of(new MessageStep.TextStep(str));
        } else if (content instanceof List list) {
            List<MessageStep> steps = new ArrayList<>();
            for (Object input : list) {
                if (input instanceof InputItem.TextInput textInput) {
                    steps.add(new MessageStep.TextStep(textInput.getContent()));
                } else if (input instanceof InputItem.ImageInput imageInput) {
                    steps.add(new MessageStep.ImageStep(imageInput.getDataUrl()));
                } else {
                    steps.addAll(convertUserInput(input));
                }
            }
            return steps;
        } else {
            return List.of();
        }
    }

    protected String userInputToJson(Object content) throws JsonProcessingException {
        List<MessageStep> steps = convertUserInput(content);
        MessageStep.MessageContent toMessageContent = toMessageContent(steps);
        return jsonMapper().writeValueAsString(toMessageContent);
    }

    protected MessageStep.MessageContent toMessageContent(List<MessageStep> steps) {
        return new MessageStep.MessageContent(1, PROVIDER, steps);
    }
    
    protected String stepsToJson(List<MessageStep> steps) throws JsonProcessingException{
        return jsonMapper().writeValueAsString(toMessageContent(steps));
    }

}
