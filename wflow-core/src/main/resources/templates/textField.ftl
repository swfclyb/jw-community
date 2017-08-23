<div class="form-cell" ${elementMetaData!}>
    <#if !(includeMetaData!) && element.properties.style! != "" >
        <script type="text/javascript" src="${request.contextPath}/plugin/org.joget.apps.form.lib.TextField/js/jquery.numberFormatting.js"></script>
        <script type="text/javascript">
            $(document).ready(function(){
                $('#${elementParamName!}').numberFormatting({
                    format : '${element.properties.style!}',
                    numOfDecimal : '${element.properties.numOfDecimal!}',
                    useThousandSeparator : '${element.properties.useThousandSeparator!}',
                    prefix : '${element.properties.prefix!}',
                    postfix : '${element.properties.postfix!}'
                });
            });
        </script>
    </#if>
    <label class="label">${element.properties.label} <span class="form-cell-validator">${decoration}</span><#if error??> <span class="form-error-message">${error}</span></#if></label>
    <#if (element.properties.readonly! == 'true' && element.properties.readonlyLabel! == 'true') >
        <div class="form-cell-value"><span>${value!?html}</span></div>
        <input id="${elementParamName!}" name="${elementParamName!}" type="hidden" value="${value!?html}" />
    <#else>
        <input id="${elementParamName!}" name="${elementParamName!}" type="text" placeholder="${element.properties.placeholder!}" size="${element.properties.size!}" value="${value!?html}" maxlength="${element.properties.maxlength!}" <#if error??>class="form-error-cell"</#if> <#if element.properties.readonly! == 'true'>readonly</#if> />
    </#if>
</div>