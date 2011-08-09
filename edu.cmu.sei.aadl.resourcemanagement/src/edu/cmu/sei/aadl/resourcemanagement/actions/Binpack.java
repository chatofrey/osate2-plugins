/*
*
* <copyright>
* Copyright � 2004 by Carnegie Mellon University, all rights reserved.
*
* Use of the Open Source AADL Tool Environment (OSATE) is subject to the terms of the license set forth
* at http://www.eclipse.org/legal/cpl-v10.html.
*
* NO WARRANTY
*
* ANY INFORMATION, MATERIALS, SERVICES, INTELLECTUAL PROPERTY OR OTHER PROPERTY OR RIGHTS GRANTED OR PROVIDED BY
* CARNEGIE MELLON UNIVERSITY PURSUANT TO THIS LICENSE (HEREINAFTER THE �DELIVERABLES�) ARE ON AN �AS-IS� BASIS.
* CARNEGIE MELLON UNIVERSITY MAKES NO WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED AS TO ANY MATTER INCLUDING,
* BUT NOT LIMITED TO, WARRANTY OF FITNESS FOR A PARTICULAR PURPOSE, MERCHANTABILITY, INFORMATIONAL CONTENT,
* NONINFRINGEMENT, OR ERROR-FREE OPERATION. CARNEGIE MELLON UNIVERSITY SHALL NOT BE LIABLE FOR INDIRECT, SPECIAL OR
* CONSEQUENTIAL DAMAGES, SUCH AS LOSS OF PROFITS OR INABILITY TO USE SAID INTELLECTUAL PROPERTY, UNDER THIS LICENSE,
* REGARDLESS OF WHETHER SUCH PARTY WAS AWARE OF THE POSSIBILITY OF SUCH DAMAGES. LICENSEE AGREES THAT IT WILL NOT
* MAKE ANY WARRANTY ON BEHALF OF CARNEGIE MELLON UNIVERSITY, EXPRESS OR IMPLIED, TO ANY PERSON CONCERNING THE
* APPLICATION OF OR THE RESULTS TO BE OBTAINED WITH THE DELIVERABLES UNDER THIS LICENSE.
*
* Licensee hereby agrees to defend, indemnify, and hold harmless Carnegie Mellon University, its trustees, officers,
* employees, and agents from all claims or demands made against them (and any related losses, expenses, or
* attorney�s fees) arising out of, or relating to Licensee�s and/or its sub licensees� negligent use or willful
* misuse of or negligent conduct or willful misconduct regarding the Software, facilities, or other rights or
* assistance granted by Carnegie Mellon University under this License, including, but not limited to, any claims of
* product liability, personal injury, death, damage to property, or violation of any laws or regulations.
*
* Carnegie Mellon University Software Engineering Institute authored documents are sponsored by the U.S. Department
* of Defense under Contract F19628-00-C-0003. Carnegie Mellon University retains copyrights in all material produced
* under this contract. The U.S. Government retains a non-exclusive, royalty-free license to publish or reproduce these
* documents, or allow others to do so, for U.S. Government purposes only pursuant to the copyright license
* under the contract clause at 252.227.7013.
*
* </copyright>
*
*
* %W%
* @version %I% %H%
*/
package edu.cmu.sei.aadl.resourcemanagement.actions;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.common.command.Command;
import org.eclipse.emf.common.command.CommandStack;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.edit.domain.AdapterFactoryEditingDomain;
import org.eclipse.emf.edit.domain.EditingDomain;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.widgets.Display;
import org.osgi.framework.Bundle;

