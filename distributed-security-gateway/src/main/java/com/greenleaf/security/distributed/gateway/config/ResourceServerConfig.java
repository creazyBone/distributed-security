package com.greenleaf.security.distributed.gateway.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configurers.ResourceServerSecurityConfigurer;
import org.springframework.security.oauth2.provider.token.TokenStore;

@Configuration
public class ResourceServerConfig {

    public static final String RESOURCE_ID = "res1";

    @Autowired
    private TokenStore tokenStore;

    /**
     * 统一认证（资源）服务（Uaa）拦截
     */
    @Configuration
    @EnableResourceServer
    public class UaaServerConfig extends ResourceServerConfigurerAdapter{
        @Override
        public void configure(ResourceServerSecurityConfigurer resources) throws Exception {
            resources.resourceId(RESOURCE_ID)
                    .tokenStore(tokenStore)//jwt令牌的校验是通过jwtAccessTokenConverter
                    //  .tokenServices(resourceServerTokenServices())//验证令牌的服务 资源服务和授权服务在一个工程的，可以使用DefaultTokenService
                    .stateless(true);
        }

        @Override
        public void configure(HttpSecurity http) throws Exception {
            http.authorizeRequests()
                    .antMatchers("/uaa/**").permitAll();
        }
    }
    @Configuration
    @EnableResourceServer
    public class OrderServerConfig extends ResourceServerConfigurerAdapter{
        @Override
        public void configure(ResourceServerSecurityConfigurer resources) throws Exception {
            resources.resourceId(RESOURCE_ID)
                    .tokenStore(tokenStore)//jwt令牌的校验是通过jwtAccessTokenConverter
                    //  .tokenServices(resourceServerTokenServices())//验证令牌的服务 资源服务和授权服务在一个工程的，可以使用DefaultTokenService
                    .stateless(true);
        }

        @Override
        public void configure(HttpSecurity http) throws Exception {
            http.authorizeRequests()
                    .antMatchers("/order/**").access("#oauth2.hasScope('all123')");
        }
    }

    //资源服务内的令牌解析（校验）服务
/*  @Bean
    public ResourceServerTokenServices resourceServerTokenServices(){
        //使用远程授权服务器校验token，必须制定校验token的url、client_id、client_secret
        RemoteTokenServices services = new RemoteTokenServices();
        services.setCheckTokenEndpointUrl("http://localhost:53020/uaa/oauth/check_token");
        services.setClientId("c1");
        services.setClientSecret("secret");
        return services;
    }*/


}
