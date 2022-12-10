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

package org.apache.taverna.activities.wsdl;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.wsdl.WSDLException;
import javax.xml.parsers.ParserConfigurationException;

//import org.apache.taverna.activities.wsdl.security.SecurityProfiles;
import org.apache.taverna.reference.ReferenceService;
import org.apache.taverna.reference.ReferenceServiceException;
import org.apache.taverna.reference.T2Reference;
import org.apache.taverna.security.credentialmanager.CredentialManager;
import org.apache.taverna.workflowmodel.OutputPort;
import org.apache.taverna.workflowmodel.processor.activity.AbstractAsynchronousActivity;
import org.apache.taverna.workflowmodel.processor.activity.ActivityConfigurationException;
import org.apache.taverna.workflowmodel.processor.activity.AsynchronousActivityCallback;
import org.apache.taverna.workflowmodel.health.RemoteHealthChecker;
import org.apache.taverna.workflowmodel.processor.activity.ActivityOutputPort;
import org.apache.taverna.wsdl.parser.TypeDescriptor;
import org.apache.taverna.wsdl.parser.UnknownOperationException;
import org.apache.taverna.wsdl.parser.WSDLParser;

import org.apache.log4j.Logger;
import org.xml.sax.SAXException;

/**
 * An asynchronous Activity that is concerned with WSDL based web-services.
 * <p>
 * The activity is configured according to the WSDL location and the operation.<br>
 * The ports are defined dynamically according to the WSDL specification, and in
 * addition an output<br>
 * port <em>attachmentList</em> is added to represent any attachements that are
 * returned by the webservice.
 * </p>
 * 
 * @author Stuart Owen
 * @author Stian Soiland-Reyes
 */
