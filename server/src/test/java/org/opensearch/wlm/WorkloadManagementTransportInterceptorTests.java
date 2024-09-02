/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.wlm;

import org.opensearch.cluster.service.ClusterService;
import org.opensearch.test.OpenSearchTestCase;
import org.opensearch.threadpool.Scheduler;
import org.opensearch.threadpool.TestThreadPool;
import org.opensearch.threadpool.ThreadPool;
import org.opensearch.transport.TransportRequest;
import org.opensearch.transport.TransportRequestHandler;
import org.opensearch.wlm.WorkloadManagementTransportInterceptor.RequestHandler;
import org.opensearch.wlm.stats.QueryGroupState;
import org.opensearch.wlm.tracker.QueryGroupResourceUsageTrackerService;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.opensearch.threadpool.ThreadPool.Names.SAME;

public class WorkloadManagementTransportInterceptorTests extends OpenSearchTestCase {

    private ThreadPool threadPool;
    private WorkloadManagementTransportInterceptor sut;
    private QueryGroupService queryGroupService;
    private QueryGroupResourceUsageTrackerService mockQueryGroupUsageTracker;
    private ClusterService mockClusterService;
    private ThreadPool mockThreadPool;
    private WorkloadManagementSettings mockWorkloadManagementSettings;
    private Scheduler.Cancellable mockScheduledFuture;
    private Map<String, QueryGroupState> mockQueryGroupStateMap;

    public void setUp() throws Exception {
        super.setUp();
        mockQueryGroupUsageTracker = mock(QueryGroupResourceUsageTrackerService.class);
        mockClusterService = mock(ClusterService.class);
        mockThreadPool = mock(ThreadPool.class);
        mockScheduledFuture = mock(Scheduler.Cancellable.class);
        mockWorkloadManagementSettings = mock(WorkloadManagementSettings.class);
        mockQueryGroupStateMap = new HashMap<>();
        threadPool = new TestThreadPool(getTestName());
        sut = new WorkloadManagementTransportInterceptor(threadPool,
            new QueryGroupService(
                mockQueryGroupUsageTracker,
                mockClusterService,
                mockThreadPool,
                mockWorkloadManagementSettings,
                new HashMap<>()
            )
        );
    }

    public void tearDown() throws Exception {
        super.tearDown();
        threadPool.shutdown();
    }

    public void testInterceptHandler() {
        TransportRequestHandler<TransportRequest> requestHandler = sut.interceptHandler("Search", SAME, false, null);
        assertTrue(requestHandler instanceof RequestHandler);
    }
}
