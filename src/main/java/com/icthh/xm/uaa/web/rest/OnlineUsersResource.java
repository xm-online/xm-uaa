package com.icthh.xm.uaa.web.rest;

import com.codahale.metrics.annotation.Timed;
import com.icthh.xm.uaa.service.OnlineUsersService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Resource to return information about the number of online users.
 */
@RestController
@RequestMapping("/api")
public class OnlineUsersResource {

    private OnlineUsersService onlineUsersService;

    public OnlineUsersResource(OnlineUsersService onlineUsersService) {
        this.onlineUsersService = onlineUsersService;
    }

    /**
     * GET  /onlineUsers : get all online users.
     *
     * @return the ResponseEntity with status 200 (OK) and body with number of all online users
     */
    @GetMapping("/onlineUsers")
    @Timed
    @PreAuthorize("hasPermission(null, 'USER.METRIC.ONLINE')")
    public Integer getUsersOnline() {
        return onlineUsersService.find().size();
    }
}
