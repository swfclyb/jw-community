package org.joget.apps.datalist.lib;

import javax.servlet.http.HttpServletRequest;
import org.joget.apps.app.dao.FormDefinitionDao;
import org.joget.apps.app.model.AppDefinition;
import org.joget.apps.app.model.FormDefinition;
import org.joget.apps.app.service.AppService;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.datalist.model.DataList;
import org.joget.apps.datalist.model.DataListActionDefault;
import org.joget.apps.datalist.model.DataListActionResult;
import org.joget.apps.form.dao.FormDataDao;
import org.joget.apps.form.model.Form;
import org.joget.apps.form.service.FileUtil;
import org.joget.apps.form.service.FormService;
import org.joget.apps.form.service.FormUtil;
import org.joget.commons.util.ResourceBundleUtil;
import org.joget.commons.util.StringUtil;
import org.joget.workflow.util.WorkflowUtil;
import org.springframework.orm.hibernate4.HibernateObjectRetrievalFailureException;
import org.springframework.transaction.annotation.Transactional;

public class FormRowDeleteDataListAction extends DataListActionDefault {
    
    public String getName() {
        return "Form Row Delete Action";
    }

    public String getVersion() {
        return "5.0.0";
    }

    public String getDescription() {
        return "Form Row Delete Action";
    }
    
    public String getLabel() {
        return "Form Row Delete Action";
    }

    public String getLinkLabel() {
        String label = getPropertyString("label");
        if (label == null || label.isEmpty()) {
            label = "Delete";
        }
        return label;
    }

    public String getHref() {
        return getPropertyString("href");
    }

    public String getTarget() {
        return "post";
    }

    public String getHrefParam() {
        return getPropertyString("hrefParam");
    }

    public String getHrefColumn() {
        return getPropertyString("hrefColumn");
    }

    public String getConfirmation() {
        String confirm = getPropertyString("confirmation");
        if (confirm == null || confirm.isEmpty()) {
            confirm = "Please Confirm";
        }
        return confirm;
    }

    @Transactional
    public DataListActionResult executeAction(DataList dataList, String[] rowKeys) {
        DataListActionResult result = new DataListActionResult();
        result.setType(DataListActionResult.TYPE_REDIRECT);
        result.setUrl("REFERER");
        
        // only allow POST
        HttpServletRequest request = WorkflowUtil.getHttpServletRequest();
        if (request != null && !"POST".equalsIgnoreCase(request.getMethod())) {
            return null;
        }
            
        if (rowKeys != null && rowKeys.length > 0) {
            String formDefId = getPropertyString("formDefId");
            
            if ("true".equalsIgnoreCase(getPropertyString("deleteSubformData")) || "true".equalsIgnoreCase(getPropertyString("deleteGridData"))) {
                AppDefinition appDef = AppUtil.getCurrentAppDefinition();
                Form form = getForm(appDef, formDefId);
                
                if (form != null) {
                    try {
                        FormDataDao formDataDao = (FormDataDao) FormUtil.getApplicationContext().getBean("formDataDao");
                        for (String id : rowKeys) {
                            deleteData(formDataDao, form, id);
                        }
                    } catch (Exception e) {
                        result.setMessage(ResourceBundleUtil.getMessage("datalist.formrowdeletedatalistaction.error.delete"));
                    }
                } else {
                    result.setMessage(ResourceBundleUtil.getMessage("datalist.formrowdeletedatalistaction.noform"));
                }
            } else {
                String tableName = getSelectedFormTableName(formDefId);
                if (tableName != null) {
                    FormDataDao formDataDao = (FormDataDao) FormUtil.getApplicationContext().getBean("formDataDao");
                    formDataDao.delete(formDefId, tableName, rowKeys);
                    
                    if ("true".equalsIgnoreCase(getPropertyString("abortRelatedRunningProcesses")) || "true".equalsIgnoreCase(getPropertyString("deleteFiles"))) {
                        for (String id : rowKeys) {
                            if ("true".equalsIgnoreCase(getPropertyString("abortRelatedRunningProcesses"))) {
                                FormUtil.abortRunningProcessForRecord(id);
                            }
                            
                            if ("true".equalsIgnoreCase(getPropertyString("deleteFiles"))) {
                                FileUtil.deleteFiles(tableName, id);
                            }
                        }
                    }
                }
            }
        }

        return result;
    }

