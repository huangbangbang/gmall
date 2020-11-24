package com.atguigu.gmall.passport.controller;

import com.atguigu.gmall.util.HttpclientUtil;

public class TestOauth2 {
    public static void main(String[] args) {
        //String s1 = HttpclientUtil.doGet("https://api.weibo.com/oauth2/authorize?client_id=3387532055&response_type=code&redirect_uri=http://passport.gmall.com:8087/vlogin");
        //System.out.println(s1);

        String s2 = "http://passport.gmall.com:8087/vlogin?code=5155b7fe7d2cbe19c1c4b2afa2c1074e";

        //5155b7fe7d2cbe19c1c4b2afa2c1074e

        String s3 = "https://api.weibo.com/oauth2/access_token?client_id=3387532055&client_secret=0f61e35eec96860c222b7e8383d9e34c&grant_type=authorization_code&redirect_uri=http://passport.gmall.com:8087/vlogin&code=8ada190b080ecf4756592bc58d27c22a";

        String access_token = HttpclientUtil.doPost(s3, null);

        System.out.println(access_token);

    }
}
