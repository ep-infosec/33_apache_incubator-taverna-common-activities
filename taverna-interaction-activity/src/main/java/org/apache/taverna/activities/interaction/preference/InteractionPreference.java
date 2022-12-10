/*
* Licensed to the Apache Software Foundation (ASF) under one
* or more contributor license agreements. See the NOTICE file
* distributed with this work for additional information
* regarding copyright ownership. The ASF licenses this file
* to you under the Apache License, Version 2.0 (the
* "License"); you may not use this file except in compliance
* with the License. You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied. See the License for the
* specific language governing permissions and limitations
* under the License.
*/

package org.apache.taverna.activities.interaction.preference;

import java.awt.GraphicsEnvironment;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;

import org.apache.log4j.Logger;

import org.apache.taverna.configuration.app.ApplicationConfiguration;

/**
 * @author alanrw
 * 
 */
public class InteractionPreference {
	
	private ApplicationConfiguration appConfig;

	private static final String USE_JETTY = "useJetty";

	private static final String DEFAULT_USE_JETTY = "true";

	private static final String PORT = "port";

	private static final String DEFAULT_PORT = "8080";

	private static final String HOST = "host";

	private static final String DEFAULT_HOST = "http://localhost";

	private static final String WEBDAV_PATH = "webdavPath";

	private static final String DEFAULT_WEBDAV_PATH = "/interaction";

	private static final String FEED_PATH = "feedPath";

	private static final String DEFAULT_FEED_PATH = "/feed";

	private static final String USE_USERNAME = "Secure with username / password";

	private static final String DEFAULT_USE_USERNAME = "false";

	// private static final String USE_HTTPS = "Use HTTPS";

	// private static final String DEFAULT_USE_HTTPS = "false";

	private final Logger logger = Logger.getLogger(InteractionPreference.class);

	private final Properties properties;

	private File getConfigFile() {
		final File home = appConfig
				.getApplicationHomeDir().toFile();
		final File config = new File(home, "conf");
		if (!config.exists()) {
			config.mkdir();
		}
		final File configFile = new File(config, this.getFilePrefix() + "-"
				+ this.getUUID() + ".config");
		return configFile;
	}

	private InteractionPreference(ApplicationConfiguration appConfig) {
		setAppConfig(appConfig);
		final File configFile = this.getConfigFile();
		this.properties = new Properties();
		if (configFile.exists()) {
			try {
				final FileReader reader = new FileReader(configFile);
				this.properties.load(reader);
				reader.close();
			} catch (final FileNotFoundException e) {
				this.logger.error(e);
			} catch (final IOException e) {
				this.logger.error(e);
			}
		}
		if (GraphicsEnvironment.isHeadless()
				|| ((System.getProperty("java.awt.headless") != null) && System
						.getProperty("java.awt.headless").equals("true"))) {
			final String definedHost = System
					.getProperty("taverna.interaction.host");
			if (definedHost != null) {
				this.properties.setProperty(USE_JETTY, "false");
				this.logger.info("USE_JETTY set to false");
				this.properties.setProperty(HOST, definedHost);
			}
			final String definedPort = System
					.getProperty("taverna.interaction.port");
			if (definedPort != null) {
				this.properties.setProperty(PORT, definedPort);
			}
			final String definedWebDavPath = System
					.getProperty("taverna.interaction.webdav_path");
			if (definedWebDavPath != null) {
				this.properties.setProperty(WEBDAV_PATH, definedWebDavPath);
			}
			final String definedFeedPath = System
					.getProperty("taverna.interaction.feed_path");
			if (definedFeedPath != null) {
				this.properties.setProperty(FEED_PATH, definedFeedPath);
			}
		} else {
			this.logger.info("Running non-headless");
		}
		this.fillDefaultProperties();
	}

