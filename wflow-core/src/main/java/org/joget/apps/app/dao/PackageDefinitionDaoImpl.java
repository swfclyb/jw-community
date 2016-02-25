package org.joget.apps.app.dao;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.joget.apps.app.model.AppDefinition;
import org.joget.apps.app.model.PackageActivityForm;
import org.joget.apps.app.model.PackageActivityPlugin;
import org.joget.apps.app.model.PackageDefinition;
import org.joget.apps.app.model.PackageParticipant;
import org.joget.apps.app.service.AppUtil;
import org.joget.commons.util.LogUtil;
import org.joget.workflow.model.WorkflowActivity;
import org.joget.workflow.model.WorkflowParticipant;
import org.joget.workflow.model.WorkflowProcess;
import org.joget.workflow.model.service.WorkflowManager;
import org.joget.workflow.util.WorkflowUtil;

/**
 * DAO to load/store PackageDefinition and mapping objects
 */
public class PackageDefinitionDaoImpl extends AbstractVersionedObjectDao<PackageDefinition> implements PackageDefinitionDao {

    public static final String ENTITY_NAME = "PackageDefinition";
    private AppDefinitionDao appDefinitionDao;

    public AppDefinitionDao getAppDefinitionDao() {
        return appDefinitionDao;
    }

    public void setAppDefinitionDao(AppDefinitionDao appDefinitionDao) {
        this.appDefinitionDao = appDefinitionDao;
    }

    @Override
    public String getEntityName() {
        return ENTITY_NAME;
    }

    @Override
    public void delete(PackageDefinition obj) {
        AppDefinition appDef = obj.getAppDefinition();
        if (appDef != null) {
            // disassociate from app
            Collection<PackageDefinition> list = appDef.getPackageDefinitionList();
            for (Iterator<PackageDefinition> i = list.iterator(); i.hasNext();) {
                PackageDefinition def = i.next();
                if (def.getId() != null && def.getId().equals(obj.getId())) {
                    i.remove();
                }
            }
            appDefinitionDao.saveOrUpdate(appDef);
        }
        // delete package definition
        super.delete(getEntityName(), obj);
    }

    /**
     * Loads the package definition for a specific app version
     * @param appId
     * @param appVersion
     * @return
     */
    @Override
    public PackageDefinition loadAppPackageDefinition(String appId, Long appVersion) {
        PackageDefinition packageDef = null;

        // load the package definition
        String condition = " INNER JOIN e.appDefinition app WHERE app.id=? AND app.version=?";
        Object[] params = {appId, appVersion};
        Collection<PackageDefinition> results = find(getEntityName(), condition, params, null, null, 0, 1);
        if (results != null && !results.isEmpty()) {
            packageDef = results.iterator().next();
        }

        return packageDef;
    }

    /**
     * Loads the package definition
     * @param packageId
     * @param packageVersion
     * @return
     */
    @Override
    public PackageDefinition loadPackageDefinition(String packageId, Long packageVersion) {
        PackageDefinition packageDef = null;
        if (packageVersion != null) {
            // load the package definition
            String condition = " WHERE e.id=? AND e.version=?";
            Object[] params = {packageId, packageVersion};
            Collection<PackageDefinition> results = find(getEntityName(), condition, params, null, null, 0, 1);
            if (results != null && !results.isEmpty()) {
                packageDef = results.iterator().next();
            }
        }
        return packageDef;
    }

    /**
     * Loads the package definition based on a process definition ID
     * @param packageVersion
     * @param processDefId
     * @return
     */
    @Override
    public PackageDefinition loadPackageDefinitionByProcess(String packageId, Long packageVersion, String processDefId) {
        PackageDefinition packageDef = null;
        if (packageVersion != null) {
            processDefId = WorkflowUtil.getProcessDefIdWithoutVersion(processDefId);

            // load the package definition
            String condition = " INNER JOIN e.packageActivityFormMap paf WHERE e.id=? AND e.version=? AND paf.processDefId=?";
            Object[] params = {packageId, packageVersion, processDefId};
            Collection<PackageDefinition> results = find(getEntityName(), condition, params, null, null, 0, 1);
            if (results != null && !results.isEmpty()) {
                packageDef = results.iterator().next();
            }
        }
        return packageDef;
    }

