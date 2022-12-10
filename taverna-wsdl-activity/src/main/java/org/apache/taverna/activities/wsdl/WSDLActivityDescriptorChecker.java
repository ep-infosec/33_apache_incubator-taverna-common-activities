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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.log4j.Logger;

import org.apache.taverna.visit.VisitReport;
import org.apache.taverna.visit.VisitReport.Status;
import org.apache.taverna.workflowmodel.Dataflow;
import org.apache.taverna.workflowmodel.Datalink;
import org.apache.taverna.workflowmodel.Merge;
import org.apache.taverna.workflowmodel.MergeInputPort;
import org.apache.taverna.workflowmodel.MergeOutputPort;
import org.apache.taverna.workflowmodel.MergePort;
import org.apache.taverna.workflowmodel.Port;
import org.apache.taverna.workflowmodel.Processor;
import org.apache.taverna.workflowmodel.ProcessorInputPort;
import org.apache.taverna.workflowmodel.ProcessorPort;
import org.apache.taverna.workflowmodel.health.HealthCheck;
import org.apache.taverna.workflowmodel.health.HealthChecker;
import org.apache.taverna.workflowmodel.processor.activity.Activity;
import org.apache.taverna.workflowmodel.processor.activity.ActivityInputPort;
import org.apache.taverna.workflowmodel.utils.Tools;
import org.apache.taverna.wsdl.parser.TypeDescriptor;
import org.apache.taverna.wsdl.parser.ArrayTypeDescriptor;
import org.apache.taverna.wsdl.parser.ComplexTypeDescriptor;
import org.apache.taverna.wsdl.parser.UnknownOperationException;

/**
 * @author alanrw
 *
 */
public final class WSDLActivityDescriptorChecker implements HealthChecker<InputPortTypeDescriptorActivity> {
	
	private static Logger logger = Logger.getLogger(WSDLActivityDescriptorChecker.class);

	public boolean canVisit(Object o) {
		return ((o != null) && (o instanceof InputPortTypeDescriptorActivity));
	}

	public boolean isTimeConsuming() {
		return false;
	}

	public VisitReport visit(InputPortTypeDescriptorActivity o,
			List<Object> ancestry) {
		List<VisitReport> reports = new ArrayList<VisitReport>();
		try {
			Map<String, TypeDescriptor> typeMap = o.getTypeDescriptorsForInputPorts();
			Processor p = (Processor) VisitReport.findAncestor(ancestry, Processor.class);
			Dataflow d = (Dataflow) VisitReport.findAncestor(ancestry, Dataflow.class);
			
			
			for (Entry<String, TypeDescriptor> entry : typeMap.entrySet()) {
				TypeDescriptor descriptor = entry.getValue();
				if (!descriptor.getMimeType().contains("'text/xml'")) {
					continue;
				}
				if (!((descriptor instanceof ArrayTypeDescriptor) || (descriptor instanceof ComplexTypeDescriptor))) {
					continue;
				}
				// Find the processor port, if any that corresponds to the activity port
				ActivityInputPort aip = Tools.getActivityInputPort((Activity) o, entry.getKey());
				if (aip == null) {
					continue;
				}
				ProcessorInputPort pip = Tools.getProcessorInputPort(p, (Activity<?>) o, aip);
				
				if (pip == null) {
					continue;
				}
				
				for (Datalink dl : d.getLinks()) {

					if (dl.getSink().equals(pip)) {
						Port source = dl.getSource();
						Set<VisitReport> subReports = checkSource(source, d, (Activity) o, aip);
						for (VisitReport vr : subReports) {
						    vr.setProperty("activity", o);
						    vr.setProperty("sinkPort", pip);
						}
						reports.addAll(subReports);
					}
				}

			}
		} catch (UnknownOperationException e) {
			logger.error("Problem getting type descriptors for activity", e);
		} catch (IOException e) {
			logger.error("Problem getting type descriptors for activity", e);
		} catch (NullPointerException e) {
			logger.error("Problem getting type desciptors for activity", e);
		}
		if (reports.isEmpty()) {
			return null;
		}
		if (reports.size() == 1) {
			return reports.get(0);
		}
		else {
			return new VisitReport(HealthCheck.getInstance(), o, "Collation", HealthCheck.DEFAULT_VALUE, reports);
		}
	}
	
	private Set<VisitReport> checkSource(Port source, Dataflow d, Activity o, ActivityInputPort aip) {
		Set<VisitReport> reports = new HashSet<VisitReport>();
		if (source instanceof ProcessorPort) {
			ProcessorPort processorPort = (ProcessorPort) source;
			Processor sourceProcessor = processorPort.getProcessor();
			Activity sourceActivity = sourceProcessor.getActivityList().get(0);
			if (!(sourceActivity instanceof InputPortTypeDescriptorActivity)) {
				VisitReport newReport = new VisitReport(HealthCheck.getInstance(), o, "Source of " + aip.getName(), HealthCheck.DATATYPE_SOURCE, Status.WARNING);
				newReport.setProperty("sinkPortName", aip.getName());
				newReport.setProperty("sourceName", sourceProcessor.getLocalName());
				newReport.setProperty("isProcessorSource", "true");
				reports.add(newReport);
			}
		} else if (source instanceof MergeOutputPort) {
			Merge merge = ((MergePort) source).getMerge();
			for (MergeInputPort mip : merge.getInputPorts()) {
				for (Datalink dl : d.getLinks()) {
					if (dl.getSink().equals(mip)) {
						reports.addAll(checkSource(dl.getSource(), d, o, aip));
					}
				}
				
			}
		} else /* if (source instanceof DataflowInputPort) */  {
			VisitReport newReport = new VisitReport(HealthCheck.getInstance(), o, "Source of " + aip.getName(), HealthCheck.DATATYPE_SOURCE, Status.WARNING);
			newReport.setProperty("sinkPortName", aip.getName());
			newReport.setProperty("sourceName", source.getName());
			newReport.setProperty("isProcessorSource", "false");
			reports.add(newReport);
		} 
		return reports;
	}

}
