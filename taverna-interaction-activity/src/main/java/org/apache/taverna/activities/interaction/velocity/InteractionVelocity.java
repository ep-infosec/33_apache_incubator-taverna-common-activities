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

package org.apache.taverna.activities.interaction.velocity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import org.apache.taverna.activities.interaction.InteractionActivity;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.apache.velocity.Template;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.StringResourceLoader;
import org.apache.velocity.runtime.resource.util.StringResourceRepository;

/**
 * @author alanrw
 * 
 */
public class InteractionVelocity {

	public static Logger logger = Logger.getLogger(InteractionVelocity.class);

	private static boolean velocityInitialized = false;

	private static final String TEMPLATE_SUFFIX = ".vm";

	private Template interactionTemplate = null;
	private static final String INTERACTION_TEMPLATE_NAME = "interaction";

	private ArrayList<String> templateNames = new ArrayList<String>();

	private VelocityEngine ve = new VelocityEngine();
	
	@SuppressWarnings("deprecation")
	public synchronized void checkVelocity() {
		if (velocityInitialized) { 
			return;
		}
		velocityInitialized = true;
		ve.setProperty(RuntimeConstants.RESOURCE_LOADER, "string");
		ve.setProperty("resource.loader.class",
				"org.apache.velocity.runtime.resource.loader.StringResourceLoader");
		ve.setProperty(RuntimeConstants.RUNTIME_LOG_LOGSYSTEM_CLASS,
				"org.apache.velocity.runtime.log.Log4JLogChute");
		ve.setProperty("runtime.log.logsystem.log4j.logger",
				"net.sf.taverna.t2.activities.interaction.velocity.InteractionVelocity");
		ve.init();
		ve.loadDirective(RequireDirective.class.getName());
		ve.loadDirective(ProduceDirective.class.getName());
		ve.loadDirective(NotifyDirective.class.getName());

		loadTemplates();

		interactionTemplate = ve.getTemplate(INTERACTION_TEMPLATE_NAME);
		if (interactionTemplate == null) {
			logger.error("Could not open interaction template "
					+ INTERACTION_TEMPLATE_NAME);
		}
	}

	private void loadTemplates() {
		final InputStream is = InteractionActivity.class
				.getResourceAsStream("/index");
		if (is == null) {
			logger.error("Unable to read /index");
			return;
		}
		final BufferedReader br = new BufferedReader(new InputStreamReader(is));
		try {
			for (String line = br.readLine(); line != null; line = br
					.readLine()) {
				if (line.startsWith("#")) {
					continue;
				}
				line = line.trim();
				if (line.isEmpty()) {
					continue;
				}
				final String templatePath = line + TEMPLATE_SUFFIX;
				logger.info("Looking for " + templatePath);
				final StringResourceRepository repo = StringResourceLoader
						.getRepository();
				try {
					repo.putStringResource(line,
							getTemplateFromResource(templatePath));
				} catch (final IOException e) {
					logger.error(
							"Failed reading template from " + templatePath, e);
				}
				final Template t = Velocity.getTemplate(line);
				if (t == null) {
					logger.error("Registration failed");
				}
				if (!line.equals(INTERACTION_TEMPLATE_NAME)) {
					templateNames.add(line);
				}
			}
		} catch (final IOException e) {
			logger.error("Failed reading template index", e);
		}
	}

	public Template getInteractionTemplate() {
		checkVelocity();
		return interactionTemplate;
	}

	private String getTemplateFromResource(final String templatePath)
			throws IOException {
		checkVelocity();
		final InputStream stream = InteractionVelocity.class
				.getResourceAsStream("/" + templatePath);
		final String result = IOUtils.toString(stream, "UTF-8");
		return result;
	}

	public ArrayList<String> getTemplateNames() {
		checkVelocity();
		return templateNames;
	}
}
