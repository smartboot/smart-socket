package org.smartboot.socket.protocol.http.servlet;

import org.smartboot.socket.protocol.http.servlet.api.DeploymentInfo;
import org.smartboot.socket.protocol.http.servlet.api.ServletContainer;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * @author 三刀
 * @version V1.0 , 2017/10/31
 */
public class ServletContainerImpl implements ServletContainer {
    private final Map<String, DeploymentInfo> deployments = Collections.synchronizedMap(new HashMap<String, DeploymentInfo>());
    private final Map<String, DeploymentInfo> deploymentsByPath = Collections.synchronizedMap(new HashMap<String, DeploymentInfo>());

    @Override
    public Collection<String> listDeployments() {
        return new HashSet<>(deployments.keySet());
    }

    @Override
    public void addDeployment(final DeploymentInfo dep) {
        deployments.put(dep.getDeploymentName(), dep);
        deploymentsByPath.put(dep.getContextPath(), dep);
    }

    @Override
    public DeploymentInfo getDeployment(final String deploymentName) {
        return deployments.get(deploymentName);
    }

    @Override
    public void removeDeployment(final DeploymentInfo deploymentInfo) {
//        if (deploymentManager.getState() != DeploymentManager.State.UNDEPLOYED) {
//            throw UndertowServletMessages.MESSAGES.canOnlyRemoveDeploymentsWhenUndeployed(deploymentManager.getState());
//        }
        deployments.remove(deploymentInfo.getDeploymentName());
        deploymentsByPath.remove(deploymentInfo.getContextPath());
    }

    @Override
    public DeploymentInfo getDeploymentByPath(final String path) {

        DeploymentInfo exact = deploymentsByPath.get(path.isEmpty() ? "/" : path);
        if (exact != null) {
            return exact;
        }
        int length = path.length();
        int pos = length;

        while (pos > 1) {
            --pos;
            if (path.charAt(pos) == '/') {
                String part = path.substring(0, pos);
                DeploymentInfo deployment = deploymentsByPath.get(part);
                if (deployment != null) {
                    return deployment;
                }
            }
        }
        return deploymentsByPath.get("/");
    }
}