import EAnalysis.BinPacking.AssignmentResult;
import EAnalysis.BinPacking.BFCPBinPacker;
import EAnalysis.BinPacking.BandwidthComparator;
import EAnalysis.BinPacking.CapacityComparator;
import EAnalysis.BinPacking.CompositeSoftNode;
import EAnalysis.BinPacking.DFBPBinPacker;
import EAnalysis.BinPacking.DFCPBinPacker;
import EAnalysis.BinPacking.Disjoint;
import EAnalysis.BinPacking.Expansor;
import EAnalysis.BinPacking.HardwareNode;
import EAnalysis.BinPacking.LowLevelBinPacker;
import EAnalysis.BinPacking.Message;
import EAnalysis.BinPacking.NFCHoBinPacker;
import EAnalysis.BinPacking.NoExpansionExpansor;
import EAnalysis.BinPacking.OutDegreeAssignmentProblem;
import EAnalysis.BinPacking.OutDegreeComparator;
import EAnalysis.BinPacking.Processor;
import EAnalysis.BinPacking.SetConstraint;
import EAnalysis.BinPacking.Site;
import EAnalysis.BinPacking.SiteArchitecture;
import EAnalysis.BinPacking.SiteGuest;
import EAnalysis.BinPacking.SoftwareNode;
import edu.cmu.sei.aadl.aadl2.Classifier;
import edu.cmu.sei.aadl.aadl2.ClassifierValue;
import edu.cmu.sei.aadl.aadl2.ComponentCategory;
import edu.cmu.sei.aadl.aadl2.ComponentClassifier;
import edu.cmu.sei.aadl.aadl2.DataClassifier;
import edu.cmu.sei.aadl.aadl2.Element;
import edu.cmu.sei.aadl.aadl2.Feature;
import edu.cmu.sei.aadl.aadl2.ProcessorClassifier;
import edu.cmu.sei.aadl.aadl2.Property;
import edu.cmu.sei.aadl.aadl2.PropertyConstant;
import edu.cmu.sei.aadl.aadl2.SystemClassifier;
import edu.cmu.sei.aadl.aadl2.UnitLiteral;
import edu.cmu.sei.aadl.aadl2.instance.ComponentInstance;
import edu.cmu.sei.aadl.aadl2.instance.ConnectionInstance;
import edu.cmu.sei.aadl.aadl2.instance.ConnectionInstanceEnd;
import edu.cmu.sei.aadl.aadl2.instance.ConnectionKind;
import edu.cmu.sei.aadl.aadl2.instance.FeatureInstance;
import edu.cmu.sei.aadl.aadl2.instance.InstanceObject;
import edu.cmu.sei.aadl.aadl2.instance.InstanceReferenceValue;
import edu.cmu.sei.aadl.aadl2.instance.SystemInstance;
import edu.cmu.sei.aadl.aadl2.instance.SystemOperationMode;
import edu.cmu.sei.aadl.aadl2.properties.InstanceUtil;
import edu.cmu.sei.aadl.aadl2.properties.InvalidModelException;
import edu.cmu.sei.aadl.aadl2.properties.PropertyNotPresentException;
import edu.cmu.sei.aadl.modelsupport.eclipseinterface.OsateResourceManager;
import edu.cmu.sei.aadl.modelsupport.errorreporting.AnalysisErrorReporterManager;
import edu.cmu.sei.aadl.modelsupport.modeltraversal.ForAllElement;
import edu.cmu.sei.aadl.resourcemanagement.ResourcemanagementPlugin;
import edu.cmu.sei.contributes.sei.names.SEI;
import edu.cmu.sei.osate.ui.actions.AbstractInstanceOrDeclarativeModelReadOnlyAction;
import edu.cmu.sei.osate.workspace.names.standard.AadlProject;
import edu.cmu.sei.osate.workspace.names.standard.CommunicationProperties;
import edu.cmu.sei.osate.workspace.names.standard.DeploymentProperties;
import edu.cmu.sei.osate.workspace.names.standard.MemoryProperties;
import edu.cmu.sei.osate.workspace.names.standard.TimingProperties;

/**
 * Action performs a binpacking on all the threads in a given system.  Tries
 * to map them to the processors based on their allowed processor bindings
 * and not_collocated property values.
 * 
 * <p>This used to extend AaxlModifyAction, but (1) that class seems to be
 * broken right now, and (2) we don't want to automatically save the resource
 * because we are going to change it using an command via the core editor.
 */
public class Binpack extends AbstractInstanceOrDeclarativeModelReadOnlyAction {
	private static final int IMMEDIATE_PARTITION = 0;
	private static final int DEFER_EXEC_TIME = 1;
	private static final int DEFER_BANDWIDTH = 2;

	private static final int MAX_MULTIPLIER = 10;
	
	private BinpackProperties properties;
	private int partitionChoice;
	
	
	
	protected void initPropertyReferences() {
		Property period = lookupPropertyDefinition(TimingProperties.PERIOD);
		Property deadline = lookupPropertyDefinition(TimingProperties.DEADLINE);
		Property computeExecutionTime = lookupPropertyDefinition(TimingProperties.COMPUTE_EXECUTION_TIME);
		Property schedulingProtocol = lookupPropertyDefinition(DeploymentProperties.SCHEDULING_PROTOCOL);
		Property notCollocated = lookupPropertyDefinition(DeploymentProperties.NOT_COLLOCATED);
		Property actualProcessorBinding = lookupPropertyDefinition(DeploymentProperties.ACTUAL_PROCESSOR_BINDING);
		Property allowedProcessorBinding = lookupPropertyDefinition(DeploymentProperties.ALLOWED_PROCESSOR_BINDING);
		Property allowedProcessorBindingClass = lookupPropertyDefinition(DeploymentProperties.ALLOWED_PROCESSOR_BINDING_CLASS);
		Property transmissionTime = lookupPropertyDefinition(CommunicationProperties.TRANSMISSION_TIME);
		
		UnitLiteral second = lookupUnitLiteral(AadlProject.TIME_UNITS, AadlProject.SEC_LITERAL);
		UnitLiteral nanoSecond = lookupUnitLiteral(AadlProject.TIME_UNITS, AadlProject.NS_LITERAL);
				
		Property referenceProcessor = lookupPropertyDefinition(SEI._NAME, SEI.REFERENCE_PROCESSOR);
		PropertyConstant referenceCycleTime = lookupPropertyConstant(SEI._NAME, SEI.REFERENCE_CYCLE_TIME);
		Property cycleTime = lookupPropertyDefinition(SEI._NAME, SEI.CYCLE_TIME);

		Property size = OsateResourceManager.findProperty(MemoryProperties.SOURCE_DATA_SIZE);
		UnitLiteral bits = lookupUnitLiteral(AadlProject.SIZE_UNITS, AadlProject.BITS_LITERAL);

		properties = new BinpackProperties(period, deadline, computeExecutionTime, schedulingProtocol, notCollocated, actualProcessorBinding,
				allowedProcessorBinding, allowedProcessorBindingClass, second, nanoSecond, referenceProcessor, referenceCycleTime, 
				cycleTime, transmissionTime, size, bits);
		
	}
	
