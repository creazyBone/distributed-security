package com.greenleaf.security.distributed.uaa.service;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class MyUserDetailsService implements UserDetailsService{

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        //模拟数据库查询
        System.out.println("username="+username);
        UserDetails userDetails = User.withUsername("zhangsan").
                password("$2a$10$ri5af5YypS/I5cSkUDY/p.7Wfi/hcIh1DkE20si1HXhqQMG.Ckv8S")
                .authorities("p1").build();
      //  UserDetails userDetails = User.withUsername("zhangsan").password("123").authorities("p1").build();
        return userDetails;
    }
}
