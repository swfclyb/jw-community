var VisibilityMonitor = function(targetEl, data) {
    this.target = targetEl;
    this.rules = data['rules'];
};

VisibilityMonitor.prototype.rules = null; 

VisibilityMonitor.prototype.init = function() {
    var thisObject = this;
    
    var targetEl = $(this.target);
    var id = $(this.target).attr("id");
    
    var changeEvent = function(field) {
        thisObject.handleChange(targetEl, thisObject.rules);
    };
    
    for (var i in thisObject.rules) {
        var field = thisObject.rules[i].field;
        if (field !== "(" && field !== ")") {
            var controlEl = $("[name=" + field + "]");
            var parent = $(controlEl).closest(".subform-section");
            if (parent.length === 0) {
                parent = $(controlEl).closest(".form-section");
            }
            if (!$(parent).is(targetEl)) { // prevent it keep trigger itself
                $(controlEl).addClass("control-field");
            }
            $('body').off("change."+id+"_"+field);
            $('body').on("change."+id+"_"+field, "[name=" + field + "]", changeEvent);
        }
    }
    
    thisObject.handleChange(targetEl, this.rules);
};

VisibilityMonitor.prototype.handleChange = function(targetEl, rules) {
    var thisObject = this;
    var match = false;
    
    try {
        var rule = "";
        for (var i in rules) {
            var field = rules[i].field;
            var join = rules[i].join;
            var value = rules[i].value;
            var regex = rules[i].regex;
            var reverse = rules[i].reverse;
            
            if (rule !== "" && rule.substr(rule.length - 1) !== "(" && field !== ")") {
                if (join === "or") {
                    rule += " || ";
                } else {
                    rule += " && ";
                }
            }
            if (field !== ")") {
                rule += " ";
            }
            if (reverse !== "" && field !== ")") {
                rule += "!";
            }
            if (field === "(" || field === ")" ) {
                rule += field;
            } else {
                var controlEl = $("[name=" + field + "]");
                rule += thisObject.checkValue(thisObject, controlEl, value, regex);
            }
        }
        match = eval(rule);
    } catch (err) {}
    
    if (match) {
        targetEl.css("display", "block");
        targetEl.removeClass("section-visibility-hidden");
        thisObject.enableInputField(targetEl);
    } else {
        targetEl.css("display", "none");
        targetEl.addClass("section-visibility-hidden");
        thisObject.disableInputField(targetEl);
    }
};

VisibilityMonitor.prototype.checkValue = function(thisObject, controlEl, controlValue, isRegex) {
    //get enabled input field oni
    controlEl = $(controlEl).filter("input[type=hidden]:not([disabled=true]), :enabled, [disabled=false]");
    controlEl = $(controlEl).filter(":not(.section-visibility-disabled)"); //must put in newline to avoid conflict with above condition
    
    var match = false;
    if ($(controlEl).length > 0) {
        if ($(controlEl).attr("type") === "checkbox" || $(controlEl).attr("type") === "radio") {
            controlEl = $(controlEl).filter(":checked");
        } else if ($(controlEl).is("select")) {
            controlEl = $(controlEl).find("option:selected");
        }
        
        if ($(controlEl).length > 0) {
            $(controlEl).each(function() {
                match = thisObject.isMatch($(this).val(), controlValue, isRegex);
                if (match) {
                    return false;
                }
            });
        } else {
            match = thisObject.isMatch("", controlValue, isRegex);
        }
    }
    return match;
};
VisibilityMonitor.prototype.isMatch = function(value, controlValue, isRegex) {
    if (isRegex !== undefined && "true" === isRegex) {
        try {
            var regex = new RegExp(controlValue);
            var result = regex.exec(value);
            return result.length > 0 && result[0] === value;
        } catch (err) {
            return false;
        }
    } else {
        return value === controlValue;
    }
};
VisibilityMonitor.prototype.disableInputField = function(targetEl) {
    var thisObject = this;
    
    var names = new Array();
    $(targetEl).find('input, select, textarea, .form-element').each(function(){
        if($(this).is("input[type=hidden]:not([disabled=true]), :enabled, [disabled=false]")){
            $(this).addClass("section-visibility-disabled").attr("disabled", true);
            
            var mobileSelector = ".ui-input-text, .ui-checkbox, .ui-radio, .ui-select";
            
            //mobile
            if ($(this).is(mobileSelector) 
                    || $(this).parent().is(mobileSelector) 
                    || $(this).parent().parent().is(mobileSelector)) {
                if ($(this).is("[type='checkbox'], [type='radio']")) {
                    $(this).checkboxradio("disable");
                } else if ($(this).is("select")) {
                    $(this).selectmenu("disable");
                } else {
                    $(this).textinput("disable");
                }
            }
        } 
        if ($(this).is("[name].control-field")) {
            var n = $(this).attr("name");
            if ($.inArray(n, names) < 0 && n !== "") {
                names.push(n);
            }
        }
    });
    thisObject.triggerChange(targetEl, names);
};
VisibilityMonitor.prototype.enableInputField = function(targetEl) {
    var thisObject = this;
    
    var names = new Array();
    $(targetEl).find('input, select, textarea, .form-element').each(function(){
        if($(this).is(".section-visibility-disabled")){
            $(this).removeClass("section-visibility-disabled").removeAttr("disabled");
            
            var mobileSelector = ".ui-input-text, .ui-checkbox, .ui-radio, .ui-select";
            
            //mobile
            if ($(this).is(mobileSelector) 
                    || $(this).parent().is(mobileSelector) 
                    || $(this).parent().parent().is(mobileSelector)) {
                if ($(this).is("[type='checkbox'], [type='radio']")) {
                    $(this).checkboxradio("enable");
                } else if ($(this).is("select")) {
                    $(this).selectmenu("enable");
                } else {
                    $(this).textinput("enable");
                }
            }
        } 
        if ($(this).is("[name].control-field")) {
            var n = $(this).attr("name");
            if ($.inArray(n, names) < 0 && n !== "") {
                names.push(n);
            }
        }
    });
    
    thisObject.triggerChange(targetEl, names);
    $(window).trigger("resize");
};
VisibilityMonitor.prototype.triggerChange = function(targetEl, names) {
    $.each(names, function(i){
        var temp = false;
        var newObject = $("[name=" + names[i] + "]");
        if (newObject.length === 0) {
            newObject = $('<input name="'+names[i]+'" class="control-field" style="display:none;" />');
            $(targetEl).append(newObject);
            temp = true;
        }
        $("[name=" + names[i] + "]").trigger("change");
        if (temp) {
            $(newObject).remove();
        }
    });
};