	protected Bundle getBundle() {
		return ResourcemanagementPlugin.getDefault().getBundle();
	}
	
	protected String getMarkerType() {
		return "edu.cmu.sei.aadl.resourcemanagement.BinpackObjectMarker";
	}
	
	protected String getActionName() {
		return "Bind threads to processors";
	}

	// Don't allow analysis in all modes.
	protected boolean analyzeInSingleModeOnly() {
		return true;
	}
	
	protected boolean initializeAnalysis() {
		// Select the bin packing strategy
		partitionChoice = edu.cmu.sei.osate.ui.dialogs.Dialog.askQuestion(
				"Choose partitioning algorithm",
                "This bin packing algorithm groups threads that "+
                "communicate with each other in groups and try to fit them "+
                "together. If it is not possible it will partition the groups.\n\n"+
                "Two main partitioning strategies exist: \n"+
                "(1) Partition group as soon as they are discovered not to fit.\n"+
                "(2) Put them aside and continue packing all groups that fit and after "+
                "partition those that did not fit. Additionally it can select the "+
                "group to partition based on the amount of data being communicated or "+
                "the amount of computing cycles required by the group.",
                new String[] {
						"Immediate Partition of Groups",
						"Defer Partition of Groups Based on Exec. Time",
						"Defer Partition of Groups Based on Comm. Bandwidth" },
				IMMEDIATE_PARTITION);
		return partitionChoice != -1;
	}
	
	@Override
	protected void analyzeDeclarativeModel(IProgressMonitor monitor, AnalysisErrorReporterManager errManager, Element declarativeObject) {
		edu.cmu.sei.osate.ui.dialogs.Dialog.showError(
				"Binding Error",
				"Can only SW/HW bind (binpack) system instances");
	}

