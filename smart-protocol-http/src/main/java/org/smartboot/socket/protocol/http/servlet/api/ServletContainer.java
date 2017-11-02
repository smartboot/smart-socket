package org.smartboot.socket.protocol.http.servlet.api;

import org.smartboot.socket.protocol.http.servlet.ServletContainerImpl;

import java.util.Collection;

/**
 * @author 三刀
 * @version V1.0 , 2017/10/31
 */
public interface ServletContainer {
    /**
     *
     * @return The names of the deployments in this container
     */
    Collection<String> listDeployments();

    void addDeployment(DeploymentInfo deployment);

    DeploymentInfo getDeployment(String deploymentName);

    void removeDeployment(DeploymentInfo deploymentInfo);

    DeploymentInfo getDeploymentByPath(String uripath);

    class Factory {

        public static ServletContainer newInstance() {
            return new ServletContainerImpl();
        }
    }
}
