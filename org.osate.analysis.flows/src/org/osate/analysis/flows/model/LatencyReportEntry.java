package org.osate.analysis.flows.model;

import java.util.ArrayList;
import java.util.List;

import org.osate.aadl2.instance.ConnectionInstance;
import org.osate.aadl2.instance.EndToEndFlowInstance;
import org.osate.aadl2.instance.SystemInstance;
import org.osate.aadl2.instance.SystemOperationMode;
import org.osate.aadl2.modelsupport.errorreporting.AnalysisErrorReporterManager;
import org.osate.aadl2.util.Aadl2Util;
import org.osate.analysis.flows.FlowLatencyUtil;
import org.osate.analysis.flows.model.LatencyContributor.LatencyContributorMethod;
import org.osate.analysis.flows.preferences.Values;
import org.osate.analysis.flows.reporting.model.Line;
import org.osate.analysis.flows.reporting.model.ReportSeverity;
import org.osate.analysis.flows.reporting.model.ReportedCell;
import org.osate.analysis.flows.reporting.model.Section;
import org.osate.xtext.aadl2.properties.util.GetProperties;

/*
 * A report entry corresponds to the entry within the report.
 * It just contains all the contributors for a flow latency.
 * We should have one report entry for each end to end flow
 */

public class LatencyReportEntry {

	List<LatencyContributor> contributors;
	EndToEndFlowInstance relatedEndToEndFlow;
	List<ReportedCell> issues;
	// lastSampled may be a task, partition if no tasks inside the partition, sampling bus, or a sampling device/system
	LatencyContributor lastSampled = null;
	SystemOperationMode som = null;
	double expectedMaxLatency = 0;
	double expectedMinLatency = 0;

	public LatencyReportEntry(EndToEndFlowInstance etef, SystemOperationMode som) {
		this.contributors = new ArrayList<LatencyContributor>();
		this.relatedEndToEndFlow = etef;
		this.som = som;

		expectedMaxLatency = GetProperties.getMaximumLatencyinMilliSec(this.relatedEndToEndFlow);
		expectedMinLatency = GetProperties.getMinimumLatencyinMilliSec(this.relatedEndToEndFlow);

	}

	public boolean doSynchronous() {
		return Values.doSynchronousSystem();
	}

	public SystemOperationMode getSOM() {
		return this.som;
	}

	public void addContributor(LatencyContributor lc) {
		this.contributors.add(lc);
	}

	public void setLastSampled(LatencyContributor lc) {
		lastSampled = lc;
	}

	public boolean wasSampled() {
		return lastSampled != null;
	}

	public double getMinimumCumLatency(LatencyContributor current) {
		double result = 0;
		if (lastSampled == null) {
			return 0;
		}
		int idx = this.contributors.indexOf(lastSampled);
		int me = this.contributors.indexOf(current);
		if (idx < 0 || me < 0) {
			return 0;
		}
		for (int i = idx + 1; i < me; i++) {
			LatencyContributor lc = this.contributors.get(i);
			result = result + lc.getTotalMinimum();
		}
		return result;
	}

	/** 
	 * get cumulative latency since last sampled component. Ignore any sampling contributors.
	 * do not include current.
	 * @param current LatencyContributor
	 * @param doMax boolean true do max, false do min
	 * @return sum of latencies
	 */
	public double getCumLatency(LatencyContributor current, boolean doMax) {
		if (doMax)
			return getMaximumCumLatency(current);
		else
			return getMinimumCumLatency(current);
	}

	public double getMaximumCumLatency(LatencyContributor current) {
		double result = 0;
		if (lastSampled == null) {
			return 0;
		}
		int idx = this.contributors.indexOf(lastSampled);
		int me = this.contributors.indexOf(current);
		if (idx < 0 || me < 0)
			return 0;
		for (int i = idx + 1; i < me; i++) {
			LatencyContributor lc = this.contributors.get(i);
			result = result + lc.getTotalMaximum();
		}
		return result;
	}

	public List<LatencyContributor> getContributors() {
		return this.contributors;
	}