	protected void analyzeInstanceModel(final IProgressMonitor monitor,
			final AnalysisErrorReporterManager errManager,
			final SystemInstance root, final SystemOperationMode som) {
		try {
			monitor.beginTask("Binding threads to processors in " + root.getName(),
					IProgressMonitor.UNKNOWN);
			
			/* Find and report all processor instances that don't have a
			 * cycle time specified.
			 */ 
			final EList incompleteprocessors = new ForAllElement() {
				protected boolean suchThat(Element obj){
					try {
						properties.getCycleTimePropertyValue((ComponentInstance)obj);
						return false;
					} catch (PropertyNotPresentException e) {
						return true;
					}
				}
			}.processPreOrderComponentInstance(root,ComponentCategory.PROCESSOR);
			for (final Iterator i = incompleteprocessors.iterator(); i.hasNext();) {
				final Element o = (Element) i.next();
				warning(o, "Processor (or SEI property set) is missing cycle time property. Using default of 1 ns");
			}
			
			/* Verify that all the busses have a transmission time
			 */
			final ForAllElement addBuses = new ForAllElement(errManager) {
				public void process(Element obj){				
					ComponentInstance bi = (ComponentInstance) obj;
					try {
						final List transTime = properties.getTransmissionTimePropertyValue(bi);
						if (transTime.size() < 2) {
							warning(obj, "Bus has badly formed Transmission Time property (length of list is < 2), using default multiplier of " + AADLBus.DEFAULT_TRANSMISSION_TIME);
						}
					} catch (PropertyNotPresentException e) {
						warning(obj, "Bus is missing Transmission Time property, using default multiplier of " + AADLBus.DEFAULT_TRANSMISSION_TIME);
					}
				}
			};
			addBuses.processPreOrderComponentInstance(root,ComponentCategory.BUS);				
			
			/* Find and report all thread and device instances that don't have a 
			 * period specified.
			 */
			EList incompletethreads = new ForAllElement(){
				protected boolean suchThat(Element obj){
					final ComponentCategory cat = ((ComponentInstance) obj).getCategory();
					if (cat == ComponentCategory.THREAD|| cat == ComponentCategory.DEVICE) {
						try {
							properties.getPeriodPropertyValue((ComponentInstance) obj);
							return false;
						} catch (PropertyNotPresentException e) {
							return true;
						}							
					} else {
						return false;
					}
				}
			}.processPreOrderComponentInstance(root);
			for (final Iterator i = incompletethreads.iterator(); i.hasNext();) {
				final Element o = (Element) i.next();
				warning(o, "Thread or device is missing period property. Using default of 1 ns");
			} 
			
			/* Find and report all thread instances that don't have a 
			 * compute execution time specified.
			 */
			incompletethreads = new ForAllElement(){
				protected boolean suchThat(Element obj){
					try {
						properties.getComputeExecutionTime((ComponentInstance)obj);
						return false;
					} catch (PropertyNotPresentException e) {
						return true;
					}
				}
			}.processPreOrderComponentInstance(root,ComponentCategory.THREAD);
			for (final Iterator i = incompletethreads.iterator(); i.hasNext();) {
				final Element o = (Element) i.next();
				warning(o, "Thread is missing compute execution time property. Using default of 0 ns");
			} 

			/* Find if all the port connections have data size
			 */
			final ForAllElement addThreadConnections = new ForAllElement(errManager){
				public void process(Element obj){
					if (obj instanceof ConnectionInstance){
						final ConnectionInstance connInst = (ConnectionInstance) obj;
						if (connInst.getKind() == ConnectionKind.PORT_CONNECTION && 
								connInst.getSource() instanceof ConnectionInstanceEnd){
							final FeatureInstance src = (FeatureInstance) connInst.getSource();

							Feature srcAP =  src.getFeature();
							Classifier cl = srcAP.getClassifier();
							if (cl instanceof DataClassifier){
								DataClassifier srcDC = (DataClassifier) cl;

								try {
									properties.getSize(srcDC);
								} catch(PropertyNotPresentException e) {
									warning(obj,"Data size of port connection not specified");
								}
							}
						}
					}
				}
			};
			addThreadConnections.processPreOrderAll(root); 

			/* The partitionChoice is set in initializeANalysis() */
			NoExpansionExpansor expansor = new NoExpansionExpansor(); 
			LowLevelBinPacker packer = null;
			if (partitionChoice == IMMEDIATE_PARTITION) {
				packer = new BFCPBinPacker(expansor);
			} else if (partitionChoice == DEFER_EXEC_TIME) {
				packer = new DFCPBinPacker(expansor);
			} else if (partitionChoice == DEFER_BANDWIDTH) {
				packer = new DFBPBinPacker(expansor);
			}		
			
			/* Try to bin pack the system.  If it doesn't work, we 
			 * try increasing the speed of the processor by a multiplier.
			 * If it still doesn't work by the time the multiplier hits
			 * 10x, then give up.
			 */
			int processorMultiplier = 0;
			AssignmentResult result;
			do {
				processorMultiplier += 1;					
				result = binPackSystem(root, processorMultiplier, expansor, packer, errManager);
			} while (!result.success && processorMultiplier < MAX_MULTIPLIER);
			
			if (result.success) {
				showResults(som, root, result, processorMultiplier);
			} else {
				showNoResults(som, processorMultiplier);
			}
		} catch (InvalidModelException e) {
			error(e.getElement(), e.getMessage());
		}
	}
		
