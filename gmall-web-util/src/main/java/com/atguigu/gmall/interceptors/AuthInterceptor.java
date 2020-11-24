package com.atguigu.gmall.interceptors;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.annotations.LoginRequired;
import com.atguigu.gmall.util.CookieUtil;
import com.atguigu.gmall.util.HttpclientUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

@Component
public class AuthInterceptor extends HandlerInterceptorAdapter{


        public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
            /*String newToken = request.getParameter("newToken");
            if(newToken!=null&&newToken.length()>0){
                CookieUtil.setCookie(request,response,"token",newToken,WebConst.cookieExpire,false);
            }*/

            HandlerMethod hm = (HandlerMethod) handler;
            LoginRequired methodAnnotation = hm.getMethodAnnotation(LoginRequired.class);

            if (methodAnnotation==null){
                return true;
            }
            String token = "";

            String oldToken = CookieUtil.getCookieValue(request,"oldToken",true);
            if (StringUtils.isNotBlank(oldToken)){
                token=oldToken;
            }

            String newToken = request.getParameter("token");
            if (StringUtils.isNotBlank(newToken)){
                token=newToken;
            }
            boolean loginSuccess = methodAnnotation.loginSuccess();
            Map<String,String> successMap = new HashMap<>();
            String success = "fail";
            if (StringUtils.isNotBlank(token)){
                String ip = request.getHeader("x-forwarded-for");
                if (StringUtils.isBlank(ip)){
                    ip = request.getRemoteAddr();
                    if (StringUtils.isBlank(ip)){
                        ip = "127.0.0.1";
                    }
                }
                String successJson = HttpclientUtil.doGet("http://passport.gmall.com:8087/verify?token="+token+"&currentIp="+ip);

                successMap = JSON.parseObject(successJson,Map.class);
                success = successMap.get("status");
            }

            if (loginSuccess) {
                if (!success.equals("success")) {
                    StringBuffer requestURL = request.getRequestURL();
                    response.sendRedirect("http://passport.gmall.com:8087/index?ReturnUrl="+requestURL);
                    return false;
                }
                request.setAttribute("memberId", successMap.get("memberId"));
                request.setAttribute("nickname", successMap.get("nickname"));
                if (StringUtils.isNotBlank(token)){
                    CookieUtil.setCookie(request,response,"oldToken",token,60*60*2,true);
                }
            }else {
                if (success.equals("success")){
                    request.setAttribute("memberId", successMap.get("memberId"));
                    request.setAttribute("nickname", successMap.get("nickname"));
                    if (StringUtils.isNotBlank(token)){
                        CookieUtil.setCookie(request,response,"oldToken",token,60*60*2,true);
                    }
                }
            }

            return true;
        }
}
