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

package org.apache.taverna.activities.externaltool.ssh;

import java.io.File;

import org.apache.taverna.activities.externaltool.invocation.InvocationException;
import org.apache.taverna.activities.externaltool.manager.InvocationPersister;
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

import org.apache.taverna.security.credentialmanager.CredentialManager;

import org.apache.log4j.Logger;

/**
 * @author alanrw
 *
 */
public class SshInvocationPersister extends InvocationPersister {

	private static Logger logger = Logger.getLogger(SshInvocationPersister.class);
	private CredentialManager credentialManager;



	/* (non-Javadoc)
	 * @see net.sf.taverna.t2.activities.externaltool.manager.InvocationPersister#load()
	 */
	@Override
	public void load(File directory) {
		SshToolInvocation.load(directory);
	}

	/* (non-Javadoc)
	 * @see net.sf.taverna.t2.activities.externaltool.manager.InvocationPersister#persist()
	 */
	@Override
	public void persist(File directory) {
		SshToolInvocation.persist(directory);
	}

	@Override
	public void deleteRun(String runId) {
		try {
			SshToolInvocation.cleanup(runId, credentialManager);
		} catch (InvocationException e) {
			logger.error(e);
		}
	}

	public void setCredentialManager(CredentialManager credentialManager) {
		this.credentialManager = credentialManager;
	}

}
