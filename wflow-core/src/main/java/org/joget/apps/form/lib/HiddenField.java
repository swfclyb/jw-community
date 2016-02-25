package org.joget.apps.form.lib;

import java.util.Map;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.model.Element;
import org.joget.apps.form.model.FormBuilderPaletteElement;
import org.joget.apps.form.model.FormBuilderPalette;
import org.joget.apps.form.model.FormData;
import org.joget.apps.form.service.FormUtil;

public class HiddenField extends Element implements FormBuilderPaletteElement {

    @Override
    public String getName() {
        return "Hidden Field";
    }

    @Override
    public String getVersion() {
        return "5.0.0";
    }

    @Override
    public String getDescription() {
        return "Hidden Field Element";
    }

    @Override
    public String renderTemplate(FormData formData, Map dataModel) {
        String template = "hiddenField.ftl";

        // set value
        String value = FormUtil.getElementPropertyValue(this, formData);
        String priority = getPropertyString("useDefaultWhenEmpty");
        
        if (priority != null && !priority.isEmpty()) {
            if (("true".equals(priority) && (value == null || value.isEmpty()))
                    || "valueOnly".equals(priority)) {
                value = getPropertyString("value");
            }
        } else {
            if (getPropertyString("value") != null && !getPropertyString("value").isEmpty()) {
                value = getPropertyString("value");
            }
        } 

        dataModel.put("value", value);

        String html = FormUtil.generateElementHtml(this, formData, template, dataModel);
        return html;
    }

    @Override
    public String getClassName() {
        return getClass().getName();
    }

    @Override
    public String getFormBuilderTemplate() {
        return "<label class='label'>HiddenField</label>";
    }

    @Override
    public String getLabel() {
        return "Hidden Field";
    }

    @Override
    public String getPropertyOptions() {
        return AppUtil.readPluginResource(getClass().getName(), "/properties/form/hiddenField.json", null, true, "message/form/HiddenField");
    }

    @Override
    public String getFormBuilderCategory() {
        return FormBuilderPalette.CATEGORY_GENERAL;
    }

    @Override
    public int getFormBuilderPosition() {
        return 100;
    }

    @Override
    public String getFormBuilderIcon() {
        return "/plugin/org.joget.apps.form.lib.HiddenField/images/textField_icon.gif";
    }
}