    @Override
    public PackageDefinition createPackageDefinition(AppDefinition appDef, Long packageVersion) {
        PackageDefinition packageDef = new PackageDefinition();
        packageDef.setId(appDef.getId());
        packageDef.setVersion(packageVersion);
        packageDef.setName(appDef.getName());
        packageDef.setAppDefinition(appDef);
        saveOrUpdate(packageDef);
        return packageDef;
    }

    @Override
    public PackageDefinition updatePackageDefinitionVersion(PackageDefinition packageDef, Long packageVersion) {
        String packageId = packageDef.getId();

        // detach previous package version
        delete(packageDef);

        // update package definition
        packageDef.setId(packageId);
        packageDef.setVersion(packageVersion);
        
        //remove not exist participants, activities and tools in mapping
        Collection<String> activityIds = new ArrayList<String>();
        Collection<String> toolIds = new ArrayList<String>();
        Collection<String> participantIds = new ArrayList<String>();
        Map<String, PackageActivityForm> packageActivityFormMap = new HashMap<String, PackageActivityForm>();
        Map<String, PackageActivityPlugin> packageActivityPluginMap = new HashMap<String, PackageActivityPlugin>();
        Map<String, PackageParticipant> packageParticipantMap = new HashMap<String, PackageParticipant>();
        try {
            WorkflowManager workflowManager = (WorkflowManager) AppUtil.getApplicationContext().getBean("workflowManager");
            Collection<WorkflowProcess> processList = workflowManager.getProcessList(packageDef.getAppDefinition().getAppId(), packageVersion.toString());
            for (WorkflowProcess wp : processList) {
                String processDefId = WorkflowUtil.getProcessDefIdWithoutVersion(wp.getId());
                Collection<WorkflowActivity> activityList = workflowManager.getProcessActivityDefinitionList(wp.getId());
                activityIds.add(processDefId+"::"+WorkflowUtil.ACTIVITY_DEF_ID_RUN_PROCESS);
                participantIds.add(processDefId+"::"+"processStartWhiteList");
                for (WorkflowActivity a : activityList) {
                    if (a.getType().equalsIgnoreCase("normal")) {
                        activityIds.add(processDefId+"::"+a.getId());
                    } else if (a.getType().equalsIgnoreCase("tool")) {
                        toolIds.add(processDefId+"::"+a.getId());
                    }
                }

                Collection<WorkflowParticipant> participantList = workflowManager.getProcessParticipantDefinitionList(wp.getId());
                for (WorkflowParticipant p : participantList) {
                    participantIds.add(processDefId+"::"+p.getId());
                }
            }

            Map<String, PackageActivityForm> activityForms = packageDef.getPackageActivityFormMap();
            for (String key : activityForms.keySet()) {
                if (activityIds.contains(key)) {
                    packageActivityFormMap.put(key, activityForms.get(key));
                }
            }
            Map<String, PackageActivityPlugin> activityPluginMap = packageDef.getPackageActivityPluginMap();
            for (String key : activityPluginMap.keySet()) {
                if (toolIds.contains(key)) {
                    packageActivityPluginMap.put(key, activityPluginMap.get(key));
                }
            }
            Map<String, PackageParticipant> participantMap = packageDef.getPackageParticipantMap();
            for (String key : participantMap.keySet()) {
                if (participantIds.contains(key)) {
                    packageParticipantMap.put(key, participantMap.get(key));
                }
            }
        } catch (Exception e) {
            LogUtil.error(PackageDefinitionDaoImpl.class.getName(), e, "");
        }
                
        packageDef.setPackageActivityFormMap(packageActivityFormMap);
        packageDef.setPackageActivityPluginMap(packageActivityPluginMap);
        packageDef.setPackageParticipantMap(packageParticipantMap);

        // save app and package definition
        saveOrUpdate(packageDef);
        return packageDef;
    }

