package com.github.laim0nas100.llm4j.lmstudio.v1.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.laim0nas100.commonslb.iteration.streams.MakeStream;
import com.github.laim0nas100.llm4j.GlobalCTX;
import com.github.laim0nas100.llm4j.lmstudio.v1.ChatRequest;
import com.github.laim0nas100.llm4j.lmstudio.v1.InputItem;
import com.github.laim0nas100.llm4j.lmstudio.v1.LLMEvent;
import com.github.laim0nas100.llm4j.store.Conversation.ChatMessage;
import com.github.laim0nas100.llm4j.store.Helpers;
import com.github.laim0nas100.llm4j.store.MessageStep;
import com.github.laim0nas100.llm4j.store.SqlService;
import com.github.laim0nas100.llm4j.store.TableCreation;
import com.github.laim0nas100.uncheckedutils.SafeOpt;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 *
 * @author Lemmin
 */
public class LMStudioPersistentClient extends LMStudioChatStore {

    public static final String PROVIDER = "LM-Studio";

    protected LMStudioChattingClient client;

    public LMStudioPersistentClient(SqlService sqlService, TableCreation tableCreation, LMStudioChattingClient client) {
        super(sqlService, tableCreation);
        this.client = Objects.requireNonNull(client);
    }

    public SafeOpt startNewChat(String templateName, Object input, List<String> attachments) {
        return getConfig(templateName).map(config -> {
            ChatRequest chatRequest = new ChatRequest(config);
            chatRequest.setInput(input);
            safeHandle(handle -> {
                var conv = createConversation(templateName).throwAnyGet();
                var branchHold = Helpers.ChatBranchHold.newRootBranch(conv.id());
                var branch = createBranch(branchHold).throwAnyGet();
                var ids = new Helpers.MessageIdHold();
                ids.setBranchId(branch.id());
                ids.setConversationId(conv.id());
                ids.setParentMessageId(null);
                ids.setTemplateId(conv.chat_default_template_id());

                var content = Helpers.ChatContentHold.userMessage(PROVIDER, userInputToJson(input));
                var proxy = Helpers.MessageProxyHold.noProxy();
                var message = createMessage(ids, content, proxy).throwAnyGet();
                
                return null;
            });
            
            return null;
        });
    }
    
    protected SafeOpt createLLMResponse(ChatRequest request, ChatMessage userMessage){
        return safeHandle(handle ->{
            
            var ids = Helpers.MessageIdHold.respondingTo(userMessage);
            var proxy = Helpers.MessageProxyHold.noProxy();
            var content = Helpers.ChatContentHold.startAssitantResponse(PROVIDER);
            
            var responseStart = createMessage(ids, content, proxy).throwAnyGet();
            Stream<LLMEvent> responseStream = client.sendAsyncStream(request);

            LLMEventAccumulator accumulator = new LLMEventAccumulator(responseStream);
            return null;
        }, request,userMessage);
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
        return jsonMapper().writeValueAsString(convertUserInput(content));
    }

}
