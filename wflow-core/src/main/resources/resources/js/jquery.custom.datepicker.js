(function($){
    $.fn.extend({
        cdatepicker : function(o){
            this.each(function(){
                var element = $(this);
                var elementParent = element.parent();

                o.beforeShow = function(input, inst) {
                    $(element).addClass("popup-picker");
                    setTimeout(function(){
                        var tabbables = $("#ui-datepicker-div").find(':tabbable');
                        var first = tabbables.filter(':first');
                        var last  = tabbables.filter(':last');

                        $("#ui-datepicker-div").off("keydown", ":tabbable");
                        $("#ui-datepicker-div").on("keydown", ":tabbable", function(e) {
                            var keyCode = e.keyCode || e.which;
                            if (keyCode === 9) {
                                var focusedElement = $(e.target);

                                var isFirstInFocus = (first.get(0) === focusedElement.get(0));
                                var isLastInFocus = (last.get(0) === focusedElement.get(0));

                                var tabbingForward = !e.shiftKey;

                                if (tabbingForward) {
                                    if (isLastInFocus) {
                                        first.focus();
                                        e.preventDefault();
                                    }
                                } else {
                                    if (isFirstInFocus) {
                                        last.focus();
                                        e.preventDefault();
                                    }
                                }
                            } else if (keyCode === 27) {
                                $(element).datepicker( "hide" );
                                $(element).next("a.trigger").focus();
                            }
                        });
                        first.focus();
                    }, 100);
                };
                o.onClose = function(selectedDate) {
                    $(element).removeClass("popup-picker");
                    $(element).focus();
                };
                
                $(element).datepicker(o);
                
                if($.placeholder) {
                    $(element).placeholder();
                }
                
                var a = $("<a>").attr("href","#");
                $(element).next("img.ui-datepicker-trigger").wrap("<a class=\"trigger\" href=\"#\"></a>");

                $(element).next("a.trigger").after("<a class=\"close-icon\" type=\"reset\"></a>");
                
                $(element).change(function() {
                    if ( $(element).val() !== "" ) {
                        $(elementParent).find("a.close-icon").show();
                    } else {
                        $(elementParent).find("a.close-icon").hide();
                    }
                });
                
                //Always check first if value already exists
                $(element).change();
                
                $(elementParent).find("a.close-icon").click(function(){
                    $(element).val("").change();
                });
                
                $(elementParent).find("input[readonly] , a.trigger").click(function(evt){
                    evt.preventDefault();
                    evt.stopImmediatePropagation();
                    $(element).datepicker( "show" );
                });
                $(elementParent).find("input , a.trigger").off("keydown").on("keydown", function(evt){
                    if (evt.keyCode === 13) {
                        show(element, evt);
                    }
                }).on("focus", function() {
                    $(element).addClass("focus");
                }).on("focusout", function(){
                    if (!$(element).hasClass("popup-picker")) {
                        $(element).removeClass("focus");
                    }
                });
                $(elementParent).find("input[readonly]").on("keydown", function(evt){
                    if (evt.keyCode === 8) {
                        $(element).val("").change();
                        return false;
                    }
                });
                
                if (o.startDateFieldId  !== undefined && o.startDateFieldId !== "") {
                    var startDate = FormUtil.getField(o.startDateFieldId);
                    
                    startDate.live("change", function(){
                        setDateRange(startDate, "minDate", element);
                    });
                    setDateRange(startDate, "minDate", element);
                }
                
                if (o.endDateFieldId  !== undefined && o.endDateFieldId !== "") {
                    var endDate = FormUtil.getField(o.endDateFieldId);
                    
                    endDate.live("change", function(){
                        setDateRange(endDate, "maxDate", element);
                    });
                    setDateRange(endDate, "maxDate", element);
                }
                
                if (o.currentDateAs !== undefined && o.currentDateAs !== "") {
                    var option = $(element).datepicker( "option", o.currentDateAs);
                    if (option === undefined || option === null) {
                        $(element).datepicker( "option", o.currentDateAs, new Date());
                    }
                }
            });
        }
    });
    
    function setDateRange(element, type, target) {
        var value = $(element).val();
        if (value !== "") {
            $(target).datepicker( "option", type, value);
        }
    }
})(jQuery);