public class WSDLActivity extends AbstractAsynchronousActivity<JsonNode> implements
		InputPortTypeDescriptorActivity, OutputPortTypeDescriptorActivity {
    
        public static final String URI = "http://ns.taverna.org.uk/2010/activity/wsdl";
	public static final String ENDPOINT_REFERENCE = "EndpointReference";
        
	private JsonNode configurationBean;
	private WSDLParser parser;
	private boolean isWsrfService = false;
	private String endpointReferenceInputPortName;
	private CredentialManager credentialManager;

	public WSDLActivity(CredentialManager credentialManager) {
		this.credentialManager = credentialManager;
	}

	public boolean isWsrfService() {
		return isWsrfService;
	}

	private static Logger logger = Logger.getLogger(WSDLActivity.class);

	/**
	 * Configures the activity according to the information passed by the
	 * configuration bean.<br>
	 * During this process the WSDL is parsed to determine the input and output
	 * ports.
	 *
	 * @param bean
	 *            the {@link WSDLActivityConfigurationBean} configuration bean
	 */
	@Override
	public void configure(JsonNode bean)
			throws ActivityConfigurationException {
            this.configurationBean = bean;
            try {
                    parseWSDL();
            } catch (Exception ex) {
                throw new ActivityConfigurationException(
                            "Unable to parse the WSDL " + bean.get("operation").get("wsdl").textValue(), ex);
            }
	}

	@Override
	public JsonNode getConfiguration() {
		return configurationBean;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seenet.sf.taverna.t2.activities.wsdl.InputPortTypeDescriptorActivity#
	 * getTypeDescriptorForInputPort(java.lang.String)
	 */
        @Override
	public TypeDescriptor getTypeDescriptorForInputPort(String portName)
			throws UnknownOperationException, IOException {
		List<TypeDescriptor> inputDescriptors = parser
				.getOperationInputParameters(configurationBean.get("operation").get("name").textValue());
		TypeDescriptor result = null;
		for (TypeDescriptor descriptor : inputDescriptors) {
			if (descriptor.getName().equals(portName)) {
				result = descriptor;
				break;
			}
		}
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seenet.sf.taverna.t2.activities.wsdl.InputPortTypeDescriptorActivity#
	 * getTypeDescriptorsForInputPorts()
	 */
        @Override
	public Map<String, TypeDescriptor> getTypeDescriptorsForInputPorts()
			throws UnknownOperationException, IOException {
		Map<String, TypeDescriptor> descriptors = new HashMap<String, TypeDescriptor>();
		List<TypeDescriptor> inputDescriptors = parser
				.getOperationInputParameters(configurationBean.get("operation").get("name").textValue());
		for (TypeDescriptor descriptor : inputDescriptors) {
			descriptors.put(descriptor.getName(), descriptor);
		}
		return descriptors;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seenet.sf.taverna.t2.activities.wsdl.OutputPortTypeDescriptorActivity#
	 * getTypeDescriptorForOutputPort(java.lang.String)
	 */
        @Override
	public TypeDescriptor getTypeDescriptorForOutputPort(String portName)
			throws UnknownOperationException, IOException {
		TypeDescriptor result = null;
		List<TypeDescriptor> outputDescriptors = parser
				.getOperationOutputParameters(configurationBean.get("operation").get("name").textValue());
		for (TypeDescriptor descriptor : outputDescriptors) {
			if (descriptor.getName().equals(portName)) {
				result = descriptor;
				break;
			}
		}
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seenet.sf.taverna.t2.activities.wsdl.OutputPortTypeDescriptorActivity#
	 * getTypeDescriptorsForOutputPorts()
	 */
        @Override
	public Map<String, TypeDescriptor> getTypeDescriptorsForOutputPorts()
			throws UnknownOperationException, IOException {
		Map<String, TypeDescriptor> descriptors = new HashMap<String, TypeDescriptor>();
		List<TypeDescriptor> inputDescriptors = parser
				.getOperationOutputParameters(configurationBean.get("operation").get("name").textValue());
		for (TypeDescriptor descriptor : inputDescriptors) {
			descriptors.put(descriptor.getName(), descriptor);
		}
		return descriptors;
	}

	private void parseWSDL() throws ParserConfigurationException,
			WSDLException, IOException, SAXException, UnknownOperationException {
	    URLConnection connection = null;
	    try {
		URL wsdlURL = new URL(configurationBean.get("operation").get("wsdl").textValue());
		connection = wsdlURL.openConnection();
		connection.setConnectTimeout(RemoteHealthChecker.getTimeoutInSeconds() * 1000);
		connection.connect();
	    } catch (MalformedURLException e) {
		throw new IOException("Malformed URL", e);
	    } catch (SocketTimeoutException e) {
		throw new IOException("Timeout", e);
	    } catch (IOException e) {
		throw e;
	    } finally {
		if ((connection != null) && (connection.getInputStream() != null)) {
		    connection.getInputStream().close();
		}
	    }
	    parser = new WSDLParser(configurationBean.get("operation").get("wsdl").textValue());
            isWsrfService = parser.isWsrfService();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void executeAsynch(final Map<String, T2Reference> data,
			final AsynchronousActivityCallback callback) {

		callback.requestRun(new Runnable() {

			public void run() {

				ReferenceService referenceService = callback.getContext()
						.getReferenceService();

				Map<String, T2Reference> outputData = new HashMap<String, T2Reference>();
				Map<String, Object> invokerInputMap = new HashMap<String, Object>();

				try {
					String endpointReference = null;
					for (String key : data.keySet()) {
						Object renderIdentifier = referenceService
								.renderIdentifier(data.get(key), String.class,
										callback.getContext());
						if (isWsrfService()
								&& key.equals(endpointReferenceInputPortName)) {
							endpointReference = (String) renderIdentifier;
						} else {
							invokerInputMap.put(key, renderIdentifier);
						}
					}
					List<String> outputNames = new ArrayList<String>();
					for (OutputPort port : getOutputPorts()) {
						outputNames.add(port.getName());
					}

					T2WSDLSOAPInvoker invoker = new T2WSDLSOAPInvoker(parser,
							configurationBean.get("operation").get("name").textValue(), outputNames,
							endpointReference, credentialManager);

					Map<String, Object> invokerOutputMap = invoker.invoke(
							invokerInputMap, configurationBean);

					for (String outputName : invokerOutputMap.keySet()) {
						Object value = invokerOutputMap.get(outputName);

						if (value != null) {
                                                        Integer depth = getOutputPortDepth(outputName);
							if (depth != null) {
								outputData.put(outputName, referenceService
										.register(value, depth, true, callback
												.getContext()));
							} else {
								logger.info("Skipping unknown output port :"
												+ outputName);
//								// TODO what should the depth be in this case?
//								outputData.put(outputName, referenceService
//										.register(value, 0, true, callback
//												.getContext()));
							}
						}
					}
					callback.receiveResult(outputData, new int[0]);
				} catch (ReferenceServiceException e) {
					logger.error("Error finding the input data for "
							+ getConfiguration().get("operation"), e);
					callback.fail("Unable to find input data", e);
				} catch (Exception e) {
					logger.error("Error invoking WSDL service "
							+ getConfiguration().get("operation"), e);
					callback.fail("An error occurred invoking the WSDL service", e);
				}

			}

		});

	}

	private Integer getOutputPortDepth(String portName) {
		for (ActivityOutputPort outputPort : getOutputPorts()) {
			if (outputPort.getName().equals(portName)) {
				return outputPort.getDepth();
                        }
                }
                return null;
        }
}