	protected EList getExecutionUnits(SystemInstance root){
		return new ForAllElement().processPreOrderComponentInstance(root,
				ComponentCategory.THREAD);
	}

	
	protected AssignmentResult binPackSystem(
			final SystemInstance root, final int processorMultiplier,
			Expansor expansor, LowLevelBinPacker packer,
			final AnalysisErrorReporterManager errManager) {
		/* Map from AADL ComponentInstances representing threads to
		 * the bin packing SoftwareNode that models the thread.
		 */ 
		final Map threadToSoftwareNode = new HashMap();
		/* Set of thread components.  This is is the keySet of
		 * threadToSoftwareNode.
		 */
		final Set threads = threadToSoftwareNode.keySet();
		/* Map from AADL ComponentInstances representing threads to
		 * the set of AADL ComponentInstances that cannot be collocated
		 * with it.
		 */
		final Map notCollocated = new HashMap();
		
		/* Map from AADL ComponentInstance representing processors to
		 * the bin packing Processor that models them.
		 */
		final Map procToHardware = new HashMap();
		
		/* Map from AADL BusInstance representing Buses to
		 * The bin packing Link that models them.
		 */
		final Map busToHardware = new HashMap();
			
		
		/* One site to rule them all!  We don't care about the site
		 * architecture, so just create one site to hold everything.
		 * We aren't worried about power or space issues either, so
		 * we just set them to 100.0 because those are nice values.
		 * The site accepts AADL processors. 
		 */
		final SiteArchitecture siteArchitecture = new SiteArchitecture();
		AADLProcessor ap = AADLProcessor.PROTOTYPE;
		final Site theSite = new Site(
				100.0, 100.0, new SiteGuest[] { ap });
		siteArchitecture.addSite(theSite);

		/* The hardware is fixed based on the AADL specification, so we 
		 * use the NoExpansionExpansor to keep the hardware from being
		 * generated for us.
		 */
		
		expansor.setSiteArchitecture(siteArchitecture);

		/* Populate the problem space based on the AADL specification.  First
		 * we walk the instance model and add all the processors.  Then we 
		 * walk the instance model again to add all the threads.
		 */
		final OutDegreeAssignmentProblem problem = new OutDegreeAssignmentProblem(
				new OutDegreeComparator(), new BandwidthComparator(),
				new CapacityComparator());

		// Add procs
		final ForAllElement addProcessors = new ForAllElement(errManager) {
			public void process(Element obj) {			
				ComponentInstance ci = (ComponentInstance) obj;
				final Processor proc = AADLProcessor.createInstance(ci, processorMultiplier, properties);
				if (proc != null) {
					siteArchitecture.addSiteGuest(proc, theSite);
					problem.hardwareGraph.add(proc);
					// add reverse mapping
					procToHardware.put(ci, proc);
				}
			}
		};
		addProcessors.processPreOrderComponentInstance(
				root, ComponentCategory.PROCESSOR);
		
		/*   Get all the links 
		 */
		
		
		final ForAllElement addBuses = new ForAllElement(errManager) {
			public void process(Element obj){				
				ComponentInstance bi = (ComponentInstance) obj;
				
				final AADLBus bus = AADLBus.createInstance(bi,properties);
				busToHardware.put(bi,bus);
			}
		};
		
		addBuses.processPreOrderComponentInstance(root,ComponentCategory.BUS);
		
		/* create the links between processors and busses
		 * (i.e., process connections)
		 */
		for (final Iterator i = root.getConnectionInstances().iterator(); i.hasNext();) {
			final ConnectionInstance connInst = (ConnectionInstance) i.next();
			if (connInst.getKind() == ConnectionKind.ACCESS_CONNECTION){
				InstanceObject src = connInst.getSource();
				InstanceObject dst = connInst.getDestination();

				AADLBus bus=null;
				AADLProcessor processor=null;

				// swap if i got them in the opposite order
				if (src instanceof FeatureInstance){
					InstanceObject tmp = dst;
					dst = src;
					src = tmp;
				}
				
				bus = (AADLBus) busToHardware.get(src);
				FeatureInstance fi = (FeatureInstance) dst;

				processor = (AADLProcessor) procToHardware.get(fi.getContainingComponentInstance());

				if (bus != null && processor != null){
					bus.add(processor);
					processor.attachToLink(bus);
				}
			}
		}

		// Now add all the links so the connectivity matrix in the problem is
		// updated correctly
		
		for (Iterator iBus = busToHardware.values().iterator(); iBus.hasNext();){
			AADLBus bus = (AADLBus) iBus.next();
			problem.addLink(bus);
			siteArchitecture.addSiteGuest(bus,theSite);			
		}
		
		
		// Add threads
		final ForAllElement addThreads = new ForAllElement(errManager) {
			public void process(Element obj) {
				final ComponentInstance ci = (ComponentInstance) obj;
				final SoftwareNode thread = AADLThread.createInstance(ci, properties);
				problem.softwareGraph.add(thread);
				
				// add reverse mapping
				threadToSoftwareNode.put(ci, thread);

				// Process NOT_COLLOCATED property. 
				List disjunctFrom;
				try
				{
					disjunctFrom = properties.getNotCollocated(ci);
				}
				catch (PropertyNotPresentException e)
				{
					//Ignore this situation and move on.
					disjunctFrom = Collections.EMPTY_LIST;
				}
				final Set disjunctSet = new HashSet();
				for (final Iterator i = disjunctFrom.iterator(); i.hasNext();) {
					/* Add all the instances rooted at the named instance.
					 * For example, the thread may be declared to be disjunct 
					 * from another process, so we really want to be disjunct
					 * from the other threads contained in that process.
					 */
					final InstanceReferenceValue rv = (InstanceReferenceValue) i.next();
					final ComponentInstance refCI = (ComponentInstance) rv.getReferencedInstanceObject();
					disjunctSet.addAll(refCI.getAllComponentInstances());
				}
				if (!disjunctSet.isEmpty()) {
					notCollocated.put(ci, disjunctSet);
				}
			}
		};
		addThreads.processPreOrderComponentInstance(
				root, ComponentCategory.THREAD);
		
		// Add thread connections (Messages)
		for (final Iterator i = root.getConnectionInstances().iterator(); i.hasNext();) {
			final ConnectionInstance connInst = (ConnectionInstance) i.next();
			if (connInst.getKind() == ConnectionKind.PORT_CONNECTION ){
				final ConnectionInstance portConnInst = (ConnectionInstance) connInst;
				if (!(portConnInst.getSource() instanceof FeatureInstance && portConnInst.getDestination() instanceof FeatureInstance))
						continue;
				final FeatureInstance src = (FeatureInstance) portConnInst.getSource();
				final FeatureInstance dst = (FeatureInstance) portConnInst.getDestination();
				
				final ComponentInstance ci = src.getContainingComponentInstance();
				AADLThread t1 = (AADLThread) threadToSoftwareNode.get(ci);
				AADLThread t2 = (AADLThread) threadToSoftwareNode.get(dst.getContainingComponentInstance());
				if (t1 != null && t2 != null) {
					Feature srcAP =  src.getFeature();
					// TODO: get the property directly
					Classifier cl = srcAP.getClassifier();
					if (cl instanceof DataClassifier){
						DataClassifier srcDC  = (DataClassifier) cl;
						double dataSize=0.0;
						double threadPeriod=0.0;
						try {
							dataSize= properties.getSize(srcDC);
						} catch (Exception e){
							errManager.warning(connInst, "No Data Size for connection");
						}
						try{
							threadPeriod = properties.getPeriodAsNanoSecond(ci);
						} catch (Exception e){
							errManager.warning(connInst, "No Period for connection");
						}
		
						// Now I can create the Message
						
						Message msg = new Message((long) dataSize, (long)threadPeriod, (long)threadPeriod, t1, t2);
						System.out.println(">>>>>>>>>> Adding message ("+Long.toString((long)dataSize)+"/"+
								Long.toString((long)threadPeriod)+") between " + t1.getName() + " and " + t2.getName() + " based on connection " + portConnInst.getName());
						problem.addMessage(msg);						
					} else {
						errManager.warning(connInst, "No Data Classifier for connection");
					}
				}
			}
		}
		
		
		// Add collocation constraints
		for (final Iterator constrained = notCollocated.keySet().iterator(); constrained.hasNext();) {
			final ComponentInstance ci = (ComponentInstance) constrained.next();
			final SoftwareNode sn = (SoftwareNode) threadToSoftwareNode.get(ci);
			final Set disjunctFrom = (Set) notCollocated.get(ci);
			for(final Iterator dfIter = disjunctFrom.iterator(); dfIter.hasNext();) {
				/* Items in the disjunctFrom set do not have to be thread 
				 * instances because of the way we add items to it (see above).
				 * We are only interested in the thread instances here, in 
				 * particular because we only create SoftwareNodes for the
				 * thread instances, and we don't want to get null return 
				 * values from the threadToSoftwareNode map.
				 */ 
				final ComponentInstance ci2 = (ComponentInstance) dfIter.next();
				if (ci2.getCategory() == ComponentCategory.THREAD) {
					final SoftwareNode sn2 = (SoftwareNode) threadToSoftwareNode.get(ci2);
					final SoftwareNode[] disjunction = new SoftwareNode[] { sn, sn2 };
					problem.addConstraint(new Disjoint(disjunction));
				}
			}			
		}
		
		/* Add Allowed_Processor_Binding and
		 * Allowed_Processor_Binding_Class constraints
		 */
		for (final Iterator i = threads.iterator(); i.hasNext();) {
			final ComponentInstance thr = (ComponentInstance) i.next();
			final SoftwareNode thrSN = (SoftwareNode) threadToSoftwareNode.get(thr);
			final Set allowed = getAllowedProcessorBindings(thr);
			if (allowed.size() >0){
				final Object[] allowedProcs = new Object[allowed.size()];
				int idx = 0;
				for (Iterator j = allowed.iterator(); j.hasNext(); idx++) {
					final ComponentInstance proc = (ComponentInstance) j.next();
					allowedProcs[idx] = procToHardware.get(proc);
				}
				problem.addConstraint(
						new SetConstraint(new SoftwareNode[] { thrSN }, allowedProcs));
			}
		}
		
				
		// Try to bin pack
		final NFCHoBinPacker highPacker = new NFCHoBinPacker(packer);
		final boolean res = highPacker.solve(problem);
		return new AssignmentResult(problem, res);
	}
	
