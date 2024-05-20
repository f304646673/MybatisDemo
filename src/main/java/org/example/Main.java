package org.example;

import com.alibaba.fastjson.JSON;
import org.example.model.JsonType;

import java.util.Arrays;
import java.util.List;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    public static void main(String[] args) {

        List<JsonType.JsonElem> jsonElemList = Arrays.asList(
                new JsonType.JsonElem(1,"1"),
                new JsonType.JsonElem(2,"2")
        );
        JsonType.JsonList jsonList = new JsonType.JsonList(jsonElemList);

        String jsonStr = JSON.toJSONString(jsonList);
        JsonType.JsonList jsonList2 = JSON.parseObject(jsonStr, JsonType.JsonList.class);

        System.out.println("This is Main");
        for (JsonType.JsonElem b: jsonList2.getJsonElemList()) {
            System.out.printf("%d %s\n", b.getFirst(), b.getSecond());
        }
    }
}