    public String getPropertyOptions() {
        String formDefField = null;
        AppDefinition appDef = AppUtil.getCurrentAppDefinition();
        if (appDef != null) {
            String formJsonUrl = "[CONTEXT_PATH]/web/json/console/app/" + appDef.getId() + "/" + appDef.getVersion() + "/forms/options";
            formDefField = "{name:'formDefId',label:'@@datalist.formrowdeletedatalistaction.formId@@',type:'selectbox',options_ajax:'" + formJsonUrl + "',required:'True'}";
        } else {
            formDefField = "{name:'formDefId',label:'@@datalist.formrowdeletedatalistaction.formId@@',type:'textfield',required:'True'}";
        }
        Object[] arguments = new Object[]{formDefField};
        String json = AppUtil.readPluginResource(getClass().getName(), "/properties/datalist/formRowDeleteDataListAction.json", arguments, true, "message/datalist/formRowDeleteDataListAction");
        return json;
    }

    protected String getSelectedFormTableName(String formDefId) {
        String tableName = null;
        AppDefinition appDef = AppUtil.getCurrentAppDefinition();
        AppService appService = (AppService) AppUtil.getApplicationContext().getBean("appService");
        if (formDefId != null) {
            tableName = appService.getFormTableName(appDef, formDefId);
        }
        return tableName;
    }
    
    protected Form getForm(AppDefinition appDef, String formDefId) {
        FormService formService = (FormService) AppUtil.getApplicationContext().getBean("formService");
        FormDefinitionDao formDefinitionDao = (FormDefinitionDao) AppUtil.getApplicationContext().getBean("formDefinitionDao");
        
        Form form = null;
        FormDefinition formDef = formDefinitionDao.loadById(formDefId, appDef);
        
        if (formDef != null && formDef.getJson() != null) {
            String formJson = formDef.getJson();
            formJson = AppUtil.processHashVariable(formJson, null, StringUtil.TYPE_JSON, null);
            form = (Form) formService.createElementFromJson(formJson);
        }
        
        return form;
    } 
    
    protected void deleteData(FormDataDao formDataDao, Form form, String primaryKey) {
        try {
            if ("true".equalsIgnoreCase(getPropertyString("abortRelatedRunningProcesses"))) {
                FormUtil.abortRunningProcessForRecord(primaryKey);
            }
            formDataDao.delete(form.getPropertyString(FormUtil.PROPERTY_ID), form.getPropertyString(FormUtil.PROPERTY_TABLE_NAME), new String[]{primaryKey});
            
            if ("true".equalsIgnoreCase(getPropertyString("deleteFiles"))) {
                FileUtil.deleteFiles(form, primaryKey);
            }
        } catch (HibernateObjectRetrievalFailureException e) {
            //ignore
        }
        
        if ("true".equalsIgnoreCase(getPropertyString("deleteGridData")) || "true".equalsIgnoreCase(getPropertyString("deleteSubformData"))) {
            FormUtil.recursiveDeleteChildFormData(form, primaryKey, "true".equalsIgnoreCase(getPropertyString("deleteGridData")), "true".equalsIgnoreCase(getPropertyString("deleteSubformData")), "true".equalsIgnoreCase(getPropertyString("abortRelatedRunningProcesses")), "true".equalsIgnoreCase(getPropertyString("deleteFiles")));
        }
    }

    public String getClassName() {
        return this.getClass().getName();
    }
}