	private abstract static class ShowDialog implements Runnable {
		public volatile int result;
	}
	
	String getBindingText(final Map threadsToProc){
		String bindings ="";		
		for (Iterator iter = threadsToProc.keySet().iterator(); iter.hasNext(); ) {
			final ComponentInstance thread = (ComponentInstance) iter.next();
			final ComponentInstance proc = (ComponentInstance) threadsToProc.get(thread);
			bindings += "Actual_Processor_Binding => reference ("+proc.getInstanceObjectPath()+") applies to "+thread.getInstanceObjectPath()+";\n";
		}
		
		return bindings;
	}
	
	public void showResults(final SystemOperationMode som, final SystemInstance root,
			final AssignmentResult result, final int processorMultiplier) {
		final Map threadsToProc = getThreadBindings(result.problem.hardwareGraph);
//		final Properties props = constructDeclarativeBindings(threadsToProc);
//		final AadlUnparser unparser = new AadlUnparser();
		final String propText = getBindingText(threadsToProc); //unparser.doUnparse(props);
		
		boolean done = false;
		while (!done) {
			final Dialog d = new PackingSuccessfulDialog(getShell(), som,
					root.getSystemImplementation().getName(), threadsToProc, 
					result.problem.hardwareGraph, propText, processorMultiplier);
			final ShowDialog sd = new ShowDialog() {
				public void run() {
					this.result = d.open();
				}
			};
			Display.getDefault().syncExec(sd);
			
			if (sd.result == PackingSuccessfulDialog.INSTANCE_ID) {
				setInstanceModelBindings(root, threadsToProc);
			} 
			// XXX: Don't set properties in the declarative model any more?
//				else if (button == PackingSuccessfulDialog.DECLARATIVE_ID) {
//					setDeclarativeBindings(root, threadsToProc);
//				} 
			else {
				done = true;
			}
		}
	}
	
