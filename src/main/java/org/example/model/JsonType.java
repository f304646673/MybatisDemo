package org.example.model;

import com.alibaba.fastjson.annotation.JSONField;

import java.util.ArrayList;
import java.util.List;

public class JsonType {

    public static class JsonList {
        public List<JsonElem> getJsonElemList() {
            return jsonElemList;
        }

        public void setJsonElemList(List<JsonElem> jsonElemList) {
            this.jsonElemList = jsonElemList;
        }
        @JSONField()
        private List<JsonElem> jsonElemList = new ArrayList<>();

        public JsonList(List<JsonElem> jsonElemList) {
            this.jsonElemList.addAll(jsonElemList);
        }
    }

    public static class JsonElem implements Cloneable {
        public int getFirst() {
            return first;
        }

        public void setFirst(int first) {
            this.first = first;
        }

        public String getSecond() {
            return second;
        }

        public void setSecond(String second) {
            this.second = second;
        }

        @JSONField(name = "First")
        private int first;
        @JSONField(name = "Second")
        private String second;

        public JsonElem(int first, String second) {
            this.first = first;
            this.second = second;
        }

        @Override
        public JsonElem clone() throws CloneNotSupportedException {
            JsonElem clonedJsonElem = null;
            try {
                clonedJsonElem = (JsonElem) super.clone();
                clonedJsonElem.setFirst(this.getFirst());
                clonedJsonElem.setSecond(this.getSecond());
            } finally {
            }

            return clonedJsonElem;
        }
    }

    public int getIntInfo() {
        return intInfo;
    }

    public void setIntInfo(int intInfo) {
        this.intInfo = intInfo;
    }

    public JsonList getJsonElemList() {
        return jsonElemList;
    }

    public void setJsonElemList(JsonList jsonElemList) {
        this.jsonElemList = jsonElemList;
    }

    private int intInfo;

    private JsonList jsonElemList;
}