	public LatencyContributor getPrevious(LatencyContributor lc) {
		int idx = contributors.indexOf(lc);
		return idx == 0 ? null : contributors.get(idx - 1);
	}

	public boolean isConnection(LatencyContributor lc) {
		return (lc.getContributor() instanceof ConnectionInstance);
	}

	public boolean isPreviousConnectionSynchronous(LatencyContributor lc) {
		int idx = contributors.indexOf(lc);
		for (int i = idx - 1; i >= 0; i--) {
			LatencyContributor plc = contributors.get(i);
			if (plc.getContributor() instanceof ConnectionInstance) {
				return plc.isSynchronous();
			}
		}
		return false;
	}

	public boolean isPreviousConnectionSyncUnknown(LatencyContributor lc) {
		int idx = contributors.indexOf(lc);
		for (int i = idx - 1; i >= 0; i--) {
			LatencyContributor plc = contributors.get(i);
			if (plc.getContributor() instanceof ConnectionInstance) {
				return plc.isSyncUnknown();
			}
		}
		return false;
	}

	public double getLastPartitionOffset(LatencyContributor lc) {
		int idx = contributors.indexOf(lc);
		for (int i = idx - 1; i >= 0; i--) {
			LatencyContributor plc = contributors.get(i);
			if (plc.isPartitionOffset()) {
				return plc.getPartitionOffset();
			}
		}
		return -1;
	}