	public void showNoResults(
			final SystemOperationMode som, final int processorMultiplier){
		edu.cmu.sei.osate.ui.dialogs.Dialog.showError(
				"Application Binding Results",
				"In system operation mode " + som.getName() + 
				"the application system is not schedulable with processor speed " +
				processorMultiplier + "X");	
	}

	private Map getThreadBindings(final Set hardware) {
		final Map threadsToProc = new HashMap();
		for (Iterator iter = hardware.iterator(); iter.hasNext();) {
			HardwareNode n = (HardwareNode) iter.next();
			for (Iterator taskSet = n.getTaskSet().iterator(); taskSet.hasNext();) {
				SoftwareNode m = (SoftwareNode) taskSet.next();
				if (m instanceof CompositeSoftNode) {
					final Set set = ((CompositeSoftNode) m).getBasicComponents();
					for (Iterator software = set.iterator(); software.hasNext(); ) {
						final SoftwareNode sn = (SoftwareNode) software.next();
						threadsToProc.put(sn.getSemanticObject(), n.getSemanticObject());
					}
				} else {
					if (!(m instanceof Message)) {
						threadsToProc.put(m.getSemanticObject(), n.getSemanticObject());
					}
				}
			}
		}
		return threadsToProc;
	}
	
	private void setInstanceModelBindings(
			final SystemInstance root, final Map threadsToProc) {
		final EditingDomain editingDomain =
			AdapterFactoryEditingDomain.getEditingDomainFor(root);
		if (editingDomain != null) {
			final CommandStack cmdStack = editingDomain.getCommandStack();
			final Command setBindings = new SetInstanceModelBindings(threadsToProc, properties);
			cmdStack.execute(setBindings);
		} else {
			internalError("Couldn't get editing domain");
		}

		/* XXX: Keep this around for now.  May want to keep the ability to
		 * modify the model directly so that we can use this action without
		 * using an editor. 
		 */
//		for (Iterator iter = threadsToProc.keySet().iterator(); iter.hasNext(); ) {
//			final ComponentInstance thread = (ComponentInstance) iter.next();
//			final ComponentInstance proc = (ComponentInstance) threadsToProc.get(thread);
//			final InstanceReferenceValue val = InstanceFactory.eINSTANCE.createInstanceReferenceValue();
//			val.setReferencedInstanceObject(proc);
//			thread.setPropertyValue(Binpack.actualProcessorBinding, val);
//		}
	}
	
	/* We don't want to do this anymore?  Keep the code around for the moment
	 * though.
	 */
//	/** @deprecated */
//	private void setDeclarativeBindings(
//			final SystemInstance root, final Map threadsToProc) {
//		final SystemImpl system = root.getSystemImpl();
//		for (Iterator iter = threadsToProc.keySet().iterator(); iter.hasNext(); ) {
//			final ComponentInstance thread = (ComponentInstance) iter.next();
//			final ComponentInstance proc = (ComponentInstance) threadsToProc.get(thread);
//			final ReferenceValue procRef = proc.getReferenceTo();
//			final List threadPath = thread.getReferencePathTo();
//			system.setContainedPropertyValue(
//					Binpack.actualProcessorBinding, 
//					threadPath, procRef);
//		}
//	}

//	private Properties constructDeclarativeBindings(final Map threadsToProc) {
//		final Properties props = PropertyFactory.eINSTANCE.createProperties();
//		for (Iterator iter = threadsToProc.keySet().iterator(); iter.hasNext(); ) {
//			final ComponentInstance thread = (ComponentInstance) iter.next();
//			final ComponentInstance proc = (ComponentInstance) threadsToProc.get(thread);
//			final ReferenceValue procRef = proc.getReferenceTo();
//			final List threadPath = thread.getReferencePathTo();
//			
//			// create the Property Association
//			final PropertyAssociation pa = 
//				PropertyFactory.eINSTANCE.createPropertyAssociation();
//			properties.setActualProcessorBindingDefinitionToAssociation(pa);
//			for (final Iterator i = threadPath.iterator(); i.hasNext(); ) {
//				pa.addAppliesTo((PropertyHolder) i.next());
//			}
//			pa.addPropertyValue(procRef);
//			props.addPropertyAssociation(pa);
//		}
//		return props;
//	}

