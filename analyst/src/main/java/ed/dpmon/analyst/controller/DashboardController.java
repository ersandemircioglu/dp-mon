package ed.dpmon.analyst.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import ed.dpmon.analyst.service.AnalystService;

@Controller
public class DashboardController {

    @Autowired
    private AnalystService analystService;

    @GetMapping("/dashboard")
    public String monitor(Model model) {
        model.addAttribute("historicData", analystService.fetch());
        return "dashboard";
    }

}