	public double getActualLatency(boolean doMaximum) {
		double res = 0.0;
		for (LatencyContributor lc : contributors) {
			// do some consistency checking
			if (lc.getImmediateDeadline() > 0.0) {
				// No sampling. we are the last of an immediate connection sequence.
				double cum = this.getCumLatency(lc, doMaximum) + lc.getTotal(doMaximum);
				if (cum > lc.getImmediateDeadline()) {
					lc.reportError(doMaximum,
							"immediate latency sequence exceeds deadline " + lc.getImmediateDeadline() + "ms");
				}
			}

			if (lc.getLatencyContributorMethod(doMaximum).equals(LatencyContributorMethod.FIRST_SAMPLED)) {
				// skip the first sampled if it is the first element in the contributor list
				// and remember initial sample
				// TODO: if we sample external events by sensor this might have to be added as sampling latency
				lastSampled = lc;
			} else if (lc.getLatencyContributorMethod(doMaximum).equals(LatencyContributorMethod.SAMPLED)) {
				// lets deal with the sampling case
				LatencyContributor last = getPrevious(lc);
				if (last != null && last.isPartition()) {
					// if it is a task and is preceded by a partition then accommodate for additional sampling latency
					if (lc.getSamplingPeriod() > last.getSamplingPeriod()) {
						double diff = lc.getSamplingPeriod() - last.getSamplingPeriod();
						res = res + diff;
						lc.setActualValue(diff, doMaximum);
						lc.reportSubtotal(res, doMaximum);
						lc.reportInfoOnce(doMaximum, "Sampling period " + lc.getSamplingPeriod() + "ms");
					} else if ( // XXX added check for > 0
					lc.getSamplingPeriod() > 0 && lc.getSamplingPeriod() < last.getSamplingPeriod()) {
						lc.reportWarningOnce(doMaximum, "Task period smaller than partition period");
					} else {
						lc.reportInfoOnce(doMaximum, "Latency accounted for in partition latency");
					}
				} else if (((doSynchronous() && isPreviousConnectionSyncUnknown(lc)) || isPreviousConnectionSynchronous(lc))
						&& wasSampled()) {
					// there was a previous sampling component. We can to the roundup game.
					double diff = FlowLatencyUtil.roundUpDiff(getCumLatency(lc, doMaximum), lc.getSamplingPeriod());
					res = res + diff;
					lc.setActualValue(diff, doMaximum);
					lc.reportSubtotal(res, doMaximum);
					lc.reportInfo(doMaximum, "Round up to sampling period " + lc.getSamplingPeriod() + "ms");
					if (doSynchronous() && isPreviousConnectionSyncUnknown(lc)) {
						lc.reportInfoOnce(doMaximum, "Assume synchronous communication");
					} else if (isPreviousConnectionSynchronous(lc)) {
						lc.reportInfoOnce(doMaximum, "Synchronous communication on same platform");
					} else {
						lc.reportInfoOnce(doMaximum, "Assume synchronous communication");
					}
				} else {
					if (doMaximum) {
						res = res + lc.getSamplingPeriod();
						lc.setActualValue(lc.getSamplingPeriod(), doMaximum);
						lc.reportSubtotal(res, doMaximum);
						lc.reportInfo("Best case 0 ms worst case " + lc.getSamplingPeriod()
								+ "ms (period) sampling delay");
					} else {
//						TODO: may want to enable				lc.reportInfo(doMaximum, "Best case: no sampling delay");
					}
				}

				lc.reportSubtotal(res, doMaximum);
				lastSampled = lc;
			} else if (lc.getLatencyContributorMethod(doMaximum).equals(LatencyContributorMethod.DELAYED)) {
				// if it is a task and is preceded by a partition then accommodate for additional sampling latency
				LatencyContributor last = getPrevious(lc);
				if (last.isPartition()) {
					if (lc.getSamplingPeriod() > last.getSamplingPeriod()) {
						double diff = lc.getSamplingPeriod() - last.getSamplingPeriod();
						res = res + diff;
						lc.setActualValue(diff, doMaximum);
						lc.reportSubtotal(res, doMaximum);
						lc.reportInfo(doMaximum, "Delay to next sampling period " + lc.getSamplingPeriod() + "ms");
					} else if (lc.getSamplingPeriod() < last.getSamplingPeriod()) {
						lc.reportWarning(doMaximum, "Task period smaller than partition period");
					} else {
						lc.reportInfo(doMaximum, "No added latency");
					}
				} else if (((doSynchronous() && isPreviousConnectionSyncUnknown(lc)) || isPreviousConnectionSynchronous(lc))
						&& wasSampled()) {
					// there was a previous sampling component. We can to the roundup game.
					double cumMin = getMinimumCumLatency(lc);
					// for delayed the frame delay for the minimum and the maximum should be the same
					// if both cumulative are within the same frame we are ok
					// if one of them goes into the next frame we give a warning
					double framediff = FlowLatencyUtil.roundUp(getMaximumCumLatency(lc), lc.getSamplingPeriod())
							- FlowLatencyUtil.roundUp(cumMin, lc.getSamplingPeriod());
					if (framediff > 0) {
						lc.reportWarning(doMaximum,
								"Min and max delay for delayed connection differ by a frame or more");
					}
					double diff = FlowLatencyUtil.roundUpDiff(getCumLatency(lc, doMaximum), lc.getSamplingPeriod());
					res = res + diff;
					lc.setActualValue(diff, doMaximum);
					lc.reportSubtotal(res, doMaximum);
					lc.reportInfo(doMaximum, "Sampling period " + lc.getSamplingPeriod() + "ms");
					if (doSynchronous() && isPreviousConnectionSyncUnknown(lc)) {
						lc.reportInfoOnce(doMaximum, "Assume synchronous communication");
					} else if (isPreviousConnectionSynchronous(lc)) {
						lc.reportInfoOnce(doMaximum, "Synchronous communication on same platform");
					} else {
						lc.reportInfoOnce(doMaximum, "Assume synchronous communication");
					}
				} else {
					res = res + lc.getSamplingPeriod();
					lc.setActualValue(lc.getSamplingPeriod(), doMaximum);
					lc.reportSubtotal(res, doMaximum);
					lc.reportInfo(doMaximum, "Sampling period " + lc.getSamplingPeriod() + "ms");
				}
				lastSampled = lc;
			} else if (lc.isPartition()) {
				// ignore if partition is the first entry as it goes along with FIRST_SAMPLED
				// partition boundary has been crossed
				if (contributors.indexOf(lc) > 0) {
					// add partition latency unless the first component
					if (((doSynchronous() && isPreviousConnectionSyncUnknown(lc)) || isPreviousConnectionSynchronous(lc))
							&& wasSampled()) {
						// there was a previous sampling component. We can to the roundup game.
						if (lc.isPartitionFrame()) {
							double diff = FlowLatencyUtil.roundUpDiff(getCumLatency(lc, doMaximum),
									lc.getSamplingPeriod());
							res = res + diff;
							lc.setActualValue(diff, doMaximum);
							lc.reportSubtotal(res, doMaximum);
							lc.reportInfo(doMaximum, "(S) major frame " + lc.getSamplingPeriod() + "ms");
						} else {
							// we have a partition offset.
							double cum = getCumLatency(lc, doMaximum);
							double myOffset = lc.getPartitionOffset();
							if (myOffset > -1) {
								// get the previous partition offset
								// it could be the lastSampled or the partition latency contributor right before that
								double prevOffset = getLastPartitionOffset(lc);
								if (prevOffset > -1) {
									// now we do the offset based roundup
									double prevPlus = prevOffset + cum;
									while ((myOffset - prevPlus) < 0) {
										myOffset = myOffset + lc.getSamplingPeriod();
									}
									double diff = myOffset - prevPlus;
									res = res + diff;
									lc.setActualValue(diff, doMaximum);
									lc.reportSubtotal(res, doMaximum);
								} else {
									// the previous one is not based on a schedule
									// this branch should not be reached since both partitions are on same processor
									// thus they have the same schedule
									double diff = FlowLatencyUtil.roundUpDiff(cum, lc.getSamplingPeriod());
									res = res + diff;
									lc.setActualValue(diff, doMaximum);
									lc.reportSubtotal(res, doMaximum);
									lc.reportInfo(doMaximum, "(S) major frame " + lc.getSamplingPeriod() + "ms");
								}
							} else {
								// the current does not have an offset despite being marked as PARTITION_SCHEDULE
								// this branch should not be reached
								double diff = FlowLatencyUtil.roundUpDiff(cum, lc.getSamplingPeriod());
								res = res + diff;
								lc.setActualValue(diff, doMaximum);
								lc.reportSubtotal(res, doMaximum);
								lc.reportWarning(doMaximum, "Partition schedule without partition offset");
							}
						}
						if (doSynchronous() && isPreviousConnectionSyncUnknown(lc)) {
							lc.reportInfoOnce(doMaximum, "Assume synchronous communication");
						} else if (isPreviousConnectionSynchronous(lc)) {
							lc.reportInfoOnce(doMaximum, "Synchronous communication on same platform");
						} else {
							lc.reportInfoOnce(doMaximum, "Assume synchronous communication");
						}
					} else {
						// add the period. Even for partition with offset we have the worst case of a period
						if (doMaximum) {
							res = res + lc.getSamplingPeriod();
							lc.setActualValue(lc.getSamplingPeriod(), doMaximum);
						}
						lc.reportSubtotal(res, doMaximum);
					}
				} else {
					lc.reportInfoOnce(doMaximum, "Initial " + lc.getSamplingPeriod() + "ms partition latency not added");

				}
				// remember the partition in all cases as a sampling unit
				lastSampled = lc;
			} else if (lc.isPartitionOutputDelay()) {
				// deal with partition I/O delay, then add communication latency
				// do it as major frame delay or as partition end delay depending on preference setting
				if (Values.doMajorFrameDelay()) {
					// round up to next major frame
					double diff = FlowLatencyUtil.roundUpDiff(getCumLatency(lc, doMaximum) + lc.getPartitionOffset(),
							lc.getSamplingPeriod());
					res = res + diff;
					lc.setActualValue(diff, doMaximum);
					lc.reportInfoOnce(doMaximum, "Output at " + lc.getSamplingPeriod() + "ms major frame");
				} else {
					// round up to window duration. Note the cumulative could be more than the window
					double diff = FlowLatencyUtil.roundUpDiff(getCumLatency(lc, doMaximum), lc.getSamplingPeriod(),
							lc.getPartitionDuration());
					res = res + diff;
					lc.setActualValue(diff, doMaximum);
					lc.reportInfoOnce(doMaximum, "Output at " + lc.getPartitionDuration() + "ms partition end");
				}
				lc.reportSubtotal(res, doMaximum);
			} else if (lc instanceof LatencyContributorConnection) {
				// check recursively for sampling protocol
				doSampledProtocol(lc, doMaximum, null);
				res = res + lc.getTotal(doMaximum);
				lc.reportSubtotal(res, doMaximum);
			} else {
				res = res + lc.getTotal(doMaximum);
				lc.reportSubtotal(res, doMaximum);
			}
		}

		return res;
	}

