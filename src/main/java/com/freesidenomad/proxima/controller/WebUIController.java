package com.freesidenomad.proxima.controller;

import com.freesidenomad.proxima.service.ConfigurationService;
import com.freesidenomad.proxima.service.RouteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/proxima/ui")
public class WebUIController {

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private RouteService routeService;

    @GetMapping({"", "/"})
    public String dashboard(Model model) {
        model.addAttribute("activePreset", configurationService.getActivePresetName());
        model.addAttribute("downstreamUrl", configurationService.getDownstreamUrl());
        model.addAttribute("totalPresets",
                          configurationService.getAllPresets() != null ?
                          configurationService.getAllPresets().size() : 0);
        model.addAttribute("currentHeaders", configurationService.getCurrentHeaders());
        return "dashboard";
    }

    @GetMapping("/presets")
    public String presets(Model model) {
        model.addAttribute("presets", configurationService.getAllPresets());
        model.addAttribute("activePreset", configurationService.getActivePresetName());
        return "presets";
    }

    @GetMapping("/presets/{name}")
    public String presetDetail(@PathVariable String name, Model model) {
        return configurationService.getPresetByName(name)
                .map(preset -> {
                    model.addAttribute("preset", preset);
                    model.addAttribute("isActive", name.equals(configurationService.getActivePresetName()));
                    return "preset-detail";
                })
                .orElse("redirect:/proxima/ui/presets");
    }

    @GetMapping("/headers")
    public String headers(Model model) {
        model.addAttribute("currentHeaders", configurationService.getCurrentHeaders());
        model.addAttribute("activePreset", configurationService.getActivePresetName());
        return "headers";
    }

    @GetMapping("/status")
    public String status(Model model) {
        model.addAttribute("downstreamUrl", configurationService.getDownstreamUrl());
        model.addAttribute("activePreset", configurationService.getActivePresetName());
        model.addAttribute("configErrors", configurationService.validateConfiguration());
        return "status";
    }

    @GetMapping("/routes")
    public String routes(Model model) {
        model.addAttribute("routes", routeService.getAllRoutes());
        model.addAttribute("hasRoutes", routeService.hasRoutes());
        model.addAttribute("routeCount", routeService.getRouteCount());
        model.addAttribute("enabledRouteCount", routeService.getEnabledRouteCount());
        return "routes";
    }
}