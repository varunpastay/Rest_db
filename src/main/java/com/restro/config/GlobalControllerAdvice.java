package com.restro.config;

import org.springframework.beans.propertyeditors.CustomNumberEditor;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.InitBinder;

import java.math.BigDecimal;

/**
 * Plain HTML forms always submit an empty string for a blank optional field
 * (there is no way to "omit" a text input the way a JSON client can omit a
 * key) - e.g. leaving "Offer price" blank on the menu-item form posts
 * offerPrice="", and the food-item edit modal's hidden foodItemId field is
 * "" for a brand-new item. Spring's default binder cannot convert "" to a
 * BigDecimal or an Integer and throws a 400 before the controller method
 * even runs. Registering CustomNumberEditor with allowEmpty=true here
 * (once, for every controller) treats an empty string as null instead,
 * matching what every optional numeric field in this app actually expects.
 */
@ControllerAdvice
public class GlobalControllerAdvice {

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(BigDecimal.class, new CustomNumberEditor(BigDecimal.class, true));
        binder.registerCustomEditor(Integer.class, new CustomNumberEditor(Integer.class, true));
    }
}
