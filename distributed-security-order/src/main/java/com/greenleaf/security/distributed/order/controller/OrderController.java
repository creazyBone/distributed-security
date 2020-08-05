package com.greenleaf.security.distributed.order.controller;

import com.greenleaf.security.distributed.order.model.UserDto;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OrderController {

    @GetMapping(value = "/r1")
    @PreAuthorize("hasAuthority('p3')")
    public String r1(){
        UserDto principal = (UserDto)SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return  principal.getUsername() + "访问order-资源r1";
    }

    @GetMapping(value = "/q1")
    @PreAuthorize("hasAuthority('p1')")
    public String q1(){
        return  "order-访问资源q1";
    }
}
