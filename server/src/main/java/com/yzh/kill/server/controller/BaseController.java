package com.yzh.kill.server.controller;

import com.yzh.kill.api.enums.StatusCode;
import com.yzh.kill.api.response.BaseResponse;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.logging.Logger;

@Controller
@RequestMapping("base")
public class BaseController {
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(BaseController.class);
    /**
     * 跳转页面
     * @param name
     * @param modelMap
     * @return 返回跳转页面
     */
    @GetMapping("/welcome")
    public String  welcome(String name, ModelMap modelMap){
        if(StringUtils.isBlank(name)){
            name = "hello seckill";
        }
        modelMap.put("name", name);
        return "welcome";
    }
    /**
     * 前端发起请求获取数据,不跳转页面s
     * @param name
     * @return 返回json
     */
    @RequestMapping(value = "/data", method = RequestMethod.GET)
    @ResponseBody
    public  String data(String name){
        if(StringUtils.isBlank(name)){
            name = "hello seckill";
        }
        return name;
    }

    /**
     * 标准请求-响应数据格式
     * @param name
     * @return
     */
    @RequestMapping(value = "/response", method = RequestMethod.GET)
    @ResponseBody
    public BaseResponse response(String name){
        BaseResponse response = new BaseResponse(StatusCode.Success);
        if(StringUtils.isBlank(name)){
            name = "hello seckill";
        }
        response.setData(name);
        return response;
    }


}
