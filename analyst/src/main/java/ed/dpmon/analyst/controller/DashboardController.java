package ed.dpmon.analyst.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import ed.dpmon.analyst.service.AnalystService;

@Controller
public class DashboardController {

    @Autowired
    private AnalystService analystService;

    @GetMapping("/home")
    public String home(Model model) {
        model.addAttribute("productNames", analystService.getProductNames());
        return "home";
    }

    @GetMapping("/dashboard/{prodName}")
    public String monitor(@PathVariable("prodName") String prodName, Model model) {
        model.addAttribute("productName", prodName);
        model.addAttribute("historicData", analystService.getHistoricalData(prodName));
        model.addAttribute("liveData", analystService.getLiveData(prodName));
        return "dashboard";
    }

}
