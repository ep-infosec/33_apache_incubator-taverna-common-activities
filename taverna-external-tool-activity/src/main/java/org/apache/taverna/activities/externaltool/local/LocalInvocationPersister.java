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

package org.apache.taverna.activities.externaltool.local;

import java.io.File;

import org.apache.log4j.Logger;
import org.apache.taverna.activities.externaltool.manager.InvocationPersister;

/**
 * @author alanrw
 *
 */
public class LocalInvocationPersister extends InvocationPersister {
	
	private static Logger logger = Logger.getLogger(LocalInvocationPersister.class);

	/* (non-Javadoc)
	 * @see net.sf.taverna.t2.activities.externaltool.manager.InvocationPersister#load()
	 */
	@Override
	public void load(File directory) {
		LocalToolInvocation.load(directory);
	}

	/* (non-Javadoc)
	 * @see net.sf.taverna.t2.activities.externaltool.manager.InvocationPersister#persist()
	 */
	@Override
	public void persist(File directory) {
		LocalToolInvocation.persist(directory);
	}
	
	@Override
	public void deleteRun(String runId) {
			LocalToolInvocation.cleanup(runId);
	}

}