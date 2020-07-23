package com.greenleaf.security.distributed.uaa.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.config.annotation.builders.InMemoryClientDetailsServiceBuilder;
import org.springframework.security.oauth2.config.annotation.configurers.ClientDetailsServiceConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configuration.AuthorizationServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableAuthorizationServer;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerEndpointsConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerSecurityConfigurer;
import org.springframework.security.oauth2.provider.ClientDetailsService;
import org.springframework.security.oauth2.provider.token.AuthorizationServerTokenServices;
import org.springframework.security.oauth2.provider.token.DefaultAccessTokenConverter;
import org.springframework.security.oauth2.provider.token.DefaultTokenServices;
import org.springframework.security.oauth2.provider.token.TokenStore;

@Configuration
@EnableAuthorizationServer
public class AuthorizationServer extends AuthorizationServerConfigurerAdapter {

    @Autowired
    private TokenStore tokenStore;

    //从何而来？参见下面的配置原理及程序执行步骤
    @Autowired
    private ClientDetailsService clientDetailsService;


    public AuthorizationServerTokenServices authorizationServerTokenServices() {
        DefaultTokenServices service = new DefaultTokenServices();
        service.setClientDetailsService(clientDetailsService);
        service.setSupportRefreshToken(true);
        service.setTokenStore(tokenStore);
        service.setAccessTokenValiditySeconds(7200);//令牌默认有效期2小时
        service.setRefreshTokenValiditySeconds(259200);//刷新令牌默认有效期3天
        return service;
    }