	private void fillDefaultProperties() {
		if (!this.properties.containsKey(USE_JETTY)) {
			this.properties.setProperty(USE_JETTY, DEFAULT_USE_JETTY);
			this.logger.info("USE_JETTY set to " + DEFAULT_USE_JETTY);
		}
		if (!this.properties.containsKey(PORT)) {
			this.properties.setProperty(PORT, DEFAULT_PORT);
		}
		if (!this.properties.containsKey(HOST)) {
			this.properties.setProperty(HOST, DEFAULT_HOST);
		}
		if (!this.properties.containsKey(WEBDAV_PATH)) {
			this.properties.setProperty(WEBDAV_PATH, DEFAULT_WEBDAV_PATH);
		}
		if (!this.properties.containsKey(FEED_PATH)) {
			this.properties.setProperty(FEED_PATH, DEFAULT_FEED_PATH);
		}
		if (!this.properties.containsKey(USE_USERNAME)) {
			this.properties.setProperty(USE_USERNAME, DEFAULT_USE_USERNAME);
		}
		/*
		 * if (!properties.containsKey(USE_HTTPS)) {
		 * properties.setProperty(USE_HTTPS, DEFAULT_USE_HTTPS); }
		 */
	}

	public String getFilePrefix() {
		return "Interaction";
	}

	public void store() {
		try {
			final FileOutputStream out = new FileOutputStream(
					this.getConfigFile());
			this.properties.store(out, "");
			out.close();
		} catch (final FileNotFoundException e) {
			this.logger.error(e);
		} catch (final IOException e) {
			this.logger.error(e);
		}
	}

	public String getUUID() {
		return "DA992717-5A46-469D-AE25-883F0E4CD348";
	}

	public void setPort(final String text) {
		this.properties.setProperty(PORT, text);
	}

	public void setHost(final String text) {
		this.properties.setProperty(HOST, text);
	}

	public void setUseJetty(final boolean use) {
		this.properties.setProperty(USE_JETTY, Boolean.toString(use));
	}

	public void setFeedPath(final String path) {
		this.properties.setProperty(FEED_PATH, path);
	}

	public void setWebDavPath(final String path) {
		this.properties.setProperty(WEBDAV_PATH, path);
	}

	public String getPort() {
		return this.properties.getProperty(PORT);
	}

	public String getHost() {
		return this.properties.getProperty(HOST);
	}

	public boolean getUseJetty() {
		return (Boolean.parseBoolean(this.properties.getProperty(USE_JETTY)));
	}

	public String getFeedPath() {
		return this.properties.getProperty(FEED_PATH);
	}

	public String getWebDavPath() {
		return this.properties.getProperty(WEBDAV_PATH);
	}

	public String getDefaultHost() {
		return DEFAULT_HOST;
	}

	public String getDefaultFeedPath() {
		return DEFAULT_FEED_PATH;
	}

	public String getDefaultWebDavPath() {
		return DEFAULT_WEBDAV_PATH;
	}

	public String getFeedUrlString() {
		return this.getHost() + ":" + this.getPort() + this.getFeedPath();
	}

	public String getLocationUrl() {
		return this.getHost() + ":" + this.getPort() + this.getWebDavPath();
	}

	public boolean getUseUsername() {
		return (Boolean.parseBoolean(this.properties.getProperty(USE_USERNAME)));
	}

	public void setUseUsername(final boolean useUsername) {
		this.properties
				.setProperty(USE_USERNAME, Boolean.toString(useUsername));
	}

	public String getOutputDataUrlString(final String interactionId) {
		return this.getLocationUrl()
				+ "/interaction" + interactionId + "OutputData.json";
	}

	public String getInputDataUrlString(final String interactionId) {
		return this.getLocationUrl()
				+ "/interaction" + interactionId + "InputData.json";
	}

	public URL getFeedUrl() throws MalformedURLException {
		return new URL(this.getFeedUrlString());
	}

	public String getInteractionUrlString(final String interactionId) {
		return this.getLocationUrl()
				+ "/interaction" + interactionId + ".html";
	}

	public String getPresentationUrlString(final String interactionId) {
		return this.getLocationUrl()
				+ "/presentation" + interactionId + ".html";
	}

	public String getPublicationUrlString(final String interactionId,
			final String key) {
		return this.getLocationUrl()
				+ "/interaction" + interactionId + "_" + key;
	}

	public void setAppConfig(ApplicationConfiguration appConfig) {
		this.appConfig = appConfig;
	}

}
