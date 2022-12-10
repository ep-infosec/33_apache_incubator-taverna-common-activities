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

package org.apache.taverna.activities.interaction;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 
 * This class is used to remember and forget interactions and their associated
 * ATOM entries and files
 * 
 * @author alanrw
 * 
 */
public class InteractionRecorder {

	private static final Logger logger = Logger
			.getLogger(InteractionRecorder.class);

	static Map<String, Map<String, Set<String>>> runToInteractionMap = Collections
			.synchronizedMap(new HashMap<String, Map<String, Set<String>>>());
	
	private InteractionUtils interactionUtils;

	private InteractionRecorder() {
		super();
	}

	public void deleteRun(final String runToDelete) {
		final Set<String> interactionIds = new HashSet<String>(
				getInteractionMap(runToDelete).keySet());
		for (final String interactionId : interactionIds) {
			deleteInteraction(runToDelete, interactionId);
		}
		runToInteractionMap.remove(runToDelete);
	}

	public void deleteInteraction(final String runId,
			final String interactionId) {
		for (final String urlString : getResourceSet(runId, interactionId)) {
			try {
				deleteUrl(urlString);
			} catch (final IOException e) {
				logger.info("Unable to delete " + urlString, e);
			}

		}
		getInteractionMap(runId).remove(interactionId);
	}

	private void deleteUrl(final String urlString) throws IOException {
		logger.info("Deleting resource " + urlString);
		final URL url = new URL(urlString);
		final HttpURLConnection httpCon = (HttpURLConnection) url
				.openConnection();
		httpCon.setRequestMethod("DELETE");
		final int response = httpCon.getResponseCode();
		if (response >= 400) {
			logger.info("Received response code" + response);
		}
	}

	public void addResource(final String runId,
			final String interactionId, final String resourceId) {
		if (resourceId == null) {
			logger.error("Attempt to add null resource",
					new NullPointerException(""));
			return;
		}
		logger.info("Adding resource " + resourceId);
		final Set<String> resourceSet = getResourceSet(runId, interactionId);

		resourceSet.add(resourceId);
	}

	private Set<String> getResourceSet(final String runId,
			final String interactionId) {
		final Map<String, Set<String>> interactionMap = getInteractionMap(runId);
		Set<String> resourceSet = interactionMap.get(interactionId);
		if (resourceSet == null) {
			resourceSet = Collections.synchronizedSet(new HashSet<String>());
			interactionMap.put(interactionId, resourceSet);
		}
		return resourceSet;
	}

	private Map<String, Set<String>> getInteractionMap(final String runId) {
		Map<String, Set<String>> interactionMap = InteractionRecorder.runToInteractionMap
				.get(runId);
		if (interactionMap == null) {
			interactionMap = Collections.synchronizedMap(Collections
					.synchronizedMap(new HashMap<String, Set<String>>()));
			InteractionRecorder.runToInteractionMap.put(runId, interactionMap);
		}
		return interactionMap;
	}

	public void persist() {
		final File outputFile = getUsageFile();
		try {
			FileUtils.writeStringToFile(outputFile, InteractionUtils
					.objectToJson(InteractionRecorder.runToInteractionMap));
		} catch (final IOException e) {
			logger.error(e);
		}
	}

	private File getUsageFile() {
		return new File(getInteractionUtils().getInteractionServiceDirectory(),
				"usage");
	}

	public void load() {
		final File inputFile = getUsageFile();
		try {
			final String usageString = FileUtils.readFileToString(inputFile);
			final ObjectMapper mapper = new ObjectMapper();
			@SuppressWarnings("unchecked")
			final Map<String, Object> rootAsMap = mapper.readValue(usageString,
					Map.class);
			InteractionRecorder.runToInteractionMap.clear();
			for (final String runId : rootAsMap.keySet()) {
				@SuppressWarnings("unchecked")
				final Map<String, Object> runMap = (Map<String, Object>) rootAsMap
						.get(runId);
				for (final String interactionId : runMap.keySet()) {
					@SuppressWarnings("unchecked")
					final List<String> urlList = (List<String>) runMap
							.get(interactionId);
					for (final String url : urlList) {
						addResource(runId, interactionId, url);
					}
				}
			}
		} catch (final IOException e) {
			logger.info(e);
		}
	}

	public InteractionUtils getInteractionUtils() {
		return interactionUtils;
	}

	public void setInteractionUtils(InteractionUtils interactionUtils) {
		this.interactionUtils = interactionUtils;
	}

}
