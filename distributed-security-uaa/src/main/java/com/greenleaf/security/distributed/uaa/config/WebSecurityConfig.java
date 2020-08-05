package com.greenleaf.security.distributed.uaa.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

//由于用了springboot，@EnableWebSecurity可以不写
@Configuration
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

    //认证管理器
    @Bean
    @Override
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }

    @Bean
    //密码编码器
    public PasswordEncoder passwordEncoder() {
        //return NoOpPasswordEncoder.getInstance();
        return new BCryptPasswordEncoder();
    }

    //安全拦截机制
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.csrf().disable()//禁用对csrf的限制,开启时会限制除了get以外的大多数方法
                    .authorizeRequests()
         //         .antMatchers("/login*").permitAll()
               /**
                 *  有  .antMatchers("/login*").permitAll()时：
                 *
                 *  浏览器输入http://localhost:53020/uaa/aaa
                 *  There was an unexpected error (type=Forbidden, status=403).
                 * Access Denied
                 *
                 * 浏览器输入http://localhost:53020/uaa/login2
                 * There was an unexpected error (type=Not Found, status=404).
                 * No handler found for GET /uaa/login2
                 */
                    .anyRequest().authenticated()
                .and()
                    .formLogin();


    }

}

