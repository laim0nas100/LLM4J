package com.github.laim0nas100.llm4j.lmstudio.v1.impl;

import com.github.laim0nas100.llm4j.lmstudio.v1.LLMEvent;
import com.github.laim0nas100.llm4j.lmstudio.v1.LLMEvent.ChatEnd;
import com.github.laim0nas100.llm4j.lmstudio.v1.LLMEvent.ChatStart;
import com.github.laim0nas100.llm4j.lmstudio.v1.LLMEvent.LLMError;
import com.github.laim0nas100.llm4j.lmstudio.v1.LLMEvent.MessageDelta;
import com.github.laim0nas100.llm4j.lmstudio.v1.LLMEvent.MessageEnd;
import com.github.laim0nas100.llm4j.lmstudio.v1.LLMEvent.MessageStart;
import com.github.laim0nas100.llm4j.lmstudio.v1.LLMEvent.PromptProcessingEnd;
import com.github.laim0nas100.llm4j.lmstudio.v1.LLMEvent.PromptProcessingProgress;
import com.github.laim0nas100.llm4j.lmstudio.v1.LLMEvent.PromptProcessingStart;
import com.github.laim0nas100.llm4j.lmstudio.v1.LLMEvent.ReasoningDelta;
import com.github.laim0nas100.llm4j.lmstudio.v1.LLMEvent.ReasoningEnd;
import com.github.laim0nas100.llm4j.lmstudio.v1.LLMEvent.ReasoningStart;
import com.github.laim0nas100.llm4j.lmstudio.v1.LLMException;
import com.github.laim0nas100.llm4j.lmstudio.v1.LLMToolEvent;
import com.github.laim0nas100.llm4j.lmstudio.v1.LLMToolEvent.ToolCallStart;
import com.github.laim0nas100.llm4j.lmstudio.v1.LLMToolEvent.ToolCallSuccess;
import com.github.laim0nas100.llm4j.store.MessageStep;
import com.github.laim0nas100.llm4j.store.MessageStep.ErrorStep;
import com.github.laim0nas100.llm4j.store.MessageStep.ReasoningStep;
import com.github.laim0nas100.llm4j.store.MessageStep.TextStep;
import com.github.laim0nas100.llm4j.store.MessageStep.ToolCallStep;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.stream.Stream;

public final class LLMEventAccumulator implements Iterator<LLMEvent>, AutoCloseable {

    private final Stream<LLMEvent> stream;
    private final Iterator<LLMEvent> source;
    private final State state = new State();

    private boolean closed;

    public LLMEventAccumulator(Stream<LLMEvent> stream) {
        this.stream = Objects.requireNonNull(stream);
        this.source = stream.iterator();
    }

    public record ResponseContent(
            int version,
            List<MessageStep> steps,
            boolean completed,
            LLMException error
            ) {

        public ResponseContent {
            steps = List.copyOf(steps);
        }
    }

    @Override
    public boolean hasNext() {
        if (closed) {
            return false;
        }

        try {
            if (source.hasNext()) {
                return true;
            }

            // Unexpected end of stream without chat.end.
            close();
            return false;
        } catch (RuntimeException e) {
            close();
            throw e;
        }
    }