	/**
	 * Get the processor components that a given thread is allowed to be bound to
	 * based on the thread's ALLOWED_PROCESSOR_BINDING and
	 * ALLOWED_PROCESSOR_BINDING_CLASS property values. The processors are
	 * search for in the same system instance that the given thread component is
	 * a part of.
	 *
	 * @param thread
	 *                 The thread.
	 * @return An unmodifiable set of processor ComponentInstances.
	 * @exception IllegalArgumentException
	 *                      Thrown if the category of the component instance
	 *                      referenced by <code>thread</code> is not
	 *                      {@link ComponentCategory#THREAD_LITERAL}.
	 */
	public Set getAllowedProcessorBindings(final ComponentInstance thread) {
		if (thread.getCategory() != ComponentCategory.THREAD) {
			throw new IllegalArgumentException("Component \""
					+ thread.getName() + "\" is not a thread.");
		}
		List allowedBindingsVals;
		try
		{
			allowedBindingsVals = properties.getAllowedProcessorBinding(thread);
		}
		catch (PropertyNotPresentException e)
		{
			//Ignore this situation and move on.
			allowedBindingsVals = Collections.EMPTY_LIST;
		}
		List allowedClassVals;
		try
		{
			allowedClassVals = properties.getAllowedProcessorBindingClass(thread);
		}
		catch (PropertyNotPresentException e)
		{
			//Ignore this situation and move on.
			allowedClassVals = Collections.EMPTY_LIST;
		}
		final Set searchRoots = new HashSet();
		if (allowedBindingsVals.isEmpty()) {
			searchRoots.add(thread.getSystemInstance());
		} else {
			for (final Iterator i = allowedBindingsVals.iterator(); i.hasNext();) {
				final InstanceReferenceValue rv = (InstanceReferenceValue) i
						.next();
				searchRoots.add(rv.getReferencedInstanceObject());
			}
		}
		final Set allowedSystemClassifiers = new HashSet();
		final Set allowedProcClassifiers = new HashSet();
		for (final Iterator i = allowedClassVals.iterator(); i.hasNext();) {
			final ClassifierValue cv = (ClassifierValue) i.next();
			final ComponentClassifier cc = (ComponentClassifier) cv.getClassifier();
			if (cc instanceof ProcessorClassifier){ //ComponentCategory.PROCESSOR) {
				allowedProcClassifiers.add(cc);
			} else if (cc instanceof SystemClassifier){//cv.getValue() == ComponentCategory.SYSTEM_LITERAL) {
				allowedSystemClassifiers.add(cc);
			} else {
				internalError("Ill-formed allowed_processor_binding_class value: got a non-system non-processor component classifier");
			}
		}
		final Set allowedProcs = new HashSet();
		for (final Iterator i = searchRoots.iterator(); i.hasNext();) {
			final ComponentInstance ci = (ComponentInstance) i.next();
			getAllowedProcessorBindings(ci, allowedProcs,
					allowedProcClassifiers, allowedSystemClassifiers);
		}
		return Collections.unmodifiableSet(allowedProcs);
	}

	/**
	 * Search the instance model structure rooted at the given component
	 * instance and add allowed processors to the given set of processors. A
	 * processor is added if it's component classifier matches the given set of
	 * processor classifiers (where the empty set means all processors). A
	 * system component instance is explored if it's component classifier
	 * matches the given set of system classifiers (where the empty set means
	 * all systems). This method does nothing if given a non-system,
	 * non-processor component instance.
	 *
	 * @param searchRoot
	 *                 The component instance to consider.
	 * @param allowedProcs
	 *                 The set of processors. This set is added to by this method.
	 * @param allowedProcClassifiers
	 *                 The set of component classifiers describing allowable
	 *                 processors.
	 * @param allowedSystemClassifiers
	 *                 The of component classifiers describing allowable systems.
	 */
	private void getAllowedProcessorBindings(
			final ComponentInstance searchRoot, final Set allowedProcs,
			final Set allowedProcClassifiers, final Set allowedSystemClassifiers) {
		if (searchRoot.getCategory() == ComponentCategory.PROCESSOR) {
			/* If it's a processor, only add it if the classifier is okay. */
			if (testClassifier(searchRoot, allowedProcClassifiers)) {
				allowedProcs.add(searchRoot);
			}
		} else if (searchRoot.getCategory() == ComponentCategory.SYSTEM) {
			/* If it's a system then we look inside it for processors and other
			 * systems. But only look if the classifiers match.
			 */
			if (testClassifier(searchRoot, allowedSystemClassifiers)) {
				for (final Iterator i = searchRoot.getComponentInstances()
						.iterator(); i.hasNext();) {
					getAllowedProcessorBindings((ComponentInstance) i.next(),
							allowedProcs, allowedProcClassifiers,
							allowedSystemClassifiers);
				}
			}
		} else {
			// Do nothing, not interested in non-processor, non-system instance
		}
	}
	
	/**
	 * Test a component against a set of classifiers.
	 * 
	 * @return Whether the component's classifier type is a descendent of any of
	 *         the given component classifiers. <em>Returns <code>true</code>
	 *         if the given set is empty!</em>
	 */
	public boolean testClassifier(ComponentInstance ci, final Set classifiers) {
		if (classifiers.isEmpty()) {
			return true;
		}
		boolean match = false;
		ComponentClassifier cicc = InstanceUtil.getComponentClassifier(ci, 0, null);
		if (cicc == null) {
			return true;
		}
		for (final Iterator i = classifiers.iterator(); i.hasNext() && !match;) {
			final ComponentClassifier cc = (ComponentClassifier) i.next();
			match = cicc.isDescendentOf(cc);
		}
		return match;
	}
}