	public void doSampledProtocol(LatencyContributor lc, boolean doMaximum, LatencyContributor last) {
		double sp = lc.getSamplingPeriod();
		if (lc.getWorstcaseLatencyContributorMethod().equals(LatencyContributorMethod.SAMPLED_PROTOCOL) && sp > 0.0) {
			double lastsp = last == null ? 0.0 : last.getSamplingPeriod();
			if (last != null && sp > lastsp) {
				last.setActualValue(0.0, doMaximum);
				last.reportInfoOnce(doMaximum, "Sampling period of " + lastsp
						+ "ms accounted for in suceeding protocol");
				if (doSynchronous() && wasSampled()) {
					// there was a previous sampling component. We can to the roundup game.
					double diff = FlowLatencyUtil.roundUpDiff(getCumLatency(lc, doMaximum), sp);
					lc.setActualValue(diff, doMaximum);
					last = lc;
					lc.reportInfo(doMaximum, "Round up to sampling period " + lc.getSamplingPeriod() + "ms");
					if (doSynchronous() && isPreviousConnectionSyncUnknown(lc)) {
						lc.reportInfoOnce(doMaximum, "Assume synchronous communication");
					} else if (isPreviousConnectionSynchronous(lc)) {
						lc.reportInfoOnce(doMaximum, "Synchronous communication on same platform");
					} else {
						lc.reportInfoOnce(doMaximum, "Assume synchronous communication");
					}
				} else {
					if (doMaximum) {
						lc.setActualValue(sp, doMaximum);
						last = lc;
						lc.reportInfo("Best case 0 ms worst case " + lc.getSamplingPeriod()
								+ "ms (period) sampling delay");
					} else {
//				TODO: may want to enable				lc.reportInfo(doMaximum, "Best case: no sampling delay");
					}
				}
			} else {
				lc.reportInfoOnce(doMaximum, "Sampling contribution of " + sp
						+ "ms accounted for in enclosing protocol");
			}
		}
		List<LatencyContributor> sublc = lc.getSubContributors();
		for (LatencyContributor latencyContributor : sublc) {
			doSampledProtocol(latencyContributor, doMaximum, last);
		}
		return;
	}