    @Override
    public LLMEvent next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }

        try {
            LLMEvent event = source.next();

            // Update accumulated content before exposing the event.
            state.accept(event);

            // Return chat.end to the caller, but close immediately.
            if (event instanceof ChatEnd) {
                state.finish();
                close();
            }

            return event;
        } catch (RuntimeException e) {
            close();
            throw e;
        }
    }

    /**
     * Returns an immutable snapshot of the accumulated content so far. Safe to
     * call between events.
     */
    public ResponseContent currentContent() {
        return state.snapshot();
    }

    public boolean isClosed() {
        return closed;
    }

    @Override
    public void close() {
        if (!closed) {
            closed = true;
            stream.close();
        }
    }

    public static ResponseContent accumulate(Stream<LLMEvent> stream) {

        Objects.requireNonNull(stream);

        try (stream) {
            var accumulator = new State();
            var iterator = stream.iterator();

            while (iterator.hasNext()) {
                LLMEvent event = iterator.next();

                if (event == null) {
                    continue;
                }

                if (accumulator.accept(event)) {
                    // chat.end received: do not consume any more events.
                    break;
                }
            }

            return accumulator.finish();
        }
    }

    private static final class State {

        private final List<MessageStep> steps = new ArrayList<>();

        private int activeTextIndex = -1;
        private int activeReasoningIndex = -1;
        private int activeToolIndex = -1;

        private LLMToolEvent lastToolEvent;
        private LLMException terminalError;
        private boolean chatEnded;

        /**
         * Returns true when chat.end is received.
         */
        boolean accept(LLMEvent event) {

            // Chat lifecycle
            if (event instanceof ChatStart) {
                return false;
            }

            if (event instanceof ChatEnd) {
                closeOpenSteps();
                chatEnded = true;
                return true;
            }

            // Prompt processing lifecycle.
            // These events are not message content.
            if (event instanceof PromptProcessingStart
                    || event instanceof PromptProcessingProgress
                    || event instanceof PromptProcessingEnd) {
                return false;
            }

            // Assistant text
            if (event instanceof MessageStart) {
                closeText();
                activeTextIndex = steps.size();
                steps.add(new TextStep(""));
                return false;
            }

            if (event instanceof MessageDelta delta) {
                ensureTextStep();
                TextStep current = (TextStep) steps.get(activeTextIndex);

                steps.set(
                        activeTextIndex,
                        new TextStep(current.text() + delta.content())
                );
                return false;
            }

            if (event instanceof MessageEnd) {
                closeText();
                return false;
            }

            // Assistant reasoning
            if (event instanceof ReasoningStart) {
                closeReasoning();
                activeReasoningIndex = steps.size();
                steps.add(new ReasoningStep(""));
                return false;
            }

            if (event instanceof ReasoningDelta delta) {
                ensureReasoningStep();
                ReasoningStep current
                        = (ReasoningStep) steps.get(activeReasoningIndex);

                steps.set(
                        activeReasoningIndex,
                        new ReasoningStep(
                                current.text() + delta.content()
                        )
                );
                return false;
            }

            if (event instanceof ReasoningEnd) {
                closeReasoning();
                return false;
            }

            // Tool calls
            if (event instanceof LLMToolEvent toolEvent) {
                acceptToolEvent(toolEvent);
                return false;
            }

            // Errors
            if (event instanceof LLMError errorEvent) {
                LLMException error = errorEvent.error();

                if (activeToolIndex >= 0) {
                    // The tool failed or was interrupted, but the assistant
                    // may still continue and complete its response.
                    steps.set(
                            activeToolIndex,
                            new ToolCallStep(lastToolEvent, error)
                    );

                    activeToolIndex = -1;
                    lastToolEvent = null;
                } else {
                    steps.add(new ErrorStep(error));
                    terminalError = error;
                }

                return false;
            }

            // Unknown event: deliberately ignore it for now.
            return false;
        }

        private void acceptToolEvent(LLMToolEvent event) {
            if (event instanceof ToolCallStart) {
                // A new tool call begins.
                activeToolIndex = steps.size();
                lastToolEvent = event;
                steps.add(new ToolCallStep(event));
                return;
            }

            if (activeToolIndex < 0) {
                // Defensive fallback in case the stream starts midway
                // through a tool call.
                activeToolIndex = steps.size();
                lastToolEvent = event;
                steps.add(new ToolCallStep(event));
                return;
            }

            lastToolEvent = event;

            // Keep the latest cumulative event, as discussed.
            steps.set(
                    activeToolIndex,
                    new ToolCallStep(lastToolEvent)
            );

            if (event instanceof ToolCallSuccess) {
                activeToolIndex = -1;
                lastToolEvent = null;
            }
        }

        private void ensureTextStep() {
            if (activeTextIndex < 0) {
                activeTextIndex = steps.size();
                steps.add(new TextStep(""));
            }
        }

        private void ensureReasoningStep() {
            if (activeReasoningIndex < 0) {
                activeReasoningIndex = steps.size();
                steps.add(new ReasoningStep(""));
            }
        }

        private void closeText() {
            activeTextIndex = -1;
        }

        private void closeReasoning() {
            activeReasoningIndex = -1;
        }

        private void closeOpenSteps() {
            closeText();
            closeReasoning();

            // Preserve a tool call that was still in progress at chat.end.
            // It remains represented by its latest event.
            activeToolIndex = -1;
            lastToolEvent = null;
        }

        ResponseContent finish() {
            chatEnded = true;
            closeOpenSteps();

            return snapshot();
        }

        ResponseContent snapshot() {
            return new ResponseContent(
                    1,
                    steps,
                    chatEnded && terminalError == null,
                    terminalError
            );
        }
    }

}
