/*
 * Copyright 2007 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package example.hibernatetool;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

import javax.sql.DataSource;

import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.mojo.hibernate3.ExporterMojo;
import org.codehaus.mojo.hibernate3.configuration.ComponentConfiguration;
import org.hibernate.HibernateException;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.util.JDBCExceptionReporter;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;
import org.springframework.orm.hibernate3.LocalDataSourceConnectionProvider;
import org.springframework.orm.hibernate3.LocalSessionFactoryBean;
import org.springframework.util.StringUtils;

/**
* 
* @author chetan.mehrotra chetan.mehrotra@gmail.com
*/
public class SpringComponentConfiguration implements ComponentConfiguration {
	private ExporterMojo exporterMojo;
	
	private ApplicationContext applicationContext;

	public Configuration getConfiguration(ExporterMojo exporterMojo)
			throws MojoExecutionException {
		setExporterMojo(exporterMojo);
		initializeAppContext();
		LocalSessionFactoryBean lsfb = getLocalSessionFactoryBean();
		Configuration config = lsfb.getConfiguration();
		initializeDataSource(config);
		return config;
	}

	private void initializeDataSource(Configuration config) {
		String beanName = getExporterMojo().getComponentProperty("datasourcebean", "dataSource");
		DataSource ds = (DataSource) applicationContext.getBean(beanName);
		PrivateConnectionProvider.dataSourceToUse = ds;
		config.setProperty(Environment.CONNECTION_PROVIDER,
				PrivateConnectionProvider.class.getName());
		
	}

	private LocalSessionFactoryBean getLocalSessionFactoryBean() throws MojoExecutionException {
		String beanName = getExporterMojo().getComponentProperty("sessionfactorybean", "sessionFactory");
		LocalSessionFactoryBean lsfb = (LocalSessionFactoryBean) applicationContext.getBean("&"+beanName);
		if(lsfb == null){
			throw new MojoExecutionException("No bean named "+beanName+" present in ApplicationContext");
		}
		return lsfb;
	}

	protected void initializeAppContext() throws MojoExecutionException {
		if(applicationContext != null){
			return;
		}
		try {
			String[] appContextResources = StringUtils.tokenizeToStringArray(getAppContextLocation(), ",");
			applicationContext = constructContext(appContextResources);
		} catch (Exception e) {
			throw new MojoExecutionException("Error in initializing Spring",e);
		}
	}
	
    protected ApplicationContext constructContext(String[] appContextResources) {
        return new FileSystemXmlApplicationContext(appContextResources);
    }

	private String getAppContextLocation() {
		return getExporterMojo().getComponentProperty("appcontextlocation");
	}

	public String getName() {
		return "springconfiguration";
	}
	
    public ExporterMojo getExporterMojo(){
        return exporterMojo;
    }

    public void setExporterMojo( ExporterMojo exporterMojo ){
        this.exporterMojo = exporterMojo;
    }
    
    /**
     * A hack to provide dataSource with minimum effort :). The Spring implementation uses
     * a thread local to access the dataSource and that thread local is reset once sessionFactory
     * initializes.
     * 
     * So here we set the dataSource as a static variable. So that later when Hibernate initializes
     * the ConnectionProvider it gets the datasource as specified in spring
     * @author chetan.mehrotra
     *
     */
    public static class PrivateConnectionProvider extends LocalDataSourceConnectionProvider{
    	static DataSource dataSourceToUse;
    	@Override
    	public void configure(Properties props) throws HibernateException {
    		//let not the default one run ...else exception would be thrown
    	}
    	
    	public Connection getConnection() throws SQLException {
    		try {
    			return dataSourceToUse.getConnection();
    		}
    		catch (SQLException ex) {
    			JDBCExceptionReporter.logExceptions(ex);
    			throw ex;
    		}
    	}
    	
    }

}
