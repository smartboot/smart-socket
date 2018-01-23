package org.smartboot.socket.protocol.http.jndi.resources;

import java.io.Serializable;

/**
 * DataSourceConfig describe a DataSource Configuration. This is all parameter
 * that WinstoneDatasourceFactory can handle. If you want more functionality,
 * please use other framework like common dbcp.
 * <ul>
 * <li><i>name</i> Jndi Datasource Name</li>
 * <li><i>url</i> Connection URL to be passed to our JDBC driver. (For backwards
 * compatibility, the property driverName is also recognized.)</li>
 * <li><i>driverClassName</i> Fully qualified Java class name of the JDBC driver
 * to be used.</li>
 * <li><i>username</i> Database username to be passed to our JDBC driver.</li>
 * <li><i>password</i> Database password to be passed to our JDBC driver.</li>
 * <li><i>maxActive</i> The maximum number of active instances that can be
 * allocated from this pool at the same time (default is 20). Set to -1 for no
 * limit.</li>
 * <li><i>maxIdle</i> The maximum number of connections that can sit idle in
 * this pool at the same time (default is 10).</li>
 * <li><i>minIdle</i> The minimum number of connections that can remain idle in
 * the pool (default is one).</li>
 * <li><i>maxWait</i> The maximum number of milliseconds that the pool will wait
 * (when there are no available connections) for a connection to be returned
 * before throwing an exception (default is 10000). Set to -1 to wait
 * indefinitely.</li>
 * <li><i>validationQuery</i> SQL query that can be used by the pool to validate
 * connections before they are returned to the application. If
 * <strong>isValid</strong> is specified as value, then isValid() method of SQL
 * connection is used (note that it is available only in Java 6 drivers). If
 * <strong>isClosed</strong> is specified, then isClosed() method used for a
 * connection validation. Default is none.</li>
 * <li><i>validationTimeOut</i> The maximum number of milliseconds that system
 * will wait when executing validation query (Default is 1000 ms).</li>
 * <li><i>keepAliveSQL</i> The sql to execute on keep-alive operations. Default
 * is empty (disabled).</li>
 * <li><i>keepAliveTimeOut</i> The maximum number of milliseconds that system
 * will wait when executing keep-alive query (Default is 1000 ms).</li>
 * <li><i>keepAlivePeriod</i> Execute the keepAliveSQL on all unused connection
 * every n minutes. Default is one minute</li>
 * <li><i>killInactivePeriod</i> Kills excess unused connections every n
 * minutes. Default is disabled (-1);</li>
 * 
 * @author <a href="mailto:jguibert@intelligents-ia.com" >Jerome Guibert</a>
 */
public class DataSourceConfig implements Serializable {

	/**
	 * serialVersionUID:long
	 */
	private static final long serialVersionUID = -4762104837042371012L;

	private String name = null;

	private String url = null;
	private String driverClassName = null;
	private String username = null;
	private String password = null;

	private int maxActive = 20;
	private int maxIdle = 10;
	private int minIdle = 1;

	private int maxWait = 10000;

	private String validationQuery = null;
	private int validationTimeOut = 1000;

	private String keepAliveSQL = null;
	private int keepAlivePeriod = 1;
	private int keepAliveTimeOut = 1000;
	private int killInactivePeriod = -1;

	public DataSourceConfig() {
		super();
	}

	public String getName() {
		return name;
	}

	public void setName(final String name) {
		this.name = name;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(final String url) {
		this.url = url;
	}

	public String getDriverClassName() {
		return driverClassName;
	}

	public void setDriverClassName(final String driverClassName) {
		this.driverClassName = driverClassName;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(final String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(final String password) {
		this.password = password;
	}

	public int getMaxActive() {
		return maxActive;
	}

	public void setMaxActive(final int maxActive) {
		this.maxActive = maxActive;
	}

	public int getMaxIdle() {
		return maxIdle;
	}

	public void setMaxIdle(final int maxIdle) {
		this.maxIdle = maxIdle;
	}

	public int getMinIdle() {
		return minIdle;
	}

	public void setMinIdle(final int minIdle) {
		this.minIdle = minIdle;
	}

	public int getMaxWait() {
		return maxWait;
	}

	public void setMaxWait(final int maxWait) {
		this.maxWait = maxWait;
	}

	public String getValidationQuery() {
		return validationQuery;
	}

	public void setValidationQuery(final String validationQuery) {
		this.validationQuery = validationQuery;
	}

	public int getValidationTimeOut() {
		return validationTimeOut;
	}

	public void setValidationTimeOut(final int validationTimeOut) {
		this.validationTimeOut = validationTimeOut;
	}

	public String getKeepAliveSQL() {
		return keepAliveSQL;
	}

	public void setKeepAliveSQL(final String keepAliveSQL) {
		this.keepAliveSQL = keepAliveSQL;
	}

	public int getKeepAlivePeriod() {
		return keepAlivePeriod;
	}

	public void setKeepAlivePeriod(final int keepAlivePeriod) {
		this.keepAlivePeriod = keepAlivePeriod;
	}

	public int getKeepAliveTimeOut() {
		return keepAliveTimeOut;
	}

	public void setKeepAliveTimeOut(final int keepAliveTimeOut) {
		this.keepAliveTimeOut = keepAliveTimeOut;
	}

	public int getKillInactivePeriod() {
		return killInactivePeriod;
	}

	public void setKillInactivePeriod(final int killInactivePeriod) {
		this.killInactivePeriod = killInactivePeriod;
	}

}