    @Override
    public void addAppActivityForm(String appId, Long appVersion, PackageActivityForm activityForm) {
        PackageDefinition packageDef = loadAppPackageDefinition(appId, appVersion);
        if (packageDef == null) {
            AppDefinition appDef = getAppDefinitionDao().loadVersion(appId, appVersion);
            packageDef = createPackageDefinition(appDef, appVersion);
        }
        String processDefId = activityForm.getProcessDefId();
        processDefId = WorkflowUtil.getProcessDefIdWithoutVersion(processDefId);
        activityForm.setProcessDefId(processDefId);
        String activityDefId = activityForm.getActivityDefId();
        if (processDefId != null && activityDefId != null) {
            packageDef.removePackageActivityForm(processDefId, activityDefId);
            saveOrUpdate(packageDef);
        }
        packageDef.addPackageActivityForm(activityForm);
        saveOrUpdate(packageDef);
    }

    @Override
    public void removeAppActivityForm(String appId, Long appVersion, String processDefId, String activityDefId) {
        PackageDefinition packageDef = loadAppPackageDefinition(appId, appVersion);
        processDefId = WorkflowUtil.getProcessDefIdWithoutVersion(processDefId);
        packageDef.removePackageActivityForm(processDefId, activityDefId);
        saveOrUpdate(packageDef);
    }

    @Override
    public void addAppActivityPlugin(String appId, Long appVersion, PackageActivityPlugin activityPlugin) {
        PackageDefinition packageDef = loadAppPackageDefinition(appId, appVersion);
        if (packageDef == null) {
            AppDefinition appDef = getAppDefinitionDao().loadVersion(appId, appVersion);
            packageDef = createPackageDefinition(appDef, appVersion);
        }
        String processDefId = activityPlugin.getProcessDefId();
        processDefId = WorkflowUtil.getProcessDefIdWithoutVersion(processDefId);
        activityPlugin.setProcessDefId(processDefId);
        String activityDefId = activityPlugin.getActivityDefId();
        if (processDefId != null && activityDefId != null) {
            packageDef.removePackageActivityPlugin(processDefId, activityDefId);
            saveOrUpdate(packageDef);
        }
        packageDef.addPackageActivityPlugin(activityPlugin);
        saveOrUpdate(packageDef);
    }

    @Override
    public void removeAppActivityPlugin(String appId, Long appVersion, String processDefId, String activityDefId) {
        PackageDefinition packageDef = loadAppPackageDefinition(appId, appVersion);
        processDefId = WorkflowUtil.getProcessDefIdWithoutVersion(processDefId);
        packageDef.removePackageActivityPlugin(processDefId, activityDefId);
        saveOrUpdate(packageDef);
    }

    @Override
    public void addAppParticipant(String appId, Long appVersion, PackageParticipant participant) {
        PackageDefinition packageDef = loadAppPackageDefinition(appId, appVersion);
        if (packageDef == null) {
            AppDefinition appDef = getAppDefinitionDao().loadVersion(appId, appVersion);
            packageDef = createPackageDefinition(appDef, appVersion);
        }
        String processDefId = participant.getProcessDefId();
        processDefId = WorkflowUtil.getProcessDefIdWithoutVersion(processDefId);
        participant.setProcessDefId(processDefId);
        String participantId = participant.getParticipantId();
        if (processDefId != null && participantId != null) {
            packageDef.removePackageParticipant(processDefId, participantId);
            saveOrUpdate(packageDef);
        }
        packageDef.addPackageParticipant(participant);
        saveOrUpdate(packageDef);
    }

    @Override
    public void removeAppParticipant(String appId, Long appVersion, String processDefId, String participantId) {
        PackageDefinition packageDef = loadAppPackageDefinition(appId, appVersion);
        processDefId = WorkflowUtil.getProcessDefIdWithoutVersion(processDefId);
        packageDef.removePackageParticipant(processDefId, participantId);
        saveOrUpdate(packageDef);
    }
}
