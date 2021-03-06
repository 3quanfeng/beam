/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.beam.runners.core.metrics;

import java.util.concurrent.atomic.AtomicReference;
import org.apache.beam.sdk.annotations.Experimental;
import org.apache.beam.sdk.annotations.Experimental.Kind;
import org.apache.beam.sdk.metrics.Distribution;
import org.apache.beam.sdk.metrics.MetricName;

/**
 * Tracks the current value (and delta) for a Distribution metric.
 *
 * <p>This class generally shouldn't be used directly. The only exception is within a runner where
 * a distribution is being reported for a specific step (rather than the distribution in the current
 * context). In that case retrieving the underlying cell and reporting directly to it avoids a step
 * of indirection.
 */
@Experimental(Kind.METRICS)
public class DistributionCell implements Distribution, MetricCell<DistributionData> {

  private final DirtyState dirty = new DirtyState();
  private final AtomicReference<DistributionData> value =
      new AtomicReference<>(DistributionData.EMPTY);
  private final MetricName name;

  /**
   * Package-visibility because all {@link DistributionCell DistributionCells} should be created by
   * {@link MetricsContainerImpl#getDistribution(MetricName)}.
   */
  DistributionCell(MetricName name) {
    this.name = name;
  }

  /** Increment the distribution by the given amount. */
  @Override
  public void update(long n) {
    update(DistributionData.singleton(n));
  }

  void update(DistributionData data) {
    DistributionData original;
    do {
      original = value.get();
    } while (!value.compareAndSet(original, original.combine(data)));
    dirty.afterModification();
  }

  @Override
  public DirtyState getDirty() {
    return dirty;
  }

  @Override
  public DistributionData getCumulative() {
    return value.get();
  }

  @Override
  public MetricName getName() {
    return name;
  }
}

