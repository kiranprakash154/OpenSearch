/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.wlm;

import org.opensearch.cluster.ClusterChangedEvent;
import org.opensearch.cluster.ClusterState;
import org.opensearch.cluster.metadata.Metadata;
import org.opensearch.cluster.metadata.QueryGroup;
import org.opensearch.cluster.service.ClusterService;
import org.opensearch.common.unit.TimeValue;
import org.opensearch.test.OpenSearchTestCase;
import org.opensearch.threadpool.Scheduler;
import org.opensearch.threadpool.ThreadPool;
import org.opensearch.wlm.stats.QueryGroupState;
import org.opensearch.wlm.tracker.QueryGroupResourceUsageTrackerService;
import org.junit.Before;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class QueryGroupServiceTests extends OpenSearchTestCase {
    private QueryGroupService queryGroupService;
    private QueryGroupResourceUsageTrackerService mockQueryGroupUsageTracker;
    private ClusterService mockClusterService;
    private ThreadPool mockThreadPool;
    private WorkloadManagementSettings mockWorkloadManagementSettings;
    private Scheduler.Cancellable mockScheduledFuture;
    private Map<String, QueryGroupState> mockQueryGroupStateMap;

    @Before
    public void setup() {
        mockQueryGroupUsageTracker = mock(QueryGroupResourceUsageTrackerService.class);
        mockClusterService = mock(ClusterService.class);
        mockThreadPool = mock(ThreadPool.class);
        mockScheduledFuture = mock(Scheduler.Cancellable.class);
        mockWorkloadManagementSettings = mock(WorkloadManagementSettings.class);
        mockQueryGroupStateMap = new HashMap<>();

        queryGroupService = new QueryGroupService(
            mockQueryGroupUsageTracker,
            mockClusterService,
            mockThreadPool,
            mockWorkloadManagementSettings,
            mockQueryGroupStateMap
        );
    }

    public void testApplyClusterState() {
        ClusterChangedEvent mockClusterChangedEvent = mock(ClusterChangedEvent.class);
        ClusterState mockPreviousClusterState = mock(ClusterState.class);
        ClusterState mockClusterState = mock(ClusterState.class);
        Metadata mockPreviousMetadata = mock(Metadata.class);
        Metadata mockMetadata = mock(Metadata.class);
        QueryGroup addedQueryGroup = new QueryGroup(
            "addedQueryGroup",
            "4242",
            QueryGroup.ResiliencyMode.ENFORCED,
            Map.of(ResourceType.MEMORY, 0.5),
            1L
        );
        QueryGroup deletedQueryGroup = new QueryGroup(
            "deletedQueryGroup",
            "4241",
            QueryGroup.ResiliencyMode.ENFORCED,
            Map.of(ResourceType.MEMORY, 0.5),
            1L
        );
        Map<String, QueryGroup> previousQueryGroups = new HashMap<>();
        previousQueryGroups.put("4242", addedQueryGroup);
        Map<String, QueryGroup> currentQueryGroups = new HashMap<>();
        currentQueryGroups.put("4241", deletedQueryGroup);

        when(mockClusterChangedEvent.previousState()).thenReturn(mockPreviousClusterState);
        when(mockClusterChangedEvent.state()).thenReturn(mockClusterState);
        when(mockPreviousClusterState.metadata()).thenReturn(mockPreviousMetadata);
        when(mockClusterState.metadata()).thenReturn(mockMetadata);
        when(mockPreviousMetadata.queryGroups()).thenReturn(previousQueryGroups);
        when(mockMetadata.queryGroups()).thenReturn(currentQueryGroups);
        queryGroupService.applyClusterState(mockClusterChangedEvent);

        Set<QueryGroup> currentQueryGroupsExpected = Set.of(currentQueryGroups.get("4241"));
        Set<QueryGroup> previousQueryGroupsExpected = Set.of(previousQueryGroups.get("4242"));

        assertEquals(currentQueryGroupsExpected, queryGroupService.getActiveQueryGroups());
        assertEquals(previousQueryGroupsExpected, queryGroupService.getDeletedQueryGroups());
    }

  public void testDoStart_SchedulesTask() {
    when(mockWorkloadManagementSettings.queryGroupServiceEnabled()).thenReturn(true);
    when(mockWorkloadManagementSettings.getQueryGroupServiceRunInterval()).thenReturn(TimeValue.timeValueSeconds(1));
    queryGroupService.doStart();
    verify(mockThreadPool).scheduleWithFixedDelay(any(Runnable.class), any(TimeValue.class), eq(ThreadPool.Names.GENERIC));
  }

  public void testDoStart_DoesNOTScheduleTask_WhenQueryGroupServiceDisabled() {
    when(mockWorkloadManagementSettings.queryGroupServiceEnabled()).thenReturn(false);
    when(mockWorkloadManagementSettings.getQueryGroupServiceRunInterval()).thenReturn(TimeValue.timeValueSeconds(1));
    queryGroupService.doStart();
    verify(mockThreadPool, never()).scheduleWithFixedDelay(any(Runnable.class), any(TimeValue.class), eq(ThreadPool.Names.GENERIC));
  }

  public void testDoStop_CancelsScheduledTask() {
    when(mockWorkloadManagementSettings.queryGroupServiceEnabled()).thenReturn(true);
    when(mockThreadPool.scheduleWithFixedDelay(any(), any(), any())).thenReturn(mockScheduledFuture);
    queryGroupService.doStart();
    when(mockWorkloadManagementSettings.queryGroupServiceEnabled()).thenReturn(false);
    queryGroupService.doStop();
    verify(mockScheduledFuture).cancel();
  }

  public void testDoStop_DoesNOTCancelsScheduledTask_WhenQueryGroupServiceDisabled() {
    when(mockWorkloadManagementSettings.queryGroupServiceEnabled()).thenReturn(true);
    when(mockThreadPool.scheduleWithFixedDelay(any(), any(), any())).thenReturn(mockScheduledFuture);
    queryGroupService.doStart();
    when(mockWorkloadManagementSettings.queryGroupServiceEnabled()).thenReturn(true);
    queryGroupService.doStop();
    verify(mockScheduledFuture, never()).cancel();
  }
}
