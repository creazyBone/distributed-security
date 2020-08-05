package com.greenleaf.security.distributed.order.filter;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.greenleaf.security.distributed.order.common.EncryptUtil;
import com.greenleaf.security.distributed.order.model.UserDto;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class TokenAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, FilterChain filterChain) throws ServletException, IOException {
        //解析出头中的token
        String token = httpServletRequest.getHeader("json-token");
        if (token!=null){
            String json = EncryptUtil.decodeUTF8StringBase64(token);
            //将token转成json对象
            JSONObject jsonObject = JSON.parseObject(json);
            //获取用户身份信息
            String principal = jsonObject.getString("principal");
            UserDto userDto = new UserDto();
            userDto.setUsername(principal);
            //获取用户权限
            JSONArray authoritiesJsonArray = jsonObject.getJSONArray("authorities");
            String[] authoritiesArray = authoritiesJsonArray.toArray(new String[authoritiesJsonArray.size()]);
            //将用户信息和权限填充到SpringSecurity能识别的token对象中
            UsernamePasswordAuthenticationToken authenticationToken//这里用userDto替换了原先的principal
                    = new UsernamePasswordAuthenticationToken(userDto,null, AuthorityUtils.createAuthorityList(authoritiesArray));
            authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(httpServletRequest));
            //将authenticationToken填充到安全上下文
            SecurityContextHolder.getContext().setAuthentication(authenticationToken);

        }
        filterChain.doFilter(httpServletRequest,httpServletResponse);
    }
}
