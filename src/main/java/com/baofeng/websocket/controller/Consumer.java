package com.baofeng.websocket.controller;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.alibaba.fastjson.JSON;
import com.baofeng.websocket.model.People;

@Controller
@EnableAutoConfiguration
public class Consumer {
    @RequestMapping("/echo")
    @ResponseBody
    String home(@RequestParam String name) {
        return "Hello"+name;
    }
    
    @RequestMapping("/getName")
    @ResponseBody
    String getName(@RequestParam String jsonData) {
    	People people = JSON.parseObject(jsonData, People.class);
    	return people.getName();
    }
    

}