	public double getMaximumSpecifiedLatency() {
		double res = 0.0;
		for (LatencyContributor lc : contributors) {
			res = res + lc.getTotalMaximumSpecified();
		}

		return res;
	}

	public double getMinimumSpecifiedLatency() {
		double res = 0.0;
		for (LatencyContributor lc : contributors) {
			res = res + lc.getTotalMinimumSpecified();
		}

		return res;
	}

	private void reportSummaryError(String str) {
		issues.add(new ReportedCell(ReportSeverity.ERROR, str));
	}

	private void reportSummarySuccess(String str) {
		issues.add(new ReportedCell(ReportSeverity.SUCCESS, str));
	}

	private void reportSummaryWarning(String str) {
		issues.add(new ReportedCell(ReportSeverity.WARNING, str));
	}

	public Section export() {

		Section section;
		Line line;
		double minValue;
		double maxValue;
		double minSpecifiedValue;
		double maxSpecifiedValue;
		double expectedMaxLatency;
		double expectedMinLatency;
		String sectionName;

		minValue = 0.0;
		maxValue = 0.0;
		minSpecifiedValue = 0.0;
		maxSpecifiedValue = 0.0;

		issues = new ArrayList<ReportedCell>();

		expectedMaxLatency = this.expectedMaxLatency;
		expectedMinLatency = this.expectedMinLatency;

		if (relatedEndToEndFlow != null) {
			sectionName = relatedEndToEndFlow.getComponentInstancePath();
		} else {
			sectionName = "Unnamed flow";
		}
		SystemInstance si = (SystemInstance) relatedEndToEndFlow.getElementRoot();
		String systemName = si.getComponentClassifier().getName();
		String inMode = Aadl2Util.isPrintableSOMName(som) ? " in mode " + som.getName() : "";

		section = new Section(sectionName + inMode);
		String dspostfix = Values.getDataSetProcessingLabel();
		line = new Line();
		line.addHeaderContent("Latency analysis for end-to-end flow '" + sectionName + "' of system '" + systemName
				+ "'" + inMode + " with latency preference stettings " + Values.getSynchronousSystemLabel() + "-"
				+ Values.getMajorFrameDelayLabel() + "-" + Values.getWorstCaseDeadlineLabel() + "-"
				+ Values.getBestcaseEmptyQueueLabel() + (dspostfix.isEmpty() ? "" : "-" + dspostfix));
		section.addLine(line);
		line = new Line();
		section.addLine(line);
		line = new Line();
		line.addHeaderContent("Contributor");
		line.addHeaderContent("Min Specified");
		line.addHeaderContent("Min Value");
		if (Values.doReportSubtotals()) {
			line.addHeaderContent("Min Subtotals");
		}
		line.addHeaderContent("Min Method");
		line.addHeaderContent("Max Specified");
		line.addHeaderContent("Max Value");
		if (Values.doReportSubtotals()) {
			line.addHeaderContent("Max Subtotals");
		}
		line.addHeaderContent("Max Method");
		line.addHeaderContent("Comments");
		section.addLine(line);

		// will populate the comments section
		minValue = getActualLatency(false);
		maxValue = getActualLatency(true);

		minSpecifiedValue = getMinimumSpecifiedLatency();
		maxSpecifiedValue = getMaximumSpecifiedLatency();

		// reporting each entry
		for (LatencyContributor lc : this.contributors) {
			for (Line l : lc.export()) {
				section.addLine(l);
			}
		}

		line = new Line();
		line.addContent("Latency Total");
		line.addContent(minSpecifiedValue + "ms");
		line.addContent(minValue + "ms");
		if (Values.doReportSubtotals()) {
			line.addHeaderContent("");
		}
		line.addContent("");
		line.addContent(maxSpecifiedValue + "ms");
		line.addContent(maxValue + "ms");
		if (Values.doReportSubtotals()) {
			line.addHeaderContent("");
		}
		line.addContent("");
		section.addLine(line);

		line = new Line();
		line.setSeverity(ReportSeverity.SUCCESS);

		line.addContent("End to End Latency");
		line.addContent("");
		line.addContent(expectedMinLatency + "ms");
		line.addContent("");
		line.addContent("");
		line.addContent(expectedMaxLatency + "ms");
		line.addContent("");

		/*
		 * In that case, the end to end flow has a minimum latency
		 */
		boolean didError = false;
		if (expectedMaxLatency > 0) {
			if (minSpecifiedValue > expectedMaxLatency) {
				reportSummaryError("Minimum specified latency total " + minSpecifiedValue
						+ " ms exceeds expected maximum end to end latency " + expectedMaxLatency + "ms");
				didError = true;
			}
			if (minValue > expectedMaxLatency) {
				reportSummaryError("Minimum actual latency total " + minValue
						+ " ms exceeds expected maximum end to end latency " + expectedMaxLatency + "ms");
				didError = true;
			}
			if (minSpecifiedValue < expectedMinLatency) {
				if (maxSpecifiedValue - minSpecifiedValue > expectedMaxLatency - expectedMinLatency) {
					reportSummaryError("Jitter of specified latency total "
							+ (maxSpecifiedValue - minSpecifiedValue)
							+ " ms exceeds expected end to end latency jitter "
							+ (expectedMaxLatency - expectedMinLatency)
							+ "ms with minimum specified latency total less then expected minimum (better response time)");
					didError = true;
				}
			}

			if (minValue < expectedMinLatency) {
				if (maxValue - minValue > expectedMaxLatency - expectedMinLatency) {
					reportSummaryError("Jitter of actual latency total " + (maxValue - minValue)
							+ " ms exceeds expected end to end latency jitter "
							+ (expectedMaxLatency - expectedMinLatency)
							+ "ms with minimum actual latency total less then expected minimum (better response time)");
					didError = true;
				}
			}
			if (!didError) {
				if (minValue > expectedMinLatency) {
					reportSummaryWarning("Minimum actual latency total " + minValue
							+ " ms is less than expected minimum end to end latency " + expectedMinLatency
							+ "ms (Higher minimum response time");
				}
			}

			if (expectedMaxLatency < maxSpecifiedValue) {
				reportSummaryError("Maximum specified latency total " + maxSpecifiedValue
						+ "ms exceeds expected end to end latency " + expectedMaxLatency + "ms");
			}

			if (expectedMaxLatency < maxValue) {
				reportSummaryError("Maximum actual latency " + maxValue + "ms exceeds expected end to end latency "
						+ expectedMaxLatency + "ms");
			}
			if (maxValue > 0) {
				if ((minValue >= expectedMinLatency) && (expectedMaxLatency >= maxValue)) {
					reportSummarySuccess("Actual end-to-end flow latency for " + this.relatedEndToEndFlow.getName()
							+ " is within specified end to end latency");
				}
				if ((minValue <= expectedMinLatency) && (expectedMaxLatency >= maxValue)
						&& ((expectedMaxLatency - expectedMinLatency) > (maxValue - minValue))) {
					reportSummarySuccess("Actual end-to-end flow latency jitter for "
							+ this.relatedEndToEndFlow.getName()
							+ " is within specified end to end latency jitter and minimum reposne time is better");
				}
			}
		} else {
			reportSummaryWarning("Expected end to end latency is not specified");
		}

		section.addLine(line);

		if (issues.size() > 0) {
			line = new Line();
			line.addHeaderContent("End to end Latency Summary");
			section.addLine(line);
			for (ReportedCell issue : issues) {
				line = new Line();
				String msg = issue.getMessage();
				ReportedCell issueLabel = new ReportedCell(issue.getSeverity(), issue.getSeverity().toString());
				line.addCell(issueLabel);
				line.addContent(msg);
				section.addLine(line);
			}
		}

		return section;
	}

	private String getRelatedObjectLabel() {
		return this.relatedEndToEndFlow.getComponentInstancePath() + ": ";
	}

	public void generateMarkers(AnalysisErrorReporterManager errManager) {
		List<ReportedCell> doIssues = this.issues;
		for (ReportedCell reportedCell : doIssues) {
			if (reportedCell.getSeverity() == ReportSeverity.INFO) {
//				errManager.info(this.relatedEndToEndFlow, reportedCell.getMessage());
			} else if (reportedCell.getSeverity() == ReportSeverity.SUCCESS) {
				errManager.info(this.relatedEndToEndFlow, getRelatedObjectLabel() + reportedCell.getMessage());
			} else if (reportedCell.getSeverity() == ReportSeverity.WARNING) {
				errManager.warning(this.relatedEndToEndFlow, getRelatedObjectLabel() + reportedCell.getMessage());
			} else if (reportedCell.getSeverity() == ReportSeverity.ERROR) {
				errManager.error(this.relatedEndToEndFlow, getRelatedObjectLabel() + reportedCell.getMessage());
			}
		}
		if (Values.doDetailsMarkers()) {
			for (LatencyContributor lc : this.contributors) {
				lc.generateMarkers(errManager);
			}
		}
	}
}
