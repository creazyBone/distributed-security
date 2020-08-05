package com.greenleaf.security.distributed.gateway.filter;

import com.alibaba.fastjson.JSON;
import com.greenleaf.security.distributed.gateway.common.EncryptUtil;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.exception.ZuulException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.OAuth2Request;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class AuthFilter extends ZuulFilter{
    @Override
    public String filterType() {
        return "pre";
    }

    @Override
    public int filterOrder() {
        return 0;
    }

    @Override
    public boolean shouldFilter() {
        return true;
    }

    @Override
    public Object run() throws ZuulException {
        //获取令牌内容（认证信息）
        RequestContext ctx = RequestContext.getCurrentContext();
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if(!(authentication instanceof OAuth2Authentication)){
            return null;//无token访问网关内资源的情况：目前只有uaa服务直接暴露
        }
        /**
         * https://blog.csdn.net/qq_30905661/article/details/81112305
         *
         * OAuth2Authentication顾名思义是Authentication的子类，存储用户信息和客户端信息，但多了2个属性:
         *  private final OAuth2Request storedRequest;
         *  private final Authentication userAuthentication;
         *
         * 这样OAuth2Authentication可以存储2个Authentication，一个给client(必要)，
         * 一个给user(只是有些授权方式需要,比如密码模式)。除此之外同样有principle，credentials，authorities，details，authenticated等属性。
         */
        OAuth2Authentication oAuth2Authentication = (OAuth2Authentication)authentication;
        Authentication userAuthentication = oAuth2Authentication.getUserAuthentication();
        if(userAuthentication!=null) {
            //获取当前用户的身份信息
            String principal = userAuthentication.getName();
            //获取当前用户的权限信息
            List<String> authorities = new ArrayList<>();
            //从userAuthentication中取出权限，放入authorities
            userAuthentication.getAuthorities().stream().forEach(c -> authorities.add(c.getAuthority()));

            //取出一些oAuth2Authentication中的其他信息，合并principle，authorities，（userAuthentication中来的认证信息）一起生成明文token
            OAuth2Request oAuth2Request = oAuth2Authentication.getOAuth2Request();
            Map<String, String> requestParameters = oAuth2Request.getRequestParameters();
            //转成HashMap<String, Object>，方便放authorities
            HashMap<String, Object> jsonToken = new HashMap<>(requestParameters);
            //  if(userAuthentication!=null){
            jsonToken.put("principal", principal);
            jsonToken.put("authorities", authorities);
            //   }
            //把身份信息和权限信息封装成json数据，放入http的header中,转发给对应的微服务
            ctx.addZuulRequestHeader("json-token", EncryptUtil.encodeUTF8StringBase64(JSON.toJSONString(jsonToken)));
        }
        return null;
    }
}
