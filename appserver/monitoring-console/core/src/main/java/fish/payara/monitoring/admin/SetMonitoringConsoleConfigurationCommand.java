/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2019 Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * The Payara Foundation designates this particular file as subject to the "Classpath"
 * exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

package fish.payara.monitoring.admin;

import static org.glassfish.config.support.CommandTarget.DAS;

import java.nio.file.Path;
import java.nio.file.Paths;

import javax.inject.Inject;

import org.glassfish.api.ActionReport;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.CommandRunner;
import org.glassfish.api.admin.ExecuteOn;
import org.glassfish.api.admin.RuntimeType;
import org.glassfish.config.support.TargetType;
import org.glassfish.deployment.autodeploy.AutoDeployer;
import org.glassfish.deployment.autodeploy.AutoDeploymentOperation;
import org.glassfish.deployment.autodeploy.AutoUndeploymentOperation;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.hk2.api.ServiceLocator;
import org.jvnet.hk2.annotations.Service;

import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.util.SystemPropertyConstants;

@Service(name = "set-monitoring-console-configuration")
@PerLookup
@ExecuteOn({RuntimeType.DAS})
@TargetType({DAS})
public class SetMonitoringConsoleConfigurationCommand implements AdminCommand {

    private static final String MONITORING_CONSOLE_APP_NAME = "__monitoringconsole";
    private final static String GLASSFISH_LIB_INSTALL_APPLICATIONS = "glassfish/lib/install/applications";

    @Param(optional = true)
    private Boolean enabled;

    @Inject
    protected CommandRunner commandRunner;

    @Inject
    private ServiceLocator serviceLocator;

    @Inject
    private Domain domain;

    @Override
    public void execute(AdminCommandContext context) {
        if (enabled != null) {
            if (enabled.booleanValue()) {
                deployMonitoringConsole(context.getActionReport());
            } else {
                undeployMonitoringConsole(context.getActionReport());
            }
        }
    }

    private void undeployMonitoringConsole(ActionReport report) {
        Path applications = getApplicationsPath();
        Path app = applications.resolve(MONITORING_CONSOLE_APP_NAME);

        AutoUndeploymentOperation command = AutoUndeploymentOperation.newInstance(
                serviceLocator,
                app.toFile(),
                MONITORING_CONSOLE_APP_NAME,
                SystemPropertyConstants.DAS_SERVER_NAME);
        AutoDeployer.AutodeploymentStatus deploymentStatus = command.run();

        report.setActionExitCode(deploymentStatus.getExitCode());

        if (deploymentStatus.getExitCode().equals(ActionReport.ExitCode.FAILURE)) {
            if (domain.getApplications().getApplication(MONITORING_CONSOLE_APP_NAME) == null) {
                report.appendMessage("\nMonitoring Console is not enabled on any target");
            } else {
                report.appendMessage("\nFailed to disable Monitoring Console - was it enabled on the specified target?");
            }
        }
    }

    private void deployMonitoringConsole(ActionReport report) {
        Path applications = getApplicationsPath();
        Path app = applications.resolve(MONITORING_CONSOLE_APP_NAME);

        AutoDeploymentOperation command = AutoDeploymentOperation.newInstance(
                serviceLocator,
                app.toFile(),
                null,
                SystemPropertyConstants.DAS_SERVER_NAME,
                "monitoring-console"
                );

        if (domain.getApplications().getApplication(MONITORING_CONSOLE_APP_NAME) == null) {
            AutoDeployer.AutodeploymentStatus deploymentStatus = command.run();
            report.setActionExitCode(deploymentStatus.getExitCode());
        } else {
            report.setActionExitCode(ActionReport.ExitCode.WARNING);
            report.setMessage("Monitoring Console is already deployed on at least one target");
        }
    }

    private static Path getApplicationsPath() {
        return Paths.get(System.getProperty(SystemPropertyConstants.PRODUCT_ROOT_PROPERTY)).resolve(GLASSFISH_LIB_INSTALL_APPLICATIONS);
    }

}