    /**
     * 源码分析：
     *
     * 0）：
     *      ClientDetailsServiceConfiguration这个类很有用！很有用！很有用！好玩的都在此！
     *
     *      a)  ClientDetailsServiceConfigurer会在ClientDetailsServiceConfiguration类中被创建并放入ioc容器
     *      b)  此外ClientDetailsService也是在这个类中通过build()方法来创建并放入ioc容器的，详细代码如下：
     *          @Bean
     *          @Lazy
     *          @Scope(
     *              proxyMode = ScopedProxyMode.INTERFACES
     *          )
     *          public ClientDetailsService clientDetailsService() throws Exception {
     *              return ((ClientDetailsServiceBuilder)this.configurer.and()).build();
     *          }
     *
     *      两个细节：
     *              第一，在上面代码中，proxyMode属性，被设置成了ScopedProxyMode.INTERFACES。
     *              这表明ioc容器初始化时，会有一个ClientDetailsService的代理类来完成装配。
     *              而且这个代理要实现ClientDetailsService接口（区别于ScopedProxyMode.TARGET_CLASS），并将调用委托给实现bean。
     *
     *              第二，使用@Lazy正是为了配合“将调用委托给实现bean”.即：等需要用到ClientDetailsService时（注入不算），再去生成ClientDetailsService实现，
     *              而此时用户早已自定义完ClientBuilder的配置（clients.inMemory()。。。。）。
     *              因此，通过and()方法（此方法实际上是返回一个ClientDetailsServiceBuilder的子类，如
     *              InMemoryClientDetailsServiceBuilder），子类中没有重写build()方法，于是调用父类的方法，但是
     *              this.addClient(clientDetailsBldr.clientId, clientDetailsBldr.build())和this.performBuild()这两个方法
     *              都是调用子类（InMemoryClientDetailsServiceBuilder）中重写的方法。
     *
     * 1）：
     * ClientDetailsServiceBuilder类中维护了private List<ClientDetailsServiceBuilder<B>.ClientBuilder> clientBuilders = new ArrayList();
     *
     * 2）：
     * 遍历clientBuilders：
     *     public ClientDetailsService build() throws Exception {
     *          Iterator var1 = this.clientBuilders.iterator();
     *          while(var1.hasNext()) {
     *              ClientDetailsServiceBuilder<B>.ClientBuilder clientDetailsBldr = (ClientDetailsServiceBuilder.ClientBuilder)var1.next();
     *              this.addClient(clientDetailsBldr.clientId, clientDetailsBldr.build());
     *          }
     *         return this.performBuild();
     *      }
     * clientDetailsBldr.build() --->调用每一个builder的build(),返回ClientDetails类型
     *
     *3）：
     * this.addClient(clientDetailsBldr.clientId, clientDetailsBldr.build());父类的这个方法为空方法，交给子类实现：
     * 在InMemoryClientDetailsServiceBuilder子类中：
     *     private Map<String, ClientDetails> clientDetails = new HashMap();
     *     protected void addClient(String clientId, ClientDetails value) {
     *          this.clientDetails.put(clientId, value);
     *     }
     *
     * 4）：
     *同样，父类中：
     *     protected ClientDetailsService performBuild() {
     *          throw new UnsupportedOperationException("Cannot build client services (maybe use inMemory() or jdbc()).");
     *     }
     * 在InMemoryClientDetailsServiceBuilder子类中重写该方法，并被调用：
     *        protected ClientDetailsService performBuild() {
     *          InMemoryClientDetailsService clientDetailsService = new InMemoryClientDetailsService();
     *          clientDetailsService.setClientDetailsStore(this.clientDetails);
     *          return clientDetailsService;
     *        }
     *  注意，最终返回的是InMemoryClientDetailsService类型，此类无非也就是实现了以下方法：
     *      public ClientDetails loadClientByClientId(String clientId) throws ClientRegistrationException {
     *          ClientDetails details = (ClientDetails)this.clientDetailsStore.get(clientId);
     *         if (details == null) {
     *              throw new NoSuchClientException("No client with requested id: " + clientId);
     *         } else {
     *              return details;
     *         }
     *      }
     * 5）：withClient()是父类的方法：
     *  public ClientDetailsServiceBuilder<B>.ClientBuilder withClient(String clientId)｛
     *       ClientDetailsServiceBuilder<B>.ClientBuilder clientBuilder = new ClientDetailsServiceBuilder.ClientBuilder(clientId, null);
     *      this.clientBuilders.add(clientBuilder); //配置一个ClientBuilder就放到clientBuilders里一个，可以放多个
     *      return clientBuilder;
     *  ｝
     *  真正调用此方法的是子类InMemoryClientDetailsServiceBuilder的实例。但是做的事情是给与他密切关联的父类（子类实例化时也会为父类的属性分配堆内存，
     *  但是这块内存属于子类的堆内存）
     * ）的clientBuilders里放配置好的ClientBuilder，好一个摸不着的容器~~~~
     *
     *

     * 配置原理及程序执行步骤：
     * step1：
     * ClientDetailsServiceConfiguration 创建ClientDetailsServiceConfigurer 并持有ClientDetailsServiceBuilder
     * step2：
     * public void configure(ClientDetailsServiceConfigurer clients) throws Exception 方法
     * ClientDetailsServiceConfigurer变成持有InMemoryClientDetailsServiceBuilder
     * step3
     * 调用ClientDetailsService时（对应@Lazy），调用ClientDetailsServiceConfigurer持有的InMemoryClientDetailsServiceBuilder
     * 的build()方法，在父类build()方法中实际执行的是子类的addClient()和performBuild(),进而得到InMemoryClientDetailsService
     *
     *
     * 总结：套路就是先有个默认配置-->定制配置替换掉默认配置-->懒加载的方式在使用组件时把定制后的组件加载进来
     */

    //配置如何获取ClientDetailsService
    @Override
    public void configure(ClientDetailsServiceConfigurer clients) throws Exception {
        //clients.withClientDetails(clientDetailsService);

        //用内存来保存。此方法返回一个InMemoryClientDetailsServiceBuilder，
        //并且把ClientDetailsServiceConfigurer原先持有的securityBuilder替换成InMemoryClientDetailsServiceBuilder
        clients.inMemory()
                .withClient("c1")//客户端id 此方法返回ClientDetailsServiceBuilder.ClientBuilder
                .secret(new BCryptPasswordEncoder().encode("secret"))//，密钥
                .resourceIds("res1")//资源列表
                //允许的授权类型（授权模式），表示客户端可以哪种类型来申请令牌
                .authorizedGrantTypes("authorization_code", "password", "client_credentials", "implicit", "refresh_token")
                .scopes("all")//用来限制客户端的访问范围，如果为空（默认）的话，那么客户端拥有全部的访问范围。
                .autoApprove(false)//false跳转到授权页面，true就不跳转了
                .redirectUris("http://www.baidu.com");//验证回调地址
    }


    public void configure(AuthorizationServerSecurityConfigurer security) throws Exception {
    }


    public void configure(AuthorizationServerEndpointsConfigurer endpoints) throws Exception {
    }
}
