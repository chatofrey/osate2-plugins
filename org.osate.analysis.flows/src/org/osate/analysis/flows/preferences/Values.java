package org.osate.analysis.flows.preferences;

import org.eclipse.jface.preference.IPreferenceStore;
import org.osate.analysis.flows.FlowanalysisPlugin;

public class Values {

	public static boolean doSynchronousSystem() {
		IPreferenceStore store = FlowanalysisPlugin.getDefault().getPreferenceStore();
		String policy = store.getString(Constants.SYNCHRONOUS_SYSTEM);
		return policy.equalsIgnoreCase(Constants.SYNCHRONOUS_SYSTEM_YES);
	}

	public static boolean doMajorFrameDelay() {
		IPreferenceStore store = FlowanalysisPlugin.getDefault().getPreferenceStore();
		String policy = store.getString(Constants.PARTITONING_POLICY);
		return policy.equalsIgnoreCase(Constants.PARTITIONING_POLICY_MAJOR_FRAME_DELAYED_STR);
	}

	public static boolean doReportSubtotals() {
		IPreferenceStore store = FlowanalysisPlugin.getDefault().getPreferenceStore();
		String policy = store.getString(Constants.REPORT_SUBTOTALS);
		return policy.equalsIgnoreCase(Constants.REPORT_SUBTOTALS_YES);
	}

	public static boolean doWorstCaseDeadline() {
		IPreferenceStore store = FlowanalysisPlugin.getDefault().getPreferenceStore();
		String policy = store.getString(Constants.WORST_CASE_DEADLINE);
		return policy.equalsIgnoreCase(Constants.WORST_CASE_DEADLINE_YES);
	}

	public static boolean doDetailsMarkers() {
		IPreferenceStore store = FlowanalysisPlugin.getDefault().getPreferenceStore();
		String policy = store.getString(Constants.DETAILS_MARKERS);
		return policy.equalsIgnoreCase(Constants.DETAILS_MARKERS_YES);
	}

	public static boolean doDataSetProcessing() {
		IPreferenceStore store = FlowanalysisPlugin.getDefault().getPreferenceStore();
		String policy = store.getString(Constants.DATASET_PROCESSING);
		return policy.equalsIgnoreCase(Constants.DATASET_PROCESSING_YES);
	}

	public static boolean doBestcaseEmptyQueue() {
		IPreferenceStore store = FlowanalysisPlugin.getDefault().getPreferenceStore();
		String policy = store.getString(Constants.BESTCASE_EMPTY_QUEUE);
		return policy.equalsIgnoreCase(Constants.BESTCASE_EMPTY_QUEUE_YES);
	}

	public static String getMajorFrameDelayLabel() {
		return Values.doMajorFrameDelay() ? "MF" : "PE";
	}

	public static String getMajorFrameDelayName() {
		IPreferenceStore store = FlowanalysisPlugin.getDefault().getPreferenceStore();
		return store.getString(Constants.PARTITONING_POLICY);
	}

	public static String getSynchronousSystemLabel() {
		return Values.doSynchronousSystem() ? "SS" : "AS";
	}

	public static String getSynchronousSystemName() {
		IPreferenceStore store = FlowanalysisPlugin.getDefault().getPreferenceStore();
		return store.getString(Constants.SYNCHRONOUS_SYSTEM);
	}

	public static String getWorstCaseDeadlineLabel() {
		return Values.doSynchronousSystem() ? "DL" : "ET";
	}

	public static String getWorstCaseDeadlineName() {
		IPreferenceStore store = FlowanalysisPlugin.getDefault().getPreferenceStore();
		return store.getString(Constants.WORST_CASE_DEADLINE);
	}

	public static String getBestcaseEmptyQueueLabel() {
		return Values.doBestcaseEmptyQueue() ? "EQ" : "FQ";
	}

	public static String getBestcaseEmptyQueueName() {
		IPreferenceStore store = FlowanalysisPlugin.getDefault().getPreferenceStore();
		return store.getString(Constants.BESTCASE_EMPTY_QUEUE);
	}

	public static String getDataSetProcessingLabel() {
		return Values.doDataSetProcessing() ? "DS" : "";
	}

	public static String getDataSetProcessingName() {
		IPreferenceStore store = FlowanalysisPlugin.getDefault().getPreferenceStore();
		return store.getString(Constants.DATASET_PROCESSING);
	}

}
