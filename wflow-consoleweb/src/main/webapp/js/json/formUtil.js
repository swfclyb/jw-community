FormUtil = {
    getValue : function(fieldId){
        var value = "";
        var field = FormUtil.getField(fieldId);
        if ($(field).length > 0) {
            if ($(field).attr("type") == "checkbox" || $(field).attr("type") == "radio") {
                field = $(field).filter(":checked");
            } else if ($(field).is("select")) {
                field = $(field).find("option:selected");
            }
            
            value = $(field).val();
        }
        return value;
    },
    
    getValues : function(fieldId){
        var values = new Array();
        
        if (fieldId.indexOf(".") > 0) { // grid cell values
            values = FormUtil.getGridCellValues(fieldId);
        } else {
            var field = FormUtil.getField(fieldId);
            if ($(field).length > 0) {
                if ($(field).attr("type") == "checkbox" || $(field).attr("type") == "radio") {
                    field = $(field).filter(":checked");
                } else if ($(field).is("select")) {
                    field = $(field).find("option:selected");
                }

                $(field).each(function() {
                    values.push($(this).val());
                });
            }
        }
        return values;
    },
    
    getField : function(fieldId){
        var field = $("[name="+fieldId+"]");
        if ($(field).length == 0) {
            field = $("[name$=_"+fieldId+"]");
        }
        
        //filter those in hidden section
        field = $(field).filter(':parents(.section-visibility-hidden)');
        
        //to prevent return field with similar name, get the field with shorter name (Field in the subform)
        if ($(field).length > 1) {
            var fieldname;
            $(field).each(function(){
                if (fieldname === undefined) {
                    fieldname = $(this).attr("name");
                }
                if ($(this).attr("name").length < fieldname.length) {
                    fieldname = $(this).attr("name");
                }
                field = $("[name="+fieldname+"]");
            });
        }
        
        return field;
    },
    
    getGridCells : function(cellFieldId){
        var fieldId = cellFieldId.split(".")[0];
        var cells = null;
        
        var field = FormUtil.getField(fieldId);
        var gridDataObject = field.data("gridDataObject");
        if (gridDataObject !== null && gridDataObject !== undefined) {
            var cellId = cellFieldId.split(".")[1];
            
            //build dummy hidden fields for plugins using this method
            cells = new Array();
            for (var i in gridDataObject) {
                var value = gridDataObject[i][cellId];
                var temp = $("<input type='hidden'/>").val(value);
                cells.push(temp);
            }
        } else {
            cellFieldId = cellFieldId.replace(/\./g, '_');
            cells = $(field).find("[name=" + cellFieldId + "], [name$=_" + cellFieldId + "]");
            //filter those in template 
            cells = $(cells).filter(':parents(.grid-row-template)');
        }
        return cells;
    },
    
    getGridCellValues : function (cellFieldId) {
        var fieldId = cellFieldId.split(".")[0];
        var values = new Array();
        
        var field = FormUtil.getField(fieldId);
        
        var gridDataObject = field.data("gridDataObject");
        if (gridDataObject !== null && gridDataObject !== undefined) {
            var cellId = cellFieldId.split(".")[1];
            
            for (var i in gridDataObject) {
                var value = gridDataObject[i][cellId];
                values.push(value);
            }
        } else {
            field.find("tr.grid-row").each(function() {
                if ($(this).find("textarea[id$=_jsonrow]").length > 0) {
                    var cellId = cellFieldId.split(".")[1];

                    //get json data from hidden textarea
                    var data = $(this).find("textarea[id$=_jsonrow]").val();
                    var dataObj = $.parseJSON(data);

                    if (dataObj[cellId] !== undefined) {
                        values.push(dataObj[cellId]);
                    }
                } else {
                    var cellId = cellFieldId.replace(/\./g, '_');
                    var cell = $(field).find("[name=" + cellId + "], [name$=_" + cellId + "]");
                    if (cell.length > 1) {
                        values.push(cell.text());
                    }
                }
            });
        }
        
        return values;
    },
    
    getFieldsAsUrlQueryString : function(fields) {
        var queryString = "";
        
        if (fields !== undefined) {
            $.each(fields, function(i, v){
                var values = [];
                
                if (v['field'] !== "") {
                    values = FormUtil.getValues(v['field']).join(";");
                }
            
                if (values.length === 0 && v['defaultValue'] !== "") {
                    values = v['defaultValue'];
                }
                
                queryString += encodeURIComponent(v['param']) + "=" + encodeURIComponent(values) + "&";
            });
            
            if (queryString !== "") {
                queryString = queryString.substring(0, queryString.length-1);
            }
        }
        
        return queryString;
    },
    
    numberFormat : function (value, options){
        var numOfDecimal = parseInt(options.numOfDecimal);
        var decimalSeperator = ".";
        var regexDecimalSeperator = "\\\.";
        var thousandSeparator = ",";
        var regexThousandSeparator = ",";
        if(options.format.toUpperCase() === "EURO"){
            decimalSeperator = ",";
            regexDecimalSeperator = ",";
            thousandSeparator = ".";
            regexThousandSeparator = "\\\.";
        }
        
        var number = value.replace(/\s/g, "");
        number = number.replace(new RegExp(regexThousandSeparator, 'g'), '');
        number = number.replace(new RegExp(regexDecimalSeperator, 'g'), '.');
        if(options.prefix !== ""){
            number = number.replace(options.prefix, "");
        }
        if(options.postfix !== ""){
            number = number.replace(options.postfix, "");
        }
                
        var exponent = "";
        if (!isFinite(number)) {
            number = 0;
        } else {
            var numberstr = number.toString();
            var eindex = numberstr.indexOf("e");
            if (eindex > -1){
                exponent = numberstr.substring(eindex);
                number = parseFloat(numberstr.substring(0, eindex));
            }

            if (numOfDecimal !== null){
                var temp = Math.pow(10, numOfDecimal);
                number = Math.round(number * temp) / temp;
            }
        }
        
        var sign = number < 0 ? "-" : "";
        
        var integer = (number > 0 ? Math.floor (number) : Math.abs (Math.ceil (number))).toString ();
        var fractional = number.toString ().substring (integer.length + sign.length);
        fractional = numOfDecimal !== null && numOfDecimal > 0 || fractional.length > 1 ? (decimalSeperator + fractional.substring (1)) : "";
        if(numOfDecimal !== null && numOfDecimal > 0){
            for (i = fractional.length - 1, z = numOfDecimal; i < z; ++i){
                fractional += "0";
            }
        }
        
        if(options.useThousandSeparator.toUpperCase() === "TRUE"){
            for (i = integer.length - 3; i > 0; i -= 3){
                integer = integer.substring (0 , i) + thousandSeparator + integer.substring (i);
            }
        }
        
        var resultString = "";
        if(sign !== ""){
            resultString += sign;
        }
        if(options.prefix !== ""){
            resultString += options.prefix + ' ';
        }
        resultString += integer + fractional;
        if(exponent !== ""){
            resultString += ' ' + exponent;
        }
        if(options.postfix !== ""){
            resultString += ' ' + options.postfix;
        }
        
        return  resultString;
    }
}

//filter parents
jQuery.expr[':'].parents = function(a,i,m){
    return jQuery(a).parents(m[3]).length < 1;
};