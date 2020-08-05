package com.greenleaf.security.distributed.uaa.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.config.annotation.builders.InMemoryClientDetailsServiceBuilder;
import org.springframework.security.oauth2.config.annotation.builders.JdbcClientDetailsServiceBuilder;
import org.springframework.security.oauth2.config.annotation.configurers.ClientDetailsServiceConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configuration.AuthorizationServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableAuthorizationServer;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerEndpointsConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerSecurityConfigurer;
import org.springframework.security.oauth2.provider.ClientDetailsService;
import org.springframework.security.oauth2.provider.client.JdbcClientDetailsService;
import org.springframework.security.oauth2.provider.code.AuthorizationCodeServices;
import org.springframework.security.oauth2.provider.code.InMemoryAuthorizationCodeServices;
import org.springframework.security.oauth2.provider.code.JdbcAuthorizationCodeServices;
import org.springframework.security.oauth2.provider.token.*;
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.sql.DataSource;
import java.util.Arrays;

/**
 * 认证（授权）服务
 */
@Configuration
@EnableAuthorizationServer
public class AuthorizationServer extends AuthorizationServerConfigurerAdapter {

    @Autowired
    private TokenStore tokenStore;

    //从何而来？参见note.txt
    @Autowired
    private ClientDetailsService clientDetailsService;

    //授权码服务
    @Autowired
    private AuthorizationCodeServices authorizationCodeServices;

    //认证管理器
    @Autowired
    private AuthenticationManager authenticationManager;

    //jwt令牌转换器
    @Autowired
    private JwtAccessTokenConverter jwtAccessTokenConverter;

    @Autowired
    private PasswordEncoder passwordEncoder;

/*  客户端详情配置在数据库中时使用
    @Bean
    public ClientDetailsService getClientDetailsService(@Qualifier("dataSource") DataSource dataSource) {
        ClientDetailsService clientDetailsService = new JdbcClientDetailsService(dataSource);
        ((JdbcClientDetailsService) clientDetailsService).setPasswordEncoder(passwordEncoder);
        return clientDetailsService;
    }*/

    //令牌服务
    public AuthorizationServerTokenServices authorizationServerTokenServices() {
        DefaultTokenServices service = new DefaultTokenServices();
        service.setClientDetailsService(clientDetailsService);
        service.setSupportRefreshToken(true);//支持刷新令牌
        service.setTokenStore(tokenStore);//令牌存储策略

        //令牌增强
        TokenEnhancerChain tokenEnhancerChain = new TokenEnhancerChain();
        tokenEnhancerChain.setTokenEnhancers(Arrays.asList(jwtAccessTokenConverter));
        service.setTokenEnhancer(tokenEnhancerChain);

        service.setAccessTokenValiditySeconds(7200);//令牌默认有效期2小时
        service.setRefreshTokenValiditySeconds(259200);//刷新令牌默认有效期3天
        return service;
    }

    //暂时采用内存方式的授权码服务，实际生产环境中由于可能是集群环境，不会使用这种方式，否则会有单点问题
    @Bean
    public AuthorizationCodeServices authorizationCodeServices() {
      //  return new JdbcAuthorizationCodeServices();//授权码存在数据库
        return new InMemoryAuthorizationCodeServices();
    }


    //配置ClientBuilder用来返回ClientDetailsService
    @Override
    public void configure(ClientDetailsServiceConfigurer clients) throws Exception {
        //clients.withClientDetails(clientDetailsService);//客户端详情配置在数据库中时使用

        //用内存来保存。此方法返回一个InMemoryClientDetailsServiceBuilder，
        //并且把ClientDetailsServiceConfigurer原先持有的securityBuilder替换成InMemoryClientDetailsServiceBuilder
        clients.inMemory()
                .withClient("c1")//客户端id 此方法返回ClientDetailsServiceBuilder.ClientBuilder
                .secret(new BCryptPasswordEncoder().encode("secret"))//，密钥
                .resourceIds("res1")//资源列表
                //允许的授权类型（授权模式），表示客户端可以哪种类型来申请令牌
                .authorizedGrantTypes("authorization_code", "password", "client_credentials", "implicit", "refresh_token")
                .scopes("all123")//用来限制客户端的访问范围，如果为空（默认）的话，那么客户端拥有全部的访问范围。注意，这个“all”仅仅是一个标识，也可以是“aaa”
                //而且经测试，在密码模式下，在请求体中这个scope可以不带，但是一旦带上这个参数就必须与此处配置的一致！
                .autoApprove(false)//false跳转到授权页面，true就不跳转了
                .redirectUris("http://www.baidu.com");//验证回调地址
    }


    //配置令牌访问端点（个人理解：令牌其实就是一个“用户通过认证”后的信息载体,这么想就不难理解为什么要配置认证管理器了）
    public void configure(AuthorizationServerEndpointsConfigurer endpoints) throws Exception {
        endpoints
                .authenticationManager(authenticationManager)//认证管理器
                .authorizationCodeServices(authorizationCodeServices)//授权码服务
                .tokenServices(authorizationServerTokenServices())//令牌服务
                .allowedTokenEndpointRequestMethods(HttpMethod.POST);
    }

    //配置令牌端点安全约束
    public void configure(AuthorizationServerSecurityConfigurer security) throws Exception {
        security
                .tokenKeyAccess("permitAll()")//   /oauth/token_key是公开的，用户不用登录就可以访问这个url来去密钥，比如给内网资源服务来访问
                .checkTokenAccess("permitAll()")// /oauth/check_token是公开的，内网资源服务可以校验一下令牌的合法性
                .allowFormAuthenticationForClients();//表单认证（申请令牌）
    }
}
