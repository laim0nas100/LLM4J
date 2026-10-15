package com.github.laim0nas100.llm4j.lmstudio.v1;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.github.laim0nas100.llm4j.lmstudio.v1.InputItem.ImageInput;
import com.github.laim0nas100.llm4j.lmstudio.v1.InputItem.TextInput;

/**
 *
 * @author Lemmin
 */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type"
)
@JsonSubTypes({
    @JsonSubTypes.Type(
            value = TextInput.class,
            name = "text"
    ),
    @JsonSubTypes.Type(
            value = ImageInput.class,
            name = "image"
    )
})
public interface InputItem {

    public static class TextInput implements InputItem {

        private String content;

        public TextInput() {
        }

        public TextInput(String content) {
            this.content = content;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }
    }

    public static class ImageInput implements InputItem {

        @JsonProperty("data_url")
        private String dataUrl;

        public ImageInput() {
        }

        public ImageInput(String dataUrl) {
            this.dataUrl = dataUrl;
        }

        public String getDataUrl() {
            return dataUrl;
        }

        public void setDataUrl(String dataUrl) {
            this.dataUrl = dataUrl;
        }
    }
}
