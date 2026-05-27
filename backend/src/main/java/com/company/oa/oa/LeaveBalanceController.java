package com.company.oa.oa;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/leave-balance")
public class LeaveBalanceController {
    private final LeaveBalanceService leaveBalanceService;

    public LeaveBalanceController(LeaveBalanceService leaveBalanceService) {
        this.leaveBalanceService = leaveBalanceService;
    }

    @PreAuthorize("hasAnyAuthority('*', 'org:view')")
    @GetMapping("/my")
    public List<Map<String, Object>> myBalance() {
        return leaveBalanceService.getMyBalance();
    }

    @PreAuthorize("hasAnyAuthority('*', 'org:view')")
    @GetMapping("/user/{userId}")
    public List<Map<String, Object>> userBalance(@PathVariable long userId) {
        return leaveBalanceService.getUserBalance(userId);
    }
}
